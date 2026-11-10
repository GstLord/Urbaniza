# Roteiro de Demonstração (aproximadamente 10 minutos)

Roteiro para apresentar o Urbaniza ao professor, opção por opção do menu, usando os dados fictícios de exemplo (`Urbaniza/database/02_dados_exemplo.sql`) e os resultados esperados do contrato técnico. Os mesmos números são conferidos pelos testes JUnit e pelas consultas SQL de verificação.

## Preparação (antes da apresentação)

1. Recriar o banco do zero, para partir exatamente dos dados de exemplo (sem análises emitidas):

   ```
   cd Urbaniza/database && docker compose down -v && docker compose up -d
   ```

2. Compilar (a partir da raiz do repositório):

   ```
   cd Urbaniza && mvn clean package
   ```

3. Deixar abertos: um terminal com a aplicação (`java -jar Urbaniza/target/urbaniza.jar`), um terminal para a consulta SQL do passo (f) e o editor de código com a pasta `Urbaniza/src/main/java/br/com/urbaniza`.

Estado inicial esperado: 3 regiões, 5 loteamentos, 11 lotes, 2 analistas, 6 equipamentos comunitários (3 escolas e 3 unidades de saúde), 8 critérios de vedação e nenhuma análise.

## Menu principal

```
1 Regiões
2 Loteamentos
3 Equipamentos comunitários
4 Analistas
5 Critérios de vedação
6 Análises de parcelamento
0 Sair
```

Os menus de cadastro oferecem as opções Listar, Detalhar, Cadastrar, Atualizar, Excluir e Voltar.

## (a) CRUD completo de Região — cerca de 2 minutos

Requisito: base cadastral de RF01 a RF05.

| Passo | Opção | Ação | Resultado esperado |
|-------|-------|------|--------------------|
| 1 | 1 Regiões > Listar | Listar as regiões. | Aparecem "Região Norte - Jardim das Águas", "Região Central - Vila Esperança" e "Região Sul - Serra Alta". |
| 2 | 1 Regiões > Cadastrar | Cadastrar "Região Teste - Demonstração": área total 500.000 m², área verde 100.000 m², área impermeável 150.000 m², população atual 5.000, declividade média 5%. | Região gravada com um novo id. |
| 3 | 1 Regiões > Detalhar | Informar o id da região criada. | Dados exibidos conforme cadastrados (lidos do banco). |
| 4 | 1 Regiões > Atualizar | Alterar a população atual para 6.000. | Alteração confirmada; ao detalhar novamente, a população é 6.000. |
| 5 | 1 Regiões > Excluir | Excluir a "Região Teste - Demonstração" e confirmar. | Região excluída; ela não aparece mais na listagem. |

Ponto a destacar: todas as operações são gravadas no PostgreSQL (o passo (f) comprova).

## (b) Loteamento recusado por lote abaixo do mínimo — cerca de 1 minuto

Requisitos: RF02 (vínculo obrigatório com região, RN17) e RF06 (dimensões mínimas do lote, RN05).

| Passo | Opção | Ação | Resultado esperado |
|-------|-------|------|--------------------|
| 1 | 2 Loteamentos > Cadastrar | Escolher a "Região Norte - Jardim das Águas"; denominação "Loteamento Demonstração"; sem data de aprovação; gleba 10.000 m²; população estimada 100; 1 lote: quadra A, número 1, área 120 m², frente 6 m. | Cadastro **recusado** com mensagem indicando que o lote da quadra A, número 1, não atende à área mínima de 125 m² (RN05). Nada é gravado. |
| 2 | 2 Loteamentos > Listar | Listar os loteamentos. | Continuam os 5 loteamentos de exemplo. |

Ponto a destacar: a regra é validada em Java (`Lote.atendeDimensoesMinimas()`, `LoteamentoService`) e também no banco (`CHECK (area_m2 >= 125)` na tabela `lote`).

## (c) Indicadores da Região Norte — cerca de 1 minuto e meio

Requisitos: RF03, RF04 e RF05.

Opção: **1 Regiões > Indicadores da região** > "Região Norte - Jardim das Águas".

| Indicador | Valor esperado | Regra |
|-----------|----------------|-------|
| População atual | 18.000 | — |
| População projetada | 20.400 (18.000 + 2.400 do "Residencial Águas Claras", aprovado e não ocupado; "Jardim Ribeirinho" já está ocupado e "Parque das Garças" não foi aprovado) | RN13, RN14 |
| IAV | 8,82 m²/hab — Deficitário | RN08 |
| Taxa de impermeabilização | 45,00% | RN09 |
| Potencial de ilha de calor | Alto (cobertura vegetal de 9%) | RN10 |
| Escola "EM Professora Helena Duarte" | demanda 3.264, índice 0,86 — déficit | RN11, RN12 |
| Unidade de saúde "UBS Jardim das Águas" | demanda 6 equipes, índice 0,83 — déficit | RN11, RN12 |

Ponto a destacar: a análise usa a população projetada, e não a atual (RN14). Com a população atual o IAV seria 10,00 (aceitável); com a projetada, cai para 8,82 (deficitário).

## (d) Emitir as análises de parcelamento — cerca de 2 minutos

Requisitos: RF01 e RF07.

Opção: **6 Análises de parcelamento > Emitir análise**, escolhendo a região e o analista "Rafael Moreira" (matrícula AP-1041).

