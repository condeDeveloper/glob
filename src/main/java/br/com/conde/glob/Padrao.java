package br.com.conde.glob;

import java.util.ArrayList;
import java.util.List;

/**
 * Um PADRAO GLOB, lido uma vez e guardado como lista de pedacos.
 *
 * <p>A gramatica inteira cabe em cinco linhas:
 *
 * <ul>
 *   <li>{@code *} casa com qualquer sequencia, inclusive vazia;
 *   <li>{@code ?} casa com exatamente um caractere;
 *   <li>{@code [abc]} casa com um caractere da lista, e {@code [a-z]} com uma faixa;
 *   <li>{@code [!abc]} e {@code [^abc]} casam com um caractere FORA da lista;
 *   <li>a contrabarra tira o significado especial do proximo caractere.
 * </ul>
 *
 * <p>Ler o padrao uma vez e guardar os pedacos nao e otimizacao: e o que permite
 * que os tres casadores recebam a MESMA entrada. Se cada um lesse o texto do seu
 * jeito, uma divergencia entre eles poderia ser do casamento ou da leitura, e as
 * tabelas nao separariam as duas coisas.
 *
 * <p>Colapsar asteriscos seguidos tambem acontece aqui, e isso muda o assunto do
 * repositorio: {@code **} e {@code *} casam com exatamente o mesmo conjunto de
 * textos, mas o retrocesso ingenuo gasta coisas muito diferentes nos dois. A tabela
 * do retrocesso catastrofico mede com os asteriscos SEPARADOS de proposito.
 */
public final class Padrao {

    /** Um pedaco do padrao. */
    public sealed interface Pedaco {

        /** Um caractere literal. */
        record Literal(char letra) implements Pedaco {
        }

        /** Um caractere qualquer: a interrogacao. */
        record Qualquer() implements Pedaco {
        }

        /** Qualquer sequencia, inclusive vazia: o asterisco. */
        record Estrela() implements Pedaco {
        }

        /** Uma classe de caracteres, com faixas e negacao. */
        record Classe(List<char[]> faixas, boolean negada) implements Pedaco {

            public Classe {
                faixas = List.copyOf(faixas);
            }

            public boolean aceita(char c) {
                boolean dentro = false;

                for (char[] faixa : faixas)
                    if (c >= faixa[0] && c <= faixa[1]) {
                        dentro = true;
                        break;
                    }

                return negada != dentro;
            }
        }
    }

    private final String texto;
    private final List<Pedaco> pedacos;

    private Padrao(String texto, List<Pedaco> pedacos) {
        this.texto = texto;
        this.pedacos = List.copyOf(pedacos);
    }

    public String texto() {
        return texto;
    }

    public List<Pedaco> pedacos() {
        return pedacos;
    }

    public int tamanho() {
        return pedacos.size();
    }

    public Pedaco em(int i) {
        return pedacos.get(i);
    }

    public long estrelas() {
        return pedacos.stream().filter(p -> p instanceof Pedaco.Estrela).count();
    }

    /** O padrao sem estrelas seguidas, que casa o mesmo e custa outra coisa. */
    public static Padrao ler(String texto) {
        return ler(texto, true);
    }

    /**
     * @param colapsarEstrelas junta asteriscos seguidos num so. Casa o mesmo
     *                         conjunto de textos e muda o custo do retrocesso.
     */
    public static Padrao ler(String texto, boolean colapsarEstrelas) {
        if (texto == null) throw new ErroDeGlob("padrao nulo", 0);

        List<Pedaco> saida = new ArrayList<>();
        int i = 0;

        while (i < texto.length()) {
            char c = texto.charAt(i);

            switch (c) {
                case '*' -> {
                    if (!colapsarEstrelas || saida.isEmpty() || !(saida.get(saida.size() - 1) instanceof Pedaco.Estrela))
                        saida.add(new Pedaco.Estrela());

                    i++;
                }
                case '?' -> {
                    saida.add(new Pedaco.Qualquer());
                    i++;
                }
                case '[' -> i = lerClasse(texto, i, saida);
                case '\\' -> {
                    if (i + 1 >= texto.length()) throw new ErroDeGlob("contrabarra no fim do padrao", i);

                    saida.add(new Pedaco.Literal(texto.charAt(i + 1)));
                    i += 2;
                }
                default -> {
                    saida.add(new Pedaco.Literal(c));
                    i++;
                }
            }
        }

        return new Padrao(texto, saida);
    }

    private static int lerClasse(String texto, int abre, List<Pedaco> saida) {
        int i = abre + 1;
        boolean negada = false;

        if (i < texto.length() && (texto.charAt(i) == '!' || texto.charAt(i) == '^')) {
            negada = true;
            i++;
        }

        List<char[]> faixas = new ArrayList<>();

        // um colchete logo no comeco e literal, e essa regra vem do shell
        if (i < texto.length() && texto.charAt(i) == ']') {
            faixas.add(new char[] {']', ']'});
            i++;
        }

        while (i < texto.length() && texto.charAt(i) != ']') {
            char de = texto.charAt(i);

            if (de == '\\') {
                if (i + 1 >= texto.length()) throw new ErroDeGlob("contrabarra no fim da classe", i);

                de = texto.charAt(i + 1);
                i += 2;
            } else {
                i++;
            }

            if (i + 1 < texto.length() && texto.charAt(i) == '-' && texto.charAt(i + 1) != ']') {
                char ate = texto.charAt(i + 1);

                if (ate < de) throw new ErroDeGlob("faixa invertida: " + de + "-" + ate, i);

                faixas.add(new char[] {de, ate});
                i += 2;
            } else {
                faixas.add(new char[] {de, de});
            }
        }

        if (i >= texto.length()) throw new ErroDeGlob("classe sem fechar", abre);
        if (faixas.isEmpty()) throw new ErroDeGlob("classe vazia", abre);

        saida.add(new Pedaco.Classe(faixas, negada));

        return i + 1;
    }

    @Override
    public String toString() {
        return texto;
    }
}
