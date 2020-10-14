package util;

public class OrdemVenda extends Ordem {

    public OrdemVenda(String acao, int qntd, double valor, String corretora, String data) {
        super(qntd, valor, acao, "venda", corretora, data);
    }

    public OrdemVenda(String acao, int qntd, double valor, String corretora) {
        super(qntd, valor, acao, "venda", corretora);
    }
}