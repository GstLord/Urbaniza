package br.com.urbaniza.dao;

import br.com.urbaniza.config.ConnectionFactory;
import br.com.urbaniza.exception.PersistenciaException;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.criterio.CriterioAlagamento;
import br.com.urbaniza.model.criterio.CriterioDeclividade;
import br.com.urbaniza.model.criterio.CriterioGeologico;
import br.com.urbaniza.model.criterio.CriterioPreservacao;
import br.com.urbaniza.model.criterio.CriterioVedacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistência POLIMÓRFICA de {@link CriterioVedacao} (herança JOINED): tabela-mãe criterio_vedacao
 * (coluna discriminadora tipo) + subtabelas criterio_alagamento, criterio_declividade,
 * criterio_geologico e criterio_preservacao, gravadas na mesma transação.
 */
public class CriterioVedacaoDao implements CrudDaoPorRegiao<CriterioVedacao> {

    private static final String SQL_INSERIR_MAE = """
            INSERT INTO criterio_vedacao (regiao_id, codigo_interno, fundamento_legal, tipo)
            VALUES (?, ?, ?, ?)
            RETURNING id""";
    private static final String SQL_INSERIR_ALAGAMENTO = """
            INSERT INTO criterio_alagamento (saneamento_executado, id) VALUES (?, ?)""";
    private static final String SQL_INSERIR_DECLIVIDADE = """
            INSERT INTO criterio_declividade (declividade_limite, id) VALUES (?, ?)""";
    private static final String SQL_INSERIR_GEOLOGICO = """
            INSERT INTO criterio_geologico (laudo_geotecnico, id) VALUES (?, ?)""";
    private static final String SQL_INSERIR_PRESERVACAO = """
            INSERT INTO criterio_preservacao (area_protegida, id) VALUES (?, ?)""";
    private static final String SQL_ATUALIZAR_ALAGAMENTO = """
            UPDATE criterio_alagamento SET saneamento_executado = ? WHERE id = ?""";
    private static final String SQL_ATUALIZAR_DECLIVIDADE = """
            UPDATE criterio_declividade SET declividade_limite = ? WHERE id = ?""";
    private static final String SQL_ATUALIZAR_GEOLOGICO = """
            UPDATE criterio_geologico SET laudo_geotecnico = ? WHERE id = ?""";
    private static final String SQL_ATUALIZAR_PRESERVACAO = """
            UPDATE criterio_preservacao SET area_protegida = ? WHERE id = ?""";
    private static final String SQL_BUSCAR = """
            SELECT c.id AS c_id, c.regiao_id AS c_regiao_id, c.codigo_interno AS c_codigo_interno,
                   c.fundamento_legal AS c_fundamento_legal, c.tipo AS c_tipo,
                   ca.saneamento_executado AS c_saneamento_executado,
                   cd.declividade_limite AS c_declividade_limite,
                   cg.laudo_geotecnico AS c_laudo_geotecnico,
                   cp.area_protegida AS c_area_protegida
              FROM criterio_vedacao c
              LEFT JOIN criterio_alagamento ca ON ca.id = c.id
              LEFT JOIN criterio_declividade cd ON cd.id = c.id
              LEFT JOIN criterio_geologico cg ON cg.id = c.id
              LEFT JOIN criterio_preservacao cp ON cp.id = c.id
             WHERE c.id = ?""";
    private static final String SQL_LISTAR = """
            SELECT c.id AS c_id, c.regiao_id AS c_regiao_id, c.codigo_interno AS c_codigo_interno,
                   c.fundamento_legal AS c_fundamento_legal, c.tipo AS c_tipo,
                   ca.saneamento_executado AS c_saneamento_executado,
                   cd.declividade_limite AS c_declividade_limite,
                   cg.laudo_geotecnico AS c_laudo_geotecnico,
                   cp.area_protegida AS c_area_protegida
              FROM criterio_vedacao c
              LEFT JOIN criterio_alagamento ca ON ca.id = c.id
              LEFT JOIN criterio_declividade cd ON cd.id = c.id
              LEFT JOIN criterio_geologico cg ON cg.id = c.id
              LEFT JOIN criterio_preservacao cp ON cp.id = c.id
             ORDER BY c.id""";
    private static final String SQL_LISTAR_POR_REGIAO = """
            SELECT c.id AS c_id, c.regiao_id AS c_regiao_id, c.codigo_interno AS c_codigo_interno,
                   c.fundamento_legal AS c_fundamento_legal, c.tipo AS c_tipo,
                   ca.saneamento_executado AS c_saneamento_executado,
                   cd.declividade_limite AS c_declividade_limite,
                   cg.laudo_geotecnico AS c_laudo_geotecnico,
                   cp.area_protegida AS c_area_protegida
              FROM criterio_vedacao c
              LEFT JOIN criterio_alagamento ca ON ca.id = c.id
              LEFT JOIN criterio_declividade cd ON cd.id = c.id
              LEFT JOIN criterio_geologico cg ON cg.id = c.id
              LEFT JOIN criterio_preservacao cp ON cp.id = c.id
             WHERE c.regiao_id = ?
             ORDER BY c.id""";
    private static final String SQL_TIPO_ATUAL = """
            SELECT tipo FROM criterio_vedacao WHERE id = ?""";
    private static final String SQL_ATUALIZAR_MAE = """
            UPDATE criterio_vedacao
               SET regiao_id = ?, codigo_interno = ?, fundamento_legal = ?
             WHERE id = ?""";
    private static final String SQL_EXCLUIR = """
            DELETE FROM criterio_vedacao WHERE id = ?""";

