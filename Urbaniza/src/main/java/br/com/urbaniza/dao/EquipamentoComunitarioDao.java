package br.com.urbaniza.dao;

import br.com.urbaniza.config.ConnectionFactory;
import br.com.urbaniza.exception.PersistenciaException;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.equipamento.EquipamentoComunitario;
import br.com.urbaniza.model.equipamento.Escola;
import br.com.urbaniza.model.equipamento.UnidadeSaude;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persistência POLIMÓRFICA de {@link EquipamentoComunitario} (herança JOINED):
 * tabela-mãe equipamento_comunitario (coluna discriminadora tipo) + subtabelas escola e unidade_saude,
 * gravadas na mesma transação. A leitura usa LEFT JOIN e instancia a subclasse pelo tipo.
 */
public class EquipamentoComunitarioDao implements CrudDaoPorRegiao<EquipamentoComunitario> {

    private static final String SQL_INSERIR_MAE = """
            INSERT INTO equipamento_comunitario (regiao_id, nome, capacidade_instalada, tipo)
            VALUES (?, ?, ?, ?)
            RETURNING id""";
    private static final String SQL_INSERIR_ESCOLA = """
            INSERT INTO escola (id, percentual_idade_escolar, nivel_ensino)
            VALUES (?, ?, ?)""";
    private static final String SQL_INSERIR_UNIDADE_SAUDE = """
            INSERT INTO unidade_saude (id, habitantes_por_equipe, tipo_unidade)
            VALUES (?, ?, ?)""";
    private static final String SQL_BUSCAR = """
            SELECT e.id, e.regiao_id, e.nome, e.capacidade_instalada, e.tipo,
                   es.percentual_idade_escolar, es.nivel_ensino,
                   us.habitantes_por_equipe, us.tipo_unidade
              FROM equipamento_comunitario e
              LEFT JOIN escola es ON es.id = e.id
              LEFT JOIN unidade_saude us ON us.id = e.id
             WHERE e.id = ?""";
    private static final String SQL_LISTAR = """
            SELECT e.id, e.regiao_id, e.nome, e.capacidade_instalada, e.tipo,
                   es.percentual_idade_escolar, es.nivel_ensino,
                   us.habitantes_por_equipe, us.tipo_unidade
              FROM equipamento_comunitario e
              LEFT JOIN escola es ON es.id = e.id
              LEFT JOIN unidade_saude us ON us.id = e.id
             ORDER BY e.id""";
    private static final String SQL_LISTAR_POR_REGIAO = """
            SELECT e.id, e.regiao_id, e.nome, e.capacidade_instalada, e.tipo,
                   es.percentual_idade_escolar, es.nivel_ensino,
                   us.habitantes_por_equipe, us.tipo_unidade
              FROM equipamento_comunitario e
              LEFT JOIN escola es ON es.id = e.id
              LEFT JOIN unidade_saude us ON us.id = e.id
             WHERE e.regiao_id = ?
             ORDER BY e.id""";
    private static final String SQL_TIPO_ATUAL = """
            SELECT tipo FROM equipamento_comunitario WHERE id = ?""";
    private static final String SQL_ATUALIZAR_MAE = """
            UPDATE equipamento_comunitario
               SET regiao_id = ?, nome = ?, capacidade_instalada = ?
             WHERE id = ?""";
    private static final String SQL_ATUALIZAR_ESCOLA = """
            UPDATE escola
               SET percentual_idade_escolar = ?, nivel_ensino = ?
             WHERE id = ?""";
    private static final String SQL_ATUALIZAR_UNIDADE_SAUDE = """
            UPDATE unidade_saude
               SET habitantes_por_equipe = ?, tipo_unidade = ?
             WHERE id = ?""";
    private static final String SQL_EXCLUIR = """
            DELETE FROM equipamento_comunitario WHERE id = ?""";

    static final String MSG_TIPO_ALTERADO = "Não é permitido alterar o tipo; exclua e cadastre novamente";
    private static final String MSG_REGIAO_INEXISTENTE = "A região informada para o equipamento não está cadastrada.";

