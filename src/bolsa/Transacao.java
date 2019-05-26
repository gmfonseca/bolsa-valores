package bolsa;

import util.Main;
import util.OrdemCompra;
import util.OrdemVenda;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Transacao {

    private OrdemCompra compra;
    private OrdemVenda venda;

    private int qntd;
    private double valor;
    private String acao;
    private String data;

    public Transacao(OrdemCompra compra, OrdemVenda venda) {
        this.compra = compra;
        this.venda = venda;

        this.qntd = venda.getQntd();
        this.acao = venda.getAcao();
        this.valor = venda.getValor();

        Date d = new Date();
        DateFormat df = new SimpleDateFormat(Main.DATE_FORMAT);
        this.data = df.format(d);
    }

    public Transacao(String acao, int qntd, double valor, String data, String compradora, String vendedora) {
        this.qntd = qntd;
        this.valor = valor;
        this.acao = acao;
        this.data = data;
        this.compra = new OrdemCompra(acao, qntd, valor, compradora, data);
        this.venda = new OrdemVenda(acao, qntd, valor, vendedora, data);
    }

    public void print(){
        StringBuilder printText = new StringBuilder("\n[ TRANSACAO ]");
        printText.append(" \n[+] Acao: '");
        printText.append(acao);
        printText.append("'\n[+] Valor: '");
        printText.append(valor);
        printText.append("'\n[+] Quantidade: '");
        printText.append(qntd);
        printText.append("'\n[+] Vendedora: '");
        printText.append(venda.getCorretora());
        printText.append("'\n[+] Compradora: '");
        printText.append(compra.getCorretora());
        printText.append("'\n[+] Data: '");
        printText.append(data);
        printText.append("'\n[ - + - ]\n\n");

        System.out.println(printText.toString());
    }

    @Override
    public String toString() {

        return "transacao;" + acao + ";" + qntd + ";" + valor + ";" + compra.getCorretora() + ";" + venda.getCorretora() + ";" + data;
    }
}
