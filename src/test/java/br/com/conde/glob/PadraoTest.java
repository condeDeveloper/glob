package br.com.conde.glob;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PadraoTest {

    @Test
    @DisplayName("cada caractere comum vira um pedaco literal")
    void literais() {
        Padrao p = Padrao.ler("abc");

        assertThat(p.tamanho()).isEqualTo(3);
        assertThat(p.em(0)).isEqualTo(new Padrao.Pedaco.Literal('a'));
        assertThat(p.estrelas()).isZero();
    }

    @Test
    @DisplayName("asteriscos seguidos viram um so, e isso nao muda o que casa")
    void asteriscosSeguidos() {
        assertThat(Padrao.ler("a***b").tamanho()).isEqualTo(3);
        assertThat(Padrao.ler("a***b").estrelas()).isEqualTo(1);

        // e sem colapsar eles continuam la, que e como a tabela do retrocesso mede
        assertThat(Padrao.ler("a***b", false).tamanho()).isEqualTo(5);
        assertThat(Padrao.ler("a***b", false).estrelas()).isEqualTo(3);

        // as duas leituras casam exatamente o mesmo
        for (String t : Juiz.todosOsTextos("ab", 7))
            assertThat(Juiz.casa(Padrao.ler("a***b", false), t))
                    .as(t)
                    .isEqualTo(Juiz.casa(Padrao.ler("a***b", true), t));
    }

    @Test
    @DisplayName("a contrabarra tira o significado especial do proximo caractere")
    void aContrabarra() {
        Padrao p = Padrao.ler("a\\*c");

        assertThat(p.estrelas()).isZero();
        assertThat(p.tamanho()).isEqualTo(3);
        assertThat(Juiz.casa(p, "a*c")).isTrue();
        assertThat(Juiz.casa(p, "abc")).isFalse();
    }

    @Test
    @DisplayName("as classes com lista, faixa e negacao")
    void classes() {
        assertThat(Juiz.casa(Padrao.ler("[abc]"), "b")).isTrue();
        assertThat(Juiz.casa(Padrao.ler("[abc]"), "d")).isFalse();

        assertThat(Juiz.casa(Padrao.ler("[a-c]"), "b")).isTrue();
        assertThat(Juiz.casa(Padrao.ler("[a-c]"), "d")).isFalse();

        assertThat(Juiz.casa(Padrao.ler("[!abc]"), "d")).isTrue();
        assertThat(Juiz.casa(Padrao.ler("[!abc]"), "a")).isFalse();

        // o circunflexo nega igual ao ponto de exclamacao
        assertThat(Juiz.casa(Padrao.ler("[^abc]"), "d")).isTrue();
    }

    /**
     * Um colchete logo no comeco da classe e LITERAL, e um traco na ponta tambem.
     *
     * <p>Sao duas regras herdadas do shell que existem porque nao ha outra maneira de
     * escrever esses dois caracteres dentro de uma classe.
     */
    @Test
    @DisplayName("colchete no comeco e traco na ponta sao literais")
    void colcheteETraco() {
        assertThat(Juiz.casa(Padrao.ler("[]abc]"), "]")).isTrue();
        assertThat(Juiz.casa(Padrao.ler("[]abc]"), "a")).isTrue();
        assertThat(Juiz.casa(Padrao.ler("[]abc]"), "d")).isFalse();

        assertThat(Juiz.casa(Padrao.ler("[a-]"), "-")).isTrue();
        assertThat(Juiz.casa(Padrao.ler("[a-]"), "a")).isTrue();
        assertThat(Juiz.casa(Padrao.ler("[-a]"), "-")).isTrue();
    }

    @Test
    @DisplayName("o erro diz a posicao")
    void erros() {
        assertThatThrownBy(() -> Padrao.ler("[abc"))
                .isInstanceOf(ErroDeGlob.class)
                .hasMessageContaining("sem fechar");

        assertThatThrownBy(() -> Padrao.ler("[]"))
                .isInstanceOf(ErroDeGlob.class)
                .hasMessageContaining("sem fechar");

        assertThatThrownBy(() -> Padrao.ler("abc\\"))
                .isInstanceOf(ErroDeGlob.class)
                .hasMessageContaining("contrabarra no fim");

        assertThatThrownBy(() -> Padrao.ler("[z-a]"))
                .isInstanceOf(ErroDeGlob.class)
                .hasMessageContaining("faixa invertida");
    }

    @Test
    @DisplayName("padrao vazio casa so com texto vazio")
    void padraoVazio() {
        assertThat(Juiz.casa(Padrao.ler(""), "")).isTrue();
        assertThat(Juiz.casa(Padrao.ler(""), "a")).isFalse();
    }

    @Test
    @DisplayName("um asterisco sozinho casa com tudo, inclusive com o vazio")
    void estrelaSozinha() {
        Padrao p = Padrao.ler("*");

        assertThat(Juiz.casa(p, "")).isTrue();

        for (String t : Juiz.todosOsTextos("ab", 6)) assertThat(Juiz.casa(p, t)).as(t).isTrue();
    }
}
