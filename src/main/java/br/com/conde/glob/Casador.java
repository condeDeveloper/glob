package br.com.conde.glob;

/**
 * Um CASADOR: dado um padrao e um texto, o texto casa?
 *
 * <p>Os tres casadores deste repositorio respondem sempre a mesma coisa e custam
 * coisas muito diferentes. E a diferenca de custo nao e uma constante: ela e a
 * distancia entre polinomial e exponencial.
 */
public interface Casador {

    String nome();

    /**
     * @param padrao o padrao ja lido
     * @param texto  o texto
     * @param passos acumulador de passos: o casador soma aqui o que gastou
     */
    boolean casa(Padrao padrao, String texto, long[] passos);

    default boolean casa(Padrao padrao, String texto) {
        return casa(padrao, texto, new long[1]);
    }

    /** Quantos passos este casador gasta neste par. */
    default long passos(Padrao padrao, String texto) {
        long[] passos = {0};

        casa(padrao, texto, passos);

        return passos[0];
    }
}
