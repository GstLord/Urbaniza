-- =============================================================================
-- Projeto : Urbaniza - Sistema de Controle de Urbanização por Regiões
--           AEP 2026.2 - 2ª entrega
-- Arquivo : 01_schema.sql
-- Propósito: criar o esquema PostgreSQL 16 que persiste o modelo do sistema
--           (regiões, loteamentos, lotes, equipamentos comunitários, analistas,
--           critérios de vedação da Lei Federal nº 6.766/1979 e análises de
--           parcelamento).
--           Esquema compatível com as classes Java do pacote
--           br.com.urbaniza.model (mapeamento descrito em database/README.md).
--           Herança mapeada como "tabela por subclasse" (JOINED) + coluna
--           discriminadora "tipo" na tabela-mãe.
--
-- Como executar:
--   Docker (executa automaticamente na 1ª criação do volume):
--     cd Urbaniza/database && docker compose up -d
--   PostgreSQL local:
--     psql -U urbaniza -d urbaniza -f Urbaniza/database/01_schema.sql
--
-- O script é reexecutável: remove as tabelas existentes antes de recriá-las
-- (ATENÇÃO: isso apaga todos os dados).
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- Remoção (ordem inversa de dependência)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS analise_criterio        CASCADE;
DROP TABLE IF EXISTS analise_parcelamento    CASCADE;
DROP TABLE IF EXISTS criterio_preservacao    CASCADE;
DROP TABLE IF EXISTS criterio_geologico      CASCADE;
DROP TABLE IF EXISTS criterio_declividade    CASCADE;
DROP TABLE IF EXISTS criterio_alagamento     CASCADE;
DROP TABLE IF EXISTS criterio_vedacao        CASCADE;
DROP TABLE IF EXISTS analista                CASCADE;
DROP TABLE IF EXISTS unidade_saude           CASCADE;
DROP TABLE IF EXISTS escola                  CASCADE;
DROP TABLE IF EXISTS equipamento_comunitario CASCADE;
DROP TABLE IF EXISTS lote                    CASCADE;
DROP TABLE IF EXISTS loteamento              CASCADE;
DROP TABLE IF EXISTS regiao                  CASCADE;

-- -----------------------------------------------------------------------------
-- regiao  (classe br.com.urbaniza.model.Regiao)
-- -----------------------------------------------------------------------------
CREATE TABLE regiao (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY,
    nome                  VARCHAR(150)     NOT NULL,
    area_total_m2         DOUBLE PRECISION NOT NULL,
    area_verde_m2         DOUBLE PRECISION NOT NULL DEFAULT 0,
    area_impermeavel_m2   DOUBLE PRECISION NOT NULL DEFAULT 0,
    populacao_atual       INT              NOT NULL DEFAULT 0,
    declividade_media_pct DOUBLE PRECISION NOT NULL DEFAULT 0,
    CONSTRAINT pk_regiao PRIMARY KEY (id),
    CONSTRAINT uk_regiao_nome UNIQUE (nome),
    CONSTRAINT ck_regiao_area_total_positiva CHECK (area_total_m2 > 0),
    CONSTRAINT ck_regiao_area_verde_nao_negativa CHECK (area_verde_m2 >= 0),
    CONSTRAINT ck_regiao_area_impermeavel_nao_negativa CHECK (area_impermeavel_m2 >= 0),
    CONSTRAINT ck_regiao_populacao_atual_nao_negativa CHECK (populacao_atual >= 0),
    CONSTRAINT ck_regiao_declividade_nao_negativa CHECK (declividade_media_pct >= 0),
    CONSTRAINT ck_regiao_soma_areas CHECK (area_verde_m2 + area_impermeavel_m2 <= area_total_m2)
);

COMMENT ON TABLE regiao IS 'Região do município analisada quanto à aptidão ao parcelamento (RF01, RN15). Classe Java: Regiao';
COMMENT ON COLUMN regiao.id IS 'Identificador gerado automaticamente';
COMMENT ON COLUMN regiao.nome IS 'Nome único da região';
COMMENT ON COLUMN regiao.area_total_m2 IS 'Área total da região em m² (base de RN09 e RN10)';
COMMENT ON COLUMN regiao.area_verde_m2 IS 'RN08/RN10 - área verde em m², numerador do IAV e da cobertura vegetal';
COMMENT ON COLUMN regiao.area_impermeavel_m2 IS 'RN09 - área impermeável em m², numerador da taxa de impermeabilização';
COMMENT ON COLUMN regiao.populacao_atual IS 'RN13 - população atual, base da população projetada';
COMMENT ON COLUMN regiao.declividade_media_pct IS '[EXT] E1 - declividade média medida da região (%), comparada com criterio_declividade.declividade_limite (RN02)';

