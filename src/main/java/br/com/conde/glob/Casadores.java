package br.com.conde.glob;

import java.util.List;

/**
 * Os TRES CASADORES, e o que cada um faz com o asterisco.
 *
 * <p>Todo o problema do glob esta numa pergunta so: quantos caracteres o asterisco
 * come? As tres respostas deste arquivo sao as tres maneiras de lidar com ela.
 *
 * <ul>
 *   <li>O RETROCESSO tenta zero, depois um, depois dois, e volta atras quando nao
 *       deu. E a traducao direta da definicao, e e exponencial.
 *   <li>A TABELA pergunta, para cada posicao do padrao e cada posicao do texto, se
 *       dali para a frente casa, e guarda a resposta. E o mesmo problema sem repetir
 *       subproblema: n vezes m.
 *   <li>OS DOIS PONTEIROS andam para a frente e guardam UM ponto de volta: o do
 *       ultimo asterisco. Tambem e n vezes m no pior caso, e e linear na pratica.
 * </ul>
 */
public final class Casadores {

    private Casadores() {
    }

    public static List<Casador> todos() {
        return List.of(new PorRetrocesso(), new PorTabela(), new PorDoisPonteiros());
    }

    // ───────────────────────── retrocesso ─────────────────────────

    /**
     * O RETROCESSO: a traducao direta da definicao, e a armadilha classica.
     *
     * <p>Quando o pedaco atual e um asterisco, ele tenta casar o resto do padrao a
     * partir de cada posicao do texto. Cada asterisco multiplica as tentativas pelo
     * tamanho do que sobrou, e dai sai a explosao.
     *
     * <p>Com {@code a*a*a*a*b} contra um texto de vinte letras a, ele gasta mais de
     * dez mil passos para dizer "nao". Com trinta letras, milhoes. E o padrao nao tem
     * nada de patologico: e o tipo de coisa que alguem digita num filtro de arquivos.
     *
     * <p>A recursao aqui e de proposito, porque e assim que esse algoritmo e
     * escrito na vida real. O limite de passos existe para a medida terminar: estourar
     * o limite E a medida.
     */
    public static final class PorRetrocesso implements Casador {

        /** Acima disso a medida vira "nao cabe", que e a resposta interessante. */
        public static final long LIMITE = 50_000_000L;

        @Override
        public String nome() {
            return "retrocesso";
        }

        @Override
        public boolean casa(Padrao padrao, String texto, long[] passos) {
            return casa(padrao, 0, texto, 0, passos);
        }

        private boolean casa(Padrao p, int i, String t, int j, long[] passos) {
            if (++passos[0] > LIMITE) throw new ExplodiuException(LIMITE);

            if (i == p.tamanho()) return j == t.length();

            Padrao.Pedaco pedaco = p.em(i);

            if (pedaco instanceof Padrao.Pedaco.Estrela) {
                // tenta comer zero, um, dois... caracteres
                for (int k = j; k <= t.length(); k++)
                    if (casa(p, i + 1, t, k, passos)) return true;

                return false;
            }

            if (j >= t.length()) return false;
            if (!aceita(pedaco, t.charAt(j))) return false;

            return casa(p, i + 1, t, j + 1, passos);
        }
    }

    /** O retrocesso passou do limite de passos. Estourar o limite e a medida. */
    public static final class ExplodiuException extends RuntimeException {

        private final long limite;

        public ExplodiuException(long limite) {
            super("o retrocesso passou de " + limite + " passos");

            this.limite = limite;
        }

        public long limite() {
            return limite;
        }
    }

    // ───────────────────────── tabela ─────────────────────────

    /**
     * A TABELA: para cada posicao do padrao e cada posicao do texto, casa dali para a
     * frente?
     *
     * <p>E exatamente o mesmo problema do retrocesso, sem resolver duas vezes o mesmo
     * subproblema. O retrocesso visita o par (i, j) uma vez por caminho que chega
     * nele; a tabela visita uma vez.
     *
     * <p>Preenchida de tras para a frente, que e a ordem em que as dependencias ja
     * estao prontas, e com isso nao precisa de recursao nem de pilha.
     */
    public static final class PorTabela implements Casador {

        @Override
        public String nome() {
            return "tabela";
        }

        @Override
        public boolean casa(Padrao p, String t, long[] passos) {
            int n = p.tamanho();
            int m = t.length();

            boolean[][] casa = new boolean[n + 1][m + 1];

            casa[n][m] = true;

            for (int i = n; i >= 0; i--)
                for (int j = m; j >= 0; j--) {
                    if (i == n && j == m) continue;

                    passos[0]++;

                    if (i == n) {
                        casa[i][j] = false;
                        continue;
                    }

                    Padrao.Pedaco pedaco = p.em(i);

                    if (pedaco instanceof Padrao.Pedaco.Estrela) {
                        // come zero caracteres, ou come um e continua no mesmo asterisco
                        casa[i][j] = casa[i + 1][j] || (j < m && casa[i][j + 1]);
                        continue;
                    }

                    casa[i][j] = j < m && aceita(pedaco, t.charAt(j)) && casa[i + 1][j + 1];
                }

            return casa[0][0];
        }
    }

    // ───────────────────────── dois ponteiros ─────────────────────────

    /**
     * OS DOIS PONTEIROS: anda para a frente e guarda UM ponto de volta.
     *
     * <p>A ideia e que nao e preciso lembrar de todos os asteriscos: basta lembrar do
     * ULTIMO. Quando o casamento falha depois de um asterisco, volta para ele, deixa
     * que ele coma um caractere a mais e segue.
     *
     * <p>E o algoritmo que esta em quase toda biblioteca de glob, e ele e a resposta
     * certa para o problema: mesma resposta da tabela, memoria constante, e linear em
     * quase todo par que nao foi construido para atrapalhar.
     */
    public static final class PorDoisPonteiros implements Casador {

        @Override
        public String nome() {
            return "dois ponteiros";
        }

        @Override
        public boolean casa(Padrao p, String t, long[] passos) {
            int i = 0;
            int j = 0;
            int estrela = -1;
            int voltaDoTexto = 0;

            while (j < t.length()) {
                passos[0]++;

                if (i < p.tamanho() && p.em(i) instanceof Padrao.Pedaco.Estrela) {
                    estrela = i++;
                    voltaDoTexto = j;
                    continue;
                }

                if (i < p.tamanho() && aceita(p.em(i), t.charAt(j))) {
                    i++;
                    j++;
                    continue;
                }

                if (estrela < 0) return false;

                // volta para o ultimo asterisco e deixa que ele coma mais um
                i = estrela + 1;
                j = ++voltaDoTexto;
            }

            while (i < p.tamanho() && p.em(i) instanceof Padrao.Pedaco.Estrela) {
                passos[0]++;
                i++;
            }

            return i == p.tamanho();
        }
    }

    // ───────────────────────── comum ─────────────────────────

    /** Um pedaco que nao e asterisco aceita este caractere? */
    static boolean aceita(Padrao.Pedaco pedaco, char c) {
        return switch (pedaco) {
            case Padrao.Pedaco.Literal l -> l.letra() == c;
            case Padrao.Pedaco.Qualquer q -> true;
            case Padrao.Pedaco.Classe cl -> cl.aceita(c);
            case Padrao.Pedaco.Estrela e -> throw new IllegalArgumentException("estrela nao e um pedaco de um caractere");
        };
    }
}
