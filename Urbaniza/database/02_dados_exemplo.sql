-- =============================================================================
-- Projeto : Urbaniza - Sistema de Controle de Urbanização por Regiões
-- Arquivo : 02_dados_exemplo.sql
-- Propósito: dados FICTÍCIOS de exemplo (seção C8 do contrato técnico da
--           2ª entrega). Servem de teste de aceitação comum: as consultas de
--           03_consultas_verificacao.sql, os testes Java e o roteiro de
--           demonstração devem chegar aos mesmos números.
--
-- Pré-requisito: 01_schema.sql executado (banco vazio).
-- Como executar:
--   Docker: executado automaticamente após 01_schema.sql na 1ª criação do volume.
--   Local : psql -U urbaniza -d urbaniza -f Urbaniza/database/02_dados_exemplo.sql
--
-- Regras de escrita:
--   - Nenhum valor é informado na coluna id (GENERATED ALWAYS AS IDENTITY).
--   - FKs resolvidas por subconsulta pela chave natural (regiao.nome,
--     loteamento.denominacao) ou por CTE "INSERT ... RETURNING id".
--   - Herança JOINED: a linha-mãe (com a coluna tipo) e a linha da subtabela
--     recebem o MESMO id via CTE com RETURNING id.
--   - Nenhuma análise é inserida: análises são emitidas pela aplicação.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- Analistas
-- -----------------------------------------------------------------------------
INSERT INTO analista (nome, matricula, setor) VALUES
    ('Rafael Moreira',  'AP-1041', 'Análise de Projetos'),
    ('Bianca Teixeira', 'CI-2036', 'Cadastro Imobiliário');

-- -----------------------------------------------------------------------------
-- Regiões
-- -----------------------------------------------------------------------------
INSERT INTO regiao (nome, area_total_m2, area_verde_m2, area_impermeavel_m2, populacao_atual, declividade_media_pct) VALUES
    ('Região Norte - Jardim das Águas',  2000000, 180000,  900000, 18000,  8),
    ('Região Central - Vila Esperança',  1200000, 300000,  600000, 20000,  4),
    ('Região Sul - Serra Alta',          3000000, 1500000, 300000,  6000, 34);

-- -----------------------------------------------------------------------------
-- Loteamentos (data_aprovacao NULL = ainda não aprovado)
-- -----------------------------------------------------------------------------
INSERT INTO loteamento (regiao_id, denominacao, data_aprovacao, area_gleba_m2, populacao_estimada, ocupado) VALUES
    ((SELECT id FROM regiao WHERE nome = 'Região Norte - Jardim das Águas'),
        'Residencial Águas Claras', DATE '2025-03-10', 150000, 2400, FALSE),
    ((SELECT id FROM regiao WHERE nome = 'Região Norte - Jardim das Águas'),
        'Jardim Ribeirinho',        DATE '2019-06-01', 200000, 3000, TRUE),
    ((SELECT id FROM regiao WHERE nome = 'Região Norte - Jardim das Águas'),
        'Parque das Garças',        NULL,               90000, 1500, FALSE),
    ((SELECT id FROM regiao WHERE nome = 'Região Central - Vila Esperança'),
        'Residencial Bela Vista',   DATE '2024-08-20',  60000, 1000, FALSE),
    ((SELECT id FROM regiao WHERE nome = 'Região Sul - Serra Alta'),
        'Chácaras Serra Alta',      NULL,              400000,  800, FALSE);

-- -----------------------------------------------------------------------------
-- Lotes (RN05: área >= 125 m² e frente >= 5 m)
-- -----------------------------------------------------------------------------
INSERT INTO lote (loteamento_id, quadra, numero, area_m2, frente_m) VALUES
    ((SELECT id FROM loteamento WHERE denominacao = 'Residencial Águas Claras'), 'A', '1',  250,  10),
    ((SELECT id FROM loteamento WHERE denominacao = 'Residencial Águas Claras'), 'A', '2',  200,   8),
    ((SELECT id FROM loteamento WHERE denominacao = 'Residencial Águas Claras'), 'B', '1',  180,   7.5),
    ((SELECT id FROM loteamento WHERE denominacao = 'Jardim Ribeirinho'),        'A', '1',  300,  12),
    ((SELECT id FROM loteamento WHERE denominacao = 'Jardim Ribeirinho'),        'A', '2',  300,  12),
    ((SELECT id FROM loteamento WHERE denominacao = 'Parque das Garças'),        'A', '1',  150,   6),
    ((SELECT id FROM loteamento WHERE denominacao = 'Parque das Garças'),        'A', '2',  160,   6.5),
    ((SELECT id FROM loteamento WHERE denominacao = 'Residencial Bela Vista'),   'A', '1',  200,   8),
    ((SELECT id FROM loteamento WHERE denominacao = 'Residencial Bela Vista'),   'A', '2',  220,   9),
    ((SELECT id FROM loteamento WHERE denominacao = 'Chácaras Serra Alta'),      'A', '1', 1000,  20),
    ((SELECT id FROM loteamento WHERE denominacao = 'Chácaras Serra Alta'),      'A', '2', 1200,  24);

-- -----------------------------------------------------------------------------
-- Equipamentos comunitários (herança JOINED: mãe + subtabela com o mesmo id)
-- Escola: capacidade_instalada = vagas | Unidade de saúde: = equipes
-- -----------------------------------------------------------------------------

-- Região Norte
WITH e AS (
    INSERT INTO equipamento_comunitario (regiao_id, nome, capacidade_instalada, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Norte - Jardim das Águas'),
            'EM Professora Helena Duarte', 2800, 'ESCOLA')
    RETURNING id
)
INSERT INTO escola (id, percentual_idade_escolar, nivel_ensino)
SELECT id, 16, 'Fundamental' FROM e;

