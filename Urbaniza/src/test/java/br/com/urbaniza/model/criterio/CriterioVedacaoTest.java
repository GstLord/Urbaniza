package br.com.urbaniza.model.criterio;

import br.com.urbaniza.model.Regiao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CriterioVedacaoTest {

    private static Regiao regiaoComDeclividade(double declividade) {
        return new Regiao(1L, "Teste", 1_000, 100, 100, 100, declividade);
    }

    @Test
    void alagamento() {
        Regiao r = regiaoComDeclividade(0);
        CriterioVedacao comSaneamento = new CriterioAlagamento(null, 1L, "ALG", null, true);
        CriterioVedacao semSaneamento = new CriterioAlagamento(null, 1L, "ALG", null, false);
        assertTrue(comSaneamento.avaliar(r));
        assertFalse(semSaneamento.avaliar(r));
        assertFalse(semSaneamento.isImpedimentoAbsoluto());
        assertEquals("ALAGAMENTO", semSaneamento.getTipo());
        assertEquals("Lei 6.766/79, art. 3º, parágrafo único, I", new CriterioAlagamento().getFundamentoLegal());
    }

    @Test
    void declividade() {
        CriterioDeclividade criterio = new CriterioDeclividade();
        criterio.setCodigoInterno("DEC");
        assertEquals(30.0, criterio.getDeclividadeLimite());
        assertTrue(criterio.avaliar(regiaoComDeclividade(29.99)));
        assertFalse(criterio.avaliar(regiaoComDeclividade(30)));
        assertFalse(criterio.avaliar(regiaoComDeclividade(34)));
        assertFalse(criterio.isImpedimentoAbsoluto());
        assertEquals("DECLIVIDADE", criterio.getTipo());
        assertEquals("Lei 6.766/79, art. 3º, parágrafo único, III", criterio.getFundamentoLegal());
        assertEquals("Declividade média de 34,00% >= limite de 30,00% (Lei 6.766/79, art. 3º, p.ú., III)",
                criterio.descreverImpedimento(regiaoComDeclividade(34)));
    }

    @Test
    void geologico() {
        Regiao r = regiaoComDeclividade(0);
        assertTrue(new CriterioGeologico(null, 1L, "GEO", null, "ADEQUADO: solo estável").avaliar(r));
        assertTrue(new CriterioGeologico(null, 1L, "GEO", null, "ADEQUADO").avaliar(r));
        assertFalse(new CriterioGeologico(null, 1L, "GEO", null, "INADEQUADO: encosta").avaliar(r));
        assertFalse(new CriterioGeologico(null, 1L, "GEO", null, "  inadequado: encosta instável").avaliar(r));
        CriterioGeologico padrao = new CriterioGeologico();
        assertTrue(padrao.isImpedimentoAbsoluto());
        assertEquals("GEOLOGICO", padrao.getTipo());
        assertEquals("Lei 6.766/79, art. 3º, parágrafo único, IV", padrao.getFundamentoLegal());
    }

    @Test
    void preservacao() {
        Regiao r = regiaoComDeclividade(0);
        assertTrue(new CriterioPreservacao(null, 1L, "PRS", null, false).avaliar(r));
        CriterioVedacao protegida = new CriterioPreservacao(null, 1L, "PRS", null, true);
        assertFalse(protegida.avaliar(r));
        assertTrue(protegida.isImpedimentoAbsoluto());
        assertEquals("PRESERVACAO", protegida.getTipo());
        assertEquals("Lei 6.766/79, art. 3º, parágrafo único, V", new CriterioPreservacao().getFundamentoLegal());
    }

    @Test
    void descricoesSobrescritasSaoEspecificas() {
        CriterioVedacao[] criterios = {
                new CriterioAlagamento(), new CriterioDeclividade(), new CriterioGeologico(), new CriterioPreservacao()};
        String[] trechos = {"saneamento", "Declividade", "geológicas", "preservação"};
        for (int i = 0; i < criterios.length; i++) {
            assertTrue(criterios[i].descreverImpedimento().contains(trechos[i]),
                    () -> "descrição inesperada");
        }
    }
}