-- -----------------------------------------------------------------------------
-- loteamento  (classe Loteamento; composição com regiao)
-- -----------------------------------------------------------------------------
CREATE TABLE loteamento (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY,
    regiao_id          BIGINT           NOT NULL,
    denominacao        VARCHAR(150)     NOT NULL,
    data_aprovacao     DATE             NULL,
    area_gleba_m2      DOUBLE PRECISION NOT NULL,
    populacao_estimada INT              NOT NULL,
    ocupado            BOOLEAN          NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_loteamento PRIMARY KEY (id),
    CONSTRAINT fk_loteamento_regiao FOREIGN KEY (regiao_id)
        REFERENCES regiao (id) ON DELETE CASCADE,
    CONSTRAINT ck_loteamento_area_gleba_positiva CHECK (area_gleba_m2 > 0),
    CONSTRAINT ck_loteamento_populacao_estimada_nao_negativa CHECK (populacao_estimada >= 0)
);

COMMENT ON TABLE loteamento IS 'RF02 - loteamento vinculado obrigatoriamente a uma região (composição). Classe Java: Loteamento';
COMMENT ON COLUMN loteamento.regiao_id IS 'RN17 - loteamento só pode ser vinculado a região previamente cadastrada';
COMMENT ON COLUMN loteamento.denominacao IS 'Denominação do loteamento';
COMMENT ON COLUMN loteamento.data_aprovacao IS 'RN13 - data de aprovação; NULL = ainda não aprovado (não entra na população projetada)';
COMMENT ON COLUMN loteamento.area_gleba_m2 IS 'Área da gleba em m²; a soma das áreas dos lotes não pode excedê-la';
COMMENT ON COLUMN loteamento.populacao_estimada IS 'RN13 - população estimada somada à projetada se aprovado e não ocupado';
COMMENT ON COLUMN loteamento.ocupado IS 'RN13 - TRUE = já ocupado (população já contada na população atual)';

-- -----------------------------------------------------------------------------
-- lote  (classe Lote; composição com loteamento)
-- -----------------------------------------------------------------------------
CREATE TABLE lote (
    id            BIGINT GENERATED ALWAYS AS IDENTITY,
    loteamento_id BIGINT           NOT NULL,
    quadra        VARCHAR(20)      NOT NULL,
    numero        VARCHAR(20)      NOT NULL,
    area_m2       DOUBLE PRECISION NOT NULL,
    frente_m      DOUBLE PRECISION NOT NULL,
    CONSTRAINT pk_lote PRIMARY KEY (id),
    CONSTRAINT fk_lote_loteamento FOREIGN KEY (loteamento_id)
        REFERENCES loteamento (id) ON DELETE CASCADE,
    CONSTRAINT uk_lote_loteamento_quadra_numero UNIQUE (loteamento_id, quadra, numero),
    CONSTRAINT ck_lote_area_minima CHECK (area_m2 >= 125),
    CONSTRAINT ck_lote_frente_minima CHECK (frente_m >= 5)
);

COMMENT ON TABLE lote IS 'RF06 - lote de um loteamento (composição). Classe Java: Lote';
COMMENT ON COLUMN lote.loteamento_id IS 'Loteamento ao qual o lote pertence';
COMMENT ON COLUMN lote.quadra IS 'Quadra do lote';
COMMENT ON COLUMN lote.numero IS 'Número do lote na quadra';
COMMENT ON COLUMN lote.area_m2 IS 'RN05 - Lei 6.766/79 art. 4º, II: mínimo 125 m²';
COMMENT ON COLUMN lote.frente_m IS 'RN05 - Lei 6.766/79 art. 4º, II: frente mínima 5 m';

