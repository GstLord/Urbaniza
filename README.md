# Urbaniza — Sistema de Controle de Urbanização por Regiões

**AEP 2026.2 — Engenharia de Software / ADS**

**Equipe:** Gustavo, Miguel, João

**Repositório:** [https://github.com/GstLord/Urbaniza](https://github.com/GstLord/Urbaniza)

---

## Sobre o projeto

O Urbaniza é uma aplicação de controle de urbanização organizada por regiões, destinada a apoiar a Secretaria Municipal de Planejamento Urbano na análise de novos loteamentos, com base na Lei Federal nº 6.766/1979 (parcelamento do solo urbano). O sistema mantém o cadastro das regiões do município — população, área total, área verde e equipamentos comunitários instalados — e dos loteamentos vinculados a cada região.

A partir desses dados, o sistema aplica as regras de negócio levantadas na 1ª entrega para produzir três informações que hoje não estão disponíveis de forma consolidada:

- a **aptidão da área ao parcelamento** diante das vedações legais (apta, apta com restrição ou inapta);
- o **índice de atendimento dos equipamentos comunitários** (escolas e unidades de saúde) frente à população projetada;
- o **Índice de Área Verde por Habitante (IAV)** da região.

O sistema **não substitui o parecer técnico** do analista nem os instrumentos formais de licenciamento. Sua função é fornecer, de forma consolidada e rastreável, os indicadores que hoje precisariam ser reunidos manualmente a partir de fontes dispersas entre setores.

## Alinhamento com os ODS

| ODS | Relação com o projeto |
|-----|-----------------------|
| ODS 11 — Cidades e Comunidades Sustentáveis (principal) | Atua diretamente sobre a capacidade de planejamento urbano do município e sobre o acompanhamento do acesso da população a áreas verdes e a equipamentos públicos, tornando mensurável o efeito das decisões de expansão urbana. |
| ODS 3 — Saúde e Bem-Estar | O cálculo de déficit de unidades de saúde por região apoia a identificação antecipada de áreas em que o crescimento populacional aprovado não é acompanhado por capacidade de atendimento. |
| ODS 4 — Educação de Qualidade | O mesmo cálculo aplicado a escolas permite antecipar a necessidade de ampliação da rede de ensino antes da sobrecarga. |
| ODS 13 — Ação Climática | A classificação de potencial de ilha de calor e o acompanhamento da cobertura vegetal por região conectam o planejamento territorial à adaptação climática urbana. |

## Requisitos Funcionais

| Código | Requisito | Regras de negócio |
|--------|-----------|-------------------|
| RF01 | O sistema deve permitir registrar e classificar a aptidão de uma região ao parcelamento (apta, apta com restrição ou inapta) a partir das vedações legais (alagamento, declividade, condições geológicas, áreas de preservação). | RN01–RN04, RN15 |
| RF02 | O sistema deve permitir cadastrar loteamentos vinculados obrigatoriamente a uma região previamente cadastrada. | RN17 |
| RF03 | O sistema deve permitir cadastrar equipamentos comunitários (escolas e unidades de saúde) com sua capacidade instalada e calcular o índice de atendimento de cada um frente à população da região. | RN11, RN12 |
| RF04 | O sistema deve permitir calcular a população projetada de uma região, somando a população atual à população estimada dos loteamentos já aprovados e ainda não ocupados. | RN13, RN14 |
| RF05 | O sistema deve permitir calcular e classificar o Índice de Área Verde por Habitante (IAV) da região (adequado, aceitável ou deficitário). | RN08 |
| RF06 | O sistema deve permitir validar as dimensões mínimas do lote (área e frente) no cadastro do loteamento. | RN05 |
| RF07 | O sistema deve permitir registrar, em cada análise emitida, a data, o responsável e a versão dos dados utilizados. | RN16 |

## Regras de negócio

| Código | Regra | Origem |
|--------|-------|--------|
| RN01 | Terrenos alagadiços ou sujeitos a inundação não podem ser parcelados antes do saneamento. | Lei 6.766/79, art. 3º, parágrafo único, I |
| RN02 | Terrenos com declividade igual ou superior a 30% não podem ser parcelados, salvo exigências específicas. | Lei 6.766/79, art. 3º, parágrafo único, III |
| RN03 | Terrenos com condições geológicas inadequadas à edificação não podem ser parcelados. | Lei 6.766/79, art. 3º, parágrafo único, IV |
| RN04 | Áreas de preservação ecológica não podem ser parceladas. | Lei 6.766/79, art. 3º, parágrafo único, V |
| RN05 | Lote mínimo de 125 m² com frente mínima de 5 m. | Lei 6.766/79, art. 4º, II |
| RN06 | Áreas destinadas a equipamento urbano e comunitário devem ser proporcionais à densidade de ocupação prevista. | Lei 6.766/79, art. 4º, I |
| RN07 | Equipamentos comunitários são os equipamentos públicos de educação, cultura, saúde, lazer e similares. | Lei 6.766/79, art. 4º, § 2º |
| RN08 | IAV = área verde / população. Acima de 12 m²/hab: adequado; de 9 a 12 m²/hab: aceitável; abaixo de 9 m²/hab: deficitário. | Literatura de indicadores urbanos; referência da OMS |
| RN09 | Taxa de impermeabilização = área impermeável / área total. | Derivada de RN01 e da literatura de drenagem urbana |
| RN10 | Potencial de ilha de calor (baixo, médio ou alto), classificado de forma qualitativa pela proporção de cobertura vegetal. | Literatura sobre cobertura vegetal e temperatura de superfície |
| RN11 | Índice de atendimento = capacidade instalada / demanda. Valor inferior a 1 caracteriza déficit. | Operacionalização de RN06 |
| RN12 | Escola: demanda pela população em idade escolar. Unidade de saúde: demanda pela população total. | Decorre de RN07 |
| RN13 | População projetada = população atual + população estimada dos loteamentos aprovados e ainda não ocupados da mesma região. | Dor D4 — ausência de visão cumulativa |
| RN14 | A análise de um novo loteamento considera a população projetada, e não a atual. | Dor D5 — postura reativa |
| RN15 | A região é classificada como apta, apta com restrição ou inapta conforme RN01 a RN04. | Dor D1 — riscos verificados manualmente |
| RN16 | Toda análise registra data, responsável e versão dos dados utilizados. | Dor D2 — dados defasados e sem rastreabilidade |
| RN17 | Um loteamento só pode ser vinculado a uma região previamente cadastrada. | Integridade referencial do domínio |

## Cronograma de Execução — 1ª entrega

| Data                    | Atividade                                                                  | Responsável         |
|---|---|---|
| 18/08/2026 – 19/08/2026 | Definição do tema e alinhamento com os ODS da ONU                          | Gustavo/Miguel/João |
| 20/08/2026 – 21/08/2026 | Pesquisa de legislação aplicável (Lei 6.766/79) e levantamento de fontes   | Gustavo             |
| 22/08/2026 – 24/08/2026 | Estudo do processo atual (AS-IS) de aprovação de loteamentos em secretarias municipais | Gustavo |
| 25/08/2026 – 26/08/2026 | Definição das partes interessadas e elaboração das personas                | Gustavo/João        |
| 27/08/2026 – 28/08/2026 | Identificação e documentação das dores e suas origens                      | Gustavo             |
| 29/08/2026 – 31/08/2026 | Elaboração do User Journey (AS-IS) e mapeamento de oportunidades           | Gustavo             |
| 01/09/2026 – 02/09/2026 | Definição das regras de negócio e da lista de requisitos                   | Miguel              |
| 03/09/2026 – 05/09/2026 | Modelagem do Diagrama de Classes (herança, composição 1:N e polimorfismo)  | Gustavo             |
| 06/09/2026 – 07/09/2026 | Modelagem do banco de dados e elaboração do DER                            | Miguel              |
| 08/09/2026 – 09/09/2026 | Redação da Justificativa Técnica e Arquitetural                            | Gustavo             |
| 10/09/2026 – 11/09/2026 | Revisão e formatação do documento da 1ª entrega                            | Gustavo/Miguel      |

## Cronograma de Execução — 2ª entrega

| Data                    | Atividade                                                                  | Responsável         |
|---|---|---|
| 09/11/2026              | Script SQL de criação (PostgreSQL), dados de exemplo e ambiente Docker     | Gustavo             |
| 09/11/2026 – 10/11/2026 | Implementação do modelo de domínio (POO) e testes unitários                | Gustavo             |
| 10/11/2026              | Camada de persistência JDBC (DAOs) e testes de integração                  | Gustavo             |
| 10/11/2026              | Camada de serviço, regras de negócio e interface de console (CRUD)         | Gustavo             |
| 10/11/2026              | Documentação técnica, guia de execução e README                            | Gustavo             |
| 10/11/2026              | Testes ponta a ponta com o banco e auditoria SQL x Java                    | Gustavo             |
| 11/11/2026              | Validação do banco e dos testes de integração em ambiente próprio          | Miguel              |
| 11/11/2026              | Revisão do guia de execução e do roteiro de demonstração                   | João                |
| 11/11/2026              | Revisão final e entrega                                                    | Gustavo/Miguel/João |

## Tecnologias

| Tecnologia | Justificativa |
|------------|---------------|
| Java 21 (LTS) | Tipagem estática e suporte nativo a classes abstratas, interfaces, herança e `@Override`, com execução idêntica em Windows, Linux e macOS pela JVM. |
| Maven 3.9+ | Build reprodutível, dependências com versões fixas e geração do jar executável `target/urbaniza.jar`. |
| PostgreSQL 16 | Banco relacional gratuito com chaves estrangeiras, restrições `CHECK` e transações que garantem as regras RN05, RN16 e RN17 no próprio banco. |
| JDBC (`java.sql` + `PreparedStatement`) | SQL explícito, sem ORM, mantendo o script de `/database` compatível 1:1 com as classes Java. |
| Docker Compose | Sobe o mesmo ambiente de banco em qualquer sistema operacional com um único comando. |
| JUnit 5 | Testes automatizados das regras de negócio com os dados de exemplo e os resultados esperados. |

Defesa completa das escolhas: [Justificativa técnica](Urbaniza/docs/justificativa-tecnica.md).

## Como executar (Windows, Linux e macOS)

### Pré-requisitos

- JDK 21 (verifique com `java -version`);
- Maven 3.9+ (verifique com `mvn -version`);
- Docker Desktop (Windows/macOS) ou Docker Engine com o plugin Compose (Linux) — **ou**, alternativamente, PostgreSQL 16 instalado localmente.

### Passo a passo com Docker

Cada comando abaixo parte da raiz do repositório (pasta que contém este README). Os comandos são os mesmos no PowerShell (Windows) e no bash/zsh (Linux e macOS).

1. Subir o banco de dados (na primeira criação do volume, os scripts `01_schema.sql` e `02_dados_exemplo.sql` são executados automaticamente):

   ```
   cd Urbaniza/database && docker compose up -d
   ```

2. A partir da raiz do repositório, compilar o projeto (gera `Urbaniza/target/urbaniza.jar`):

   ```
   cd Urbaniza && mvn clean package
   ```

3. A partir da raiz do repositório, executar a aplicação:

   ```
   java -jar Urbaniza/target/urbaniza.jar
   ```

Para recriar o banco do zero (apaga todos os dados e reexecuta os scripts):

```
cd Urbaniza/database && docker compose down -v && docker compose up -d
```

Observação para o Windows PowerShell 5.1: o operador `&&` não existe nessa versão. Use `;` no lugar (por exemplo, `cd Urbaniza/database; docker compose up -d`) ou utilize o PowerShell 7.

### Alternativa sem Docker (PostgreSQL 16 local)

Crie o usuário `urbaniza` (senha `urbaniza`) e o banco `urbaniza` no PostgreSQL local e execute, a partir da raiz do repositório:

```
psql -U urbaniza -d urbaniza -f Urbaniza/database/01_schema.sql
psql -U urbaniza -d urbaniza -f Urbaniza/database/02_dados_exemplo.sql
```

### Configuração da conexão (opcional)

Por padrão a aplicação conecta em `jdbc:postgresql://localhost:5432/urbaniza` com usuário `urbaniza` e senha `urbaniza` (arquivo `Urbaniza/src/main/resources/db.properties`). Para usar outro banco, defina as variáveis de ambiente abaixo, que têm prioridade sobre o arquivo.

Windows (PowerShell):

```powershell
$env:URBANIZA_DB_URL="jdbc:postgresql://localhost:5432/urbaniza"
$env:URBANIZA_DB_USER="urbaniza"
$env:URBANIZA_DB_PASSWORD="urbaniza"
```

Linux e macOS (bash/zsh):

```bash
export URBANIZA_DB_URL=jdbc:postgresql://localhost:5432/urbaniza
export URBANIZA_DB_USER=urbaniza
export URBANIZA_DB_PASSWORD=urbaniza
```

### Testes

Testes unitários (não precisam do banco), a partir da raiz do repositório:

```
cd Urbaniza && mvn test
```

Testes de integração com o PostgreSQL (exigem o banco no ar), a partir da pasta `Urbaniza`:

Windows (PowerShell):

```powershell
$env:URBANIZA_IT="true"; mvn test
```

Linux e macOS (bash/zsh):

```bash
URBANIZA_IT=true mvn test
```

### Problemas comuns

| Problema | Solução |
|----------|---------|
| Porta 5432 ocupada | Outro PostgreSQL já está em execução. Pare o serviço local ou altere o mapeamento de porta em `Urbaniza/database/docker-compose.yml` e ajuste `URBANIZA_DB_URL` para a nova porta. |
| Docker parado | Mensagens como "Cannot connect to the Docker daemon" indicam que o Docker Desktop/Engine não está em execução. Inicie-o e repita `docker compose up -d`. |
| `JAVA_HOME` incorreto | O Maven usa o JDK apontado por `JAVA_HOME`. Confira com `mvn -version` se a versão exibida é a 21 e, se não for, aponte a variável para a pasta de instalação do JDK 21. |
| Aplicação não conecta ao banco | Confira se o contêiner `urbaniza-db` está em execução (`docker ps`) e se as variáveis `URBANIZA_DB_*`, quando definidas, estão corretas. |

Guia detalhado por sistema operacional: [Guia de execução](Urbaniza/docs/guia-de-execucao.md).

## Estrutura do repositório

```
README.md                         Este arquivo
Urbaniza/                         Raiz do projeto de código
├── README.md                     Cópia deste README (links relativos a Urbaniza/)
├── pom.xml                       Build Maven (Java 21, driver PostgreSQL, JUnit 5, jar executável)
├── database/                     Banco de dados PostgreSQL 16
│   ├── docker-compose.yml        Serviço "postgres" (contêiner urbaniza-db, porta 5432)
│   ├── 01_schema.sql             Script de criação das 14 tabelas
│   └── 02_dados_exemplo.sql      Dados fictícios de exemplo (3 regiões)
├── docs/                         Documentação técnica da 2ª entrega
└── src/
    ├── main/java/br/com/urbaniza/
    │   ├── App.java              Ponto de entrada: testa a conexão e abre o menu
    │   ├── config/               ConnectionFactory (configuração da conexão)
    │   ├── model/                Regiao, Loteamento, Lote, Analista, AnaliseParcelamento,
    │   │                         ResultadoCriterio, ParametrosUrbanisticos
    │   │   ├── enums/            AptidaoParcelamento, ClassificacaoIAV, PotencialIlhaCalor
    │   │   ├── equipamento/      EquipamentoComunitario, Escola, UnidadeSaude
    │   │   └── criterio/         CriterioVedacao, CriterioAlagamento, CriterioDeclividade,
    │   │                         CriterioGeologico, CriterioPreservacao
    │   ├── exception/            RegraNegocioException, PersistenciaException
    │   ├── dao/                  Dao<T>, CrudDao<T> e os DAOs JDBC
    │   ├── service/              Validações e orquestração das regras de negócio
    │   └── ui/                   Menus de console
    ├── main/resources/           db.properties
    └── test/java/                Testes JUnit 5
```

## Arquitetura

A aplicação segue arquitetura em camadas; cada camada depende apenas da camada imediatamente abaixo, e todas usam as classes de domínio do pacote `model`:

```
ui (console)  ->  service  ->  dao (JDBC)  ->  PostgreSQL 16
        \            |            /
         +------  model (domínio)  ------+
```

| Camada | Pacote | Classes e responsabilidade |
|--------|--------|----------------------------|
| Interface | `br.com.urbaniza.ui` | `Entrada`, `MenuCrud<T>`, `MenuPrincipal`, `MenuRegiao`, `MenuLoteamento`, `MenuEquipamento`, `MenuAnalista`, `MenuCriterio`, `MenuAnalise`: leitura e exibição, sem SQL nem regra de negócio. |
| Serviço | `br.com.urbaniza.service` | `RegiaoService`, `LoteamentoService`, `EquipamentoService`, `AnalistaService`, `CriterioVedacaoService`, `AnaliseService`: validações (RN05, RN17) e orquestração da emissão da análise. |
| Persistência | `br.com.urbaniza.dao` | `Dao<T>`, `CrudDao<T>`, `RegiaoDao`, `LoteamentoDao`, `EquipamentoComunitarioDao`, `AnalistaDao`, `CriterioVedacaoDao`, `AnaliseParcelamentoDao`: JDBC puro com `PreparedStatement` e transações. |
| Domínio | `br.com.urbaniza.model` (+ `enums`, `equipamento`, `criterio`) | Entidades e cálculos das regras de negócio (RN05, RN08–RN16). |
| Configuração | `br.com.urbaniza.config` | `ConnectionFactory.getConnection()`. |
| Exceções | `br.com.urbaniza.exception` | `RegraNegocioException`, `PersistenciaException`. |

Pilares de Programação Orientada a Objetos:

| Pilar | Onde está |
|-------|-----------|
| Classes abstratas | `EquipamentoComunitario`, `CriterioVedacao`, `MenuCrud<T>` |
| Interfaces | `Dao<T>` e `CrudDao<T> extends Dao<T>`, implementadas pelos DAOs |
| Herança | `Escola` e `UnidadeSaude` estendem `EquipamentoComunitario`; `CriterioAlagamento`, `CriterioDeclividade`, `CriterioGeologico` e `CriterioPreservacao` estendem `CriterioVedacao`; `MenuRegiao`, `MenuLoteamento`, `MenuEquipamento`, `MenuAnalista` e `MenuCriterio` estendem `MenuCrud<T>` |
| Polimorfismo via `@Override` | `calcularDemanda(int)` e `getTipo()` em `Escola` e `UnidadeSaude`; `avaliar(Regiao)`, `isImpedimentoAbsoluto()`, `getTipo()` e `descreverImpedimento()` nos critérios; métodos de `MenuCrud<T>` nos menus. `AnaliseParcelamento.avaliarAptidao()` chama `criterio.avaliar(regiao)` sem conhecer a subclasse concreta, e `EquipamentoComunitario.calcularIndiceAtendimento(int)` chama `calcularDemanda(int)` da subclasse. |
| Composição 1:N | `Regiao` contém `List<Loteamento>`; `Loteamento` contém `List<Lote>` |
| Agregação | `Regiao` possui `List<EquipamentoComunitario>`; o equipamento sobrevive à exclusão da região |

## Documentação

| Documento | Conteúdo |
|-----------|----------|
| [Justificativa técnica](Urbaniza/docs/justificativa-tecnica.md) | Defesa da linguagem, do banco de dados, da arquitetura e das ferramentas. |
| [Decisões de modelagem](Urbaniza/docs/decisoes-de-modelagem.md) | Extensões em relação à 1ª entrega, mapeamento de herança e relacionamentos. |
| [Diagrama de classes](Urbaniza/docs/diagrama-classes.puml) | Diagrama de classes em PlantUML atualizado para a 2ª entrega. |
| [DER](Urbaniza/docs/der.md) | Diagrama entidade-relacionamento e ações de exclusão. |
| [Rastreabilidade](Urbaniza/docs/rastreabilidade.md) | Matriz dor, regra, requisito, classe, tabela e menu. |
| [Guia de execução](Urbaniza/docs/guia-de-execucao.md) | Passo a passo detalhado por sistema operacional. |
| [Roteiro de demonstração](Urbaniza/docs/roteiro-de-demonstracao.md) | Roteiro de apresentação com os dados de exemplo. |
| [Checklist das diretrizes](Urbaniza/docs/checklist-diretrizes.md) | Atendimento a cada critério das Diretrizes Gerais da AEP. |
