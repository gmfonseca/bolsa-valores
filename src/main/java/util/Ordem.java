package util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Ordem {

    private int qntd;
    private double valor;
    private String acao;
    private String operacao;
    private String data;
    private String corretora;

    public Ordem(int qntd, double valor, String acao, String operacao, String corretora, String data) {
        this.qntd = qntd;
        this.valor = valor;
        this.acao = acao.toLowerCase();
        this.operacao = operacao.toLowerCase();
        this.corretora = corretora;
        this.data = data;
    }

    public Ordem(int qntd, double valor, String acao, String operacao, String corretora) {
        this(qntd, valor, acao, operacao, corretora, "");

        Date d = new Date();
        DateFormat df = new SimpleDateFormat(Main.DATE_FORMAT);
        data = df.format(d);
    }

    public int getQntd() {
        return qntd;
    }

    public double getValor() {
        return valor;
    }

    public String getAcao() {
        return acao;
    }

    public String getOperacao() {
        return operacao;
    }

    public String getData() {
        return data;
    }

    public String getCorretora() {
        return corretora;
    }

    public void print(){
        StringBuilder printText = new StringBuilder("\n[ ORDEM DE ");
        printText.append(operacao.toUpperCase());
        printText.append(" ]\n[+] Acao: '");
        printText.append(acao);
        printText.append("'\n[+] Valor: '");
        printText.append(valor);
        printText.append("'\n[+] Corretora: '");
        printText.append(corretora);
        printText.append("'\n[+] Quantidade: '");
        printText.append(qntd);
        printText.append("'\n[+] Data: '");
        printText.append(data);
        printText.append("'\n[ - + - ]\n\n");

        System.out.println(printText.toString());

    }

    @Override
    public String toString() {
        return operacao + ";" + acao + ";" + qntd + ";" + valor + ";" + corretora;
    }
}