-- -----------------------------------------------------------------------------
-- equipamento_comunitario  (classe abstrata EquipamentoComunitario;
-- agregação com regiao)
-- -----------------------------------------------------------------------------
CREATE TABLE equipamento_comunitario (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY,
    regiao_id            BIGINT       NULL,
    nome                 VARCHAR(150) NOT NULL,
    capacidade_instalada INT          NOT NULL,
    tipo                 VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_equipamento_comunitario PRIMARY KEY (id),
    CONSTRAINT fk_equipamento_comunitario_regiao FOREIGN KEY (regiao_id)
        REFERENCES regiao (id) ON DELETE SET NULL,
    CONSTRAINT ck_equipamento_comunitario_capacidade_nao_negativa CHECK (capacidade_instalada >= 0),
    CONSTRAINT ck_equipamento_comunitario_tipo CHECK (tipo IN ('ESCOLA', 'UNIDADE_SAUDE'))
);

COMMENT ON TABLE equipamento_comunitario IS 'RF03 - equipamento comunitário (classe-mãe; herança JOINED). Classe Java abstrata: EquipamentoComunitario';
COMMENT ON COLUMN equipamento_comunitario.regiao_id IS 'Agregação: região atendida; NULL = sem região (ON DELETE SET NULL)';
COMMENT ON COLUMN equipamento_comunitario.nome IS 'Nome do equipamento';
COMMENT ON COLUMN equipamento_comunitario.capacidade_instalada IS 'RN11 - capacidade instalada (escola: vagas; unidade de saúde: equipes)';
COMMENT ON COLUMN equipamento_comunitario.tipo IS 'Discriminador da herança: ESCOLA ou UNIDADE_SAUDE (EquipamentoComunitario.getTipo())';

-- -----------------------------------------------------------------------------
-- escola  (classe Escola extends EquipamentoComunitario)
-- -----------------------------------------------------------------------------
CREATE TABLE escola (
    id                       BIGINT           NOT NULL,
    percentual_idade_escolar DOUBLE PRECISION NOT NULL,
    nivel_ensino             VARCHAR(50)      NOT NULL,
    CONSTRAINT pk_escola PRIMARY KEY (id),
    CONSTRAINT fk_escola_equipamento_comunitario FOREIGN KEY (id)
        REFERENCES equipamento_comunitario (id) ON DELETE CASCADE,
    CONSTRAINT ck_escola_percentual_idade_escolar CHECK (percentual_idade_escolar > 0 AND percentual_idade_escolar <= 100)
);

COMMENT ON TABLE escola IS 'Subtabela de equipamento_comunitario (tipo = ESCOLA). Classe Java: Escola';
COMMENT ON COLUMN escola.id IS 'PK e FK para equipamento_comunitario.id (herança JOINED)';
COMMENT ON COLUMN escola.percentual_idade_escolar IS 'RN12 - percentual da população em idade escolar (0-100], base da demanda da escola';
COMMENT ON COLUMN escola.nivel_ensino IS 'Nível de ensino ofertado';

-- -----------------------------------------------------------------------------
-- unidade_saude  (classe UnidadeSaude extends EquipamentoComunitario)
-- -----------------------------------------------------------------------------
CREATE TABLE unidade_saude (
    id                    BIGINT      NOT NULL,
    habitantes_por_equipe INT         NOT NULL,
    tipo_unidade          VARCHAR(50) NOT NULL,
    CONSTRAINT pk_unidade_saude PRIMARY KEY (id),
    CONSTRAINT fk_unidade_saude_equipamento_comunitario FOREIGN KEY (id)
        REFERENCES equipamento_comunitario (id) ON DELETE CASCADE,
    CONSTRAINT ck_unidade_saude_habitantes_por_equipe_positivo CHECK (habitantes_por_equipe > 0)
);

COMMENT ON TABLE unidade_saude IS 'Subtabela de equipamento_comunitario (tipo = UNIDADE_SAUDE). Classe Java: UnidadeSaude';
COMMENT ON COLUMN unidade_saude.id IS 'PK e FK para equipamento_comunitario.id (herança JOINED)';
COMMENT ON COLUMN unidade_saude.habitantes_por_equipe IS 'RN12 - habitantes atendidos por equipe; demanda = ceil(população total / habitantes_por_equipe)';
COMMENT ON COLUMN unidade_saude.tipo_unidade IS 'Tipo da unidade de saúde (ex.: UBS)';

