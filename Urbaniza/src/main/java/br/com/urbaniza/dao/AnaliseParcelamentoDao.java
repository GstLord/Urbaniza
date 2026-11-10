package br.com.urbaniza.dao;

import br.com.urbaniza.config.ConnectionFactory;
import br.com.urbaniza.exception.PersistenciaException;
import br.com.urbaniza.model.AnaliseParcelamento;
import br.com.urbaniza.model.Analista;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.model.ResultadoCriterio;
import br.com.urbaniza.model.criterio.CriterioVedacao;
import br.com.urbaniza.model.enums.AptidaoParcelamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistência de {@link AnaliseParcelamento} (tabelas analise_parcelamento e analise_criterio).
 * Implementa somente {@link Dao} — SEM atualizar: análise emitida é imutável (RN16).
 * Inserir grava a análise e os resultados por critério na mesma transação.
 */
public class AnaliseParcelamentoDao implements DaoPorRegiao<AnaliseParcelamento> {

    private static final String SQL_INSERIR = """
            INSERT INTO analise_parcelamento (regiao_id, analista_id, data_emissao, versao_dados, resultado,
                                              indice_area_verde_apurado, parecer_textual)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id""";
    private static final String SQL_INSERIR_RESULTADO = """
            INSERT INTO analise_criterio (analise_id, criterio_id, resultado, descricao_impedimento)
            VALUES (?, ?, ?, ?)""";
    private static final String SQL_BUSCAR = """
            SELECT a.id, a.data_emissao, a.versao_dados, a.resultado, a.indice_area_verde_apurado,
                   a.parecer_textual,
                   r.id AS r_id, r.nome AS r_nome, r.area_total_m2 AS r_area_total_m2,
                   r.area_verde_m2 AS r_area_verde_m2, r.area_impermeavel_m2 AS r_area_impermeavel_m2,
                   r.populacao_atual AS r_populacao_atual, r.declividade_media_pct AS r_declividade_media_pct,
                   an.id AS an_id, an.nome AS an_nome, an.matricula AS an_matricula, an.setor AS an_setor
              FROM analise_parcelamento a
              JOIN regiao r ON r.id = a.regiao_id
              JOIN analista an ON an.id = a.analista_id
             WHERE a.id = ?""";
    private static final String SQL_LISTAR = """
            SELECT a.id, a.data_emissao, a.versao_dados, a.resultado, a.indice_area_verde_apurado,
                   a.parecer_textual,
                   r.id AS r_id, r.nome AS r_nome, r.area_total_m2 AS r_area_total_m2,
                   r.area_verde_m2 AS r_area_verde_m2, r.area_impermeavel_m2 AS r_area_impermeavel_m2,
                   r.populacao_atual AS r_populacao_atual, r.declividade_media_pct AS r_declividade_media_pct,
                   an.id AS an_id, an.nome AS an_nome, an.matricula AS an_matricula, an.setor AS an_setor
              FROM analise_parcelamento a
              JOIN regiao r ON r.id = a.regiao_id
              JOIN analista an ON an.id = a.analista_id
             ORDER BY a.id""";
    private static final String SQL_LISTAR_POR_REGIAO = """
            SELECT a.id, a.data_emissao, a.versao_dados, a.resultado, a.indice_area_verde_apurado,
                   a.parecer_textual,
                   r.id AS r_id, r.nome AS r_nome, r.area_total_m2 AS r_area_total_m2,
                   r.area_verde_m2 AS r_area_verde_m2, r.area_impermeavel_m2 AS r_area_impermeavel_m2,
                   r.populacao_atual AS r_populacao_atual, r.declividade_media_pct AS r_declividade_media_pct,
                   an.id AS an_id, an.nome AS an_nome, an.matricula AS an_matricula, an.setor AS an_setor
              FROM analise_parcelamento a
              JOIN regiao r ON r.id = a.regiao_id
              JOIN analista an ON an.id = a.analista_id
             WHERE a.regiao_id = ?
             ORDER BY a.id""";
    private static final String SQL_RESULTADOS = """
            SELECT ac.resultado AS ac_resultado, ac.descricao_impedimento AS ac_descricao_impedimento,
                   c.id AS c_id, c.regiao_id AS c_regiao_id, c.codigo_interno AS c_codigo_interno,
                   c.fundamento_legal AS c_fundamento_legal, c.tipo AS c_tipo,
                   ca.saneamento_executado AS c_saneamento_executado,
                   cd.declividade_limite AS c_declividade_limite,
                   cg.laudo_geotecnico AS c_laudo_geotecnico,
                   cp.area_protegida AS c_area_protegida
              FROM analise_criterio ac
              JOIN criterio_vedacao c ON c.id = ac.criterio_id
              LEFT JOIN criterio_alagamento ca ON ca.id = c.id
              LEFT JOIN criterio_declividade cd ON cd.id = c.id
              LEFT JOIN criterio_geologico cg ON cg.id = c.id
              LEFT JOIN criterio_preservacao cp ON cp.id = c.id
             WHERE ac.analise_id = ?
             ORDER BY c.id""";
    private static final String SQL_EXCLUIR = """
            DELETE FROM analise_parcelamento WHERE id = ?""";

