-- =============================================================================
-- Projeto : Urbaniza - Sistema de Controle de Urbanização por Regiões
-- Arquivo : 03_consultas_verificacao.sql
-- Propósito: consultas SOMENTE DE LEITURA que reproduzem em SQL os cálculos
--           das classes Java do pacote br.com.urbaniza.model (seção C5 do
--           contrato técnico), para conferir os valores "ESPERADO" da seção C8.
--           Nenhuma consulta altera dados.
--
-- Como executar:
--   docker exec -i urbaniza-db psql -U urbaniza -d urbaniza < 03_consultas_verificacao.sql
--   psql -U urbaniza -d urbaniza -f Urbaniza/database/03_consultas_verificacao.sql
--
-- Valores esperados com 02_dados_exemplo.sql (seção C8):
--   Norte  : pop. projetada 20.400 | IAV 8,82 DEFICITARIO  | impermeab. 45,00% | ALTO  (9%)
--            escola demanda 3.264 índice 0,86 déficit | UBS demanda 6 índice 0,83 déficit
--   Central: pop. projetada 21.000 | IAV 14,29 ADEQUADO   | impermeab. 50,00% | MEDIO (25%)
--            escola demanda 3.150 índice 1,27 | UBS demanda 7 índice 1,00 (sem déficit)
--   Sul    : pop. projetada  6.000 | IAV 250,00 ADEQUADO  | impermeab. 10,00% | BAIXO (50%)
--            UBS demanda 2 índice 0,50 déficit
-- Observação: números exibidos com ponto decimal (padrão do PostgreSQL).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1) RN13 / RF04 - População projetada
--    = populacao_atual + soma de populacao_estimada dos loteamentos APROVADOS
--      (data_aprovacao IS NOT NULL) e AINDA NÃO OCUPADOS (ocupado = FALSE).
--    Java: Regiao.calcularPopulacaoProjetada() / Loteamento.contribuiParaProjecao()
-- -----------------------------------------------------------------------------
SELECT r.id,
       r.nome,
       r.populacao_atual,
       COALESCE(SUM(l.populacao_estimada)
                FILTER (WHERE l.data_aprovacao IS NOT NULL AND NOT l.ocupado), 0)
           AS populacao_loteamentos_contabilizados,
       r.populacao_atual
         + COALESCE(SUM(l.populacao_estimada)
                    FILTER (WHERE l.data_aprovacao IS NOT NULL AND NOT l.ocupado), 0)
           AS populacao_projetada
FROM regiao r
LEFT JOIN loteamento l ON l.regiao_id = r.id
GROUP BY r.id, r.nome, r.populacao_atual
ORDER BY r.id;

-- -----------------------------------------------------------------------------
-- 2) RN08 / RN14 / RF05 - Índice de Área Verde por Habitante (IAV)
--    IAV = area_verde_m2 / população PROJETADA (nunca a atual - RN14).
--    > 12 ADEQUADO | >= 9 e <= 12 ACEITAVEL | < 9 DEFICITARIO.
--    População projetada = 0 => IAV infinito (Java: Double.POSITIVE_INFINITY)
--    => classificado como ADEQUADO.
--    Java: Regiao.calcularIAV() / Regiao.classificarIAV()
-- -----------------------------------------------------------------------------
WITH pop AS (
    SELECT r.id,
           r.populacao_atual
             + COALESCE(SUM(l.populacao_estimada)
                        FILTER (WHERE l.data_aprovacao IS NOT NULL AND NOT l.ocupado), 0)
               AS populacao_projetada
    FROM regiao r
    LEFT JOIN loteamento l ON l.regiao_id = r.id
    GROUP BY r.id, r.populacao_atual
), iav AS (
    SELECT r.id,
           r.nome,
           r.area_verde_m2,
           p.populacao_projetada,
           CASE WHEN p.populacao_projetada = 0 THEN 'Infinity'::double precision
                ELSE r.area_verde_m2 / p.populacao_projetada
           END AS iav
    FROM regiao r
    JOIN pop p ON p.id = r.id
)
SELECT id,
       nome,
       area_verde_m2,
       populacao_projetada,
       CASE WHEN iav = 'Infinity'::double precision THEN 'não aplicável'
            ELSE to_char(round(iav::numeric, 2), 'FM999999990.00')
       END AS iav_m2_por_hab,
       CASE WHEN iav > 12 THEN 'ADEQUADO'
            WHEN iav >= 9 THEN 'ACEITAVEL'
            ELSE 'DEFICITARIO'
       END AS classificacao_iav
