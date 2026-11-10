package br.com.urbaniza.ui;

/**
 * Sinaliza que a entrada padrão terminou (EOF): o programa deve encerrar de forma limpa.
 */
public class EntradaEncerradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EntradaEncerradaException() {
        super("Entrada encerrada.");
    }
}
