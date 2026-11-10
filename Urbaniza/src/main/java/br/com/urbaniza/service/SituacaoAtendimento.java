package br.com.urbaniza.service;

import br.com.urbaniza.model.equipamento.EquipamentoComunitario;

/**
 * Situação de atendimento de um equipamento comunitário frente à população PROJETADA da sua região
 * (RF03 / RN11 / RN12 / RN14).
 *
 * @param equipamento        equipamento avaliado
 * @param populacaoProjetada população projetada da região usada no cálculo
 * @param demanda            demanda calculada polimorficamente (vagas ou equipes)
 * @param indice             capacidade / demanda (POSITIVE_INFINITY quando a demanda é zero)
 * @param deficit            true quando o índice é menor que 1
 */
public record SituacaoAtendimento(EquipamentoComunitario equipamento, int populacaoProjetada, int demanda,
                                  double indice, boolean deficit) {

    /** Calcula a situação do equipamento para a população informada. */
    public static SituacaoAtendimento calcular(EquipamentoComunitario equipamento, int populacaoProjetada) {
        return new SituacaoAtendimento(equipamento, populacaoProjetada,
                equipamento.calcularDemanda(populacaoProjetada),
                equipamento.calcularIndiceAtendimento(populacaoProjetada),
                equipamento.possuiDeficit(populacaoProjetada));
    }
}
