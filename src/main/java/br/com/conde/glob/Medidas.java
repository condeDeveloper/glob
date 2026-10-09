package br.com.conde.glob;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * As MEDIDAS, em passos, nunca em segundos.
 *
 * <p>Segundo nao e medida: muda com a maquina, com o que mais esta rodando e com o
 * humor do coletor de lixo. A integracao continua roda em tres sistemas, e tres
 * numeros de segundos nao se comparam.
 */
public final class Medidas {

    private Medidas() {
    }

    /** Os pares do repositorio, cada um existindo para separar alguma coisa. */
    public static Map<String, String[]> pares() {
        Map<String, String[]> mapa = new LinkedHashMap<>();

        mapa.put("literal igual", new String[] {"abc", "abc"});
        mapa.put("literal diferente", new String[] {"abc", "abd"});
        mapa.put("so estrela", new String[] {"*", "qualquer coisa"});
        mapa.put("estrela no fim", new String[] {"rel*", "relatorio"});
        mapa.put("estrela no meio", new String[] {"rel*rio", "relatorio"});
        mapa.put("estrela sem fechar", new String[] {"rel*rio", "relatorioX"});
        mapa.put("interrogacao", new String[] {"a?c", "abc"});
        mapa.put("interrogacao de menos", new String[] {"a?c", "ac"});
        mapa.put("classe", new String[] {"arquivo[0-9].txt", "arquivo7.txt"});
        mapa.put("classe negada", new String[] {"arquivo[!0-9].txt", "arquivoA.txt"});
        mapa.put("classe negada que falha", new String[] {"arquivo[!0-9].txt", "arquivo7.txt"});
        mapa.put("extensao", new String[] {"*.java", "Padrao.java"});
        mapa.put("extensao que falha", new String[] {"*.java", "Padrao.javax"});
        mapa.put("escapado", new String[] {"a\\*c", "a*c"});
        mapa.put("escapado que falha", new String[] {"a\\*c", "abc"});
        mapa.put("vazio com vazio", new String[] {"", ""});
        mapa.put("vazio com texto", new String[] {"", "a"});
        mapa.put("estrela com vazio", new String[] {"*", ""});
        mapa.put("catastrofe pequena", new String[] {"a*a*a*b", "aaaaaaaaaaaa"});
        mapa.put("muitas estrelas", new String[] {"*a*b*c*", "xaybzc"});

        return mapa;
    }

    // ───────────────────── 1. contra o automato ─────────────────────

    public record LinhaDefinicao(String caso, String padrao, String texto, boolean peloAutomato,
                                 Map<String, Boolean> casadores, boolean todosConcordam, long passosDoAutomato) {
    }

    /** Os tres casadores contra o AUTOMATO, par por par. */
    public static List<LinhaDefinicao> contraOAutomato() {
        List<LinhaDefinicao> linhas = new ArrayList<>();

        for (Map.Entry<String, String[]> e : pares().entrySet()) {
            Padrao p = Padrao.ler(e.getValue()[0]);
            String t = e.getValue()[1];

            Juiz.Veredicto v = Juiz.julgar(p, t);
            Map<String, Boolean> respostas = new LinkedHashMap<>();
            boolean concordam = true;

            for (Casador c : Casadores.todos()) {
                boolean r = c.casa(p, t);

                respostas.put(c.nome(), r);

                if (r != v.casa()) concordam = false;
            }

            linhas.add(new LinhaDefinicao(e.getKey(), e.getValue()[0], t, v.casa(),
                    respostas, concordam, v.passos()));
        }

        return linhas;
    }

    // ───────────────────── 2. a enumeracao exaustiva ─────────────────────

    public record LinhaExaustiva(String padrao, int textos, long casaram, boolean todosConcordam,
                                 long maiorDiscordancia) {
    }