-- -----------------------------------------------------------------------------
-- analista  (classe Analista)
-- -----------------------------------------------------------------------------
CREATE TABLE analista (
    id        BIGINT GENERATED ALWAYS AS IDENTITY,
    nome      VARCHAR(150) NOT NULL,
    matricula VARCHAR(30)  NOT NULL,
    setor     VARCHAR(80)  NOT NULL,
    CONSTRAINT pk_analista PRIMARY KEY (id),
    CONSTRAINT uk_analista_matricula UNIQUE (matricula)
);

COMMENT ON TABLE analista IS 'RF07/RN16 - servidor responsável pela emissão das análises. Classe Java: Analista';
COMMENT ON COLUMN analista.nome IS 'Nome do analista';
COMMENT ON COLUMN analista.matricula IS 'Matrícula funcional única';
COMMENT ON COLUMN analista.setor IS 'Setor de lotação';

-- -----------------------------------------------------------------------------
-- criterio_vedacao  (classe abstrata CriterioVedacao)
-- -----------------------------------------------------------------------------
CREATE TABLE criterio_vedacao (
    id               BIGINT GENERATED ALWAYS AS IDENTITY,
    regiao_id        BIGINT       NOT NULL,
    codigo_interno   VARCHAR(30)  NOT NULL,
    fundamento_legal VARCHAR(150) NOT NULL,
    tipo             VARCHAR(30)  NOT NULL,
    CONSTRAINT pk_criterio_vedacao PRIMARY KEY (id),
    CONSTRAINT fk_criterio_vedacao_regiao FOREIGN KEY (regiao_id)
        REFERENCES regiao (id) ON DELETE CASCADE,
    CONSTRAINT uk_criterio_vedacao_codigo_interno UNIQUE (codigo_interno),
    CONSTRAINT ck_criterio_vedacao_tipo CHECK (tipo IN ('ALAGAMENTO', 'DECLIVIDADE', 'GEOLOGICO', 'PRESERVACAO'))
);

COMMENT ON TABLE criterio_vedacao IS 'RF01/RN01-RN04 - critério de vedação legal ao parcelamento (classe-mãe; herança JOINED). Classe Java abstrata: CriterioVedacao';
COMMENT ON COLUMN criterio_vedacao.regiao_id IS '[EXT] E2 - região à qual o critério se aplica (os fatos das subclasses são de UMA região)';
COMMENT ON COLUMN criterio_vedacao.codigo_interno IS 'Código interno único do critério (ex.: ALG-NORTE-01)';
COMMENT ON COLUMN criterio_vedacao.fundamento_legal IS 'Fundamento legal (Lei 6.766/79, art. 3º, parágrafo único, I/III/IV/V)';
COMMENT ON COLUMN criterio_vedacao.tipo IS 'Discriminador da herança: ALAGAMENTO, DECLIVIDADE, GEOLOGICO ou PRESERVACAO (CriterioVedacao.getTipo())';

-- -----------------------------------------------------------------------------
-- criterio_alagamento  (classe CriterioAlagamento extends CriterioVedacao)
-- -----------------------------------------------------------------------------
CREATE TABLE criterio_alagamento (
    id                   BIGINT  NOT NULL,
    saneamento_executado BOOLEAN NOT NULL,
    CONSTRAINT pk_criterio_alagamento PRIMARY KEY (id),
    CONSTRAINT fk_criterio_alagamento_criterio_vedacao FOREIGN KEY (id)
        REFERENCES criterio_vedacao (id) ON DELETE CASCADE
);

COMMENT ON TABLE criterio_alagamento IS 'RN01 - Lei 6.766/79, art. 3º, p.ú., I: terreno alagadiço/inundável. Classe Java: CriterioAlagamento';
COMMENT ON COLUMN criterio_alagamento.id IS 'PK e FK para criterio_vedacao.id (herança JOINED)';
COMMENT ON COLUMN criterio_alagamento.saneamento_executado IS 'RN01 - não parcelar antes do saneamento; FALSE = reprovado (impedimento superável => APTA_COM_RESTRICAO)';

