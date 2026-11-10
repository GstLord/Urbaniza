# Justificativa Técnica e Arquitetural

Este documento completa a seção 4 da 1ª entrega, que registrava apenas tópicos de levantamento. Cada escolha tecnológica é defendida a partir dos requisitos funcionais RF01 a RF07 e das regras de negócio que os sustentam.

## 1. Requisitos que orientam as escolhas

| Código | Requisito funcional (texto da 1ª entrega) |
|--------|-------------------------------------------|
| RF01 | O sistema deve permitir registrar e classificar a aptidão de uma região ao parcelamento (apta, apta com restrição ou inapta) a partir das vedações legais (alagamento, declividade, condições geológicas, áreas de preservação). |
| RF02 | O sistema deve permitir cadastrar loteamentos vinculados obrigatoriamente a uma região previamente cadastrada. |
| RF03 | O sistema deve permitir cadastrar equipamentos comunitários (escolas e unidades de saúde) com sua capacidade instalada e calcular o índice de atendimento de cada um frente à população da região. |
| RF04 | O sistema deve permitir calcular a população projetada de uma região, somando a população atual à população estimada dos loteamentos já aprovados e ainda não ocupados. |
| RF05 | O sistema deve permitir calcular e classificar o Índice de Área Verde por Habitante (IAV) da região (adequado, aceitável ou deficitário). |
| RF06 | O sistema deve permitir validar as dimensões mínimas do lote (área e frente) no cadastro do loteamento. |
| RF07 | O sistema deve permitir registrar, em cada análise emitida, a data, o responsável e a versão dos dados utilizados. |

## 2. Linguagem: Java 21

**Tipagem estática.** Os cálculos do sistema (população projetada, IAV, taxa de impermeabilização, índice de atendimento) combinam inteiros, reais e enumerações. Com tipagem estática, um erro como somar área (m²) a população, ou devolver texto onde se espera uma classificação, é detectado na compilação. As classificações de RF01 e RF05 são enumerações (`AptidaoParcelamento`, `ClassificacaoIAV`, `PotencialIlhaCalor`), e o compilador impede valores fora do domínio previsto.

**Suporte nativo aos pilares de POO exigidos.** O domínio levantado na 1ª entrega já é naturalmente hierárquico, e a linguagem oferece os recursos necessários sem bibliotecas adicionais:

- **Classes abstratas:** `EquipamentoComunitario` declara `calcularDemanda(int)` sem implementá-lo, porque cada tipo de equipamento tem fórmula própria (RN12, base de RF03). `CriterioVedacao` declara `avaliar(Regiao)`, porque cada vedação da Lei 6.766/79 é verificada de forma diferente (RN01 a RN04, base de RF01).
- **Herança:** `Escola` e `UnidadeSaude` estendem `EquipamentoComunitario`; `CriterioAlagamento`, `CriterioDeclividade`, `CriterioGeologico` e `CriterioPreservacao` estendem `CriterioVedacao`.
- **Polimorfismo com `@Override`:** `AnaliseParcelamento.avaliarAptidao()` percorre a lista de critérios da região chamando `criterio.avaliar(regiao)` sem saber qual vedação concreta está avaliando; `EquipamentoComunitario.calcularIndiceAtendimento(int)` chama `calcularDemanda(int)` da subclasse. A anotação `@Override` faz o compilador garantir que a subclasse realmente sobrescreve o método da classe-mãe.
- **Interfaces:** `Dao<T>` e `CrudDao<T>` definem o contrato de persistência, implementado por cada DAO.
- **Composição 1:N:** `Regiao` contém `List<Loteamento>` e `Loteamento` contém `List<Lote>`, com coleções tipadas por *generics*.

**Versão LTS.** O Java 21 é uma versão de suporte estendido (*Long-Term Support*), o que garante atualizações de segurança durante toda a vida acadêmica do projeto e compatibilidade com as ferramentas de build e teste adotadas.

**Portabilidade entre sistemas operacionais.** O código é compilado para *bytecode* e executado pela JVM, de modo que o mesmo arquivo `urbaniza.jar` roda sem alteração em Windows, Linux e macOS. Esse ponto atende diretamente à orientação da disciplina de Sistemas Operacionais sobre compatibilidade de ambientes: a única exigência para o usuário é ter o JDK 21 instalado, e as instruções de execução são as mesmas nos três sistemas (ver [guia de execução](guia-de-execucao.md)).

## 3. Banco de dados: PostgreSQL 16

O PostgreSQL foi escolhido porque várias regras de negócio podem ser garantidas pelo próprio banco, e não apenas pela aplicação. Isso dá uma segunda linha de defesa: mesmo um dado inserido diretamente por SQL respeita as regras.

