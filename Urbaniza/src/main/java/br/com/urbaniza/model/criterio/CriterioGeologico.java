package br.com.urbaniza.model.criterio;

import br.com.urbaniza.model.Regiao;

import java.util.Locale;

/**
 * RN03 — condições geológicas que não aconselham a edificação: não parcelar
 * (Lei 6.766/79, art. 3º, parágrafo único, IV). Impedimento absoluto.
 * O laudo segue o formato "ADEQUADO" ou "INADEQUADO", opcionalmente seguido de ": resumo".
 */
public class CriterioGeologico extends CriterioVedacao {

    public static final String FUNDAMENTO_PADRAO = "Lei 6.766/79, art. 3º, parágrafo único, IV";
    public static final String LAUDO_ADEQUADO = "ADEQUADO";
    public static final String LAUDO_INADEQUADO = "INADEQUADO";

    private String laudoGeotecnico;

    public CriterioGeologico() {
        super(null, null, null, FUNDAMENTO_PADRAO);
    }

    public CriterioGeologico(Long id, Long regiaoId, String codigoInterno, String fundamentoLegal,
                             String laudoGeotecnico) {
        super(id, regiaoId, codigoInterno, fundamentoLegal == null ? FUNDAMENTO_PADRAO : fundamentoLegal);
        this.laudoGeotecnico = laudoGeotecnico;
    }

    /** RN03 — reprova quando o laudo começa com "INADEQUADO" (sem diferenciar maiúsculas). */
    @Override
    public boolean avaliar(Regiao regiao) {
        String laudo = laudoGeotecnico == null ? "" : laudoGeotecnico;
        return !laudo.trim().toUpperCase(Locale.ROOT).startsWith(LAUDO_INADEQUADO);
    }

    /** RN15 / E3 — geologia inadequada é impeditiva: gera INAPTA. */
    @Override
    public boolean isImpedimentoAbsoluto() {
        return true;
    }

    @Override
    public String getTipo() {
        return TIPO_GEOLOGICO;
    }

    @Override
    public String descreverImpedimento() {
        return "Condições geológicas inadequadas à edificação — laudo: \"" + laudoGeotecnico
                + "\" (Lei 6.766/79, art. 3º, p.ú., IV)";
    }

    public String getLaudoGeotecnico() {
        return laudoGeotecnico;
    }

    public final void setLaudoGeotecnico(String laudoGeotecnico) {
        this.laudoGeotecnico = laudoGeotecnico;
    }

    @Override
    public String toString() {
        return "CriterioGeologico[id=" + getId() + ", regiaoId=" + getRegiaoId() + ", codigoInterno="
                + getCodigoInterno() + ", fundamentoLegal=" + getFundamentoLegal()
                + ", laudoGeotecnico=" + laudoGeotecnico + "]";
    }
}
