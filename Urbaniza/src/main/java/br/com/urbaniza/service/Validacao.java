package br.com.urbaniza.service;

import br.com.urbaniza.exception.RegraNegocioException;

/**
 * Validações simples de entrada compartilhadas pelos serviços.
 */
final class Validacao {

    private Validacao() {
    }

    /** Exige texto não vazio e com até {@code tamanhoMaximo} caracteres; devolve-o sem espaços nas pontas. */
    static String textoObrigatorio(String valor, String campo, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException("Informe " + campo + ".");
        }
        String limpo = valor.trim();
        if (limpo.length() > tamanhoMaximo) {
            throw new RegraNegocioException("O campo " + campo + " aceita no máximo " + tamanhoMaximo + " caracteres.");
        }
        return limpo;
    }

    static void exigirId(Long id, String entidade) {
        if (id == null) {
            throw new RegraNegocioException("Informe o id " + entidade + " a ser atualizado.");
        }
    }
}