FROM iav
ORDER BY id;

-- -----------------------------------------------------------------------------
-- 3) RN09 - Taxa de impermeabilização = area_impermeavel_m2 / area_total_m2
--    Java: Regiao.calcularTaxaImpermeabilizacao() (0..1; exibida em %)
-- -----------------------------------------------------------------------------
SELECT id,
       nome,
       area_impermeavel_m2,
       area_total_m2,
       round((area_impermeavel_m2 / area_total_m2)::numeric, 4)       AS taxa_impermeabilizacao,
       round((area_impermeavel_m2 / area_total_m2 * 100)::numeric, 2) AS taxa_impermeabilizacao_pct
FROM regiao
ORDER BY id;

-- -----------------------------------------------------------------------------
-- 4) RN10 / E6 - Cobertura vegetal e potencial de ilha de calor
--    cobertura = area_verde_m2 / area_total_m2
--    >= 0,30 BAIXO | >= 0,15 MEDIO | < 0,15 ALTO
--    (limiares em ParametrosUrbanisticos.COBERTURA_VEGETAL_RISCO_BAIXO/MEDIO)
--    Java: Regiao.classificarIlhaDeCalor()
-- -----------------------------------------------------------------------------
SELECT id,
       nome,
       round((area_verde_m2 / area_total_m2 * 100)::numeric, 2) AS cobertura_vegetal_pct,
       CASE WHEN area_verde_m2 / area_total_m2 >= 0.30 THEN 'BAIXO'
            WHEN area_verde_m2 / area_total_m2 >= 0.15 THEN 'MEDIO'
            ELSE 'ALTO'
       END AS potencial_ilha_calor
FROM regiao
ORDER BY id;

-- -----------------------------------------------------------------------------
-- 5) RN11 / RN12 / RF03 - Demanda e índice de atendimento de cada equipamento
--    (calculados com a população PROJETADA da região - RN14)
--    Escola        : demanda = CEIL(pop * percentual_idade_escolar / 100.0)
--    Unidade saúde : demanda = CEIL(pop / habitantes_por_equipe)
--    índice = capacidade_instalada / demanda; demanda = 0 => infinito
--    ("sem demanda"); índice < 1 => déficit.
--    Java: Escola/UnidadeSaude.calcularDemanda(),
--          EquipamentoComunitario.calcularIndiceAtendimento()/possuiDeficit()
--    Equipamentos sem região (regiao_id NULL) não têm população de referência
--    e por isso não aparecem aqui.
-- -----------------------------------------------------------------------------
WITH pop AS (
    SELECT r.id,
           r.populacao_atual
             + COALESCE(SUM(l.populacao_estimada)
                        FILTER (WHERE l.data_aprovacao IS NOT NULL AND NOT l.ocupado), 0)
               AS populacao_projetada
    FROM regiao r
    LEFT JOIN loteamento l ON l.regiao_id = r.id
    GROUP BY r.id, r.populacao_atual
), dem AS (
    SELECT e.id,
           r.nome AS regiao,
           e.tipo,
           e.nome,
           e.capacidade_instalada,
           p.populacao_projetada,
           CASE e.tipo
               WHEN 'ESCOLA'        THEN CEIL(p.populacao_projetada * es.percentual_idade_escolar / 100.0)::int
               WHEN 'UNIDADE_SAUDE' THEN CEIL(p.populacao_projetada::double precision / us.habitantes_por_equipe)::int
           END AS demanda
    FROM equipamento_comunitario e
    JOIN regiao r            ON r.id = e.regiao_id
    JOIN pop p               ON p.id = r.id
    LEFT JOIN escola es      ON es.id = e.id
    LEFT JOIN unidade_saude us ON us.id = e.id
)
SELECT id,
       regiao,
       tipo,
       nome,
       capacidade_instalada,
       populacao_projetada,
       demanda,
       CASE WHEN demanda = 0 THEN 'sem demanda'
            ELSE to_char(round((capacidade_instalada::double precision / demanda)::numeric, 2), 'FM999999990.00')
       END AS indice_atendimento,
       CASE WHEN demanda > 0 AND capacidade_instalada::double precision / demanda < 1.0
            THEN 'DÉFICIT' ELSE 'sem déficit'
       END AS situacao
FROM dem
ORDER BY id;