    @Override
    public EquipamentoComunitario inserir(EquipamentoComunitario equipamento) {
        try (Connection conexao = ConnectionFactory.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR_MAE)) {
                    JdbcUtil.setLongOuNulo(ps, 1, equipamento.getRegiaoId());
                    ps.setString(2, equipamento.getNome());
                    ps.setInt(3, equipamento.getCapacidadeInstalada());
                    ps.setString(4, equipamento.getTipo());
                    equipamento.setId(JdbcUtil.executarRetornandoId(ps));
                }
                gravarSubtabela(conexao, equipamento, false);
                conexao.commit();
                return equipamento;
            } catch (SQLException | RuntimeException e) {
                JdbcUtil.rollbackSilencioso(conexao, e);
                equipamento.setId(null);
                throw e;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "inserir o equipamento", MSG_REGIAO_INEXISTENTE, null);
        }
    }

    @Override
    public Optional<EquipamentoComunitario> buscarPorId(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_BUSCAR)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "buscar o equipamento");
        }
    }

    @Override
    public List<EquipamentoComunitario> listarTodos() {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            return mapearTodos(rs);
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os equipamentos");
        }
    }

    /** Lista os equipamentos associados a uma região. */
    @Override
    public List<EquipamentoComunitario> listarPorRegiao(long regiaoId) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_LISTAR_POR_REGIAO)) {
            ps.setLong(1, regiaoId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapearTodos(rs);
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "listar os equipamentos da região");
        }
    }

    @Override
    public void atualizar(EquipamentoComunitario equipamento) {
        if (equipamento.getId() == null) {
            throw new PersistenciaException("Não é possível atualizar um equipamento sem id.");
        }
        try (Connection conexao = ConnectionFactory.getConnection()) {
            conexao.setAutoCommit(false);
            try {
                verificarTipoInalterado(conexao, equipamento.getId(), equipamento.getTipo());
                try (PreparedStatement ps = conexao.prepareStatement(SQL_ATUALIZAR_MAE)) {
                    JdbcUtil.setLongOuNulo(ps, 1, equipamento.getRegiaoId());
                    ps.setString(2, equipamento.getNome());
                    ps.setInt(3, equipamento.getCapacidadeInstalada());
                    ps.setLong(4, equipamento.getId());
                    ps.executeUpdate();
                }
                gravarSubtabela(conexao, equipamento, true);
                conexao.commit();
            } catch (SQLException | RuntimeException e) {
                JdbcUtil.rollbackSilencioso(conexao, e);
                throw e;
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "atualizar o equipamento", MSG_REGIAO_INEXISTENTE, null);
        }
    }

    @Override
    public void excluir(long id) {
        try (Connection conexao = ConnectionFactory.getConnection();
             PreparedStatement ps = conexao.prepareStatement(SQL_EXCLUIR)) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) {
                throw new PersistenciaException("Equipamento " + id + " não encontrado.");
            }
        } catch (SQLException e) {
            throw TradutorErroSql.traduzir(e, "excluir o equipamento");
        }
    }

    private static void verificarTipoInalterado(Connection conexao, long id, String tipoNovo) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_TIPO_ATUAL)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new PersistenciaException("Equipamento " + id + " não encontrado.");
                }
                if (!rs.getString("tipo").equals(tipoNovo)) {
                    throw new RegraNegocioException(MSG_TIPO_ALTERADO);
                }
            }
        }
    }

    /** Grava a subtabela correspondente ao tipo concreto (insert ou update). */
    private static void gravarSubtabela(Connection conexao, EquipamentoComunitario equipamento, boolean atualizacao)
            throws SQLException {
        switch (equipamento) {
            case Escola escola -> {
                try (PreparedStatement ps = conexao.prepareStatement(
                        atualizacao ? SQL_ATUALIZAR_ESCOLA : SQL_INSERIR_ESCOLA)) {
                    int i = 1;
                    if (!atualizacao) {
                        ps.setLong(i++, escola.getId());
                    }
                    ps.setDouble(i++, escola.getPercentualIdadeEscolar());
                    ps.setString(i++, escola.getNivelEnsino());
                    if (atualizacao) {
                        ps.setLong(i, escola.getId());
                    }
                    ps.executeUpdate();
                }
            }
            case UnidadeSaude unidade -> {
                try (PreparedStatement ps = conexao.prepareStatement(
                        atualizacao ? SQL_ATUALIZAR_UNIDADE_SAUDE : SQL_INSERIR_UNIDADE_SAUDE)) {
                    int i = 1;
                    if (!atualizacao) {
                        ps.setLong(i++, unidade.getId());
                    }
                    ps.setInt(i++, unidade.getHabitantesPorEquipe());
                    ps.setString(i++, unidade.getTipoUnidade());
                    if (atualizacao) {
                        ps.setLong(i, unidade.getId());
                    }
                    ps.executeUpdate();
                }
            }
            default -> throw new PersistenciaException("Tipo de equipamento não suportado: " + equipamento.getTipo());
        }
    }

    private static List<EquipamentoComunitario> mapearTodos(ResultSet rs) throws SQLException {
        List<EquipamentoComunitario> equipamentos = new ArrayList<>();
        while (rs.next()) {
            equipamentos.add(mapear(rs));
        }
        return equipamentos;
    }

    /** Instancia Escola ou UnidadeSaude conforme a coluna discriminadora "tipo". */
    private static EquipamentoComunitario mapear(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        Long regiaoId = JdbcUtil.getLongOuNulo(rs, "regiao_id");
        String nome = rs.getString("nome");
        int capacidade = rs.getInt("capacidade_instalada");
        String tipo = rs.getString("tipo");
        return switch (tipo) {
            case EquipamentoComunitario.TIPO_ESCOLA -> new Escola(id, regiaoId, nome, capacidade,
                    rs.getDouble("percentual_idade_escolar"), rs.getString("nivel_ensino"));
            case EquipamentoComunitario.TIPO_UNIDADE_SAUDE -> new UnidadeSaude(id, regiaoId, nome, capacidade,
                    rs.getInt("habitantes_por_equipe"), rs.getString("tipo_unidade"));
            default -> throw new PersistenciaException("Tipo de equipamento desconhecido no banco: " + tipo);
        };
    }
}