| Região | Critérios avaliados | Resultado esperado |
|--------|---------------------|--------------------|
| Região Norte - Jardim das Águas | ALG-NORTE-01 reprovado (saneamento não executado; impedimento superável); DEC-NORTE-01 atendido (8% < 30%); PRS-NORTE-01 atendido | **Apta com restrição** (`APTA_COM_RESTRICAO`) |
| Região Central - Vila Esperança | GEO-CENTRO-01 atendido (laudo "ADEQUADO: solo argiloso estável"); DEC-CENTRO-01 atendido (4% < 30%) | **Apta** (`APTA`); IAV 14,29 adequado; escola índice 1,27 e UBS índice 1,00, sem déficit |
| Região Sul - Serra Alta | DEC-SUL-01 reprovado (34% >= 30%); PRS-SUL-01 reprovado (área protegida, impedimento absoluto); GEO-SUL-01 reprovado (laudo "INADEQUADO: encosta com risco de deslizamento", impedimento absoluto) | **Inapta** (`INAPTA`); população projetada 6.000; IAV 250,00 adequado; UBS índice 0,50 — déficit |

Em cada parecer, mostrar:

- data de emissão, analista responsável e versão dos dados no formato `v-` seguido de 12 caracteres hexadecimais (RF07, RN16);
- o resultado de cada critério (atendido ou descrição do impedimento);
- população atual e projetada, IAV e classificação, taxa de impermeabilização, potencial de ilha de calor e a lista de déficits;
- a frase: "Indicadores estimados a partir dos dados cadastrados; não substituem o parecer técnico nem constituem previsão certificada."

Em seguida, **6 Análises de parcelamento > Listar** mostra as três análises gravadas.

Ponto a destacar: a aptidão é calculada polimorficamente — `AnaliseParcelamento.avaliarAptidao()` chama `criterio.avaliar(regiao)` para cada critério sem conhecer a subclasse concreta.

## (e) Exclusão bloqueada para preservar a rastreabilidade — cerca de 30 segundos

Requisito: RF07 (RN16).

| Passo | Opção | Ação | Resultado esperado |
|-------|-------|------|--------------------|
| 1 | 1 Regiões > Excluir | Tentar excluir a "Região Norte - Jardim das Águas" e confirmar. | Exclusão **bloqueada** com mensagem amigável: a região possui análise emitida (`ON DELETE RESTRICT`). O programa continua no menu. |
| 2 | 4 Analistas > Excluir (opcional) | Tentar excluir "Rafael Moreira". | Exclusão bloqueada pelo mesmo motivo. |

## (f) Prova de persistência no PostgreSQL — cerca de 1 minuto

No segundo terminal, consultar o banco diretamente:

```
docker exec -it urbaniza-db psql -U urbaniza -d urbaniza -c "SELECT a.id, r.nome AS regiao, an.nome AS analista, a.resultado, a.versao_dados, a.data_emissao FROM analise_parcelamento a JOIN regiao r ON r.id = a.regiao_id JOIN analista an ON an.id = a.analista_id ORDER BY a.id;"
```

Resultado esperado: as três análises emitidas no passo (d), com os resultados `APTA_COM_RESTRICAO`, `APTA` e `INAPTA`, o analista e a versão dos dados.

Consulta complementar (resultado por critério):

```
docker exec -it urbaniza-db psql -U urbaniza -d urbaniza -c "SELECT ac.analise_id, c.codigo_interno, c.tipo, ac.resultado FROM analise_criterio ac JOIN criterio_vedacao c ON c.id = ac.criterio_id ORDER BY ac.analise_id, c.codigo_interno;"
```

Sem Docker, execute as mesmas consultas com `psql -U urbaniza -d urbaniza -c "..."`.

## (g) Pilares de POO no código — cerca de 2 minutos

Abrir no editor (pasta `Urbaniza/src/main/java/br/com/urbaniza`):

| Pilar | Onde mostrar |
|-------|--------------|
| Classe abstrata | `model/equipamento/EquipamentoComunitario.java` (método abstrato `calcularDemanda(int)`); `model/criterio/CriterioVedacao.java` (métodos abstratos `avaliar(Regiao)`, `isImpedimentoAbsoluto()` e `getTipo()`); `ui/MenuCrud.java` (template method `exibir()`) |
| Interface | `dao/Dao.java` e `dao/CrudDao.java` (`CrudDao<T> extends Dao<T>`); `AnaliseParcelamentoDao` implementa apenas `Dao<T>`, pois a análise emitida é imutável |
| Herança | `model/equipamento/Escola.java` e `UnidadeSaude.java` (`extends EquipamentoComunitario`); `model/criterio/CriterioAlagamento.java`, `CriterioDeclividade.java`, `CriterioGeologico.java` e `CriterioPreservacao.java` (`extends CriterioVedacao`); menus que estendem `MenuCrud<T>` |
| Polimorfismo via `@Override` | `calcularDemanda(int)` em `Escola` (população em idade escolar) e em `UnidadeSaude` (população por equipe); `avaliar(Regiao)` em cada critério; chamada polimórfica em `AnaliseParcelamento.avaliarAptidao()` e em `EquipamentoComunitario.calcularIndiceAtendimento(int)` |
| Composição 1:N | `List<Loteamento>` em `model/Regiao.java` e `List<Lote>` em `model/Loteamento.java` |
| Persistência real | `dao/RegiaoDao.java` (JDBC com `PreparedStatement`) e `config/ConnectionFactory.java` |

## Encerramento

Escolher **0 Sair** no menu principal. Para devolver o banco ao estado inicial depois da apresentação:

```
cd Urbaniza/database && docker compose down -v && docker compose up -d
```

No Windows PowerShell 5.1, substitua `&&` por `;` nos comandos acima.
