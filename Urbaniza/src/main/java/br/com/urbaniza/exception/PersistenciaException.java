package br.com.urbaniza.exception;

/**
 * Falha de acesso ao banco de dados. Embrulha {@link java.sql.SQLException}
 * com uma mensagem em português compreensível para o usuário.
 */
public class PersistenciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
