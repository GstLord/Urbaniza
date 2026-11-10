package br.com.urbaniza.dao;

import br.com.urbaniza.config.ConnectionFactory;
import br.com.urbaniza.exception.PersistenciaException;
import br.com.urbaniza.model.Lote;
import br.com.urbaniza.model.Loteamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persistência de {@link Loteamento} e de seus {@link Lote}s (composição) — tabelas loteamento e lote.
 * Inserir e atualizar gravam os lotes na MESMA transação; atualizar substitui os lotes
 * (delete + reinsert).
 */
public class LoteamentoDao implements CrudDaoPorRegiao<Loteamento> {

    private static final String SQL_INSERIR = """
            INSERT INTO loteamento (regiao_id, denominacao, data_aprovacao, area_gleba_m2,
                                    populacao_estimada, ocupado)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id""";
    private static final String SQL_INSERIR_LOTE = """
            INSERT INTO lote (loteamento_id, quadra, numero, area_m2, frente_m)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id""";
    private static final String SQL_BUSCAR = """
            SELECT id, regiao_id, denominacao, data_aprovacao, area_gleba_m2, populacao_estimada, ocupado
              FROM loteamento
             WHERE id = ?""";
    private static final String SQL_LISTAR = """
            SELECT id, regiao_id, denominacao, data_aprovacao, area_gleba_m2, populacao_estimada, ocupado
              FROM loteamento
             ORDER BY id""";
    private static final String SQL_LISTAR_POR_REGIAO = """
            SELECT id, regiao_id, denominacao, data_aprovacao, area_gleba_m2, populacao_estimada, ocupado
              FROM loteamento
             WHERE regiao_id = ?
             ORDER BY id""";
    private static final String SQL_LOTES_DO_LOTEAMENTO = """
            SELECT id, loteamento_id, quadra, numero, area_m2, frente_m
              FROM lote
             WHERE loteamento_id = ?
             ORDER BY id""";
    private static final String SQL_TODOS_LOTES = """
            SELECT id, loteamento_id, quadra, numero, area_m2, frente_m
              FROM lote
             ORDER BY id""";
    private static final String SQL_LOTES_POR_REGIAO = """
            SELECT l.id, l.loteamento_id, l.quadra, l.numero, l.area_m2, l.frente_m
              FROM lote l
              JOIN loteamento lt ON lt.id = l.loteamento_id
             WHERE lt.regiao_id = ?
             ORDER BY l.id""";
    private static final String SQL_ATUALIZAR = """
            UPDATE loteamento
               SET regiao_id = ?, denominacao = ?, data_aprovacao = ?, area_gleba_m2 = ?,
                   populacao_estimada = ?, ocupado = ?
             WHERE id = ?""";
    private static final String SQL_EXCLUIR_LOTES = """
            DELETE FROM lote WHERE loteamento_id = ?""";
    private static final String SQL_EXCLUIR = """
            DELETE FROM loteamento WHERE id = ?""";

    private static final String MSG_REGIAO_INEXISTENTE =
            "A região informada não está cadastrada. Cadastre a região antes do loteamento (RN17).";
    private static final String MSG_LOTE_DUPLICADO =
            "Há lotes repetidos (mesma quadra e número) neste loteamento.";

