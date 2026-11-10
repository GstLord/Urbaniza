# Checklist das Diretrizes Gerais da AEP — 2ª Entrega

Conferência de cada critério das Diretrizes Gerais da AEP do 4º semestre (2026.2) para a 2ª entrega. A coluna "Evidência (arquivo)" traz os caminhos previstos pelo contrato técnico; a localização exata (arquivo e linha) será confirmada na conferência final da documentação, com base no código entregue.

## 1. Critérios de pontuação

| Critério | Peso | Métrica de aceitação (diretriz) | Como o Urbaniza atende | Evidência (arquivo) |
|----------|------|---------------------------------|------------------------|---------------------|
| Aderência ao Planejamento do Projeto | 0,2 | O código entregue deve refletir os requisitos funcionais e funcionalidades listados na 1ª entrega. | Os requisitos RF01 a RF07 da 1ª entrega estão implementados com o mesmo texto, as mesmas regras de negócio (RN01 a RN17) e as mesmas classes e tabelas do Diagrama de Classes e do DER. As extensões são mínimas e justificadas (E1 a E6). Cada requisito é rastreado até classe, tabela e opção de menu. | `README.md`; `Urbaniza/docs/rastreabilidade.md`; `Urbaniza/docs/decisoes-de-modelagem.md`; `Urbaniza/docs/diagrama-classes.puml`; `Urbaniza/docs/der.md` |
| Pilares de POO — 1) Classe abstrata ou interface | 0,3 (critério compartilhado) | Uso obrigatório de classe abstrata ou interface. | Classes abstratas `EquipamentoComunitario`, `CriterioVedacao` e `MenuCrud<T>`; interfaces `Dao<T>` e `CrudDao<T>`. | `Urbaniza/src/main/java/br/com/urbaniza/model/equipamento/EquipamentoComunitario.java`; `Urbaniza/src/main/java/br/com/urbaniza/model/criterio/CriterioVedacao.java`; `Urbaniza/src/main/java/br/com/urbaniza/ui/MenuCrud.java`; `Urbaniza/src/main/java/br/com/urbaniza/dao/Dao.java`; `Urbaniza/src/main/java/br/com/urbaniza/dao/CrudDao.java` |
| Pilares de POO — 2) Herança | (idem) | Uso obrigatório de herança. | `Escola` e `UnidadeSaude` estendem `EquipamentoComunitario`; `CriterioAlagamento`, `CriterioDeclividade`, `CriterioGeologico` e `CriterioPreservacao` estendem `CriterioVedacao`; `MenuRegiao`, `MenuLoteamento`, `MenuEquipamento`, `MenuAnalista` e `MenuCriterio` estendem `MenuCrud<T>`; `CrudDao<T>` estende `Dao<T>`. | `Urbaniza/src/main/java/br/com/urbaniza/model/equipamento/Escola.java`; `.../equipamento/UnidadeSaude.java`; `Urbaniza/src/main/java/br/com/urbaniza/model/criterio/CriterioAlagamento.java`; `.../criterio/CriterioDeclividade.java`; `.../criterio/CriterioGeologico.java`; `.../criterio/CriterioPreservacao.java`; `Urbaniza/src/main/java/br/com/urbaniza/ui/MenuRegiao.java` e demais menus |
| Pilares de POO — 3) Polimorfismo via `@Override` | (idem) | Uso obrigatório de polimorfismo via `@Override`. | `calcularDemanda(int)` e `getTipo()` sobrescritos em `Escola` e `UnidadeSaude`; `avaliar(Regiao)`, `isImpedimentoAbsoluto()`, `getTipo()` e `descreverImpedimento()` sobrescritos nos quatro critérios; métodos de `MenuCrud<T>` sobrescritos nos menus. Chamadas polimórficas em `AnaliseParcelamento.avaliarAptidao()` e `EquipamentoComunitario.calcularIndiceAtendimento(int)`. | `Urbaniza/src/main/java/br/com/urbaniza/model/AnaliseParcelamento.java`; `Urbaniza/src/main/java/br/com/urbaniza/model/equipamento/EquipamentoComunitario.java`; subclasses listadas acima |
| Pilares de POO — código tipado e convenções | (idem) | O código deve ser tipado e seguir convenções (CamelCase). | Java 21 com tipagem estática, *generics* sem tipos brutos (`List<Loteamento>`, `Dao<T>`), classes em PascalCase, métodos e atributos em camelCase, constantes em UPPER_SNAKE_CASE (`ParametrosUrbanisticos`), atributos privados com getters e setters. | `Urbaniza/src/main/java/br/com/urbaniza/` (todo o código); `Urbaniza/src/main/java/br/com/urbaniza/model/ParametrosUrbanisticos.java` |
| CRUD Funcional | 0,3 | Operações de Create, Read, Update e Delete operando sobre a entidade principal definida no escopo da 1ª entrega. | CRUD completo de Região (entidade principal), Loteamento (com lotes), Equipamento comunitário, Analista e Critério de vedação, pelos menus de console e pelos DAOs JDBC. A análise de parcelamento tem emissão, listagem, detalhamento e exclusão (sem atualização, por ser imutável — RN16). | `Urbaniza/src/main/java/br/com/urbaniza/ui/MenuCrud.java`; `Urbaniza/src/main/java/br/com/urbaniza/ui/MenuRegiao.java`; `Urbaniza/src/main/java/br/com/urbaniza/dao/RegiaoDao.java`; `Urbaniza/src/main/java/br/com/urbaniza/service/RegiaoService.java`; `Urbaniza/docs/roteiro-de-demonstracao.md` (passo a) |
| Persistência em BD | 0,2 | Conexão ativa com banco de dados. O script SQL de criação das tabelas deve estar na pasta `/database` e ser compatível com as classes Java. | PostgreSQL 16 acessado por JDBC puro (`PreparedStatement`, transações); nenhuma lista em memória substitui o banco. Script de criação em `Urbaniza/database/01_schema.sql`, com as 14 tabelas mapeadas 1:1 para as classes Java (herança por tabela por subclasse com coluna `tipo`). | `Urbaniza/database/01_schema.sql`; `Urbaniza/database/02_dados_exemplo.sql`; `Urbaniza/database/docker-compose.yml`; `Urbaniza/src/main/java/br/com/urbaniza/config/ConnectionFactory.java`; `Urbaniza/src/main/java/br/com/urbaniza/dao/`; `Urbaniza/docs/der.md` |

