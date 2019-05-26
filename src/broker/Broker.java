package broker;

import bolsa.BolsaDeValores;
import bolsa.Transacao;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import util.Main;
import util.Ordem;
import util.OrdemCompra;
import util.OrdemVenda;

import java.util.Scanner;
import java.util.regex.Pattern;

public class Broker {
    private String nome;

    public static void main(String[] args) throws Exception{
        byte op;
        Scanner sc = new Scanner(System.in);

        System.out.println("+ - +");
        System.out.println("+ Digite o nome da sua Corretora");
        System.out.println("+ - +");
        System.out.println("");
        System.out.print("-> ");

        Broker corretora = new Broker(sc.nextLine());

        String acao;
        int qtd;
        float valor;
        try {
            do {
                menu();
                op = sc.nextByte();
                sc.nextLine();
                switch (op) {
                    case 1: // COMPRAR
                        System.out.println("\n+ - +");
                        System.out.println("+ COMPRAR");
                        System.out.println("+ - +");
                        System.out.print("Digite o nome da Ação: ");
                        acao = sc.nextLine();
                        System.out.print("\nDigite a quantidade de ações que deseja comprar: ");
                        qtd = sc.nextInt();
                        System.out.print("\nDigite o valor das ações que deseja comprar: ");
                        valor = sc.nextFloat();

                        corretora.comprar(acao, qtd, valor);
                        break;
                    case 2: // VENDER
                        System.out.println("\n+ - +");
                        System.out.println("+ VENDER");
                        System.out.println("+ - +");
                        System.out.print("Digite o nome da Ação: ");
                        acao = sc.nextLine();
                        System.out.print("\nDigite a quantidade de ações que deseja vender: ");
                        qtd = sc.nextInt();
                        System.out.print("\nDigite o valor das ações que deseja vender: ");
                        valor = sc.nextFloat();

                        corretora.vender(acao, qtd, valor);
                        break;
                    case 3: // CONSULTAR
                        System.out.println("\n+ - +");
                        System.out.println("+ CONSULTAR");
                        System.out.println("+ - +");
                        System.out.print("Digite a data que deseja consultar(dd/MM/yyyy hh:mm): ");
                        String data = sc.nextLine();

                        corretora.consultar(data);
                        break;
                    case 4: // INSCREVER
                        System.out.println("\n+ - +");
                        System.out.println("+ INSCREVER");
                        System.out.println("+ - +");
                        System.out.print("Digite o nome da Ação que deseja inscrever: ");
                        acao = sc.nextLine();

                        corretora.inscrever(acao);
                        break;
                    case 0: // SAIR
                        break;
                    default:
                }

            } while (op != 0);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static void menu(){
        System.out.println("+ - +");
        System.out.println("+ MENU");
        System.out.println("+ - +");
        System.out.println("+");
        System.out.println("+ 1. Comprar ação");
        System.out.println("+ 2. Vender ação");
        System.out.println("+ 3. Consultar data");
        System.out.println("+ 4. Inscrever em ação");
        System.out.println("+");
        System.out.println("+ 0. Sair");
        System.out.println("+");
        System.out.println("+ - +");
        System.out.println("");
        System.out.print("-> ");
    }

    public Broker(String nome) throws Exception {
        listenBookOffers();
        this.nome = nome;
    }

    /**
     * Metodo que estabelece conexao com o servidor do RabbitMQ
     * e aguarda por atualizacoes no canal de troca 'BOLSADEVALORES'
     * atraves de uma conexao pub/sub
     */
    private void listenBookOffers() throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(Main.SERVER_URI);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(BolsaDeValores.BOOK_OFFERS, "fanout");
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, BolsaDeValores.BOOK_OFFERS, "");

        System.out.println(" [*] Aguardando transações no livro de ofertas. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            //transacao ; acao ; qntd ; valor ; compra.corretora ; venda.corretora ; data
            String[] dados = message.split(";");
            Transacao transacao = buildTransacao(dados);

            if(dados.length == 7) {
                transacao.print();
            }else{
                System.out.println("\n\nERROR: Dados de transacao INCOMPLETOS\n\n");
            }
        };

        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }

    /**
     * Metodo auxiliar para enviar as devidas Ordens para a fila
     * 'BROKER', na qual a Bolsa de Valores efetivará os pedidos
     *
     * @param msg ordem comprimida em mensagem para ser redirecionada ao servidor
     */
    private void send(String msg) throws Exception{

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(Main.SERVER_URI);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(Main.QUEUE_NAME, false, false, false, null);

        channel.basicPublish("", Main.QUEUE_NAME, null, msg.getBytes("UTF-8"));
        System.out.println("\n+ - +");
        System.out.println("+ Ordem Enviada: '" + msg + "'");
        System.out.println("+ - +\n");

        channel.close();
        connection.close();
    }

