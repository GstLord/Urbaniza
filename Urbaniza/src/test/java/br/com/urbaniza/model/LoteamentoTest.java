package br.com.urbaniza.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoteamentoTest {

    private static Loteamento loteamento(LocalDate aprovacao, boolean ocupado) {
        return new Loteamento(null, 1L, "Teste", aprovacao, 10_000, 100, ocupado, null);
    }

    @Test
    void aprovadoENaoOcupadoContribuiParaProjecao() {
        Loteamento l = loteamento(LocalDate.of(2025, 1, 1), false);
        assertTrue(l.isAprovado());
        assertTrue(l.contribuiParaProjecao());
    }

    @Test
    void aprovadoEOcupadoNaoContribui() {
        assertFalse(loteamento(LocalDate.of(2025, 1, 1), true).contribuiParaProjecao());
    }

    @Test
    void naoAprovadoNaoContribui() {
        Loteamento l = loteamento(null, false);
        assertFalse(l.isAprovado());
        assertFalse(l.contribuiParaProjecao());
    }

    @Test
    void validarDimensoesLotesTodosValidos() {
        Loteamento l = loteamento(null, false);
        l.adicionarLote(new Lote(null, "A", "1", 125, 5));
        l.adicionarLote(new Lote(null, "A", "2", 300, 12));
        assertTrue(l.validarDimensoesLotes());
        assertEquals(425.0, l.calcularAreaLoteavel(), 1e-9);
    }

    @Test
    void validarDimensoesLotesComUmInvalido() {
        Loteamento l = loteamento(null, false);
        l.adicionarLote(new Lote(null, "A", "1", 250, 10));
        l.adicionarLote(new Lote(null, "A", "2", 120, 10));
        assertFalse(l.validarDimensoesLotes());
    }

    @Test
    void populacaoEstimadaNegativaLancaExcecao() {
        Loteamento l = new Loteamento();
        assertThrows(IllegalArgumentException.class, () -> l.setPopulacaoEstimada(-1));
        assertThrows(IllegalArgumentException.class, () -> l.setAreaGlebaM2(-10));
    }
}