## 2. Condições de nota zero automática na 2ª entrega

| Condição | Como o Urbaniza evita | Evidência (arquivo) |
|----------|-----------------------|---------------------|
| O software não executar | Build reprodutível com Maven gerando jar executável; banco em contêiner com scripts de inicialização; instruções de execução para Windows, Linux e macOS; testes automatizados com JUnit 5. | `Urbaniza/pom.xml`; `Urbaniza/database/docker-compose.yml`; `README.md`; `Urbaniza/docs/guia-de-execucao.md`; `Urbaniza/src/test/java/` |
| Pilares de POO ignorados (código procedural) | Domínio modelado em classes com classes abstratas, interfaces, herança, polimorfismo via `@Override` e composição 1:N; arquitetura em camadas (ui, service, dao, model). | `Urbaniza/src/main/java/br/com/urbaniza/model/`; `Urbaniza/docs/diagrama-classes.puml` |
| Entrega divergir 100% do planejado no 1º bimestre | Mesmo tema, mesmos requisitos RF01 a RF07, mesmas regras de negócio e mesmas classes e tabelas da 1ª entrega; extensões mínimas e documentadas. | `Urbaniza/docs/rastreabilidade.md`; `Urbaniza/docs/decisoes-de-modelagem.md` |

## 3. Fundamentação para atribuição de nota baixa

