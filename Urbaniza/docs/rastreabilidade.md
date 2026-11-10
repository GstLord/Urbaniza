# Matriz de Rastreabilidade

Esta matriz liga cada requisito funcional da 1ª entrega às dores que o originaram, às regras de negócio que o fundamentam, às classes e métodos que o implementam, às tabelas que o persistem e à opção de menu em que pode ser demonstrado. As dores D1 a D5 e a associação entre dor e requisito são as da seção 3 da 1ª entrega.

Os nomes de classes e métodos seguem a seção C5 do contrato técnico. A localização exata (arquivo e linha) será confirmada na conferência final da documentação.

## Dores de origem (1ª entrega)

| Código | Dor |
|--------|-----|
| D1 | Dificuldade em visualizar todos os riscos de uma área. |
| D2 | Dados sobre infraestrutura e qualidade de vida são difíceis de organizar. |
| D3 | Dificuldade em executar a análise atendendo a todas as normas. |
| D4 | Ausência de visão histórica e cumulativa por região. |
| D5 | Postura reativa em vez de preventiva. |

## Matriz por requisito funcional

| RF | Dor | RN | Classe.método | Tabela(s) | Opção de menu |
|----|-----|----|---------------|-----------|---------------|
| RF01 | D1 | RN01, RN02, RN03, RN04, RN15 | `CriterioVedacao.avaliar(Regiao)` (abstrato) com `@Override` em `CriterioAlagamento`, `CriterioDeclividade`, `CriterioGeologico` e `CriterioPreservacao`; `CriterioVedacao.isImpedimentoAbsoluto()`; `CriterioVedacao.descreverImpedimento()`; `AnaliseParcelamento.avaliarAptidao()`; `CriterioVedacaoService`; `CriterioVedacaoDao` | `criterio_vedacao`, `criterio_alagamento`, `criterio_declividade`, `criterio_geologico`, `criterio_preservacao`, `analise_parcelamento`, `analise_criterio` | 5 Critérios de vedação > Cadastrar; 6 Análises de parcelamento > Emitir análise |
| RF02 | D4 | RN17 | `LoteamentoService` (verifica se a região existe antes de gravar); `Regiao.adicionarLoteamento(Loteamento)`; `LoteamentoDao.inserir(Loteamento)` | `regiao`, `loteamento`, `lote` | 2 Loteamentos > Cadastrar |
| RF03 | D5 | RN11, RN12 | `EquipamentoComunitario.calcularIndiceAtendimento(int)`, `EquipamentoComunitario.possuiDeficit(int)`, `EquipamentoComunitario.calcularDemanda(int)` (abstrato) com `@Override` em `Escola` e `UnidadeSaude`; `EquipamentoService`; `EquipamentoComunitarioDao` | `equipamento_comunitario`, `escola`, `unidade_saude` | 3 Equipamentos comunitários > Cadastrar; 1 Regiões > Indicadores da região |
| RF04 | D4, D5 | RN13, RN14 | `Regiao.calcularPopulacaoProjetada()`, `Loteamento.contribuiParaProjecao()`, `Loteamento.isAprovado()`; `RegiaoService.carregarCompleta(long)` | `regiao`, `loteamento` | 1 Regiões > Indicadores da região |
| RF05 | D2 | RN08 | `Regiao.calcularIAV()`, `Regiao.classificarIAV()`; enumeração `ClassificacaoIAV`; constantes `ParametrosUrbanisticos.IAV_LIMITE_ADEQUADO` e `ParametrosUrbanisticos.IAV_LIMITE_MINIMO` | `regiao`, `loteamento`, `analise_parcelamento` (`indice_area_verde_apurado`) | 1 Regiões > Indicadores da região |
| RF06 | D3 | RN05 | `Lote.atendeDimensoesMinimas()`, `Loteamento.validarDimensoesLotes()`; `LoteamentoService` (rejeita com a quadra e o número do lote inválido) | `lote` (`CHECK area_m2 >= 125` e `CHECK frente_m >= 5`) | 2 Loteamentos > Cadastrar; 2 Loteamentos > Atualizar |
| RF07 | D2 | RN16 | `AnaliseParcelamento.executar()`, `AnaliseParcelamento.calcularVersaoDados()`; `AnaliseService.emitir(long, long)`; `AnaliseParcelamentoDao.inserir(AnaliseParcelamento)` (sem atualizar: análise imutável) | `analise_parcelamento` (`data_emissao`, `analista_id`, `versao_dados`), `analista`, `analise_criterio` | 6 Análises de parcelamento > Emitir análise; 6 Análises de parcelamento > Detalhar |

## Regras complementares exibidas nos indicadores

As regras abaixo não correspondem a um requisito funcional próprio, mas compõem os indicadores exibidos e o parecer da análise (RF01, RF05 e RF07).

| RN | Classe.método | Tabela(s) | Opção de menu |
|----|---------------|-----------|---------------|
| RN09 | `Regiao.calcularTaxaImpermeabilizacao()` | `regiao` | 1 Regiões > Indicadores da região |
| RN10 | `Regiao.classificarIlhaDeCalor()`; enumeração `PotencialIlhaCalor`; constantes `ParametrosUrbanisticos.COBERTURA_VEGETAL_RISCO_BAIXO` e `ParametrosUrbanisticos.COBERTURA_VEGETAL_RISCO_MEDIO` | `regiao` | 1 Regiões > Indicadores da região |
| RN11, RN12, RN14 | `AnaliseParcelamento.listarDeficits()` (equipamentos com déficit frente à população projetada, IAV deficitário e ilha de calor alta) | `equipamento_comunitario`, `escola`, `unidade_saude` | 6 Análises de parcelamento > Emitir análise |

## Evidências previstas no código

| Camada | Arquivo previsto |
|--------|------------------|
| Domínio | `Urbaniza/src/main/java/br/com/urbaniza/model/Regiao.java`, `Loteamento.java`, `Lote.java`, `AnaliseParcelamento.java`, `ResultadoCriterio.java`, `ParametrosUrbanisticos.java` |
| Equipamentos | `Urbaniza/src/main/java/br/com/urbaniza/model/equipamento/EquipamentoComunitario.java`, `Escola.java`, `UnidadeSaude.java` |
| Critérios | `Urbaniza/src/main/java/br/com/urbaniza/model/criterio/CriterioVedacao.java`, `CriterioAlagamento.java`, `CriterioDeclividade.java`, `CriterioGeologico.java`, `CriterioPreservacao.java` |
| Serviços | `Urbaniza/src/main/java/br/com/urbaniza/service/` |
| Persistência | `Urbaniza/src/main/java/br/com/urbaniza/dao/` |
| Interface | `Urbaniza/src/main/java/br/com/urbaniza/ui/MenuPrincipal.java` e demais menus |
| Banco | `Urbaniza/database/01_schema.sql` |
