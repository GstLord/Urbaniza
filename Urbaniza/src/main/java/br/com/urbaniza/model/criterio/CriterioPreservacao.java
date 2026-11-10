package br.com.urbaniza.model.criterio;

import br.com.urbaniza.model.Regiao;

/**
 * RN04 — área de preservação ecológica: não parcelar
 * (Lei 6.766/79, art. 3º, parágrafo único, V). Impedimento absoluto.
 */
public class CriterioPreservacao extends CriterioVedacao {

    public static final String FUNDAMENTO_PADRAO = "Lei 6.766/79, art. 3º, parágrafo único, V";

    private boolean areaProtegida;

    public CriterioPreservacao() {
        super(null, null, null, FUNDAMENTO_PADRAO);
    }

    public CriterioPreservacao(Long id, Long regiaoId, String codigoInterno, String fundamentoLegal,
                               boolean areaProtegida) {
        super(id, regiaoId, codigoInterno, fundamentoLegal == null ? FUNDAMENTO_PADRAO : fundamentoLegal);
        this.areaProtegida = areaProtegida;
    }

    /** RN04 — atende somente se a área NÃO for protegida. */
    @Override
    public boolean avaliar(Regiao regiao) {
        return !areaProtegida;
    }

    /** RN15 / E3 — área de preservação é impeditiva: gera INAPTA. */
    @Override
    public boolean isImpedimentoAbsoluto() {
        return true;
    }

    @Override
    public String getTipo() {
        return TIPO_PRESERVACAO;
    }

    @Override
    public String descreverImpedimento() {
        return "Área de preservação ecológica (Lei 6.766/79, art. 3º, p.ú., V)";
    }

    public boolean isAreaProtegida() {
        return areaProtegida;
    }

    public final void setAreaProtegida(boolean areaProtegida) {
        this.areaProtegida = areaProtegida;
    }

    @Override
    public String toString() {
        return "CriterioPreservacao[id=" + getId() + ", regiaoId=" + getRegiaoId() + ", codigoInterno="
                + getCodigoInterno() + ", fundamentoLegal=" + getFundamentoLegal()
                + ", areaProtegida=" + areaProtegida + "]";
    }
}
