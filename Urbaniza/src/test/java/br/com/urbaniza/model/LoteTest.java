package br.com.urbaniza.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoteTest {

    @Test
    void loteComDimensoesMinimasExatasEhAceito() {
        assertTrue(new Lote(null, "A", "1", 125.0, 5.0).atendeDimensoesMinimas());
    }

    @Test
    void loteComAreaAbaixoDoMinimoEhRejeitado() {
        assertFalse(new Lote(null, "A", "1", 124.99, 5.0).atendeDimensoesMinimas());
    }

    @Test
    void loteComFrenteAbaixoDoMinimoEhRejeitado() {
        assertFalse(new Lote(null, "A", "1", 125.0, 4.99).atendeDimensoesMinimas());
    }

    @Test
    void areaNegativaLancaExcecao() {
        Lote lote = new Lote();
        assertThrows(IllegalArgumentException.class, () -> lote.setAreaM2(-1));
        assertThrows(IllegalArgumentException.class, () -> lote.setFrenteM(-0.5));
    }
}
