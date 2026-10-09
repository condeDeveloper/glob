# glob

Casamento de padrões glob do zero em Java 21: `*`, `?`, `[a-z]`, `[!abc]` e
contrabarra, com três algoritmos de casamento e um juiz independente.

O juiz é o **autômato**. Ele não casa padrão com texto: transforma o padrão numa
máquina de estados e deixa o texto andar por ela, mantendo o conjunto de estados
possíveis a cada caractere. É outro ramo da árvore de algoritmos, e é o único dos
quatro que **nunca retrocede**.

E os quatro são comparados em **todos** os textos até 8 caracteres sobre 3 letras:
9.841 textos contra 17 padrões, 167.297 comparações.

```
$ mvn exec:java -Dexec.args="exaustiva"

  padrao             textos      casaram      concordam      discordancias
  *                    9841         9841            sim                  0
  a*                   9841         3280            sim                  0
  *a*                  9841         9330            sim                  0
  a?b                  9841            3            sim                  0
  *a*b*                9841         7537            sim                  0
  a*b*c                9841          966            sim                  0
```

## Os três casadores

Todo o problema do glob está numa pergunta só: quantos caracteres o asterisco come?

- **Retrocesso**: tenta zero, depois um, depois dois, e volta atrás. É a tradução
  direta da definição.
- **Tabela**: para cada posição do padrão e cada posição do texto, casa dali para a
  frente? O mesmo problema sem repetir subproblema.
- **Dois ponteiros**: anda para a frente e guarda **um** ponto de volta, o do último
  asterisco. É o que está em quase toda biblioteca de glob.

## O retrocesso catastrófico

```
$ mvn exec:java -Dexec.args="catastrofe"

    letras         retrocesso       tabela   dois ponteiros     automato    casou
         4                 24           49                8           13      nao
        20              7.356          209               24          141      nao
        40            112.011          409               44          301      nao
        60            557.966          609               64          461      nao
       100          4.249.776         1009              104          781      nao
```

`a*a*a*a*b` contra um texto só de letras a. Ele não casa, e o retrocesso tenta
todas as maneiras de repartir o texto entre os quatro asteriscos antes de desistir.

Com 100 letras: **4,2 milhões de passos** contra 104 dos dois ponteiros.

```
   estrelas padrao               letras         retrocesso       tabela   dois ponteiros     automato
          1 a*b                      30                 32          123               31           59
          2 a*a*b                    30                496          185               32          115
          3 a*a*a*b                  30              4.962          247               33          169
          4 a*a*a*a*b                30             36.021          309               34          221
          5 a*a*a*a*a*b              30            202.278          371               35          271
          6 a*a*a*a*a*a*b            30            914.808          433               36          319
          7 a*a*a*a*a*a*a*b          30          3.425.628          495               37          365
```

O texto é o mesmo nas sete linhas. Só o padrão muda.

## As classes de caracteres

```
$ mvn exec:java -Dexec.args="classes"

  padrao          pedacos   negada aceita           recusa
  [abc]                 1      nao abc              xyz019-]^!
  [a-cx-z]              1      nao abcxyz           019-]^!
  [!abc]                1      sim xyz019-]^!       abc
  []abc]                1      nao abc]             xyz019-^!
  [a-]                  1      nao a-               bcxyz019]^!
  [-a]                  1      nao a-               bcxyz019]^!
```

Duas regras herdadas do shell: um colchete logo no começo da classe é **literal**, e
um traço na ponta também. Existem porque não há outra maneira de escrever esses dois
caracteres dentro de uma classe.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `definicao`, `exaustiva`, `catastrofe`, `custo`,
`classes` e `tudo`.

## O que as medidas me corrigiram

**A explosão do retrocesso não é exponencial no texto.** Eu escrevi "retrocesso
catastrófico" esperando ver os passos dobrarem a cada caractere a mais. Com um
número fixo de asteriscos ela é **polinomial**, e o grau do polinômio é o número de
asteriscos: com quatro asteriscos e 100 letras são 4.249.776 passos, que é
100⁴ dividido por 24.

O que explode de verdade é o **padrão**. Cada asterisco a mais multiplica o custo
pelo tamanho do texto, e isso muda o conselho prático: não adianta limitar o
tamanho do nome do arquivo, adianta limitar o número de asteriscos.

A segunda tabela da seção da catástrofe só existe por causa disso. A primeira, que
varia o texto, não separava as duas explicações.

**Colapsar asteriscos seguidos muda o custo e não muda a resposta.** `a***b` e `a*b`
casam exatamente o mesmo conjunto de textos, e o teste confere isso em todos os
textos até 7 caracteres. Mas com os três asteriscos separados o retrocesso custa
uma ordem de grandeza a mais.

Foi por isso que a leitura do padrão ganhou um parâmetro: as tabelas do retrocesso
precisam dos asteriscos **separados** para medir o que elas querem medir, e qualquer
uso de verdade quer os asteriscos colapsados.

**O caso que eu achei que separaria os dois ponteiros da tabela não separa.** A ideia
de guardar só o último asterisco parece frágil: e se voltar para o penúltimo fosse
necessário? Montei `*a*b*c*d` e `a*b*c*d*e` esperando pegar isso, e os dois ponteiros
acertam em todos os 19.531 textos de até 6 caracteres sobre 5 letras.

Lembrar do último asterisco basta porque voltar para um asterisco anterior nunca
ajuda: se o sufixo não casou a partir daqui, deixar um asterisco mais antigo comer
mais só adianta o ponto de partida do mesmo sufixo.

**O autômato é o mais lento dos três que não explodem.** Ele gasta mais passos que
os dois ponteiros em praticamente toda linha da tabela de custo, porque mantém um
conjunto de estados em vez de uma posição. Ele está aqui como juiz, não como
proposta: o que ele dá é a garantia de nunca retroceder, e essa garantia tem preço.

## Licença

MIT.
