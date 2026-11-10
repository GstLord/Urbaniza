package br.com.urbaniza.model;

import br.com.urbaniza.model.enums.ClassificacaoIAV;
import br.com.urbaniza.model.enums.PotencialIlhaCalor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegiaoTest {

    private static Regiao regiaoComIav(double areaVerde, int populacao) {
        return new Regiao(null, "Fronteira", 10_000_000, areaVerde, 0, populacao, 0);
    }

    @Test
    void regiaoNorteDaC8() {
        Regiao norte = DadosExemploC8.regiaoNorte();
        assertEquals(20_400, norte.calcularPopulacaoProjetada());
        assertEquals(8.82, norte.calcularIAV(), 0.01);
        assertEquals(ClassificacaoIAV.DEFICITARIO, norte.classificarIAV());
        assertEquals(0.45, norte.calcularTaxaImpermeabilizacao(), 1e-9);
        assertEquals(PotencialIlhaCalor.ALTO, norte.classificarIlhaDeCalor());
    }

    @Test
    void regiaoCentralESulDaC8() {
        Regiao central = DadosExemploC8.regiaoCentral();
        assertEquals(21_000, central.calcularPopulacaoProjetada());
        assertEquals(14.29, central.calcularIAV(), 0.01);
        assertEquals(ClassificacaoIAV.ADEQUADO, central.classificarIAV());
        assertEquals(0.50, central.calcularTaxaImpermeabilizacao(), 1e-9);
        assertEquals(PotencialIlhaCalor.MEDIO, central.classificarIlhaDeCalor());

        Regiao sul = DadosExemploC8.regiaoSul();
        assertEquals(6_000, sul.calcularPopulacaoProjetada());
        assertEquals(250.0, sul.calcularIAV(), 0.01);
        assertEquals(ClassificacaoIAV.ADEQUADO, sul.classificarIAV());
        assertEquals(0.10, sul.calcularTaxaImpermeabilizacao(), 1e-9);
        assertEquals(PotencialIlhaCalor.BAIXO, sul.classificarIlhaDeCalor());
    }

    @Test
    void fronteirasDoIav() {
        assertEquals(ClassificacaoIAV.ACEITAVEL, regiaoComIav(120_000, 10_000).classificarIAV());   // 12,00
        assertEquals(ClassificacaoIAV.ADEQUADO, regiaoComIav(120_100, 10_000).classificarIAV());    // 12,01
        assertEquals(ClassificacaoIAV.ACEITAVEL, regiaoComIav(90_000, 10_000).classificarIAV());    // 9,00
        assertEquals(ClassificacaoIAV.DEFICITARIO, regiaoComIav(89_900, 10_000).classificarIAV()); // 8,99
    }

    @Test
    void populacaoZeroResultaEmIavInfinitoEAdequado() {
        Regiao vazia = regiaoComIav(1_000, 0);
        assertEquals(Double.POSITIVE_INFINITY, vazia.calcularIAV());
        assertEquals(ClassificacaoIAV.ADEQUADO, vazia.classificarIAV());
    }

    @Test
    void fronteirasDaIlhaDeCalor() {
        assertEquals(PotencialIlhaCalor.BAIXO, new Regiao(null, "x", 100, 30, 0, 1, 0).classificarIlhaDeCalor());
        assertEquals(PotencialIlhaCalor.MEDIO, new Regiao(null, "x", 100, 15, 0, 1, 0).classificarIlhaDeCalor());
        assertEquals(PotencialIlhaCalor.ALTO, new Regiao(null, "x", 100, 14.9, 0, 1, 0).classificarIlhaDeCalor());
    }

    @Test
    void adicionarLoteamentoVinculaRegiao() {
        Regiao r = new Regiao(42L, "x", 100, 0, 0, 0, 0);
        Loteamento l = new Loteamento();
        r.adicionarLoteamento(l);
        assertEquals(42L, l.getRegiaoId());
    }

    @Test
    void valoresImpossiveisLancamExcecao() {
        Regiao r = new Regiao();
        assertThrows(IllegalArgumentException.class, () -> r.setPopulacaoAtual(-1));
        assertThrows(IllegalArgumentException.class, () -> r.setAreaVerdeM2(-1));
        assertThrows(IllegalArgumentException.class, () -> r.setAreaTotalM2(0));
    }
}
