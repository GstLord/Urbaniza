# Urbaniza — Banco de dados (PostgreSQL 16)

Camada de persistência do **Sistema de Controle de Urbanização por Regiões (Urbaniza)**.
O esquema é compatível com as classes Java do pacote `br.com.urbaniza.model` e é acessado pela
aplicação via JDBC puro (`br.com.urbaniza.dao`).

## Arquivos

| Arquivo | Conteúdo |
|---|---|
| `01_schema.sql` | Cria as 14 tabelas, PKs, FKs (`fk_*`), CHECKs (`ck_*`), UNIQUEs, índices nas FKs (`idx_*`) e comentários com a regra de negócio de origem. Reexecutável: começa com `DROP TABLE IF EXISTS ... CASCADE` (**apaga os dados**). |
| `02_dados_exemplo.sql` | Dados fictícios de exemplo (3 regiões, 5 loteamentos, 11 lotes, 6 equipamentos, 2 analistas, 8 critérios). Nenhuma análise: análises são emitidas pela aplicação. |
| `03_consultas_verificacao.sql` | Consultas somente de leitura que reproduzem em SQL os cálculos do Java (população projetada, IAV, impermeabilização, ilha de calor, demanda/índice de atendimento, ranking de déficit e aptidão esperada). |
| `docker-compose.yml` | Serviço `postgres` (imagem `postgres:16`, container `urbaniza-db`) que executa `01` e `02` automaticamente na primeira criação do volume. |
| `PROMPTS_2a_ENTREGA.txt` | Contrato técnico e prompts da 2ª entrega (não editar). |

## Ordem de execução

1. `01_schema.sql` — estrutura
2. `02_dados_exemplo.sql` — dados de exemplo
3. `03_consultas_verificacao.sql` — opcional, para conferir os indicadores

Conexão usada pela aplicação: banco `urbaniza`, usuário `urbaniza`, senha `urbaniza`,
`jdbc:postgresql://localhost:5432/urbaniza` (pode ser sobrescrita pelas variáveis de ambiente
`URBANIZA_DB_URL`, `URBANIZA_DB_USER` e `URBANIZA_DB_PASSWORD`).

## Subir com Docker (recomendado)

Os comandos são iguais no Windows PowerShell e no Linux/macOS (a partir da raiz do repositório):

```bash
cd Urbaniza/database
docker compose up -d
docker inspect --format "{{.State.Health.Status}}" urbaniza-db   # aguarde "healthy"
```

Os scripts de `/docker-entrypoint-initdb.d/` rodam **somente na primeira criação do volume**
`urbaniza_pgdata`. Para conferir que rodaram sem erro: `docker logs urbaniza-db`.

### Recriar o banco do zero

```bash
cd Urbaniza/database
docker compose down -v
docker compose up -d
```

`down -v` remove o volume `urbaniza_pgdata`; na subida seguinte `01` e `02` são reexecutados.

### Rodar as consultas de verificação no container

Linux/macOS (bash/zsh):

```bash
docker exec -i urbaniza-db psql -U urbaniza -d urbaniza < 03_consultas_verificacao.sql
```

Windows PowerShell (o operador `<` não existe no PowerShell):

```powershell
Get-Content -Raw -Encoding UTF8 .\03_consultas_verificacao.sql | docker exec -i urbaniza-db psql -U urbaniza -d urbaniza
```

## Subir sem Docker (PostgreSQL 16 local)

1. Crie o usuário e o banco (como superusuário `postgres`):

   Linux/macOS:

   ```bash
   sudo -u postgres psql -c "CREATE USER urbaniza WITH PASSWORD 'urbaniza';"
   sudo -u postgres psql -c "CREATE DATABASE urbaniza OWNER urbaniza ENCODING 'UTF8';"
   ```

   Windows PowerShell (com a pasta `bin` do PostgreSQL no `PATH`):

   ```powershell
   psql -U postgres -c "CREATE USER urbaniza WITH PASSWORD 'urbaniza';"
   psql -U postgres -c "CREATE DATABASE urbaniza OWNER urbaniza ENCODING 'UTF8';"
   ```

2. Execute os scripts a partir da raiz do repositório:

   Linux/macOS:

   ```bash
   psql -U urbaniza -d urbaniza -f Urbaniza/database/01_schema.sql
   psql -U urbaniza -d urbaniza -f Urbaniza/database/02_dados_exemplo.sql
   ```

   Windows PowerShell (garante que o psql leia os arquivos como UTF-8):

   ```powershell
   $env:PGCLIENTENCODING = "UTF8"
   psql -U urbaniza -d urbaniza -f Urbaniza/database/01_schema.sql
   psql -U urbaniza -d urbaniza -f Urbaniza/database/02_dados_exemplo.sql
   ```

Para recriar do zero sem Docker basta executar `01_schema.sql` e `02_dados_exemplo.sql` de novo
(o `01` remove e recria todas as tabelas).

## Mapeamento Tabela → Classe Java

