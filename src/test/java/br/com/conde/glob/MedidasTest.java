package br.com.conde.glob;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os testes das TABELAS que o medidor imprime.
 *
 * <p>Toda frase do README que e um numero esta aqui como afirmacao. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
class MedidasTest {

    @Test
    @DisplayName("os quatro concordam em toda linha da tabela da definicao")
    void todaLinhaConcorda() {
        List<Medidas.LinhaDefinicao> linhas = Medidas.contraOAutomato();

        assertThat(linhas).hasSize(20);
        assertThat(linhas).allSatisfy(l -> assertThat(l.todosConcordam()).as(l.caso()).isTrue());
    }

    @Test
    @DisplayName("a enumeracao exaustiva nao acha nenhuma discordancia")
    void aEnumeracaoNaoAchaDiscordancia() {
        List<Medidas.LinhaExaustiva> linhas = Medidas.aEnumeracaoExaustiva("abc", 7);

        assertThat(linhas).hasSize(14);
        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.todosConcordam()).as(l.padrao()).isTrue();
            assertThat(l.maiorDiscordancia()).as(l.padrao()).isZero();
        });

        // o asterisco sozinho casa com TODO texto, e o padrao a?b casa com quase nenhum
        Medidas.LinhaExaustiva estrela = achar(linhas, "*");

        assertThat(estrela.casaram()).isEqualTo(estrela.textos());
        assertThat(achar(linhas, "a?b").casaram()).isEqualTo(3);
    }

    /**
     * O ACHADO: no mesmo par, o retrocesso gasta milhoes e os dois ponteiros gastam
     * dezenas.
     */
    @Test
    @DisplayName("no texto de 100 letras o retrocesso gasta 40 mil vezes os dois ponteiros")
    void aCatastrofeNaTabela() {
        List<Medidas.LinhaCatastrofe> linhas = Medidas.aCatastrofe();

        assertThat(linhas).hasSize(11);
        assertThat(linhas).allSatisfy(l -> assertThat(l.casou()).as("%d letras", l.letras()).isFalse());

        Medidas.LinhaCatastrofe cem = linhas.get(linhas.size() - 1);

        assertThat(cem.letras()).isEqualTo(100);
        assertThat(cem.doisPonteiros()).isEqualTo(104);
        assertThat(cem.retrocesso()).isEqualTo("4.249.776");
        assertThat(cem.tabela()).isEqualTo(1009);

        // e os tres que nao retrocedem crescem linearmente com o texto
        for (int i = 1; i < linhas.size(); i++) {
            assertThat(linhas.get(i).doisPonteiros()).isGreaterThan(linhas.get(i - 1).doisPonteiros());
            assertThat(linhas.get(i).doisPonteiros()).isLessThan(linhas.get(i).letras() * 2L);
        }
    }

    /**
     * E a tabela que me corrigiu: o que explode e o PADRAO, nao o texto.
     *
     * <p>Com o texto parado em trinta letras, cada asterisco a mais multiplica o custo
     * do retrocesso por varias vezes, enquanto a tabela cresce de sessenta em sessenta
     * e os dois ponteiros de um em um.
     */
    @Test
    @DisplayName("cada asterisco a mais multiplica o retrocesso e soma um nos dois ponteiros")
    void cadaEstrelaMultiplicaORetrocesso() {
        List<Medidas.LinhaEstrelas> linhas = Medidas.porNumeroDeEstrelas(30);

        assertThat(linhas).hasSize(7);

        for (int i = 1; i < linhas.size(); i++) {
            long antes = Long.parseLong(linhas.get(i - 1).retrocesso().replace(".", ""));
            long depois = Long.parseLong(linhas.get(i).retrocesso().replace(".", ""));

            assertThat((double) depois / antes)
                    .as("de %d para %d estrelas", i, i + 1)
                    .isGreaterThan(3.0);

            // os dois ponteiros sobem de um em um
            assertThat(linhas.get(i).doisPonteiros() - linhas.get(i - 1).doisPonteiros()).isEqualTo(1);
        }

        assertThat(linhas.get(0).doisPonteiros()).isEqualTo(31);
        assertThat(linhas.get(6).doisPonteiros()).isEqualTo(37);
    }

    @Test
    @DisplayName("as classes negadas aceitam exatamente o complemento das nao negadas")
    void asClassesNaTabela() {
        List<Medidas.LinhaClasse> linhas = Medidas.asClasses();

        assertThat(linhas).hasSize(10);
        assertThat(linhas).allSatisfy(l -> assertThat(l.pedacos()).as(l.padrao()).isEqualTo(1));

        Medidas.LinhaClasse normal = acharClasse(linhas, "[abc]");
        Medidas.LinhaClasse negada = acharClasse(linhas, "[!abc]");

        assertThat(normal.negada()).isFalse();
        assertThat(negada.negada()).isTrue();
        assertThat(negada.aceita()).isEqualTo(normal.recusa());
        assertThat(negada.recusa()).isEqualTo(normal.aceita());

        // e o circunflexo nega igual ao ponto de exclamacao
        assertThat(acharClasse(linhas, "[^abc]").aceita()).isEqualTo(negada.aceita());
    }

    @Test
    @DisplayName("o automato nunca gasta mais que pedacos vezes letras")
    void oAutomatoRespeitaOTeto() {
        for (Medidas.LinhaCusto l : Medidas.oCusto())
            assertThat(l.passos().get("automato"))
                    .as(l.caso())
                    .isLessThanOrEqualTo((long) (l.pedacos() + 1) * l.letras() + 1);
    }

    private static Medidas.LinhaExaustiva achar(List<Medidas.LinhaExaustiva> linhas, String padrao) {
        return linhas.stream().filter(l -> l.padrao().equals(padrao)).findFirst().orElseThrow();
    }

    private static Medidas.LinhaClasse acharClasse(List<Medidas.LinhaClasse> linhas, String padrao) {
        return linhas.stream().filter(l -> l.padrao().equals(padrao)).findFirst().orElseThrow();
    }
}