WITH e AS (
    INSERT INTO equipamento_comunitario (regiao_id, nome, capacidade_instalada, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Norte - Jardim das Águas'),
            'UBS Jardim das Águas', 5, 'UNIDADE_SAUDE')
    RETURNING id
)
INSERT INTO unidade_saude (id, habitantes_por_equipe, tipo_unidade)
SELECT id, 3500, 'UBS' FROM e;

-- Região Central
WITH e AS (
    INSERT INTO equipamento_comunitario (regiao_id, nome, capacidade_instalada, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Central - Vila Esperança'),
            'EE Vila Esperança', 4000, 'ESCOLA')
    RETURNING id
)
INSERT INTO escola (id, percentual_idade_escolar, nivel_ensino)
SELECT id, 15, 'Médio' FROM e;

WITH e AS (
    INSERT INTO equipamento_comunitario (regiao_id, nome, capacidade_instalada, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Central - Vila Esperança'),
            'UBS Central', 7, 'UNIDADE_SAUDE')
    RETURNING id
)
INSERT INTO unidade_saude (id, habitantes_por_equipe, tipo_unidade)
SELECT id, 3000, 'UBS' FROM e;

-- Região Sul
WITH e AS (
    INSERT INTO equipamento_comunitario (regiao_id, nome, capacidade_instalada, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Sul - Serra Alta'),
            'UBS Serra Alta', 1, 'UNIDADE_SAUDE')
    RETURNING id
)
INSERT INTO unidade_saude (id, habitantes_por_equipe, tipo_unidade)
SELECT id, 3500, 'UBS' FROM e;

-- Equipamento SEM região (agregação: regiao_id NULL)
WITH e AS (
    INSERT INTO equipamento_comunitario (regiao_id, nome, capacidade_instalada, tipo)
    VALUES (NULL, 'Escola Rural Itinerante', 120, 'ESCOLA')
    RETURNING id
)
INSERT INTO escola (id, percentual_idade_escolar, nivel_ensino)
SELECT id, 20, 'Fundamental' FROM e;

-- -----------------------------------------------------------------------------
-- Critérios de vedação (herança JOINED: mãe + subtabela com o mesmo id)
-- -----------------------------------------------------------------------------

-- Região Norte
WITH c AS (
    INSERT INTO criterio_vedacao (regiao_id, codigo_interno, fundamento_legal, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Norte - Jardim das Águas'),
            'ALG-NORTE-01', 'Lei 6.766/79, art. 3º, parágrafo único, I', 'ALAGAMENTO')
    RETURNING id
)
INSERT INTO criterio_alagamento (id, saneamento_executado)
SELECT id, FALSE FROM c;

WITH c AS (
    INSERT INTO criterio_vedacao (regiao_id, codigo_interno, fundamento_legal, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Norte - Jardim das Águas'),
            'DEC-NORTE-01', 'Lei 6.766/79, art. 3º, parágrafo único, III', 'DECLIVIDADE')
    RETURNING id
)
INSERT INTO criterio_declividade (id, declividade_limite)
SELECT id, 30 FROM c;

WITH c AS (
    INSERT INTO criterio_vedacao (regiao_id, codigo_interno, fundamento_legal, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Norte - Jardim das Águas'),
            'PRS-NORTE-01', 'Lei 6.766/79, art. 3º, parágrafo único, V', 'PRESERVACAO')
    RETURNING id
)
INSERT INTO criterio_preservacao (id, area_protegida)
SELECT id, FALSE FROM c;

-- Região Central
WITH c AS (
    INSERT INTO criterio_vedacao (regiao_id, codigo_interno, fundamento_legal, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Central - Vila Esperança'),
            'GEO-CENTRO-01', 'Lei 6.766/79, art. 3º, parágrafo único, IV', 'GEOLOGICO')
    RETURNING id
)
INSERT INTO criterio_geologico (id, laudo_geotecnico)
SELECT id, 'ADEQUADO: solo argiloso estável' FROM c;

WITH c AS (
    INSERT INTO criterio_vedacao (regiao_id, codigo_interno, fundamento_legal, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Central - Vila Esperança'),
            'DEC-CENTRO-01', 'Lei 6.766/79, art. 3º, parágrafo único, III', 'DECLIVIDADE')
    RETURNING id
)
INSERT INTO criterio_declividade (id, declividade_limite)
SELECT id, 30 FROM c;

-- Região Sul
WITH c AS (
    INSERT INTO criterio_vedacao (regiao_id, codigo_interno, fundamento_legal, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Sul - Serra Alta'),
            'DEC-SUL-01', 'Lei 6.766/79, art. 3º, parágrafo único, III', 'DECLIVIDADE')
    RETURNING id
)
INSERT INTO criterio_declividade (id, declividade_limite)
SELECT id, 30 FROM c;

WITH c AS (
    INSERT INTO criterio_vedacao (regiao_id, codigo_interno, fundamento_legal, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Sul - Serra Alta'),
            'PRS-SUL-01', 'Lei 6.766/79, art. 3º, parágrafo único, V', 'PRESERVACAO')
    RETURNING id
)
INSERT INTO criterio_preservacao (id, area_protegida)
SELECT id, TRUE FROM c;

WITH c AS (
    INSERT INTO criterio_vedacao (regiao_id, codigo_interno, fundamento_legal, tipo)
    VALUES ((SELECT id FROM regiao WHERE nome = 'Região Sul - Serra Alta'),
            'GEO-SUL-01', 'Lei 6.766/79, art. 3º, parágrafo único, IV', 'GEOLOGICO')
    RETURNING id
)
INSERT INTO criterio_geologico (id, laudo_geotecnico)
SELECT id, 'INADEQUADO: encosta com risco de deslizamento' FROM c;

COMMIT;
