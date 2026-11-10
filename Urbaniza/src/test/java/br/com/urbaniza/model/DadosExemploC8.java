package br.com.urbaniza.model;

import br.com.urbaniza.model.criterio.CriterioAlagamento;
import br.com.urbaniza.model.criterio.CriterioDeclividade;
import br.com.urbaniza.model.criterio.CriterioGeologico;
import br.com.urbaniza.model.criterio.CriterioPreservacao;
import br.com.urbaniza.model.criterio.CriterioVedacao;
import br.com.urbaniza.model.equipamento.Escola;
import br.com.urbaniza.model.equipamento.UnidadeSaude;

import java.time.LocalDate;
import java.util.List;

/**
 * Monta em memória os dados de exemplo da seção C8 do contrato técnico
 * (teste de aceitação comum entre SQL, Java e roteiro de demonstração).
 */
public final class DadosExemploC8 {

    private DadosExemploC8() {
    }

    public static Analista rafaelMoreira() {
        return new Analista(1L, "Rafael Moreira", "AP-1041", "Análise de Projetos");
    }

    public static Analista biancaTeixeira() {
        return new Analista(2L, "Bianca Teixeira", "CI-2036", "Cadastro Imobiliário");
    }

    /** REGIÃO 1 — esperado: pop. projetada 20.400, IAV 8,82 DEFICITARIO, APTA_COM_RESTRICAO. */
    public static Regiao regiaoNorte() {
        Regiao regiao = new Regiao(1L, "Região Norte - Jardim das Águas", 2_000_000, 180_000, 900_000, 18_000, 8);

        Loteamento aguasClaras = new Loteamento(1L, null, "Residencial Águas Claras", LocalDate.of(2025, 3, 10),
                150_000, 2_400, false, null);
        aguasClaras.adicionarLote(new Lote(1L, "A", "1", 250, 10));
        aguasClaras.adicionarLote(new Lote(2L, "A", "2", 200, 8));
        aguasClaras.adicionarLote(new Lote(3L, "B", "1", 180, 7.5));
        regiao.adicionarLoteamento(aguasClaras);

        Loteamento ribeirinho = new Loteamento(2L, null, "Jardim Ribeirinho", LocalDate.of(2019, 6, 1),
                200_000, 3_000, true, null);
        ribeirinho.adicionarLote(new Lote(4L, "A", "1", 300, 12));
        ribeirinho.adicionarLote(new Lote(5L, "A", "2", 300, 12));
        regiao.adicionarLoteamento(ribeirinho);

        Loteamento garcas = new Loteamento(3L, null, "Parque das Garças", null, 90_000, 1_500, false, null);
        garcas.adicionarLote(new Lote(6L, "A", "1", 150, 6));
        garcas.adicionarLote(new Lote(7L, "A", "2", 160, 6.5));
        regiao.adicionarLoteamento(garcas);

        regiao.adicionarEquipamento(new Escola(1L, null, "EM Professora Helena Duarte", 2_800, 16, "Fundamental"));
        regiao.adicionarEquipamento(new UnidadeSaude(2L, null, "UBS Jardim das Águas", 5, 3_500, "UBS"));
        return regiao;
    }

    public static List<CriterioVedacao> criteriosNorte() {
        return List.of(
                new CriterioAlagamento(1L, 1L, "ALG-NORTE-01", null, false),
                new CriterioDeclividade(2L, 1L, "DEC-NORTE-01", null, 30),
                new CriterioPreservacao(3L, 1L, "PRS-NORTE-01", null, false));
    }

    /** REGIÃO 2 — esperado: pop. projetada 21.000, IAV 14,29 ADEQUADO, APTA. */
    public static Regiao regiaoCentral() {
        Regiao regiao = new Regiao(2L, "Região Central - Vila Esperança", 1_200_000, 300_000, 600_000, 20_000, 4);

        Loteamento belaVista = new Loteamento(4L, null, "Residencial Bela Vista", LocalDate.of(2024, 8, 20),
                60_000, 1_000, false, null);
        belaVista.adicionarLote(new Lote(8L, "A", "1", 200, 8));
        belaVista.adicionarLote(new Lote(9L, "A", "2", 220, 9));
        regiao.adicionarLoteamento(belaVista);

        regiao.adicionarEquipamento(new Escola(3L, null, "EE Vila Esperança", 4_000, 15, "Médio"));
        regiao.adicionarEquipamento(new UnidadeSaude(4L, null, "UBS Central", 7, 3_000, "UBS"));
        return regiao;
    }

    public static List<CriterioVedacao> criteriosCentral() {
        return List.of(
                new CriterioGeologico(4L, 2L, "GEO-CENTRO-01", null, "ADEQUADO: solo argiloso estável"),
                new CriterioDeclividade(5L, 2L, "DEC-CENTRO-01", null, 30));
    }

    /** REGIÃO 3 — esperado: pop. projetada 6.000, IAV 250,00 ADEQUADO, INAPTA. */
    public static Regiao regiaoSul() {
        Regiao regiao = new Regiao(3L, "Região Sul - Serra Alta", 3_000_000, 1_500_000, 300_000, 6_000, 34);

        Loteamento chacaras = new Loteamento(5L, null, "Chácaras Serra Alta", null, 400_000, 800, false, null);
        chacaras.adicionarLote(new Lote(10L, "A", "1", 1_000, 20));
        chacaras.adicionarLote(new Lote(11L, "A", "2", 1_200, 24));
        regiao.adicionarLoteamento(chacaras);

        regiao.adicionarEquipamento(new UnidadeSaude(5L, null, "UBS Serra Alta", 1, 3_500, "UBS"));
        return regiao;
    }

    public static List<CriterioVedacao> criteriosSul() {
        return List.of(
                new CriterioDeclividade(6L, 3L, "DEC-SUL-01", null, 30),
                new CriterioPreservacao(7L, 3L, "PRS-SUL-01", null, true),
                new CriterioGeologico(8L, 3L, "GEO-SUL-01", null,
                        "INADEQUADO: encosta com risco de deslizamento"));
    }

    /** Equipamento sem região (agregação — regiao_id NULL). */
    public static Escola escolaRuralItinerante() {
        return new Escola(6L, null, "Escola Rural Itinerante", 120, 20, "Fundamental");
    }
}