    private static final String MSG_CODIGO_DUPLICADO = "Já existe um critério cadastrado com este código interno.";
    private static final String MSG_REGIAO_INEXISTENTE = "A região informada para o critério não está cadastrada.";
    private static final String MSG_EXCLUIR_USADO =
            "Não é possível excluir: o critério foi utilizado em análise emitida (rastreabilidade RN16)";

    @Override
    public CriterioVedacao inserir(CriterioVedacao criterio) {
        try (Connection conexao = ConnectionFactory.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR_MAE)) {
                    JdbcUtil.setLongOuNulo(ps, 1, criterio.getRegiaoId());
                    ps.setString(2, criterio.getCodigoInterno());
                    ps.setString(3, criterio.getFundamentoLegal());
                    ps.setString(4, criterio.getTipo());
                    criterio.setId(JdbcUtil.executarRetornandoId(ps));
                }
                gravarSubtabela(conexao, criterio, false);
                conexao.commit();
                return criterio;
            } catch (SQLException | RuntimeException e) {
                JdbcUtil.rollbackSilencioso(conexao, e);
                criterio.setId(null);
                throw e;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "inserir o critério", MSG_REGIAO_INEXISTENTE, MSG_CODIGO_DUPLICADO);
        }
    }

    @Override
    public Optional<CriterioVedacao> buscarPorId(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_BUSCAR)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar o critério");
        }
    }

    @Override
    public List<CriterioVedacao> listarTodos() {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return mapearTodos(rs);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os critérios");
        }
    }

    /** Lista os critérios de vedação cadastrados para uma região (E2). */
    @Override
    public List<CriterioVedacao> listarPorRegiao(long regiaoId) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR_POR_REGIAO)) {
            ps.setLong(1, regiaoId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapearTodos(rs);
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os critérios da região");
        }
    }

    @Override
    public void atualizar(CriterioVedacao criterio) {
        if (criterio.getId() == null) {
            throw new PersistenciaException("Não é possível atualizar um critério sem id.");
        }
        try (Connection conexao = ConnectionFactory.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                verificarTipoInalterado(conexao, criterio.getId(), criterio.getTipo());
                try (PreparedStatement ps = conexao.prepareStatement(SQL_ATUALIZAR_MAE)) {
                    JdbcUtil.setLongOuNulo(ps, 1, criterio.getRegiaoId());
                    ps.setString(2, criterio.getCodigoInterno());
                    ps.setString(3, criterio.getFundamentoLegal());
                    ps.setLong(4, criterio.getId());
                    ps.executeUpdate();
                }
                gravarSubtabela(conexao, criterio, true);
                conexao.commit();
            } catch (SQLException | RuntimeException e) {
                JdbcUtil.rollbackSilencioso(conexao, e);
                throw e;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar o critério", MSG_REGIAO_INEXISTENTE, MSG_CODIGO_DUPLICADO);
        }
    }

    @Override
    public void excluir(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_EXCLUIR)) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Critério " + id + " não encontrado.");
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir o critério", MSG_EXCLUIR_USADO, null);
        }
    }

    private static void verificarTipoInalterado(Connection conexao, long id, String tipoNovo) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_TIPO_ATUAL)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PersistenciaException("Critério " + id + " não encontrado.");
                }
                if (!rs.getString("tipo").equals(tipoNovo)) {
                    throw new RegraNegocioException(EquipamentoComunitarioDao.MSG_TIPO_ALTERADO);
                }
            }
        }
    }

    /**
     * Grava a subtabela do tipo concreto. Os comandos de insert e update têm os mesmos parâmetros
     * (valor específico, id), por isso a única diferença é o SQL escolhido.
     */
    private static void gravarSubtabela(Connection conexao, CriterioVedacao criterio, boolean atualizacao)
            throws SQLException {
        switch (criterio) {
            case CriterioAlagamento c -> {
                try (PreparedStatement ps = conexao.prepareStatement(
                        atualizacao ? SQL_ATUALIZAR_ALAGAMENTO : SQL_INSERIR_ALAGAMENTO)) {
                    ps.setBoolean(1, c.isSaneamentoExecutado());
                    ps.setLong(2, c.getId());
                    ps.executeUpdate();
                }
            }
            case CriterioDeclividade c -> {
                try (PreparedStatement ps = conexao.prepareStatement(
                        atualizacao ? SQL_ATUALIZAR_DECLIVIDADE : SQL_INSERIR_DECLIVIDADE)) {
                    ps.setDouble(1, c.getDeclividadeLimite());
                    ps.setLong(2, c.getId());
                    ps.executeUpdate();
                }
            }
            case CriterioGeologico c -> {
                try (PreparedStatement ps = conexao.prepareStatement(
                        atualizacao ? SQL_ATUALIZAR_GEOLOGICO : SQL_INSERIR_GEOLOGICO)) {
                    ps.setString(1, c.getLaudoGeotecnico());
                    ps.setLong(2, c.getId());
                    ps.executeUpdate();
                }
            }
            case CriterioPreservacao c -> {
                try (PreparedStatement ps = conexao.prepareStatement(
                        atualizacao ? SQL_ATUALIZAR_PRESERVACAO : SQL_INSERIR_PRESERVACAO)) {
                    ps.setBoolean(1, c.isAreaProtegida());
                    ps.setLong(2, c.getId());
                    ps.executeUpdate();
                }
            }
            default -> throw new PersistenciaException("Tipo de critério não suportado: " + criterio.getTipo());
        }
    }

    private static List<CriterioVedacao> mapearTodos(ResultSet rs) throws SQLException {
        List<CriterioVedacao> criterios = new ArrayList<>();
        while (rs.next()) {
            criterios.add(mapear(rs));
        }
        return criterios;
    }

    /**
     * Instancia a subclasse concreta pela coluna discriminadora "tipo".
     * Espera os rótulos com prefixo "c_" (c_id, c_tipo, c_laudo_geotecnico, ...).
     */
    static CriterioVedacao mapear(ResultSet rs) throws SQLException {
        long id = rs.getLong("c_id");
        long regiaoId = rs.getLong("c_regiao_id");
        String codigo = rs.getString("c_codigo_interno");
        String fundamento = rs.getString("c_fundamento_legal");
        String tipo = rs.getString("c_tipo");
        return switch (tipo) {
            case CriterioVedacao.TIPO_ALAGAMENTO -> new CriterioAlagamento(id, regiaoId, codigo, fundamento,
                    rs.getBoolean("c_saneamento_executado"));
            case CriterioVedacao.TIPO_DECLIVIDADE -> new CriterioDeclividade(id, regiaoId, codigo, fundamento,
                    rs.getDouble("c_declividade_limite"));
            case CriterioVedacao.TIPO_GEOLOGICO -> new CriterioGeologico(id, regiaoId, codigo, fundamento,
                    rs.getString("c_laudo_geotecnico"));
            case CriterioVedacao.TIPO_PRESERVACAO -> new CriterioPreservacao(id, regiaoId, codigo, fundamento,
                    rs.getBoolean("c_area_protegida"));
            default -> throw new PersistenciaException("Tipo de critério desconhecido no banco: " + tipo);
        };
    }
}