-- -----------------------------------------------------------------------------
-- criterio_declividade  (classe CriterioDeclividade extends CriterioVedacao)
-- -----------------------------------------------------------------------------
CREATE TABLE criterio_declividade (
    id                 BIGINT           NOT NULL,
    declividade_limite DOUBLE PRECISION NOT NULL DEFAULT 30,
    CONSTRAINT pk_criterio_declividade PRIMARY KEY (id),
    CONSTRAINT fk_criterio_declividade_criterio_vedacao FOREIGN KEY (id)
        REFERENCES criterio_vedacao (id) ON DELETE CASCADE,
    CONSTRAINT ck_criterio_declividade_limite_positivo CHECK (declividade_limite > 0)
);

COMMENT ON TABLE criterio_declividade IS 'RN02 - Lei 6.766/79, art. 3º, p.ú., III: declividade igual ou superior a 30%. Classe Java: CriterioDeclividade';
COMMENT ON COLUMN criterio_declividade.id IS 'PK e FK para criterio_vedacao.id (herança JOINED)';
COMMENT ON COLUMN criterio_declividade.declividade_limite IS 'RN02 - limite de declividade (%), padrão 30; reprovado se regiao.declividade_media_pct >= limite';

-- -----------------------------------------------------------------------------
-- criterio_geologico  (classe CriterioGeologico extends CriterioVedacao)
-- -----------------------------------------------------------------------------
CREATE TABLE criterio_geologico (
    id               BIGINT       NOT NULL,
    laudo_geotecnico VARCHAR(150) NOT NULL,
    CONSTRAINT pk_criterio_geologico PRIMARY KEY (id),
    CONSTRAINT fk_criterio_geologico_criterio_vedacao FOREIGN KEY (id)
        REFERENCES criterio_vedacao (id) ON DELETE CASCADE,
    CONSTRAINT ck_criterio_geologico_laudo_formato CHECK (laudo_geotecnico ~ '^(ADEQUADO|INADEQUADO)')
);

COMMENT ON TABLE criterio_geologico IS 'RN03 - Lei 6.766/79, art. 3º, p.ú., IV: condições geológicas não aconselham a edificação. Classe Java: CriterioGeologico';
COMMENT ON COLUMN criterio_geologico.id IS 'PK e FK para criterio_vedacao.id (herança JOINED)';
COMMENT ON COLUMN criterio_geologico.laudo_geotecnico IS 'RN03 - "ADEQUADO" ou "INADEQUADO", opcionalmente seguido de ": resumo do laudo"; INADEQUADO = impedimento absoluto (INAPTA)';

-- -----------------------------------------------------------------------------
-- criterio_preservacao  (classe CriterioPreservacao extends CriterioVedacao)
-- -----------------------------------------------------------------------------
CREATE TABLE criterio_preservacao (
    id             BIGINT  NOT NULL,
    area_protegida BOOLEAN NOT NULL,
    CONSTRAINT pk_criterio_preservacao PRIMARY KEY (id),
    CONSTRAINT fk_criterio_preservacao_criterio_vedacao FOREIGN KEY (id)
        REFERENCES criterio_vedacao (id) ON DELETE CASCADE
);

COMMENT ON TABLE criterio_preservacao IS 'RN04 - Lei 6.766/79, art. 3º, p.ú., V: área de preservação ecológica. Classe Java: CriterioPreservacao';
COMMENT ON COLUMN criterio_preservacao.id IS 'PK e FK para criterio_vedacao.id (herança JOINED)';
COMMENT ON COLUMN criterio_preservacao.area_protegida IS 'RN04 - TRUE = área de preservação (impedimento absoluto => INAPTA)';

-- -----------------------------------------------------------------------------
-- analise_parcelamento  (classe AnaliseParcelamento)
-- -----------------------------------------------------------------------------
CREATE TABLE analise_parcelamento (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY,
    regiao_id                 BIGINT           NOT NULL,
    analista_id               BIGINT           NOT NULL,
    data_emissao              TIMESTAMP        NOT NULL DEFAULT now(),
    versao_dados              VARCHAR(50)      NOT NULL,
    resultado                 VARCHAR(25)      NOT NULL,
    indice_area_verde_apurado DOUBLE PRECISION NULL,
    parecer_textual           TEXT             NOT NULL,
    CONSTRAINT pk_analise_parcelamento PRIMARY KEY (id),
    CONSTRAINT fk_analise_parcelamento_regiao FOREIGN KEY (regiao_id)
        REFERENCES regiao (id) ON DELETE RESTRICT,
    CONSTRAINT fk_analise_parcelamento_analista FOREIGN KEY (analista_id)
        REFERENCES analista (id) ON DELETE RESTRICT,
    CONSTRAINT ck_analise_parcelamento_resultado CHECK (resultado IN ('APTA', 'APTA_COM_RESTRICAO', 'INAPTA'))
);