| Tabela | Classe Java (`br.com.urbaniza.model...`) | Observação |
|---|---|---|
| `regiao` | `Regiao` | `declividade_media_pct` é a extensão E1 |
| `loteamento` | `Loteamento` | composição com `Regiao` (`ON DELETE CASCADE`); `data_aprovacao` NULL = não aprovado |
| `lote` | `Lote` | composição com `Loteamento`; CHECK RN05 (≥ 125 m², frente ≥ 5 m) |
| `equipamento_comunitario` | `equipamento.EquipamentoComunitario` (abstrata) | tabela-mãe; `tipo` = `ESCOLA` / `UNIDADE_SAUDE`; agregação com `Regiao` (`ON DELETE SET NULL`) |
| `escola` | `equipamento.Escola extends EquipamentoComunitario` | subtabela; `id` é PK e FK para a mãe |
| `unidade_saude` | `equipamento.UnidadeSaude extends EquipamentoComunitario` | subtabela; `id` é PK e FK para a mãe |
| `analista` | `Analista` | responsável pela análise (RF07/RN16) |
| `criterio_vedacao` | `criterio.CriterioVedacao` (abstrata) | tabela-mãe; `tipo` = `ALAGAMENTO` / `DECLIVIDADE` / `GEOLOGICO` / `PRESERVACAO`; `regiao_id` é a extensão E2 |
| `criterio_alagamento` | `criterio.CriterioAlagamento extends CriterioVedacao` | RN01 |
| `criterio_declividade` | `criterio.CriterioDeclividade extends CriterioVedacao` | RN02 |
| `criterio_geologico` | `criterio.CriterioGeologico extends CriterioVedacao` | RN03 |
| `criterio_preservacao` | `criterio.CriterioPreservacao extends CriterioVedacao` | RN04 |
| `analise_parcelamento` | `AnaliseParcelamento` | imutável depois de emitida (sem UPDATE); `regiao_id` e `analista_id` com `RESTRICT` |
| `analise_criterio` | `ResultadoCriterio` | N:N análise × critério (`AnaliseParcelamento.resultados`) |

Atributos que **não** são colunas: `Regiao.loteamentos` e `Regiao.equipamentos`,
`Loteamento.lotes` e `AnaliseParcelamento.criterios`/`resultados` são coleções carregadas pelos
DAOs a partir das FKs das tabelas filhas (`loteamento.regiao_id`, `equipamento_comunitario.regiao_id`,
`lote.loteamento_id`, `criterio_vedacao.regiao_id`, `analise_criterio.analise_id`).

### Herança: "tabela por subclasse" (JOINED) + coluna `tipo`

As hierarquias `EquipamentoComunitario` e `CriterioVedacao` são mapeadas assim:

- A **tabela-mãe** guarda os atributos comuns e uma coluna discriminadora `tipo`, cujo valor é o
  retorno de `getTipo()` da subclasse.
- Cada **subclasse concreta** tem sua própria tabela com os atributos específicos. A PK dessa
  subtabela (`id`) é **também FK** para a tabela-mãe com `ON DELETE CASCADE`: a linha-mãe e a da
  subtabela compartilham o mesmo `id`, e apagar a mãe apaga a filha.
- **Gravação**: o DAO insere a linha-mãe (obtendo o `id` gerado) e, na mesma transação, a linha da
  subtabela com esse `id`.
- **Leitura**: o DAO faz `LEFT JOIN` da mãe com todas as subtabelas e instancia a subclasse
  correta pela coluna `tipo` (ex.: `ESCOLA` → `new Escola(...)`).

Exemplo (equipamentos):

```sql
SELECT e.*, es.percentual_idade_escolar, es.nivel_ensino,
       us.habitantes_por_equipe, us.tipo_unidade
FROM equipamento_comunitario e
LEFT JOIN escola        es ON es.id = e.id
LEFT JOIN unidade_saude us ON us.id = e.id;
```

## Consequências das ações de FK (tratadas pela aplicação com mensagem amigável)

- Excluir região **com** análises emitidas é bloqueado (`RESTRICT`) — rastreabilidade (RN16).
- Excluir região **sem** análises apaga em cascata loteamentos, lotes e critérios; os equipamentos
  ficam sem região (`regiao_id = NULL`) — agregação.
- Excluir critério usado em alguma análise é bloqueado (`RESTRICT`).
- Excluir analista com análises é bloqueado (`RESTRICT`).

## Valores esperados com os dados de exemplo

| Região | Pop. projetada | IAV (m²/hab) | Impermeab. | Ilha de calor | Equipamentos (índice) | Aptidão |
|---|---|---|---|---|---|---|
| Região Norte - Jardim das Águas | 20.400 | 8,82 DEFICITARIO | 45,00% | ALTO (9%) | escola 0,86 déficit; UBS 0,83 déficit | APTA_COM_RESTRICAO |
| Região Central - Vila Esperança | 21.000 | 14,29 ADEQUADO | 50,00% | MEDIO (25%) | escola 1,27; UBS 1,00 (sem déficit) | APTA |
| Região Sul - Serra Alta | 6.000 | 250,00 ADEQUADO | 10,00% | BAIXO (50%) | UBS 0,50 déficit | INAPTA |