    private static final String MSG_REFERENCIA_INEXISTENTE =
            "A região, o analista ou algum critério da análise não está mais cadastrado.";

    @Override
    public AnaliseParcelamento inserir(AnaliseParcelamento analise) {
        validarParaInsercao(analise);
        try (Connection conexao = ConnectionFactory.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR)) {
                    ps.setLong(1, analise.getRegiao().getId());
                    ps.setLong(2, analise.getAnalista().getId());
                    JdbcUtil.setDataHora(ps, 3, analise.getDataEmissao());
                    ps.setString(4, analise.getVersaoDados());
                    ps.setString(5, analise.getResultado().name());
                    JdbcUtil.setDoubleOuNulo(ps, 6, analise.getIndiceAreaVerdeApurado());
                    ps.setString(7, analise.getParecerTextual());
                    analise.setId(JdbcUtil.executarRetornandoId(ps));
                }
                try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR_RESULTADO)) {
                    for (ResultadoCriterio rc : analise.getResultados()) {
                        ps.setLong(1, analise.getId());
                        ps.setLong(2, rc.criterio().getId());
                        ps.setBoolean(3, rc.atendido());
                        JdbcUtil.setTextoOuNulo(ps, 4, rc.descricaoImpedimento());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conexao.commit();
                return analise;
            } catch (SQLException | RuntimeException e) {
                JdbcUtil.rollbackSilencioso(conexao, e);
                analise.setId(null);
                throw e;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "gravar a análise", MSG_REFERENCIA_INEXISTENTE, null);
        }
    }

    @Override
    public Optional<AnaliseParcelamento> buscarPorId(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_BUSCAR)) {
            ps.setLong(1, id);
            List<AnaliseParcelamento> analises = carregar(conexao, ps);
            return analises.isEmpty() ? Optional.empty() : Optional.of(analises.get(0));
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar a análise");
        }
    }

    @Override
    public List<AnaliseParcelamento> listarTodos() {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR)) {
            return carregar(conexao, ps);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar as análises");
        }
    }

    /** Lista as análises emitidas para uma região. */
    @Override
    public List<AnaliseParcelamento> listarPorRegiao(long regiaoId) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR_POR_REGIAO)) {
            ps.setLong(1, regiaoId);
            return carregar(conexao, ps);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar as análises da região");
        }
    }

    @Override
    public void excluir(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_EXCLUIR)) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Análise " + id + " não encontrada.");
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir a análise");
        }
    }

    private static void validarParaInsercao(AnaliseParcelamento analise) {
        if (analise.getRegiao() == null || analise.getRegiao().getId() == null
                || analise.getAnalista() == null || analise.getAnalista().getId() == null
                || analise.getDataEmissao() == null || analise.getVersaoDados() == null
                || analise.getResultado() == null || analise.getParecerTextual() == null) {
            throw new PersistenciaException("A análise precisa ser executada antes de ser gravada.");
        }
        for (ResultadoCriterio rc : analise.getResultados()) {
            if (rc.criterio().getId() == null) {
                throw new PersistenciaException("Critério sem id na análise: " + rc.criterio().getCodigoInterno());
            }
        }
    }

    /** Executa a consulta de análises e reconstrói cada uma com região, analista e resultados. */
    private static List<AnaliseParcelamento> carregar(Connection conexao, PreparedStatement ps) throws SQLException {
        List<AnaliseParcelamento> analises = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                analises.add(mapear(rs));
            }
        }
        try (PreparedStatement psResultados = conexao.prepareStatement(SQL_RESULTADOS)) {
            for (AnaliseParcelamento analise : analises) {
                psResultados.setLong(1, analise.getId());
                List<ResultadoCriterio> resultados = new ArrayList<>();
                List<CriterioVedacao> criterios = new ArrayList<>();
                try (ResultSet rs = psResultados.executeQuery()) {
                    while (rs.next()) {
                        CriterioVedacao criterio = CriterioVedacaoDao.mapear(rs);
                        criterios.add(criterio);
                        resultados.add(new ResultadoCriterio(criterio, rs.getBoolean("ac_resultado"),
                                rs.getString("ac_descricao_impedimento")));
                    }
                }
                analise.setCriterios(criterios);
                analise.setResultados(resultados);
            }
        }
        return analises;
    }

    private static AnaliseParcelamento mapear(ResultSet rs) throws SQLException {
        Regiao regiao = RegiaoDao.mapear(rs, "r_");
        Analista analista = AnalistaDao.mapear(rs, "an_");
        return new AnaliseParcelamento(
                rs.getLong("id"),
                regiao,
                analista,
                JdbcUtil.getDataHora(rs, "data_emissao"),
                rs.getString("versao_dados"),
                AptidaoParcelamento.valueOf(rs.getString("resultado")),
                JdbcUtil.getDoubleOuNulo(rs, "indice_area_verde_apurado"),
                rs.getString("parecer_textual"),
                null,
                null);
    }
}
