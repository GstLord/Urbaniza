package br.com.urbaniza.dao;

import br.com.urbaniza.config.ConnectionFactory;
import br.com.urbaniza.exception.PersistenciaException;
import br.com.urbaniza.model.Regiao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistência da entidade principal {@link Regiao} (tabela regiao) — CRUD completo.
 * Carrega somente os dados básicos; loteamentos e equipamentos são carregados pelos respectivos DAOs.
 */
public class RegiaoDao implements CrudDao<Regiao> {

    private static final String SQL_INSERIR = """
            INSERT INTO regiao (nome, area_total_m2, area_verde_m2, area_impermeavel_m2,
                                populacao_atual, declividade_media_pct)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id""";
    private static final String SQL_BUSCAR = """
            SELECT id, nome, area_total_m2, area_verde_m2, area_impermeavel_m2, populacao_atual, declividade_media_pct
              FROM regiao
             WHERE id = ?""";
    private static final String SQL_LISTAR = """
            SELECT id, nome, area_total_m2, area_verde_m2, area_impermeavel_m2, populacao_atual, declividade_media_pct
              FROM regiao
             ORDER BY id""";
    private static final String SQL_ATUALIZAR = """
            UPDATE regiao
               SET nome = ?, area_total_m2 = ?, area_verde_m2 = ?, area_impermeavel_m2 = ?,
                   populacao_atual = ?, declividade_media_pct = ?
             WHERE id = ?""";
    private static final String SQL_EXCLUIR = """
            DELETE FROM regiao WHERE id = ?""";

    static final String MSG_NOME_DUPLICADO = "Já existe uma região cadastrada com este nome.";
    static final String MSG_EXCLUIR_COM_ANALISE =
            "Não é possível excluir: existem análises emitidas para esta região (rastreabilidade RN16)";

    @Override
    public Regiao inserir(Regiao regiao) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR)) {
            preencher(ps, regiao);
            regiao.setId(JdbcUtil.executarRetornandoId(ps));
            return regiao;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "inserir a região", null, MSG_NOME_DUPLICADO);
        }
    }

    @Override
    public Optional<Regiao> buscarPorId(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_BUSCAR)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar a região");
        }
    }

    @Override
    public List<Regiao> listarTodos() {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            List<Regiao> regioes = new ArrayList<>();
            while (rs.next()) {
                regioes.add(mapear(rs));
            }
            return regioes;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar as regiões");
        }
    }

    @Override
    public void atualizar(Regiao regiao) {
        if (regiao.getId() == null) {
            throw new PersistenciaException("Não é possível atualizar uma região sem id.");
        }
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_ATUALIZAR)) {
            preencher(ps, regiao);
            ps.setLong(7, regiao.getId());
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Região " + regiao.getId() + " não encontrada.");
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar a região", null, MSG_NOME_DUPLICADO);
        }
    }

    @Override
    public void excluir(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_EXCLUIR)) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Região " + id + " não encontrada.");
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir a região", MSG_EXCLUIR_COM_ANALISE, null);
        }
    }

    private static void preencher(PreparedStatement ps, Regiao regiao) throws SQLException {
        ps.setString(1, regiao.getNome());
        ps.setDouble(2, regiao.getAreaTotalM2());
        ps.setDouble(3, regiao.getAreaVerdeM2());
        ps.setDouble(4, regiao.getAreaImpermeavelM2());
        ps.setInt(5, regiao.getPopulacaoAtual());
        ps.setDouble(6, regiao.getDeclividadeMediaPct());
    }

    /** Monta uma região a partir das colunas da tabela regiao (rótulos com o prefixo informado). */
    static Regiao mapear(ResultSet rs, String prefixo) throws SQLException {
        return new Regiao(
                rs.getLong(prefixo + "id"),
                rs.getString(prefixo + "nome"),
                rs.getDouble(prefixo + "area_total_m2"),
                rs.getDouble(prefixo + "area_verde_m2"),
                rs.getDouble(prefixo + "area_impermeavel_m2"),
                rs.getInt(prefixo + "populacao_atual"),
                rs.getDouble(prefixo + "declividade_media_pct"));
    }

    private static Regiao mapear(ResultSet rs) throws SQLException {
        return mapear(rs, "");
    }
}
