package br.com.urbaniza.model.criterio;

import br.com.urbaniza.model.Regiao;

/**
 * RN01 — terreno alagadiço ou sujeito a inundação: não parcelar antes do saneamento
 * (Lei 6.766/79, art. 3º, parágrafo único, I). Impedimento superável.
 */
public class CriterioAlagamento extends CriterioVedacao {

    public static final String FUNDAMENTO_PADRAO = "Lei 6.766/79, art. 3º, parágrafo único, I";

    private boolean saneamentoExecutado;

    public CriterioAlagamento() {
        super(null, null, null, FUNDAMENTO_PADRAO);
    }

    public CriterioAlagamento(Long id, Long regiaoId, String codigoInterno, String fundamentoLegal,
                              boolean saneamentoExecutado) {
        super(id, regiaoId, codigoInterno, fundamentoLegal == null ? FUNDAMENTO_PADRAO : fundamentoLegal);
        this.saneamentoExecutado = saneamentoExecutado;
    }

    /** RN01 — atende somente se o saneamento já foi executado. */
    @Override
    public boolean avaliar(Regiao regiao) {
        return saneamentoExecutado;
    }

    /** RN15 / E3 — superável após o saneamento: gera APTA_COM_RESTRICAO. */
    @Override
    public boolean isImpedimentoAbsoluto() {
        return false;
    }

    @Override
    public String getTipo() {
        return TIPO_ALAGAMENTO;
    }

    @Override
    public String descreverImpedimento() {
        return "Terreno alagadiço ou sujeito a inundação sem saneamento executado "
                + "(Lei 6.766/79, art. 3º, p.ú., I)";
    }

    public boolean isSaneamentoExecutado() {
        return saneamentoExecutado;
    }

    public final void setSaneamentoExecutado(boolean saneamentoExecutado) {
        this.saneamentoExecutado = saneamentoExecutado;
    }

    @Override
    public String toString() {
        return "CriterioAlagamento[id=" + getId() + ", regiaoId=" + getRegiaoId() + ", codigoInterno="
                + getCodigoInterno() + ", fundamentoLegal=" + getFundamentoLegal()
                + ", saneamentoExecutado=" + saneamentoExecutado + "]";
    }
}
