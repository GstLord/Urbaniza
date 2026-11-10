package br.com.urbaniza.model;

import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.enums.AptidaoParcelamento;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnaliseParcelamentoTest {

    private static AnaliseParcelamento analiseNorte() {
        return new AnaliseParcelamento(DadosExemploC8.regiaoNorte(), DadosExemploC8.rafaelMoreira(),
                DadosExemploC8.criteriosNorte());
    }

    @Test
    void regiaoNorteEhAptaComRestricao() {
        AnaliseParcelamento analise = analiseNorte();
        analise.executar();
        assertEquals(AptidaoParcelamento.APTA_COM_RESTRICAO, analise.getResultado());
        assertEquals(8.82, analise.getIndiceAreaVerdeApurado(), 0.01);
        assertEquals(3, analise.getResultados().size());
        ResultadoCriterio alagamento = analise.getResultados().get(0);
        assertEquals("ALG-NORTE-01", alagamento.criterio().getCodigoInterno());
        assertFalse(alagamento.atendido());
        assertNotNull(alagamento.descricaoImpedimento());
        assertTrue(analise.getResultados().get(1).atendido());
        assertNull(analise.getResultados().get(1).descricaoImpedimento());
        assertNotNull(analise.getDataEmissao());
        assertEquals(0, analise.getDataEmissao().getNano());

        String parecer = analise.getParecerTextual();
        assertTrue(parecer.contains("Apta com restrição"));
        assertTrue(parecer.contains("APTA_COM_RESTRICAO"));
        assertTrue(parecer.contains("20.400"));
        assertTrue(parecer.contains("8,82"));
        assertTrue(parecer.contains("Deficitário"));
        assertTrue(parecer.contains("45,00%"));
        assertTrue(parecer.contains("Rafael Moreira"));
    }

    @Test
    void deficitsDaRegiaoNorte() {
        AnaliseParcelamento analise = analiseNorte();
        List<String> deficits = analise.listarDeficits();
        assertEquals(4, deficits.size());
        assertEquals("ESCOLA EM Professora Helena Duarte: índice 0,86 — déficit", deficits.get(0));
        assertEquals("UNIDADE_SAUDE UBS Jardim das Águas: índice 0,83 — déficit", deficits.get(1));
        assertTrue(deficits.get(2).contains("Deficitário"));
        assertTrue(deficits.get(3).contains("Alto"));
    }

    @Test
    void regiaoCentralEhApta() {
        AnaliseParcelamento analise = new AnaliseParcelamento(DadosExemploC8.regiaoCentral(),
                DadosExemploC8.biancaTeixeira(), DadosExemploC8.criteriosCentral());
        analise.executar();
        assertEquals(AptidaoParcelamento.APTA, analise.getResultado());
        assertEquals(14.29, analise.getIndiceAreaVerdeApurado(), 0.01);
        assertTrue(analise.listarDeficits().isEmpty());
        assertTrue(analise.getParecerTextual().contains("Nenhum déficit identificado."));
    }

    @Test
    void regiaoSulEhInapta() {
        AnaliseParcelamento analise = new AnaliseParcelamento(DadosExemploC8.regiaoSul(),
                DadosExemploC8.rafaelMoreira(), DadosExemploC8.criteriosSul());
        analise.executar();
        assertEquals(AptidaoParcelamento.INAPTA, analise.getResultado());
        assertEquals(250.0, analise.getIndiceAreaVerdeApurado(), 0.01);
        for (ResultadoCriterio rc : analise.getResultados()) {
            assertFalse(rc.atendido());
        }
        assertTrue(analise.getParecerTextual().contains("Declividade média de 34,00% >= limite de 30,00%"));
        assertEquals(List.of("UNIDADE_SAUDE UBS Serra Alta: índice 0,50 — déficit"), analise.listarDeficits());
    }

    @Test
    void semCriteriosLancaRegraNegocio() {
        AnaliseParcelamento analise = new AnaliseParcelamento(DadosExemploC8.regiaoNorte(),
                DadosExemploC8.rafaelMoreira(), List.of());
        RegraNegocioException e = assertThrows(RegraNegocioException.class, analise::executar);
        assertEquals("Cadastre ao menos um critério de vedação para a região antes de emitir a análise",
                e.getMessage());
    }

    @Test
    void versaoDadosEhDeterministicaESensivelAAlteracoes() {
        AnaliseParcelamento a1 = analiseNorte();
        AnaliseParcelamento a2 = analiseNorte();
        a1.executar();
        a2.executar();
        assertEquals(a1.getVersaoDados(), a2.getVersaoDados());
        assertTrue(a1.getVersaoDados().startsWith("v-"));
        assertEquals(14, a1.getVersaoDados().length());
        assertTrue(a1.getVersaoDados().substring(2).matches("[0-9a-f]{12}"));

        Regiao alterada = DadosExemploC8.regiaoNorte();
        alterada.setPopulacaoAtual(18_001);
        AnaliseParcelamento a3 = new AnaliseParcelamento(alterada, DadosExemploC8.rafaelMoreira(),
                DadosExemploC8.criteriosNorte());
        a3.executar();
        assertNotEquals(a1.getVersaoDados(), a3.getVersaoDados());
    }

    @Test
    void versaoDadosMudaQuandoLoteOuCriterioMuda() {
        String base = analiseNorte().calcularVersaoDados();

        Regiao comLoteAlterado = DadosExemploC8.regiaoNorte();
        comLoteAlterado.getLoteamentos().get(0).getLotes().get(0).setAreaM2(251);
        assertNotEquals(base, new AnaliseParcelamento(comLoteAlterado, DadosExemploC8.rafaelMoreira(),
                DadosExemploC8.criteriosNorte()).calcularVersaoDados());

        List<br.com.urbaniza.model.criterio.CriterioVedacao> criterios = DadosExemploC8.criteriosNorte();
        ((br.com.urbaniza.model.criterio.CriterioAlagamento) criterios.get(0)).setSaneamentoExecutado(true);
        assertNotEquals(base, new AnaliseParcelamento(DadosExemploC8.regiaoNorte(), DadosExemploC8.rafaelMoreira(),
                criterios).calcularVersaoDados());
    }

    @Test
    void parecerContemFraseObrigatoria() {
        AnaliseParcelamento analise = analiseNorte();
        analise.executar();
        assertTrue(analise.getParecerTextual().contains("Indicadores estimados a partir dos dados cadastrados; "
                + "não substituem o parecer técnico nem constituem previsão certificada."));
    }

    @Test
    void populacaoProjetadaZeroResultaEmIavNulo() {
        Regiao vazia = new Regiao(9L, "Vazia", 1_000, 100, 0, 0, 0);
        AnaliseParcelamento analise = new AnaliseParcelamento(vazia, DadosExemploC8.rafaelMoreira(),
                List.of(new br.com.urbaniza.model.criterio.CriterioPreservacao(null, 9L, "P", null, false)));
        analise.executar();
        assertNull(analise.getIndiceAreaVerdeApurado());
        assertEquals(AptidaoParcelamento.APTA, analise.getResultado());
    }
}