    @Override
    public Loteamento inserir(Loteamento loteamento) {
        try (Connection conexao = ConnectionFactory.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR)) {
                    preencher(ps, loteamento);
                    loteamento.setId(JdbcUtil.executarRetornandoId(ps));
                }
                inserirLotes(conexao, loteamento);
                conexao.commit();
                return loteamento;
            } catch (SQLException | RuntimeException e) {
                JdbcUtil.rollbackSilencioso(conexao, e);
                loteamento.setId(null);
                throw e;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "inserir o loteamento", MSG_REGIAO_INEXISTENTE, MSG_LOTE_DUPLICADO);
        }
    }

    @Override
    public Optional<Loteamento> buscarPorId(long id) {
        try (Connection conexao = ConnectionFactory.getConnection()) {
            Loteamento loteamento;
            try (PreparedStatement ps = conexao.prepareStatement(SQL_BUSCAR)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    loteamento = mapear(rs);
                }
            }
            try (PreparedStatement ps = conexao.prepareStatement(SQL_LOTES_DO_LOTEAMENTO)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        loteamento.adicionarLote(mapearLote(rs));
                    }
                }
            }
            return Optional.of(loteamento);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar o loteamento");
        }
    }

    @Override
    public List<Loteamento> listarTodos() {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement psLoteamentos = conexao.prepareStatement(SQL_LISTAR);
             PreparedStatement psLotes = conexao.prepareStatement(SQL_TODOS_LOTES)) {
            return carregar(psLoteamentos, psLotes);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os loteamentos");
        }
    }

    /** Lista os loteamentos (com lotes) de uma região. */
    @Override
    public List<Loteamento> listarPorRegiao(long regiaoId) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement psLoteamentos = conexao.prepareStatement(SQL_LISTAR_POR_REGIAO);
             PreparedStatement psLotes = conexao.prepareStatement(SQL_LOTES_POR_REGIAO)) {
            psLoteamentos.setLong(1, regiaoId);
            psLotes.setLong(1, regiaoId);
            return carregar(psLoteamentos, psLotes);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os loteamentos da região");
        }
    }

    @Override
    public void atualizar(Loteamento loteamento) {
        if (loteamento.getId() == null) {
            throw new PersistenciaException("Não é possível atualizar um loteamento sem id.");
        }
        try (Connection conexao = ConnectionFactory.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conexao.prepareStatement(SQL_ATUALIZAR)) {
                    preencher(ps, loteamento);
                    ps.setLong(7, loteamento.getId());
                    if (ps.executeUpdate() == 0) {
                        throw new PersistenciaException("Loteamento " + loteamento.getId() + " não encontrado.");
                    }
                }
                try (PreparedStatement ps = conexao.prepareStatement(SQL_EXCLUIR_LOTES)) {
                    ps.setLong(1, loteamento.getId());
                    ps.executeUpdate();
                }
                inserirLotes(conexao, loteamento);
                conexao.commit();
            } catch (SQLException | RuntimeException e) {
                JdbcUtil.rollbackSilencioso(conexao, e);
                throw e;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar o loteamento", MSG_REGIAO_INEXISTENTE, MSG_LOTE_DUPLICADO);
        }
    }

    @Override
    public void excluir(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_EXCLUIR)) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Loteamento " + id + " não encontrado.");
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir o loteamento");
        }
    }

    private static void inserirLotes(Connection conexao, Loteamento loteamento) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR_LOTE)) {
            for (Lote lote : loteamento.getLotes()) {
                ps.setLong(1, loteamento.getId());
                ps.setString(2, lote.getQuadra());
                ps.setString(3, lote.getNumero());
                ps.setDouble(4, lote.getAreaM2());
                ps.setDouble(5, lote.getFrenteM());
                lote.setId(JdbcUtil.executarRetornandoId(ps));
            }
        }
    }

    private static List<Loteamento> carregar(PreparedStatement psLoteamentos, PreparedStatement psLotes)
            throws SQLException {
        Map<Long, Loteamento> porId = new LinkedHashMap<>();
        try (ResultSet rs = psLoteamentos.executeQuery()) {
            while (rs.next()) {
                Loteamento loteamento = mapear(rs);
                porId.put(loteamento.getId(), loteamento);
            }
        }
        try (ResultSet rs = psLotes.executeQuery()) {
            while (rs.next()) {
                Loteamento dono = porId.get(rs.getLong("loteamento_id"));
                if (dono != null) {
                    dono.adicionarLote(mapearLote(rs));
                }
            }
        }
        return new ArrayList<>(porId.values());
    }

    private static void preencher(PreparedStatement ps, Loteamento loteamento) throws SQLException {
        JdbcUtil.setLongOuNulo(ps, 1, loteamento.getRegiaoId());
        ps.setString(2, loteamento.getDenominacao());
        JdbcUtil.setDataOuNulo(ps, 3, loteamento.getDataAprovacao());
        ps.setDouble(4, loteamento.getAreaGlebaM2());
        ps.setInt(5, loteamento.getPopulacaoEstimada());
        ps.setBoolean(6, loteamento.isOcupado());
    }

    private static Loteamento mapear(ResultSet rs) throws SQLException {
        return new Loteamento(
                rs.getLong("id"),
                rs.getLong("regiao_id"),
                rs.getString("denominacao"),
                JdbcUtil.getData(rs, "data_aprovacao"),
                rs.getDouble("area_gleba_m2"),
                rs.getInt("populacao_estimada"),
                rs.getBoolean("ocupado"),
                null);
    }

    private static Lote mapearLote(ResultSet rs) throws SQLException {
        return new Lote(
                rs.getLong("id"),
                rs.getString("quadra"),
                rs.getString("numero"),
                rs.getDouble("area_m2"),
                rs.getDouble("frente_m"));
    }
}
