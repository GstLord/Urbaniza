package br.com.urbaniza.model;

import br.com.urbaniza.model.criterio.CriterioVedacao;

/**
 * Resultado da avaliação de um critério de vedação dentro de uma análise
 * (mapeia a tabela analise_criterio — E4). Imutável.
 *
 * @param criterio             critério avaliado
 * @param atendido             true quando a região atende ao critério (sem impedimento)
 * @param descricaoImpedimento descrição do impedimento; null quando atendido
 */
public record ResultadoCriterio(CriterioVedacao criterio, boolean atendido, String descricaoImpedimento) {

    public ResultadoCriterio {
        if (criterio == null) {
            throw new IllegalArgumentException("O critério do resultado não pode ser nulo.");
        }
        if (atendido) {
            descricaoImpedimento = null;
        }
    }
}
