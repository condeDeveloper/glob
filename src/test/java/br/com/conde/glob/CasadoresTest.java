package br.com.conde.glob;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os tres casadores contra o AUTOMATO, que e o juiz.
 *
 * <p>O automato nao percorre o padrao durante o casamento: ele o compilou antes, e
 * deixa o texto andar por um conjunto de estados. E outro ramo da arvore de
 * algoritmos, e por isso serve de juiz.
 */
class CasadoresTest {

    /**
     * A conferencia principal: TODOS os textos ate oito caracteres sobre tres letras,
     * contra catorze padroes. Sao mais de cento e trinta mil comparacoes.
     *
     * <p>Isso transforma "os quatro concordam nestes exemplos" em outra coisa. Nao e
     * prova, e e muito mais do que uma lista de exemplos escolhidos a mao por quem
     * escreveu o algoritmo.
     */
    @Test
    @DisplayName("os quatro concordam em todos os textos ate 8 caracteres sobre abc")
    void osQuatroConcordamEmTudo() {
        List<String> textos = Juiz.todosOsTextos("abc", 8);

        assertThat(textos).hasSize(9_841);

        for (String texto : List.of(
                "*", "a*", "*a", "*a*", "a*b", "a?b", "*a*b*", "a*a", "[ab]*c",
                "*[!a]", "?*?", "a**b", "*ab*", "a*b*c", "??*", "*c", "a*b*c*")) {

            Padrao p = Padrao.ler(texto);

            for (String t : textos) {
                boolean certo = Juiz.casa(p, t);

                for (Casador c : Casadores.todos())
                    assertThat(c.casa(p, t))
                            .as("%s com \"%s\" por %s", texto, t, c.nome())
                            .isEqualTo(certo);
            }
        }
    }

    @Test
    @DisplayName("e concordam tambem nos pares nomeados do repositorio")
    void concordamNosParesNomeados() {
        Medidas.pares().forEach((caso, par) -> {
            Padrao p = Padrao.ler(par[0]);
            boolean certo = Juiz.casa(p, par[1]);

            for (Casador c : Casadores.todos())
                assertThat(c.casa(p, par[1])).as("%s por %s", caso, c.nome()).isEqualTo(certo);
        });
    }

    @Test
    @DisplayName("o automato olha cada caractere do texto uma vez e nunca retrocede")
    void oAutomatoNuncaRetrocede() {
        for (String texto : List.of("*", "a*b", "a*a*a*a*b", "*a*b*c*")) {
            Padrao p = Padrao.ler(texto);

            for (int letras : new int[] {10, 50, 200}) {
                Juiz.Veredicto v = Juiz.julgar(p, "a".repeat(letras));

                // no maximo um estado por pedaco do padrao, por caractere do texto
                assertThat(v.passos())
                        .as("%s com %d letras", texto, letras)
                        .isLessThanOrEqualTo((long) letras * (p.tamanho() + 1));
            }
        }
    }

    /**
     * O ACHADO QUE ME CORRIGIU: a explosao do retrocesso nao e exponencial no TEXTO.
     *
     * <p>Com um numero fixo de asteriscos ela e polinomial, e o grau do polinomio e o
     * numero de asteriscos. O que explode e o PADRAO: cada asterisco a mais multiplica
     * o custo pelo tamanho do texto.
     */
    @Test
    @DisplayName("o retrocesso cresce como o texto elevado ao numero de asteriscos")
    void oRetrocessoCresceComoPotencia() {
        Padrao p = Padrao.ler("a*a*a*a*b");

        long com20 = new Casadores.PorRetrocesso().passos(p, "a".repeat(20));
        long com40 = new Casadores.PorRetrocesso().passos(p, "a".repeat(40));

        // dobrar o texto com quatro asteriscos multiplica por cerca de dezesseis
        assertThat((double) com40 / com20).isBetween(10.0, 22.0);

        // e acrescentar um asterisco ao padrao multiplica pelo tamanho do texto
        long quatro = new Casadores.PorRetrocesso().passos(Padrao.ler("a*a*a*a*b"), "a".repeat(30));
        long cinco = new Casadores.PorRetrocesso().passos(Padrao.ler("a*a*a*a*a*b"), "a".repeat(30));

        assertThat((double) cinco / quatro).isBetween(3.0, 10.0);
    }

    @Test
    @DisplayName("a tabela e os dois ponteiros nao explodem no mesmo caso")
    void aTabelaEOsPonteirosNaoExplodem() {
        Padrao p = Padrao.ler("a*a*a*a*a*a*a*b");
        String t = "a".repeat(200);

        long tabela = new Casadores.PorTabela().passos(p, t);
        long ponteiros = new Casadores.PorDoisPonteiros().passos(p, t);

        assertThat(tabela).isLessThanOrEqualTo((long) (p.tamanho() + 1) * (t.length() + 1));
        assertThat(ponteiros).isLessThan(2_000);
    }

    @Test
    @DisplayName("o retrocesso estoura o limite onde os outros nem suam")
    void oRetrocessoEstoura() {
        Padrao p = Padrao.ler("a*a*a*a*a*a*a*b");
        String t = "a".repeat(200);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new Casadores.PorRetrocesso().passos(p, t))
                .isInstanceOfAny(Casadores.ExplodiuException.class, StackOverflowError.class);

        // e os outros tres dao a resposta
        assertThat(Juiz.casa(p, t)).isFalse();
        assertThat(new Casadores.PorTabela().casa(p, t)).isFalse();
        assertThat(new Casadores.PorDoisPonteiros().casa(p, t)).isFalse();
    }

    @Test
    @DisplayName("os dois ponteiros nao precisam lembrar de todos os asteriscos")
    void osDoisPonteirosSoLembramDoUltimo() {
        // o caso em que a memoria de um asterisco so poderia falhar, e nao falha
        for (String texto : List.of("*a*b*c*d", "a*b*c*d*e", "*x*y*z*")) {
            Padrao p = Padrao.ler(texto);

            for (String t : Juiz.todosOsTextos("abcde", 6))
                assertThat(new Casadores.PorDoisPonteiros().casa(p, t))
                        .as("%s com \"%s\"", texto, t)
                        .isEqualTo(Juiz.casa(p, t));
        }
    }
}
