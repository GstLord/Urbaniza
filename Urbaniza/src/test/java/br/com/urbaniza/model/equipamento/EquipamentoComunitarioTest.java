package br.com.urbaniza.model.equipamento;

import br.com.urbaniza.model.DadosExemploC8;
import br.com.urbaniza.model.Regiao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipamentoComunitarioTest {

    @Test
    void escolaHelenaDuarteTemDeficit() {
        Regiao norte = DadosExemploC8.regiaoNorte();
        EquipamentoComunitario escola = norte.getEquipamentos().get(0);
        int pop = norte.calcularPopulacaoProjetada();
        assertEquals(3_264, escola.calcularDemanda(pop));
        assertEquals(0.86, escola.calcularIndiceAtendimento(pop), 0.005);
        assertTrue(escola.possuiDeficit(pop));
    }

    @Test
    void ubsJardimDasAguasTemDeficit() {
        Regiao norte = DadosExemploC8.regiaoNorte();
        EquipamentoComunitario ubs = norte.getEquipamentos().get(1);
        int pop = norte.calcularPopulacaoProjetada();
        assertEquals(6, ubs.calcularDemanda(pop));
        assertEquals(0.83, ubs.calcularIndiceAtendimento(pop), 0.005);
        assertTrue(ubs.possuiDeficit(pop));
    }

    @Test
    void ubsCentralNoLimiteExatoNaoTemDeficit() {
        Regiao central = DadosExemploC8.regiaoCentral();
        EquipamentoComunitario ubs = central.getEquipamentos().get(1);
        int pop = central.calcularPopulacaoProjetada();
        assertEquals(7, ubs.calcularDemanda(pop));
        assertEquals(1.0, ubs.calcularIndiceAtendimento(pop), 1e-9);
        assertFalse(ubs.possuiDeficit(pop));

        EquipamentoComunitario escola = central.getEquipamentos().get(0);
        assertEquals(3_150, escola.calcularDemanda(pop));
        assertEquals(1.27, escola.calcularIndiceAtendimento(pop), 0.005);
        assertFalse(escola.possuiDeficit(pop));
    }

    @Test
    void ubsSerraAltaTemDeficit() {
        Regiao sul = DadosExemploC8.regiaoSul();
        EquipamentoComunitario ubs = sul.getEquipamentos().get(0);
        int pop = sul.calcularPopulacaoProjetada();
        assertEquals(2, ubs.calcularDemanda(pop));
        assertEquals(0.50, ubs.calcularIndiceAtendimento(pop), 1e-9);
        assertTrue(ubs.possuiDeficit(pop));
    }

    @Test
    void demandaZeroResultaEmInfinitoSemDeficit() {
        EquipamentoComunitario escola = new Escola(null, null, "Vazia", 10, 15, "Fundamental");
        assertEquals(0, escola.calcularDemanda(0));
        assertEquals(Double.POSITIVE_INFINITY, escola.calcularIndiceAtendimento(0));
        assertFalse(escola.possuiDeficit(0));
    }

    @Test
    void calculoDeDemandaPolimorfico() {
        List<EquipamentoComunitario> equipamentos = List.of(
                new Escola(null, null, "Escola", 100, 16, "Fundamental"),
                new UnidadeSaude(null, null, "UBS", 1, 3_500, "UBS"));
        int[] demandasEsperadas = {3_264, 6};
        String[] tiposEsperados = {"ESCOLA", "UNIDADE_SAUDE"};
        for (int i = 0; i < equipamentos.size(); i++) {
            EquipamentoComunitario e = equipamentos.get(i);
            assertEquals(demandasEsperadas[i], e.calcularDemanda(20_400));
            assertEquals(tiposEsperados[i], e.getTipo());
        }
    }

    @Test
    void valoresImpossiveisLancamExcecao() {
        Escola escola = new Escola();
        assertThrows(IllegalArgumentException.class, () -> escola.setPercentualIdadeEscolar(0));
        assertThrows(IllegalArgumentException.class, () -> escola.setPercentualIdadeEscolar(100.01));
        UnidadeSaude ubs = new UnidadeSaude();
        assertThrows(IllegalArgumentException.class, () -> ubs.setHabitantesPorEquipe(0));
        assertThrows(IllegalArgumentException.class, () -> ubs.setCapacidadeInstalada(-1));
    }
}
