package br.com.urbaniza.ui;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntradaTest {

    private static Entrada entrada(String texto) {
        return new Entrada(new ByteArrayInputStream(texto.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    @Test
    void aceitaNumerosNoFormatoBrasileiroEInternacional() {
        assertEquals(1234.56, Entrada.converterNumeroBrasileiro("1.234,56"), 1e-9);
        assertEquals(1234.56, Entrada.converterNumeroBrasileiro("1234,56"), 1e-9);
        assertEquals(1234.56, Entrada.converterNumeroBrasileiro("1234.56"), 1e-9);
        assertEquals(180_000, Entrada.converterNumeroBrasileiro("180.000"), 1e-9);
        assertEquals(2_000_000, Entrada.converterNumeroBrasileiro("2.000.000"), 1e-9);
        assertEquals(7.5, Entrada.converterNumeroBrasileiro("7.5"), 1e-9);
        assertEquals(7.5, Entrada.converterNumeroBrasileiro("7,5"), 1e-9);
    }

    @Test
    void repeteAteValorValido() {
        Entrada e = entrada("abc\n-5\n12,5\n");
        assertEquals(12.5, e.lerDouble("x", 0), 1e-9);
        Entrada i = entrada("9\n0\n3\n");
        assertEquals(3, i.lerInteiro("x", 1, 5));
    }

    @Test
    void lerDataESimNao() {
        Entrada e = entrada("31/02/2025\n10/03/2025\n\ntalvez\nS\nnão\n");
        assertEquals(LocalDate.of(2025, 3, 10), e.lerData("d"));
        assertNull(e.lerData("d"));
        assertTrue(e.lerSimNao("p"));
        assertFalse(e.lerSimNao("p"));
    }

    @Test
    void enterMantemValorAtual() {
        Entrada e = entrada("\n\n\n");
        assertEquals("atual", e.lerTexto("t", "atual"));
        assertEquals(42, e.lerInteiro("i", 0, 100, 42));
        assertEquals(1.5, e.lerDouble("d", 0, 1.5), 1e-9);
    }

    @Test
    void fimDaEntradaLancaExcecaoPropria() {
        Entrada e = entrada("");
        assertThrows(EntradaEncerradaException.class, () -> e.lerTexto("t"));
    }
}