    /**
     * TODOS os textos de um alfabeto pequeno, contra um punhado de padroes.
     *
     * <p>Transforma "os quatro concordam nestes exemplos" em "os quatro concordam em
     * todos os textos ate oito caracteres sobre tres letras". Nao e prova; e muito
     * mais do que exemplos escolhidos por quem escreveu o algoritmo.
     */
    public static List<LinhaExaustiva> aEnumeracaoExaustiva(String alfabeto, int comprimento) {
        List<String> textos = Juiz.todosOsTextos(alfabeto, comprimento);
        List<LinhaExaustiva> linhas = new ArrayList<>();

        for (String texto : List.of(
                "*", "a*", "*a", "*a*", "a*b", "a?b", "*a*b*", "a*a", "[ab]*c", "*[!a]",
                "?*?", "a**b", "*ab*", "a*b*c")) {

            Padrao p = Padrao.ler(texto);
            long casaram = 0;
            boolean concordam = true;
            long discordancias = 0;

            for (String t : textos) {
                boolean certo = Juiz.casa(p, t);

                if (certo) casaram++;

                for (Casador c : Casadores.todos())
                    if (c.casa(p, t) != certo) {
                        concordam = false;
                        discordancias++;
                    }
            }

            linhas.add(new LinhaExaustiva(texto, textos.size(), casaram, concordam, discordancias));
        }

        return linhas;
    }

    // ───────────────────── 3. a catastrofe ─────────────────────

    public record LinhaCatastrofe(int letras, String retrocesso, long tabela, long doisPonteiros,
                                  long automato, boolean casou) {
    }

    /**
     * O RETROCESSO CATASTROFICO: {@code a*a*a*a*b} contra um texto so de letras a.
     *
     * <p>O padrao tem nove pedacos e o texto nao casa. O retrocesso tenta todas as
     * maneiras de repartir o texto entre os quatro asteriscos antes de desistir, e o
     * numero de maneiras cresce como o tamanho do texto elevado ao numero de
     * asteriscos.
     *
     * <p>Os asteriscos NAO sao colapsados aqui, porque colapsa-los mudaria o padrao.
     * Eles estao separados por letras, que e justamente o que impede qualquer
     * simplificacao.
     */
    public static List<LinhaCatastrofe> aCatastrofe() {
        List<LinhaCatastrofe> linhas = new ArrayList<>();
        Padrao p = Padrao.ler("a*a*a*a*b");

        for (int letras : new int[] {4, 8, 12, 16, 20, 24, 28, 32, 40, 60, 100}) {
            String t = "a".repeat(letras);

            String retrocesso;

            try {
                retrocesso = comPontos(new Casadores.PorRetrocesso().passos(p, t));
            } catch (Casadores.ExplodiuException e) {
                retrocesso = "nao cabe";
            } catch (StackOverflowError e) {
                retrocesso = "pilha";
            }

            linhas.add(new LinhaCatastrofe(letras, retrocesso,
                    new Casadores.PorTabela().passos(p, t),
                    new Casadores.PorDoisPonteiros().passos(p, t),
                    Juiz.julgar(p, t).passos(),
                    Juiz.casa(p, t)));
        }

        return linhas;
    }

    public record LinhaEstrelas(int estrelas, String padrao, int letras, String retrocesso,
                                long tabela, long doisPonteiros, long automato) {
    }

