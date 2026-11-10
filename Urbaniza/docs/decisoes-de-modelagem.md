# Decisões de Modelagem

Este documento registra as decisões de modelagem da 2ª entrega. Tudo o que foi planejado na 1ª entrega (Diagrama de Classes e DER) foi mantido integralmente; acrescentou-se apenas o mínimo necessário para que as regras de negócio planejadas se tornassem executáveis.

## 1. Extensões em relação à 1ª entrega

| Código | O que mudou | Por quê | Regra que viabiliza |
|--------|-------------|---------|---------------------|
| E1 | Nova coluna `regiao.declividade_media_pct` e novo atributo `Regiao.declividadeMediaPct`. | O critério de declividade precisa comparar o dado medido da região com o `declividadeLimite` do critério. O DER original não tinha onde guardar a declividade da região. | RN02 (declividade igual ou superior a 30%), RF01 |
| E2 | Nova coluna `criterio_vedacao.regiao_id` (FK para `regiao`) e novo atributo `CriterioVedacao.regiaoId`. | Os atributos das subclasses de critério (saneamento executado, laudo geotécnico, área protegida) são fatos de uma região específica. Sem esse vínculo não há como saber quais critérios aplicar na análise de cada região. | RN01 a RN04 e RN15, RF01 |
| E3 | Novo método abstrato `CriterioVedacao.isImpedimentoAbsoluto()`. | Operacionaliza RN15: a própria lei admite superação para alagamento (após o saneamento) e declividade (exigências específicas), o que resulta em apta com restrição; condições geológicas inadequadas e áreas de preservação são impeditivas, o que resulta em inapta. Reforça o polimorfismo. | RN15, RF01 |
| E4 | Nova classe `ResultadoCriterio` (`criterio`, `atendido`, `descricaoImpedimento`). | Representa em Java a tabela `analise_criterio`, que já existia no DER da 1ª entrega. | RN16, RF07 |
| E5 | Interfaces `Dao<T>` e `CrudDao<T>` e camada de serviço (`br.com.urbaniza.service`). | Arquitetura em camadas (ui, service, dao, banco). A análise emitida não possui operação de atualizar: `AnaliseParcelamentoDao` implementa apenas `Dao<T>`, porque a análise é imutável depois de emitida. | RN16, RF07 |
| E6 | Limiares de ilha de calor (30% e 15% de cobertura vegetal) centralizados em `ParametrosUrbanisticos`. | RN10 é qualitativa e não fixa valores; os limiares são parâmetros de projeto, documentados e mantidos em um único lugar. | RN10 |

## 2. Mapeamento de herança: tabela por subclasse (JOINED) com coluna `tipo`

As duas hierarquias de herança do Diagrama de Classes são mapeadas pela estratégia **tabela por subclasse** (*JOINED*), acrescida de uma coluna **discriminadora** `tipo` na tabela-mãe.

| Classe abstrata (tabela-mãe) | Subclasse | Subtabela | Valor de `tipo` |
|------------------------------|-----------|-----------|-----------------|
| `EquipamentoComunitario` (`equipamento_comunitario`) | `Escola` | `escola` | `ESCOLA` |
| `EquipamentoComunitario` (`equipamento_comunitario`) | `UnidadeSaude` | `unidade_saude` | `UNIDADE_SAUDE` |
| `CriterioVedacao` (`criterio_vedacao`) | `CriterioAlagamento` | `criterio_alagamento` | `ALAGAMENTO` |
| `CriterioVedacao` (`criterio_vedacao`) | `CriterioDeclividade` | `criterio_declividade` | `DECLIVIDADE` |
| `CriterioVedacao` (`criterio_vedacao`) | `CriterioGeologico` | `criterio_geologico` | `GEOLOGICO` |
| `CriterioVedacao` (`criterio_vedacao`) | `CriterioPreservacao` | `criterio_preservacao` | `PRESERVACAO` |

Funcionamento:

- A tabela-mãe guarda os atributos comuns (por exemplo, `nome` e `capacidade_instalada` do equipamento) e a coluna `tipo`, restrita por `CHECK` aos valores da tabela acima.
- Cada subtabela guarda apenas os atributos específicos. Sua chave primária `id` é também chave estrangeira para a tabela-mãe, com `ON DELETE CASCADE`: excluir a linha-mãe exclui a linha da subtabela.
- **Gravação:** o DAO insere a linha-mãe, obtém o `id` gerado e insere a linha da subtabela com o mesmo `id`, tudo na mesma transação.
- **Leitura:** o DAO consulta a tabela-mãe com `LEFT JOIN` nas subtabelas e instancia a subclasse concreta de acordo com a coluna `tipo`. O objeto devolvido é usado polimorficamente pelo restante do sistema.
- O valor de `tipo` corresponde ao retorno de `getTipo()` de cada subclasse.

Motivos da escolha: a estrutura normalizada não deixa colunas nulas irrelevantes (como aconteceria com uma tabela única para toda a hierarquia), permite restrições `NOT NULL` e `CHECK` específicas por subclasse (por exemplo, `habitantes_por_equipe > 0` só para unidades de saúde) e mantém em uma única tabela os dados comuns usados em consultas gerais, como a listagem de equipamentos de uma região.

## 3. Relacionamentos

| Relacionamento | Tipo UML | Multiplicidade | Implementação Java | Implementação SQL |
|----------------|----------|----------------|--------------------|-------------------|
| `Regiao` - `Loteamento` | Composição | 1 : 1..* (1:N) | `Regiao.loteamentos : List<Loteamento>`; `Regiao.adicionarLoteamento(Loteamento)`; `Loteamento.regiaoId` | `loteamento.regiao_id BIGINT NOT NULL` FK `regiao(id)` `ON DELETE CASCADE` |
| `Loteamento` - `Lote` | Composição | 1 : 1..* (1:N) | `Loteamento.lotes : List<Lote>`; `Loteamento.adicionarLote(Lote)`; no mínimo 1 lote para salvar | `lote.loteamento_id BIGINT NOT NULL` FK `loteamento(id)` `ON DELETE CASCADE` |
| `Regiao` - `EquipamentoComunitario` | Agregação | 0..1 : 0..* | `Regiao.equipamentos : List<EquipamentoComunitario>`; `Regiao.adicionarEquipamento(EquipamentoComunitario)`; `EquipamentoComunitario.regiaoId` pode ser nulo | `equipamento_comunitario.regiao_id BIGINT NULL` FK `regiao(id)` `ON DELETE SET NULL` |
| `AnaliseParcelamento` - `Analista` | Associação (responsável) | 0..* : 1 | `AnaliseParcelamento.analista : Analista` | `analise_parcelamento.analista_id BIGINT NOT NULL` FK `analista(id)` `ON DELETE RESTRICT` |
| `AnaliseParcelamento` - `Regiao` | Associação (analisa) | 0..* : 1 | `AnaliseParcelamento.regiao : Regiao` | `analise_parcelamento.regiao_id BIGINT NOT NULL` FK `regiao(id)` `ON DELETE RESTRICT` |
| `AnaliseParcelamento` - `CriterioVedacao` | Associação N:N (aplica) | 0..* : 1..* | `AnaliseParcelamento.criterios : List<CriterioVedacao>` e `AnaliseParcelamento.resultados : List<ResultadoCriterio>` | Tabela associativa `analise_criterio` (PK `analise_id, criterio_id`); `analise_id` `ON DELETE CASCADE`, `criterio_id` `ON DELETE RESTRICT` |
| `Regiao` - `CriterioVedacao` (E2) | Composição | 1 : 0..* | `CriterioVedacao.regiaoId` | `criterio_vedacao.regiao_id BIGINT NOT NULL` FK `regiao(id)` `ON DELETE CASCADE` |
| `EquipamentoComunitario` - `Escola` / `UnidadeSaude` | Herança | — | `extends EquipamentoComunitario` | Subtabelas `escola` e `unidade_saude` com PK = FK `ON DELETE CASCADE` |
| `CriterioVedacao` - quatro critérios concretos | Herança | — | `extends CriterioVedacao` | Subtabelas `criterio_alagamento`, `criterio_declividade`, `criterio_geologico` e `criterio_preservacao` com PK = FK `ON DELETE CASCADE` |

Observações:

- Na composição `Regiao` - `Loteamento`, o banco aceita uma região sem loteamentos (a região é cadastrada antes deles, conforme RN17); a multiplicidade 1..* da 1ª entrega expressa que o loteamento não existe sem a região. Na composição `Loteamento` - `Lote`, a exigência de ao menos um lote é validada pela camada de serviço (`LoteamentoService`) antes da gravação.
- A diferença entre composição e agregação é expressa no banco pela ação de exclusão: `CASCADE` (a parte não existe sem o todo) e `SET NULL` (a parte sobrevive à exclusão do todo).
- As associações da análise usam `RESTRICT` para preservar a rastreabilidade exigida por RN16: não é possível excluir a região, o analista ou o critério que fundamentaram uma análise emitida.

## 4. Parâmetros numéricos de `ParametrosUrbanisticos`

A classe `ParametrosUrbanisticos` é `final`, possui construtor privado e reúne as constantes usadas pelas regras de negócio.

| Constante | Valor | Regra | Fonte |
|-----------|-------|-------|-------|
| `LOTE_AREA_MINIMA_M2` | 125.0 | RN05 | Lei Federal nº 6.766/1979, art. 4º, II (lote mínimo de 125 m²) |
| `LOTE_FRENTE_MINIMA_M` | 5.0 | RN05 | Lei Federal nº 6.766/1979, art. 4º, II (frente mínima de 5 m) |
| `DECLIVIDADE_LIMITE_PADRAO_PCT` | 30.0 | RN02 | Lei Federal nº 6.766/1979, art. 3º, parágrafo único, III (declividade igual ou superior a 30%) |
| `IAV_LIMITE_ADEQUADO` | 12.0 | RN08 | Literatura de indicadores urbanos: acima de 12 m²/hab o IAV é adequado |
| `IAV_LIMITE_MINIMO` | 9.0 | RN08 | Parâmetro de referência da OMS, com 9 m²/hab adotado como mínimo para a América do Sul e o Caribe |
| `COBERTURA_VEGETAL_RISCO_BAIXO` | 0.30 | RN10 | Parâmetro de projeto (E6): cobertura vegetal igual ou superior a 30% indica potencial baixo de ilha de calor |
| `COBERTURA_VEGETAL_RISCO_MEDIO` | 0.15 | RN10 | Parâmetro de projeto (E6): cobertura vegetal igual ou superior a 15% e inferior a 30% indica potencial médio; abaixo de 15%, alto |

Os mesmos limites de lote (125 m² e 5 m) também são garantidos no banco por `CHECK` na tabela `lote`, e o limite de declividade é o valor padrão da coluna `criterio_declividade.declividade_limite`.

## 5. Classificações calculadas

| Indicador | Fórmula | Classificação |
|-----------|---------|---------------|
| População projetada (RN13) | população atual + soma da população estimada dos loteamentos aprovados (`dataAprovacao` preenchida) e não ocupados | — |
| IAV (RN08, RN14) | área verde / população projetada | maior que 12: `ADEQUADO`; de 9 a 12: `ACEITAVEL`; menor que 9: `DEFICITARIO`. Com população projetada igual a zero, o IAV é infinito e classificado como `ADEQUADO`. |
| Taxa de impermeabilização (RN09) | área impermeável / área total | valor entre 0 e 1, exibido em percentual |
| Potencial de ilha de calor (RN10) | cobertura vegetal = área verde / área total | maior ou igual a 0,30: `BAIXO`; maior ou igual a 0,15: `MEDIO`; menor que 0,15: `ALTO` |
| Índice de atendimento (RN11, RN12) | capacidade instalada / demanda. Escola: demanda = teto(população × percentual em idade escolar / 100). Unidade de saúde: demanda = teto(população / habitantes por equipe). | menor que 1: déficit. Com demanda igual a zero, o índice é infinito (sem demanda). |
| Aptidão (RN15) | resultado de `avaliar(Regiao)` de cada critério da região | algum critério reprovado com impedimento absoluto: `INAPTA`; algum reprovado sem impedimento absoluto: `APTA_COM_RESTRICAO`; nenhum reprovado: `APTA` |
| Versão dos dados (RN16) | `"v-"` + 12 primeiros caracteres hexadecimais do SHA-256 de um texto canônico com os dados da região, loteamentos e lotes, equipamentos e critérios, ordenados por `id` | mesmos dados geram a mesma versão; qualquer alteração gera versão diferente |