-- -----------------------------------------------------------------------------
-- 6) RN11 - Ranking de déficit: regiões ordenadas pela quantidade de
--    equipamentos com índice de atendimento < 1 (população projetada).
-- -----------------------------------------------------------------------------
WITH pop AS (
    SELECT r.id,
           r.populacao_atual
             + COALESCE(SUM(l.populacao_estimada)
                        FILTER (WHERE l.data_aprovacao IS NOT NULL AND NOT l.ocupado), 0)
               AS populacao_projetada
    FROM regiao r
    LEFT JOIN loteamento l ON l.regiao_id = r.id
    GROUP BY r.id, r.populacao_atual
), dem AS (
    SELECT e.regiao_id,
           e.capacidade_instalada,
           CASE e.tipo
               WHEN 'ESCOLA'        THEN CEIL(p.populacao_projetada * es.percentual_idade_escolar / 100.0)::int
               WHEN 'UNIDADE_SAUDE' THEN CEIL(p.populacao_projetada::double precision / us.habitantes_por_equipe)::int
           END AS demanda
    FROM equipamento_comunitario e
    JOIN pop p                 ON p.id = e.regiao_id
    LEFT JOIN escola es        ON es.id = e.id
    LEFT JOIN unidade_saude us ON us.id = e.id
)
SELECT RANK() OVER (ORDER BY COUNT(*) FILTER (
                        WHERE d.demanda > 0
                          AND d.capacidade_instalada::double precision / d.demanda < 1.0) DESC) AS posicao,
       r.id,
       r.nome,
       COUNT(d.regiao_id) AS equipamentos,
       COUNT(*) FILTER (WHERE d.demanda > 0
                          AND d.capacidade_instalada::double precision / d.demanda < 1.0)
           AS equipamentos_com_deficit
FROM regiao r
LEFT JOIN dem d ON d.regiao_id = r.id
GROUP BY r.id, r.nome
ORDER BY equipamentos_com_deficit DESC, r.id;

-- -----------------------------------------------------------------------------
-- 7) RN01-RN04 / RN15 - Resultado de cada critério de vedação e aptidão
--    esperada por região (o que AnaliseParcelamento.avaliarAptidao() produz).
--    Atendido: ALAGAMENTO = saneamento_executado | DECLIVIDADE =
--    declividade_media_pct < declividade_limite | GEOLOGICO = laudo não começa
--    com "INADEQUADO" | PRESERVACAO = NOT area_protegida.
--    Impedimento absoluto (=> INAPTA): GEOLOGICO e PRESERVACAO.
--    Impedimento superável (=> APTA_COM_RESTRICAO): ALAGAMENTO e DECLIVIDADE.
-- -----------------------------------------------------------------------------
WITH avaliacao AS (
    SELECT c.regiao_id,
           c.codigo_interno,
           c.tipo,
           CASE c.tipo
               WHEN 'ALAGAMENTO'  THEN ca.saneamento_executado
               WHEN 'DECLIVIDADE' THEN r.declividade_media_pct < cd.declividade_limite
               WHEN 'GEOLOGICO'   THEN NOT (upper(trim(cg.laudo_geotecnico)) LIKE 'INADEQUADO%')
               WHEN 'PRESERVACAO' THEN NOT cp.area_protegida
           END AS atendido,
           c.tipo IN ('GEOLOGICO', 'PRESERVACAO') AS impedimento_absoluto
    FROM criterio_vedacao c
    JOIN regiao r                     ON r.id = c.regiao_id
    LEFT JOIN criterio_alagamento ca  ON ca.id = c.id
    LEFT JOIN criterio_declividade cd ON cd.id = c.id
    LEFT JOIN criterio_geologico cg   ON cg.id = c.id
    LEFT JOIN criterio_preservacao cp ON cp.id = c.id
)
SELECT r.id,
       r.nome,
       string_agg(a.codigo_interno || CASE WHEN a.atendido THEN ' atendido' ELSE ' REPROVADO' END,
                  ', ' ORDER BY a.codigo_interno) AS criterios,
       CASE WHEN COUNT(a.codigo_interno) = 0 THEN 'sem critérios (análise não pode ser emitida)'
            WHEN bool_or(NOT a.atendido AND a.impedimento_absoluto) THEN 'INAPTA'
            WHEN bool_or(NOT a.atendido) THEN 'APTA_COM_RESTRICAO'
            ELSE 'APTA'
       END AS aptidao_esperada
FROM regiao r
LEFT JOIN avaliacao a ON a.regiao_id = r.id
GROUP BY r.id, r.nome
ORDER BY r.id;
