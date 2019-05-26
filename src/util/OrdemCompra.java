package util;

public class OrdemCompra extends Ordem {

    public OrdemCompra(String acao, int qntd, double valor, String corretora, String data) {
        super(qntd, valor, acao, "compra", corretora, data);
    }

    public OrdemCompra(String acao, int qntd, double valor, String corretora) {
        super(qntd, valor, acao, "compra", corretora);
    }
}
