package br.com.urbaniza.dao;

import br.com.urbaniza.config.ConnectionFactory;
import br.com.urbaniza.exception.PersistenciaException;
import br.com.urbaniza.model.Analista;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistência de {@link Analista} (tabela analista) — CRUD completo.
 */
public class AnalistaDao implements CrudDao<Analista> {

    private static final String SQL_INSERIR = """
            INSERT INTO analista (nome, matricula, setor)
            VALUES (?, ?, ?)
            RETURNING id""";
    private static final String SQL_BUSCAR = """
            SELECT id, nome, matricula, setor
              FROM analista
             WHERE id = ?""";
    private static final String SQL_LISTAR = """
            SELECT id, nome, matricula, setor
              FROM analista
             ORDER BY id""";
    private static final String SQL_ATUALIZAR = """
            UPDATE analista
               SET nome = ?, matricula = ?, setor = ?
             WHERE id = ?""";
    private static final String SQL_EXCLUIR = """
            DELETE FROM analista WHERE id = ?""";

    private static final String MSG_MATRICULA_DUPLICADA = "Já existe um analista cadastrado com esta matrícula.";
    private static final String MSG_EXCLUIR_COM_ANALISE =
            "Não é possível excluir: o analista é responsável por análises emitidas (rastreabilidade RN16)";

    @Override
    public Analista inserir(Analista analista) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR)) {
            preencher(ps, analista);
            analista.setId(JdbcUtil.executarRetornandoId(ps));
            return analista;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "inserir o analista", null, MSG_MATRICULA_DUPLICADA);
        }
    }

    @Override
    public Optional<Analista> buscarPorId(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_BUSCAR)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs, "")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar o analista");
        }
    }

    @Override
    public List<Analista> listarTodos() {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            List<Analista> analistas = new ArrayList<>();
            while (rs.next()) {
                analistas.add(mapear(rs, ""));
            }
            return analistas;
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os analistas");
        }
    }

    @Override
    public void atualizar(Analista analista) {
        if (analista.getId() == null) {
            throw new PersistenciaException("Não é possível atualizar um analista sem id.");
        }
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_ATUALIZAR)) {
            preencher(ps, analista);
            ps.setLong(4, analista.getId());
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Analista " + analista.getId() + " não encontrado.");
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar o analista", null, MSG_MATRICULA_DUPLICADA);
        }
    }

    @Override
    public void excluir(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_EXCLUIR)) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Analista " + id + " não encontrado.");
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir o analista", MSG_EXCLUIR_COM_ANALISE, null);
        }
    }

    private static void preencher(PreparedStatement ps, Analista analista) throws SQLException {
        ps.setString(1, analista.getNome());
        ps.setString(2, analista.getMatricula());
        ps.setString(3, analista.getSetor());
    }

    /** Monta um analista a partir das colunas da tabela analista (rótulos com o prefixo informado). */
    static Analista mapear(ResultSet rs, String prefixo) throws SQLException {
        return new Analista(
                rs.getLong(prefixo + "id"),
                rs.getString(prefixo + "nome"),
                rs.getString(prefixo + "matricula"),
                rs.getString(prefixo + "setor"));
    }
}