| Recurso do PostgreSQL | Regra garantida | Requisito |
|-----------------------|-----------------|-----------|
| Chave estrangeira `loteamento.regiao_id NOT NULL` referenciando `regiao(id)` | RN17: loteamento só existe vinculado a uma região previamente cadastrada | RF02 |
| `CHECK (area_m2 >= 125)` e `CHECK (frente_m >= 5)` na tabela `lote` | RN05: dimensões mínimas do lote | RF06 |
| `CHECK (tipo IN (...))` em `equipamento_comunitario`, `criterio_vedacao` e `analise_parcelamento.resultado` | Domínio fechado de tipos e de resultados de aptidão (RN15) | RF01, RF03 |
| `CHECK (area_verde_m2 + area_impermeavel_m2 <= area_total_m2)` em `regiao` | Coerência dos dados usados por RN08, RN09 e RN10 | RF05 |
| Transações (`BEGIN`/`COMMIT`/`ROLLBACK`) | Gravação atômica de classe-mãe + subtabela (equipamentos e critérios), de loteamento + lotes e de análise + resultados por critério | RF01, RF02, RF03, RF07 |
| `ON DELETE CASCADE` | Composição: excluir a região apaga seus loteamentos, lotes e critérios; excluir o loteamento apaga seus lotes | RF02 |
| `ON DELETE SET NULL` | Agregação: excluir a região mantém os equipamentos, sem vínculo (`regiao_id = NULL`) | RF03 |
| `ON DELETE RESTRICT` | Rastreabilidade (RN16): região, analista e critério usados em análise emitida não podem ser excluídos | RF07 |

Além disso, o PostgreSQL é gratuito e de código aberto, sem custo de licença para a Secretaria Municipal, possui imagem oficial para Docker e driver JDBC mantido pelo próprio projeto (`org.postgresql:postgresql`).

## 4. Acesso a dados: JDBC puro

O acesso ao banco é feito com JDBC (`java.sql`), usando exclusivamente `PreparedStatement`. As razões são:

- **SQL explícito.** Cada comando executado pela aplicação está escrito no DAO correspondente e pode ser lido e conferido. Não há geração automática de SQL nem comportamento oculto de um framework de mapeamento objeto-relacional.
- **Compatibilidade 1:1 com o script de `/database`.** As diretrizes da AEP exigem que o script de criação das tabelas seja compatível com as classes Java. Como os nomes de tabelas e colunas aparecem literalmente nos DAOs, a conferência entre `01_schema.sql` e o código é direta.
- **Herança mapeada de forma visível.** A estratégia de tabela por subclasse com coluna `tipo` (ver [decisões de modelagem](decisoes-de-modelagem.md)) é implementada pelos próprios DAOs: gravação da linha-mãe e da subtabela na mesma transação e leitura com `LEFT JOIN`, instanciando `Escola`, `UnidadeSaude` ou o critério concreto conforme a coluna `tipo`.
- **Segurança.** O uso exclusivo de `PreparedStatement` impede injeção de SQL, pois nenhum dado digitado pelo usuário é concatenado ao comando.
- **Persistência real.** Todas as operações de CRUD são gravadas no PostgreSQL; nenhuma lista em memória substitui o banco.

## 5. Arquitetura em camadas

```
ui (console)  ->  service  ->  dao (JDBC)  ->  PostgreSQL 16
        \            |            /
         +------  model (domínio)  ------+
```

| Camada | Pacote | Responsabilidade |
|--------|--------|------------------|
| Interface | `br.com.urbaniza.ui` | Ler dados do usuário e exibir resultados. Não contém SQL nem regra de negócio. |
| Serviço | `br.com.urbaniza.service` | Validar as regras que dependem do banco (RN17: região existe) e da entrada (RN05: dimensões dos lotes; soma das áreas dos lotes não supera a gleba), orquestrar a emissão da análise. |
| Persistência | `br.com.urbaniza.dao` | Traduzir objetos em linhas e linhas em objetos, com transações. |
| Domínio | `br.com.urbaniza.model` | Representar as entidades e executar os cálculos das regras RN05 e RN08 a RN16. |

**Por que a regra de negócio fica no modelo e no serviço:**

