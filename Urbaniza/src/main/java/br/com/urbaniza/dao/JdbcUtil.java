package br.com.urbaniza.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Utilitários JDBC: colunas anuláveis, conversões de data e rollback seguro.
 */
final class JdbcUtil {

    private JdbcUtil() {
    }

    static void setLongOuNulo(PreparedStatement ps, int indice, Long valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.BIGINT);
        } else {
            ps.setLong(indice, valor);
        }
    }

    static void setDoubleOuNulo(PreparedStatement ps, int indice, Double valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.DOUBLE);
        } else {
            ps.setDouble(indice, valor);
        }
    }

    static void setTextoOuNulo(PreparedStatement ps, int indice, String valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.VARCHAR);
        } else {
            ps.setString(indice, valor);
        }
    }

    /** LocalDate -&gt; java.sql.Date (null vira NULL). */
    static void setDataOuNulo(PreparedStatement ps, int indice, LocalDate valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.DATE);
        } else {
            ps.setDate(indice, Date.valueOf(valor));
        }
    }

    /** LocalDateTime -&gt; java.sql.Timestamp. */
    static void setDataHora(PreparedStatement ps, int indice, LocalDateTime valor) throws SQLException {
        ps.setTimestamp(indice, Timestamp.valueOf(valor));
    }

    static Long getLongOuNulo(ResultSet rs, String coluna) throws SQLException {
        long valor = rs.getLong(coluna);
        return rs.wasNull() ? null : valor;
    }

    static Double getDoubleOuNulo(ResultSet rs, String coluna) throws SQLException {
        double valor = rs.getDouble(coluna);
        return rs.wasNull() ? null : valor;
    }

    /** java.sql.Date -&gt; LocalDate (NULL vira null). */
    static LocalDate getData(ResultSet rs, String coluna) throws SQLException {
        Date data = rs.getDate(coluna);
        return data == null ? null : data.toLocalDate();
    }

    /** java.sql.Timestamp -&gt; LocalDateTime (NULL vira null). */
    static LocalDateTime getDataHora(ResultSet rs, String coluna) throws SQLException {
        Timestamp ts = rs.getTimestamp(coluna);
        return ts == null ? null : ts.toLocalDateTime();
    }

    /** Executa um INSERT ... RETURNING id e devolve o id gerado. */
    static long executarRetornandoId(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("O banco não devolveu o id gerado.");
            }
            return rs.getLong(1);
        }
    }

    /** Desfaz a transação sem mascarar a exceção original. */
    static void rollbackSilencioso(Connection conexao, Exception original) {
        try {
            conexao.rollback();
        } catch (SQLException erroRollback) {
            original.addSuppressed(erroRollback);
        }
    }
}