    /**
     * O mesmo texto, com um numero CRESCENTE de asteriscos no padrao.
     *
     * <p>E a tabela que me corrigiu. A explosao do retrocesso nao e exponencial no
     * TEXTO: com um numero fixo de asteriscos ela e polinomial, e o grau do polinomio
     * e o numero de asteriscos. Com quatro asteriscos e cem letras sao quatro milhoes
     * de passos, que e cem elevado a quatro dividido por vinte e quatro.
     *
     * <p>O que explode de verdade e o PADRAO: cada asterisco a mais multiplica o custo
     * pelo tamanho do texto. E uma distincao que muda o conselho pratico: nao adianta
     * limitar o tamanho do nome do arquivo, adianta limitar o numero de asteriscos.
     */
    public static List<LinhaEstrelas> porNumeroDeEstrelas(int letras) {
        List<LinhaEstrelas> linhas = new ArrayList<>();
        String t = "a".repeat(letras);

        for (int estrelas = 1; estrelas <= 7; estrelas++) {
            String texto = "a*".repeat(estrelas) + "b";
            Padrao p = Padrao.ler(texto);

            String retrocesso;

            try {
                retrocesso = comPontos(new Casadores.PorRetrocesso().passos(p, t));
            } catch (Casadores.ExplodiuException e) {
                retrocesso = "nao cabe";
            } catch (StackOverflowError e) {
                retrocesso = "pilha";
            }

            linhas.add(new LinhaEstrelas(estrelas, texto, letras, retrocesso,
                    new Casadores.PorTabela().passos(p, t),
                    new Casadores.PorDoisPonteiros().passos(p, t),
                    Juiz.julgar(p, t).passos()));
        }

        return linhas;
    }

    // ───────────────────── 4. o custo ─────────────────────

    public record LinhaCusto(String caso, int pedacos, int letras, Map<String, Long> passos) {
    }

    /** Quantos passos cada casador gasta em cada par do repositorio. */
    public static List<LinhaCusto> oCusto() {
        List<LinhaCusto> linhas = new ArrayList<>();

        for (Map.Entry<String, String[]> e : pares().entrySet()) {
            Padrao p = Padrao.ler(e.getValue()[0]);
            String t = e.getValue()[1];

            Map<String, Long> passos = new LinkedHashMap<>();

            for (Casador c : Casadores.todos()) passos.put(c.nome(), c.passos(p, t));

            passos.put("automato", Juiz.julgar(p, t).passos());

            linhas.add(new LinhaCusto(e.getKey(), p.tamanho(), t.length(), passos));
        }

        return linhas;
    }

    // ───────────────────── 5. as classes ─────────────────────

    public record LinhaClasse(String padrao, int pedacos, String aceita, String recusa, boolean negada) {
    }

    /** As classes de caracteres, lidas e testadas letra por letra. */
    public static List<LinhaClasse> asClasses() {
        List<LinhaClasse> linhas = new ArrayList<>();
        String alfabeto = "abcxyz019-]^!";

        for (String texto : List.of(
                "[abc]", "[a-c]", "[a-cx-z]", "[!abc]", "[^abc]", "[0-9]", "[]abc]", "[a-]", "[-a]", "[!0-9a-c]")) {

            Padrao p = Padrao.ler(texto);
            StringBuilder aceita = new StringBuilder();
            StringBuilder recusa = new StringBuilder();

            for (char c : alfabeto.toCharArray())
                if (Juiz.casa(p, String.valueOf(c))) aceita.append(c);
                else recusa.append(c);

            boolean negada = p.em(0) instanceof Padrao.Pedaco.Classe cl && cl.negada();

            linhas.add(new LinhaClasse(texto, p.tamanho(), aceita.toString(), recusa.toString(), negada));
        }

        return linhas;
    }

    /**
     * Um numero com PONTO de milhar, sempre, em qualquer maquina.
     *
     * <p>O {@code String.format("%,d")} usa o idioma da maquina: aqui sai 2.097.151 e
     * numa maquina em ingles sai 2,097,151. A integracao continua roda em tres
     * sistemas e quebrou exatamente nisso, com um teste que comparava a cadeia
     * formatada.
     *
     * <p>Agrupar a mao tira o idioma da conta. Medida nao pode depender de onde esta
     * rodando.
     */
    public static String comPontos(long valor) {
        String digitos = Long.toString(Math.abs(valor));
        StringBuilder saida = new StringBuilder();

        for (int i = 0; i < digitos.length(); i++) {
            if (i > 0 && (digitos.length() - i) % 3 == 0) saida.append('.');

            saida.append(digitos.charAt(i));
        }

        return (valor < 0 ? "-" : "") + saida;
    }
}