- **Não fica na interface** porque a interface de console poderia ser substituída (por exemplo, por uma interface web) sem que os cálculos mudassem. Se o IAV fosse calculado no menu, uma segunda interface teria de duplicar a fórmula, com risco de divergência.
- **Não fica no banco** porque as regras são cálculos de domínio com classificação (RN08, RN10, RN11, RN15) que precisam ser testados isoladamente com JUnit, sem banco no ar, e que dependem de polimorfismo (cada critério e cada equipamento calcula de forma diferente). O banco guarda as **restrições de integridade** (RN05, RN17, RN16 por `RESTRICT`), que são invariantes dos dados; os **cálculos** ficam em Java.
- **Fica no modelo** quando é uma propriedade das próprias entidades: `Regiao.calcularPopulacaoProjetada()` (RN13), `Regiao.calcularIAV()` (RN08), `Lote.atendeDimensoesMinimas()` (RN05), `AnaliseParcelamento.avaliarAptidao()` (RN15).
- **Fica no serviço** quando depende de consultas ao banco ou da coordenação de várias entidades: `LoteamentoService` verifica se a região existe (RN17) antes de gravar; `AnaliseService.emitir(long, long)` carrega a região completa, os critérios e o analista, chama `executar()` e persiste a análise (RF07).

Os parâmetros numéricos das regras (125 m², 5 m, 30%, 12 e 9 m²/hab, 30% e 15% de cobertura vegetal) ficam centralizados na classe `ParametrosUrbanisticos`, com origem documentada em [decisões de modelagem](decisoes-de-modelagem.md).

## 6. Ferramentas de apoio

**Maven 3.9+.** Garante build reprodutível: as versões das dependências (`org.postgresql:postgresql:42.7.4` e `org.junit.jupiter:junit-jupiter:5.11.3`) e dos plugins ficam fixadas no `pom.xml`, e o comando `mvn clean package` compila, executa os testes e gera o jar executável `target/urbaniza.jar` com o driver JDBC embutido. Qualquer integrante da equipe ou o professor obtém o mesmo artefato a partir do mesmo código.

**Docker Compose.** O arquivo `Urbaniza/database/docker-compose.yml` sobe o PostgreSQL 16 com banco, usuário e senha já configurados e executa automaticamente `01_schema.sql` e `02_dados_exemplo.sql` na primeira criação do volume. O ambiente de banco é idêntico em Windows, Linux e macOS, eliminando diferenças de instalação entre as máquinas da equipe e do avaliador. A alternativa sem Docker (PostgreSQL 16 local) está documentada no [guia de execução](guia-de-execucao.md).

**JUnit 5.** Os testes automatizados verificam as regras de negócio com os dados fictícios de exemplo e seus resultados esperados (por exemplo, Região Norte: população projetada 20.400, IAV 8,82 deficitário, resultado apta com restrição). Os mesmos números são conferidos pelo SQL de verificação e pelo roteiro de demonstração, o que dá uma referência única de aceitação para RF01, RF03, RF04 e RF05. Os testes de integração com o banco são ativados pela variável de ambiente `URBANIZA_IT=true`.

**Interface de console.** As Diretrizes Gerais da AEP informam que não será cobrada interface gráfica avançada e que a avaliação foca em fundamentação técnica, funcionamento, orientação a objetos e persistência. Uma interface de console em texto atende a todas as operações de CRUD e à emissão de análises, roda em qualquer terminal dos três sistemas operacionais e mantém o esforço da equipe concentrado nas regras de negócio.

## 7. Evolução futura (fora do escopo desta entrega)

A 1ª entrega registrou como decisão em aberto a integração com uma fonte externa de dados ambientais e sinalizou que ela poderia ficar como evolução futura. Esta entrega confirma essa decisão: **os itens abaixo estão explicitamente fora do escopo da 2ª entrega** e não fazem parte de RF01 a RF07.

- **Integração com a API de qualidade do ar da plataforma IQAir.** A plataforma, de origem suíça, agrega dados de mais de 30 mil sensores terrestres e poderia fornecer, para cada região, um dado medido de qualidade do ar.
- **Previsão por aprendizado de máquina.** A literatura recente trata a previsão de qualidade do ar majoritariamente por aprendizado de máquina sobre séries temporais de sensores, evitando o custo computacional das simulações atmosféricas tradicionais. Um modelo desse tipo poderia, no futuro, estimar o efeito de novos loteamentos sobre a qualidade do ar de uma região.

**Distinção entre dado medido e estimativa.** Uma consulta à API de qualidade do ar retornaria o **estado atual medido** da região. Já os indicadores produzidos pelo Urbaniza — IAV (RN08), taxa de impermeabilização (RN09), potencial de ilha de calor (RN10), índice de atendimento (RN11) e população projetada (RN13) — são **estimativas** derivadas dos dados cadastrados e devem ser apresentados como tal, nunca como previsão certificada. Por isso, todo parecer emitido pelo sistema contém a frase: "Indicadores estimados a partir dos dados cadastrados; não substituem o parecer técnico nem constituem previsão certificada."
