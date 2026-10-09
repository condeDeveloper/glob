package br.com.conde.glob;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao
 * quebra quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("definicao") || comando.equals("tudo")) definicao();
        if (comando.equals("exaustiva") || comando.equals("tudo")) exaustiva();
        if (comando.equals("catastrofe") || comando.equals("tudo")) catastrofe();
        if (comando.equals("custo") || comando.equals("tudo")) custo();
        if (comando.equals("classes") || comando.equals("tudo")) classes();
    }

    private static void definicao() {
        System.out.println();
        System.out.println("OS TRES CASADORES CONTRA O AUTOMATO, par por par.");
        System.out.println();

        var linhas = Medidas.contraOAutomato();
        String[] nomes = linhas.get(0).casadores().keySet().toArray(new String[0]);

        StringBuilder cabecalho = new StringBuilder(String.format(
                "  %-24s %-20s %-16s %10s", "caso", "padrao", "texto", "automato"));

        for (String n : nomes) cabecalho.append(String.format(" %16s", n));

        cabecalho.append(String.format(" %11s %10s", "concordam", "passos"));
        System.out.println(cabecalho);

        for (var l : linhas) {
            StringBuilder linha = new StringBuilder(String.format(
                    "  %-24s %-20s %-16s %10s", l.caso(), l.padrao(), l.texto(), sim(l.peloAutomato())));

            for (String n : nomes) linha.append(String.format(" %16s", sim(l.casadores().get(n))));

            linha.append(String.format(" %11s %10d", l.todosConcordam() ? "sim" : "NAO", l.passosDoAutomato()));
            System.out.println(linha);
        }

        System.out.println();
    }

    private static void exaustiva() {
        System.out.println();
        System.out.println("A ENUMERACAO EXAUSTIVA: todos os textos ate 8 caracteres sobre abc.");
        System.out.println();
        System.out.printf("  %-14s %10s %12s %14s %18s%n",
                "padrao", "textos", "casaram", "concordam", "discordancias");

        for (var l : Medidas.aEnumeracaoExaustiva("abc", 8))
            System.out.printf("  %-14s %10d %12d %14s %18d%n",
                    l.padrao(), l.textos(), l.casaram(),
                    l.todosConcordam() ? "sim" : "NAO", l.maiorDiscordancia());

        System.out.println();
    }

    private static void catastrofe() {
        System.out.println();
        System.out.println("O RETROCESSO CATASTROFICO: a*a*a*a*b contra um texto so de letras a.");
        System.out.println();
        System.out.printf("  %8s %18s %12s %16s %12s %8s%n",
                "letras", "retrocesso", "tabela", "dois ponteiros", "automato", "casou");

        for (var l : Medidas.aCatastrofe())
            System.out.printf("  %8d %18s %12d %16d %12d %8s%n",
                    l.letras(), l.retrocesso(), l.tabela(), l.doisPonteiros(), l.automato(), sim(l.casou()));

        System.out.println();
        System.out.println("O MESMO TEXTO de 30 letras, com um numero crescente de asteriscos.");
        System.out.println();
        System.out.printf("  %9s %-18s %8s %18s %12s %16s %12s%n",
                "estrelas", "padrao", "letras", "retrocesso", "tabela", "dois ponteiros", "automato");

        for (var l : Medidas.porNumeroDeEstrelas(30))
            System.out.printf("  %9d %-18s %8d %18s %12d %16d %12d%n",
                    l.estrelas(), l.padrao(), l.letras(), l.retrocesso(),
                    l.tabela(), l.doisPonteiros(), l.automato());

        System.out.println();
    }

    private static void custo() {
        System.out.println();
        System.out.println("O CUSTO de cada casador, par por par.");
        System.out.println();

        var linhas = Medidas.oCusto();
        String[] nomes = linhas.get(0).passos().keySet().toArray(new String[0]);

        StringBuilder cabecalho = new StringBuilder(String.format(
                "  %-24s %8s %8s", "caso", "pedacos", "letras"));

        for (String n : nomes) cabecalho.append(String.format(" %16s", n));

        System.out.println(cabecalho);

        for (var l : linhas) {
            StringBuilder linha = new StringBuilder(String.format(
                    "  %-24s %8d %8d", l.caso(), l.pedacos(), l.letras()));

            for (String n : nomes) linha.append(String.format(" %16d", l.passos().get(n)));

            System.out.println(linha);
        }

        System.out.println();
    }

    private static void classes() {
        System.out.println();
        System.out.println("AS CLASSES DE CARACTERES, testadas letra por letra em abcxyz019-]^!.");
        System.out.println();
        System.out.printf("  %-14s %8s %8s %-16s %-16s%n", "padrao", "pedacos", "negada", "aceita", "recusa");

        for (var l : Medidas.asClasses())
            System.out.printf("  %-14s %8d %8s %-16s %-16s%n",
                    l.padrao(), l.pedacos(), l.negada() ? "sim" : "nao", l.aceita(), l.recusa());

        System.out.println();
    }

    private static String sim(boolean valor) {
        return valor ? "sim" : "nao";
    }
}