    /**
     * Metodo para criar uma ordem de compra de uma acao especifica
     *
     * @param acao nome da acao que se deseja vender
     * @param qtd numero de acoes que se deseja vender
     * @param valor valor da acao que se deseja vender
     */
    public void comprar(String acao, int qtd, float valor) throws Exception {
        Ordem order = new OrdemCompra(acao, qtd, valor, nome);
        send(order.toString());
    }

    /**
     * Metodo para criar uma ordem de venda de uma acao especifica
     *
     * @param acao nome da acao que se deseja vender
     * @param qtd numero de acoes que se deseja vender
     * @param valor valor da acao que se deseja vender
     */
    public void vender(String acao, int qtd, float valor) throws Exception {
        send(new OrdemVenda(acao, qtd, valor, nome).toString());
    }

    /**
     * Metodo para solicitar operações efetivadas em uma data e hora
     * específica, solicitando pela fila 'INFOQUEUE'
     *
     * @param dateTime data que se deseja consultar
     */
    public void consultar(String dateTime) throws Exception {
        if(dateTime.length() != 16) throw new Exception("ERROR: Informe uma data válida.");

        send("info;"+dateTime);

        //criando conexão
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(Main.SERVER_URI);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(BolsaDeValores.INFO_QUEUE, false, false, false, null);

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            String[] ordens = message.split(Pattern.quote("$"));

            System.out.println("+ - +");
            System.out.println("+ ORDENS NA DATA '" + dateTime + "'");
            System.out.println("+ - +\n");

            if(ordens.length>0) {
                if(ordens[0].equalsIgnoreCase("error")){
                    System.out.println("+ - +");
                    System.out.println("+ NÃO FORAM ENCONTRADAS INFORMAÇÕES");
                    System.out.println("+ NA DATA '" + dateTime + "'");
                    System.out.println("+ - +\n");
                }else {
                    for (String ordem : ordens) {
                        buildOrdem(ordem.split(";")).print();
                    }
                }
            }

            System.out.println("+ - +");
            System.out.println("+ FIM");
            System.out.println("+ - +\n");
        };

        channel.basicConsume(BolsaDeValores.INFO_QUEUE, true, deliverCallback, consumerTag->{});
    }

    /**
     * Metodo que cria uma instancia para cada acao que o usuario
     * queira receber atualizacoes, atraves da fila 'BOLSA'
     * utilizando o metodo de Topicos
     *
     */
    public void inscrever(String acao) throws Exception{
        String bindingKey = "*." + acao;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(Main.SERVER_URI);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(Main.NOTIFICATION_QUEUE, "topic");
        String queueName = channel.queueDeclare().getQueue();

        channel.queueBind(queueName, Main.NOTIFICATION_QUEUE, bindingKey);

        System.out.println(" [*] Acompanhando a acao '" + acao + "'.");
        System.out.println(" [*--] Aguardando Operacoes. Para sair, pressione CTRL+C\n");

        DeliverCallback cb = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            Ordem ordem = buildOrdem(message.split(";"));
            System.out.println("NOVA OPERAÇÃO REGISTRADA: '" + delivery.getEnvelope().getRoutingKey() + "'");
            ordem.print();
        };
        channel.basicConsume(queueName, true, cb, consumerTag -> {});

    }

    /**
     * Metodo auxiliar para organizar uma Transacao
     * que esta em forma de Array
     *
     * @param dados dados que compoem uma transacao
     */
    private Transacao buildTransacao(String[] dados){
        Transacao transacao = null;

        if(dados[0].equalsIgnoreCase("transacao")) {
            String acao = dados[1],
                    compradora = dados[4],
                    vendedora = dados[5],
                    data = dados[6];
            int qtd = Integer.parseInt(dados[2]);
            double valor = Double.parseDouble(dados[3]);
            transacao = new Transacao(acao, qtd, valor, data, compradora, vendedora);
        }else{
            System.out.println("\n[TRANSACAO] ERROR AO NOTIFICAR: Algum dados nao pode ser processado\n");
        }

        return transacao;
    }

    /**
     * Metodo auxiliar para organizar uma Ordem
     * que esta em forma de Array
     *
     * @param dados dados que compoem uma ordem
     */
    private Ordem buildOrdem(String[] dados){
        Ordem ordem = null;

        if(dados[0].equalsIgnoreCase("compra") || dados[0].equalsIgnoreCase("venda")) {

            String operacao = dados[0],
                    acao = dados[1],
                    corretora = dados[4],
                    data = dados[5];
            int qtd = Integer.parseInt(dados[2]);
            double valor = Double.parseDouble(dados[3]);

            if (operacao.equalsIgnoreCase("compra"))
                ordem = new OrdemCompra(acao, qtd, valor, corretora, data);
            else
                ordem = new OrdemVenda(acao, qtd, valor, corretora, data);
        }else{
            System.out.println("\n[ORDEM] ERROR AO NOTIFICAR: Algum dado nao pode ser processado\n");
        }

        return ordem;
    }
}