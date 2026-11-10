package br.com.urbaniza.dao;

import br.com.urbaniza.exception.PersistenciaException;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.sql.BatchUpdateException;
import java.sql.SQLException;

/**
 * Centraliza a tradução de {@link SQLException} em {@link PersistenciaException} com mensagens
 * em português compreensíveis para o usuário.
 * <ul>
 *   <li>23503 — violação de chave estrangeira (FK);</li>
 *   <li>23505 — violação de unicidade (UNIQUE);</li>
 *   <li>23514 — violação de regra CHECK;</li>
 *   <li>23502 — campo obrigatório nulo;</li>
 *   <li>08xxx — falha de comunicação com o banco.</li>
 * </ul>
 */
public final class TradutorErroSql {

    public static final String SQLSTATE_FK = "23503";
    public static final String SQLSTATE_UNIQUE = "23505";
    public static final String SQLSTATE_CHECK = "23514";
    public static final String SQLSTATE_NOT_NULL = "23502";
    private static final String PREFIXO_SQLSTATE_CONEXAO = "08";

    private TradutorErroSql() {
    }

    /**
     * Traduz a exceção usando mensagens padrão.
     *
     * @param e    exceção original
     * @param acao descrição curta da operação (ex.: "excluir a região")
     */
    public static PersistenciaException traduzir(SQLException e, String acao) {
        return traduzir(e, acao, null, null);
    }

    /**
     * Traduz a exceção permitindo mensagens específicas para FK e UNIQUE.
     *
     * @param e             exceção original
     * @param acao          descrição curta da operação (ex.: "excluir a região")
     * @param mensagemFk    mensagem para violação de FK (null = padrão)
     * @param mensagemUnico mensagem para violação de UNIQUE (null = padrão)
     */
    public static PersistenciaException traduzir(SQLException original, String acao, String mensagemFk,
                                                 String mensagemUnico) {
        SQLException e = causaReal(original);
        String estado = e.getSQLState() == null ? "" : e.getSQLState();
        String restricao = obterRestricao(e);
        String mensagem;
        switch (estado) {
            case SQLSTATE_FK -> mensagem = mensagemFk != null ? mensagemFk
                    : "Operação recusada: o registro está vinculado a outro registro inexistente ou dependente ("
                    + restricao + ").";
            case SQLSTATE_UNIQUE -> mensagem = mensagemUnico != null ? mensagemUnico
                    : "Já existe registro com este nome/matrícula/código (" + restricao + ").";
            case SQLSTATE_CHECK -> mensagem = "Valor fora das regras do banco: " + restricao;
            case SQLSTATE_NOT_NULL -> mensagem = "Campo obrigatório não informado: " + obterColuna(e);
            default -> {
                if (estado.startsWith(PREFIXO_SQLSTATE_CONEXAO)) {
                    mensagem = "Falha de comunicação com o banco de dados ao " + acao
                            + ". Verifique se o banco está no ar (cd Urbaniza/database && docker compose up -d).";
                } else {
                    mensagem = "Erro no banco de dados ao " + acao + ": " + e.getMessage();
                }
            }
        }
        return new PersistenciaException(mensagem, original);
    }

    /** Em lotes (executeBatch) o driver embrulha o erro real em getNextException(). */
    private static SQLException causaReal(SQLException e) {
        if (e instanceof BatchUpdateException && e.getNextException() != null) {
            return e.getNextException();
        }
        return e;
    }

    /** @return nome da restrição violada (ou "desconhecida") */
    public static String obterRestricao(SQLException e) {
        ServerErrorMessage detalhe = detalhe(e);
        if (detalhe != null && detalhe.getConstraint() != null) {
            return detalhe.getConstraint();
        }
        return "desconhecida";
    }

    private static String obterColuna(SQLException e) {
        ServerErrorMessage detalhe = detalhe(e);
        if (detalhe != null && detalhe.getColumn() != null) {
            return detalhe.getColumn();
        }
        return "desconhecido";
    }

    private static ServerErrorMessage detalhe(SQLException e) {
        if (e instanceof PSQLException psql) {
            return psql.getServerErrorMessage();
        }
        return null;
    }
}
