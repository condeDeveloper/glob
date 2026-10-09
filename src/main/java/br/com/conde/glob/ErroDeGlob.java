package br.com.conde.glob;

/**
 * Erro de sintaxe num padrao glob, apontando a POSICAO.
 *
 * <p>A posicao esta na mensagem porque um padrao glob errado e quase sempre errado
 * num lugar so, e "padrao invalido" obriga quem leu a procurar o lugar de novo.
 */
public class ErroDeGlob extends IllegalArgumentException {

    private final int posicao;

    public ErroDeGlob(String motivo, int posicao) {
        super(motivo + ", na posicao " + posicao);

        this.posicao = posicao;
    }

    public int posicao() {
        return posicao;
    }
}