COMMENT ON TABLE analise_parcelamento IS 'RF01/RF07/RN15/RN16 - análise de parcelamento emitida (imutável). Classe Java: AnaliseParcelamento';
COMMENT ON COLUMN analise_parcelamento.regiao_id IS 'Região analisada; RESTRICT preserva a rastreabilidade (RN16)';
COMMENT ON COLUMN analise_parcelamento.analista_id IS 'RF07/RN16 - responsável pela análise; RESTRICT preserva a rastreabilidade';
COMMENT ON COLUMN analise_parcelamento.data_emissao IS 'RF07/RN16 - data e hora de emissão';
COMMENT ON COLUMN analise_parcelamento.versao_dados IS 'RF07/RN16 - versão dos dados utilizados ("v-" + 12 hex do SHA-256 dos dados)';
COMMENT ON COLUMN analise_parcelamento.resultado IS 'RN15 - aptidão: APTA, APTA_COM_RESTRICAO ou INAPTA';
COMMENT ON COLUMN analise_parcelamento.indice_area_verde_apurado IS 'RN08/RN14 - IAV apurado com a população projetada; NULL se população projetada = 0';
COMMENT ON COLUMN analise_parcelamento.parecer_textual IS 'Parecer textual completo gerado pela análise';

-- -----------------------------------------------------------------------------
-- analise_criterio  (N:N análise x critério; classe ResultadoCriterio)
-- -----------------------------------------------------------------------------
CREATE TABLE analise_criterio (
    analise_id            BIGINT  NOT NULL,
    criterio_id           BIGINT  NOT NULL,
    resultado             BOOLEAN NOT NULL,
    descricao_impedimento TEXT    NULL,
    CONSTRAINT pk_analise_criterio PRIMARY KEY (analise_id, criterio_id),
    CONSTRAINT fk_analise_criterio_analise_parcelamento FOREIGN KEY (analise_id)
        REFERENCES analise_parcelamento (id) ON DELETE CASCADE,
    CONSTRAINT fk_analise_criterio_criterio_vedacao FOREIGN KEY (criterio_id)
        REFERENCES criterio_vedacao (id) ON DELETE RESTRICT
);

COMMENT ON TABLE analise_criterio IS 'RN15 - resultado de cada critério de vedação em uma análise (N:N). Classe Java: ResultadoCriterio';
COMMENT ON COLUMN analise_criterio.analise_id IS 'Análise à qual o resultado pertence';
COMMENT ON COLUMN analise_criterio.criterio_id IS 'Critério avaliado; RESTRICT impede excluir critério usado em análise (RN16)';
COMMENT ON COLUMN analise_criterio.resultado IS 'RN15 - TRUE = critério atendido (sem impedimento)';
COMMENT ON COLUMN analise_criterio.descricao_impedimento IS 'Descrição do impedimento; preenchida só quando resultado = FALSE';

-- -----------------------------------------------------------------------------
-- Índices nas FKs que não são PK
-- -----------------------------------------------------------------------------
CREATE INDEX idx_loteamento_regiao_id              ON loteamento (regiao_id);
CREATE INDEX idx_lote_loteamento_id                ON lote (loteamento_id);
CREATE INDEX idx_equipamento_comunitario_regiao_id ON equipamento_comunitario (regiao_id);
CREATE INDEX idx_criterio_vedacao_regiao_id        ON criterio_vedacao (regiao_id);
CREATE INDEX idx_analise_parcelamento_regiao_id    ON analise_parcelamento (regiao_id);
CREATE INDEX idx_analise_parcelamento_analista_id  ON analise_parcelamento (analista_id);
CREATE INDEX idx_analise_criterio_criterio_id      ON analise_criterio (criterio_id);

COMMIT;
