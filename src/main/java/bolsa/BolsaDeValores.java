package bolsa;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import util.Main;
import util.Ordem;
import util.OrdemCompra;
import util.OrdemVenda;
import java.util.ArrayList;
import java.util.List;

public class BolsaDeValores {

    private static final List<Ordem> ordens_venda = new ArrayList<>();
    private static final List<Ordem> ordens_compra = new ArrayList<>();
    public static final String BOOK_OFFERS = "BOLSADEVALORES";
    public static final String INFO_QUEUE = "INFOQUEUE";
    public static final List<Transacao> transacoes = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        Main.BOLSA_DE_VALORES.openChannel();
    }

    /**
     *
     * Metodo para abrir o servidor principal da Bolsa de Valores
     * que recebe dados pela fila 'BROKER' e trata todos os tipos de
     * Ordens: {compra.acao}, {venda.acao}, {info.date-time}, e guarda
     * em listas as respectivas ordens de compra e venda que chegam
     * até que as mesmas formem uma transacao
     *
     */
    public void openChannel() throws Exception{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(Main.SERVER_URI);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(Main.QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] BOLSA DE VALORES INICIADA");
        System.out.println(" [*--] Aguardando operações. Para sair, pressione CTRL+C\n");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8").replaceAll(",", ".").toUpperCase();

            if(message.contains("COMPRA") || message.contains("VENDA")) {

                // operacao ; acao ; qtd ; preco ; corretora
                String[] dados = message.split(";");

                if (dados.length == 5) {
                    String operacao = dados[0],
                            acao = dados[1],
                            corretora = dados[4];
                    int qtd = Integer.parseInt(dados[2]);
                    double valor = Double.parseDouble(dados[3]);

                    //criar ordem de compra/venda
                    synchronized (this) {
                        Ordem ordem;
                        if (operacao.equalsIgnoreCase("compra")) {
                            ordem = new OrdemCompra(acao, qtd, valor, corretora);
                            ordens_compra.add(ordem);

                            ordens_compra.sort((o1, o2) -> (int) (o1.getValor() - o2.getValor()));
                        } else {
                            ordem = new OrdemVenda(acao, qtd, valor, corretora);
                            ordens_venda.add(ordem);

                            ordens_venda.sort((o1, o2) -> (int) (o1.getValor() - o2.getValor()));
                        }
                        try {
                            notificarClientes(ordem);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        ordem.print();
                        try {
                            Transacao t = testarTransacao(ordem);
                            if(t != null){
                                notificarTransacao(t);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                } else{
                    System.out.println("\nERROR: Dados insuficientes para a escolha da acao\n\n");
                }
            }else if(message.contains("INFO")){
                String data = message.split(";")[1];
                List<Ordem> ordens = getOrdensInfo(data);
                try {
                    if(ordens.size()>0) {
                        System.out.println("MOSTRANDO INFO NA DATA '" + data + "'\n\n");
                        StringBuilder infoMessage = new StringBuilder();
                        for (Ordem ordem : ordens) {
                            infoMessage.append(ordem.toString() + ";" + data);

                            if(ordens.indexOf(ordem) != ordens.size()-1)
                                infoMessage.append("$");
                        }
                        sendInfo(infoMessage.toString());
                        System.out.println("FIM\n\n");
                    }else{
                        System.out.println("\nERROR: Não foram encontradas ORDENS postados em '" + data + "'\n\n");
                        sendInfo("ERROR");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        channel.basicConsume(Main.QUEUE_NAME, true, deliverCallback, consumerTag->{});

    }

    /**
     * Metodo para recuperar todas as Ordens guardadas pela Bolsa
     * que foram postados na data e horario que se deseja as informações
     *
     * @param data data e hora que se deseja recuperar as informações
     */
    private List<Ordem> getOrdensInfo(String data){
        List<Ordem> ordens = new ArrayList<>();

        ordens_compra.forEach(compra -> {
            if(compra.getData().equalsIgnoreCase(data)) {
                ordens.add(compra);
            }
        });

        ordens_venda.forEach(venda -> {
            if(venda.getData().equalsIgnoreCase(data)) {
                ordens.add(venda);
            }
        });

        return ordens;
    }

    /**
     * Metodo que estabelece a conexao com o servidor do RabbitMQ para
     * enviar ao Broker que solicitou uma consulta a uma data especifica
     * atraves da fila 'INFOQUEUE'
     *
     * @param message mensagem que guarda todas as informacoes encontradas na data especifica
     **/
    private void sendInfo(String message) throws Exception{
        ConnectionFactory factory = new ConnectionFactory();

        factory.setUri(Main.SERVER_URI);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(INFO_QUEUE, false, false, false, null);
        channel.basicPublish("", INFO_QUEUE, null, message.getBytes("UTF-8"));
        System.out.println(" [x] Enviado '" + message + "'");

        channel.close();
        connection.close();
    }

    /**
     * Metodo que notifica todos os clientes que sao inscritas na acao que
     * pertence a Ordem passada por parametro
     *
     * @param ordem Ordem que foi realizada para uma acao especifica
     *
     */
    private void notificarClientes(Ordem ordem) throws Exception{
        if(ordem != null) {
            String key = ordem.getOperacao().toLowerCase() + "." + ordem.getAcao().toLowerCase();

            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(Main.SERVER_URI);

            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {

                channel.exchangeDeclare(Main.NOTIFICATION_QUEUE, "topic");

                channel.basicPublish(Main.NOTIFICATION_QUEUE, key, null, (ordem.toString() + ";" + ordem.getData()).getBytes("UTF-8"));
                System.out.println("[x] Enviado '" + ordem.toString() + ";" + ordem.getData() + "' para - '" + key + "':'" );
            }
        }
    }

    /**
     * Metodo para testar se uma ordem atende aos
     * valores e quantidades de uma ordem 'inversa' da mesma acao
     * para registrar a transacao e retirar as ordens da bolsa
     *
     * @param ordem ordem usada para verificar se pode gerar uma transacao
     *
     */
    private synchronized Transacao testarTransacao(Ordem ordem) throws Exception{
        Transacao transacao;

        if(ordem instanceof OrdemCompra){
            transacao = testar(ordens_venda, ordem);
        }else { // ORDEM VENDA
            transacao = testar(ordens_compra, ordem);
        }

        return transacao;
    }

    /**
     * Metodo auxiliar para testar se existe um par de ordens
     * que podem concretizar uma transacao
     *
     * @param ordem  ordem que efetuara o teste de par
     * @param ordens lista de ordens de tipo oposto a ordem em questao
     **/
    private Transacao testar(List<Ordem> ordens, Ordem ordem){
        Transacao transacao = null;
        boolean efetuada = false;

        for (Ordem o : ordens) {
            if(o.getAcao().equalsIgnoreCase(ordem.getAcao()) && (ordem.getQntd() == o.getQntd())){
                if(o instanceof OrdemCompra) {
                    if (ordem.getValor() <= o.getValor()) {
                        transacao = registrarTransacao((OrdemCompra) o, (OrdemVenda) ordem);

                        efetuada = true;
                        break;
                    }
                }else{
                    if (o.getValor() <= ordem.getValor()) {
                        transacao = registrarTransacao((OrdemCompra) ordem, (OrdemVenda) o);

                        efetuada = true;
                        break;
                    }
                }
            }
        }

        if(efetuada) {
            ordens_venda.sort((v1, v2) -> (int) (v1.getValor() - v2.getValor()));
            ordens_compra.sort((c1, c2) -> (int) (c1.getValor() - c2.getValor()));
        }

        return transacao;
    }

    /**
     * Metodo auxiliar para registrar que a transacao foi efetuada
     * e remove as ordens de compra e venda da bolsa, para evitar
     * conflitos futuros
     *
     * @param compra Ordem de compra que efetivou uma transacao
     * @param venda Ordem de venda que efetivou uma transacao
     */
    private Transacao registrarTransacao(OrdemCompra compra, OrdemVenda venda){
        ordens_venda.remove(venda);
        ordens_compra.remove(compra);

        Transacao transacao = new Transacao(compra, venda);
        transacoes.add(transacao);

        return transacao;
    }

    /**
     * Metodo que notifica todos os usuarios sobre a efetivação
     * de uma transacao atraves do canal 'BOLSADEVALORES'
     *
     * @param t transacao efetivada
     */
    private void notificarTransacao(Transacao t)throws Exception{

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(Main.SERVER_URI);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(BOOK_OFFERS, "fanout");

            String message = t.toString();

            channel.basicPublish(BOOK_OFFERS, "", null, message.getBytes("UTF-8"));
            System.out.println(" [x] Enviado '" + message + "'");
        }

    }
}
