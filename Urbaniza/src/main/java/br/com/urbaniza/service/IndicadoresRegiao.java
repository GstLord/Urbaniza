package br.com.urbaniza.service;

import br.com.urbaniza.model.enums.ClassificacaoIAV;
import br.com.urbaniza.model.enums.PotencialIlhaCalor;

/**
 * Resumo dos indicadores urbanísticos de uma região (RF04, RF05, RN08–RN10, RN13, RN14).
 *
 * @param populacaoAtual        população atual (habitantes)
 * @param populacaoProjetada    RN13 — população atual + loteamentos aprovados e não ocupados
 * @param iav                   RN08/RN14 — m²/hab sobre a população projetada (POSITIVE_INFINITY se ela for 0)
 * @param classificacaoIav      RN08 — adequado, aceitável ou deficitário
 * @param taxaImpermeabilizacao RN09 — fração 0..1
 * @param potencialIlhaCalor    RN10 — baixo, médio ou alto
 */
public record IndicadoresRegiao(int populacaoAtual, int populacaoProjetada, double iav,
                                ClassificacaoIAV classificacaoIav, double taxaImpermeabilizacao,
                                PotencialIlhaCalor potencialIlhaCalor) {
}