| Item | Como o Urbaniza atende | Evidência (arquivo) |
|------|------------------------|---------------------|
| Falta de requisitos | Lista RF01 a RF07 no README, com o texto literal da 1ª entrega. | `README.md` (seção "Requisitos Funcionais"); `Urbaniza/README.md` |
| Falta de cronograma | Tabelas de cronograma da 1ª e da 2ª entrega (data, atividade e responsável) no README. | `README.md` (seções "Cronograma de Execução"); `Urbaniza/README.md` |
| Inconsistência entre 1ª e 2ª entrega | Matriz de rastreabilidade e registro das extensões em relação à 1ª entrega. | `Urbaniza/docs/rastreabilidade.md`; `Urbaniza/docs/decisoes-de-modelagem.md` |
| Simulação de CRUD (listas em memória) | Todas as operações passam pelos DAOs JDBC e são gravadas no PostgreSQL; a demonstração inclui consulta SQL direta ao banco. | `Urbaniza/src/main/java/br/com/urbaniza/dao/`; `Urbaniza/docs/roteiro-de-demonstracao.md` (passo f) |

## 4. Orientações finais aos alunos

| Orientação | Como o Urbaniza atende | Evidência (arquivo) |
|------------|------------------------|---------------------|
| Independência de disciplinas: não é cobrada interface gráfica avançada; o foco é fundamentação técnica, funcionamento, orientação a objetos e persistência | Interface de console em texto; esforço concentrado nas regras de negócio, na orientação a objetos e na persistência. Justificativa técnica completa. | `Urbaniza/src/main/java/br/com/urbaniza/ui/`; `Urbaniza/docs/justificativa-tecnica.md` |
| Objetividade na correção: a ausência de um pilar de POO zera o critério | Os três pilares estão presentes e documentados com localização no código. | Seção 1 deste checklist; `Urbaniza/docs/diagrama-classes.puml` |
| Ambiente de Sistemas Operacionais: instruções claras de execução e instalação de dependências, respeitando a compatibilidade de ambientes | Instruções separadas para Windows (PowerShell), Linux e macOS, com links oficiais de instalação, verificação de versões, alternativa sem Docker e solução de problemas. A JVM e o Docker garantem o mesmo comportamento nos três sistemas. | `README.md` (seção "Como executar"); `Urbaniza/docs/guia-de-execucao.md` |
| Contribuição de todos no repositório | Cada integrante faz os commits da própria parte com a própria conta, conforme o cronograma da 2ª entrega (Gustavo: domínio e serviços; Miguel: banco de dados e persistência; João: interface de console e documentação). | Histórico do repositório (`git log`); `README.md` (seção "Cronograma de Execução — 2ª entrega") |
| Plágio: estrutura de classes, nomes e contexto idênticos entre grupos são zerados | Tema, regras e modelo derivados do levantamento próprio da 1ª entrega (Lei 6.766/79, personas e dores levantadas pela equipe); nomes de classes e tabelas específicos do domínio do projeto. | `Urbaniza/docs/rastreabilidade.md`; `Urbaniza/docs/decisoes-de-modelagem.md` |

## 5. Requisitos técnicos da 2ª entrega (seção II das diretrizes)

| Requisito | Como o Urbaniza atende | Evidência (arquivo) |
|-----------|------------------------|---------------------|
| Código-fonte completo da aplicação | Projeto Maven completo em `Urbaniza/`, com código, recursos e testes. | `Urbaniza/pom.xml`; `Urbaniza/src/` |
| Aplicação prática dos pilares de POO (herança, composição, polimorfismo etc.) | Herança e polimorfismo nas hierarquias de equipamentos, critérios e menus; composição 1:N em `Regiao`/`Loteamento` e `Loteamento`/`Lote`; agregação em `Regiao`/`EquipamentoComunitario`. | `Urbaniza/src/main/java/br/com/urbaniza/model/Regiao.java`; `Urbaniza/src/main/java/br/com/urbaniza/model/Loteamento.java` |
| Operações de CRUD completas | Ver critério "CRUD Funcional". | `Urbaniza/src/main/java/br/com/urbaniza/ui/`; `Urbaniza/src/main/java/br/com/urbaniza/dao/` |
| Conexão ativa e integração com banco de dados | Ver critério "Persistência em BD". | `Urbaniza/src/main/java/br/com/urbaniza/config/ConnectionFactory.java` |
| Contribuição de TODOS no repositório | Ver orientação "Contribuição de todos no repositório". | `git log` |
