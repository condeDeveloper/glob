package br.com.conde.glob;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * O JUIZ: o AUTOMATO.
 *
 * <p>Ele nao casa padrao com texto. Ele transforma o padrao numa maquina de estados
 * e deixa o texto andar por ela, mantendo o CONJUNTO de estados possiveis a cada
 * caractere.
 *
 * <p>E outro ramo da arvore de algoritmos, e por isso serve de juiz. O retrocesso, a
 * tabela e os dois ponteiros sao tres variacoes da mesma ideia de percorrer o padrao
 * e o texto juntos; o automato nao percorre o padrao durante o casamento, ele o
 * compilou antes. Quando os quatro concordam, as coisas que poderiam estar erradas
 * teriam de estar erradas do mesmo jeito.
 *
 * <p>E ele e o unico dos quatro que NUNCA retrocede: cada caractere do texto e
 * olhado uma vez e so uma. Dai sai a garantia de custo que os outros nao dao.
 *
 * <p>O estado i quer dizer "ja casei os i primeiros pedacos do padrao". O conjunto
 * comeca em zero, mais o fecho pelos asteriscos, e anda. O texto casa quando o
 * conjunto final contem o estado do tamanho do padrao.
 */
public final class Juiz {

    private Juiz() {
    }

    /**
     * @param casa   o texto casa com o padrao
     * @param passos quantos caracteres vezes estados foram olhados
     * @param maiorConjunto o maior numero de estados vivos ao mesmo tempo
     */
    public record Veredicto(boolean casa, long passos, int maiorConjunto) {
    }

    public static boolean casa(Padrao padrao, String texto) {
        return julgar(padrao, texto).casa();
    }

    public static Veredicto julgar(Padrao padrao, String texto) {
        int n = padrao.tamanho();
        BitSet vivos = new BitSet(n + 1);

        vivos.set(0);
        fechar(padrao, vivos);

        long passos = 0;
        int maior = vivos.cardinality();

        for (int j = 0; j < texto.length(); j++) {
            char c = texto.charAt(j);
            BitSet proximos = new BitSet(n + 1);

            for (int i = vivos.nextSetBit(0); i >= 0; i = vivos.nextSetBit(i + 1)) {
                passos++;

                if (i >= n) continue;

                Padrao.Pedaco pedaco = padrao.em(i);

                // o estado de um asterisco ja esta fechado para a frente; aqui ele
                // come um caractere e continua no mesmo lugar
                if (pedaco instanceof Padrao.Pedaco.Estrela) proximos.set(i);
                else if (Casadores.aceita(pedaco, c)) proximos.set(i + 1);
            }

            fechar(padrao, proximos);

            vivos = proximos;
            maior = Math.max(maior, vivos.cardinality());

            if (vivos.isEmpty()) break;
        }

        return new Veredicto(vivos.get(n), passos, maior);
    }

    /** Todo estado parado num asterisco tambem pode ter pulado o asterisco. */
    private static void fechar(Padrao padrao, BitSet vivos) {
        boolean mudou = true;

        while (mudou) {
            mudou = false;

            for (int i = vivos.nextSetBit(0); i >= 0; i = vivos.nextSetBit(i + 1))
                if (i < padrao.tamanho()
                        && padrao.em(i) instanceof Padrao.Pedaco.Estrela
                        && !vivos.get(i + 1)) {

                    vivos.set(i + 1);
                    mudou = true;
                }
        }
    }

    // ───────────────────── a enumeracao exaustiva ─────────────────────

    /**
     * TODOS os textos de um alfabeto pequeno ate um comprimento.
     *
     * <p>E o que transforma "os quatro concordam nestes exemplos" em "os quatro
     * concordam em todos os textos ate oito caracteres sobre tres letras", que sao
     * mais de nove mil. Nao e prova, e e muito mais do que uma lista de exemplos
     * escolhidos a mao por quem escreveu o algoritmo.
     */
    public static List<String> todosOsTextos(String alfabeto, int ateOComprimento) {
        List<String> saida = new ArrayList<>();
        List<String> nivel = List.of("");

        saida.add("");

        for (int c = 1; c <= ateOComprimento; c++) {
            List<String> proximo = new ArrayList<>();

            for (String base : nivel)
                for (char letra : alfabeto.toCharArray()) proximo.add(base + letra);

            saida.addAll(proximo);
            nivel = proximo;
        }

        return saida;
    }
}
