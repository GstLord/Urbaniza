package br.com.urbaniza.model.criterio;

import br.com.urbaniza.model.ParametrosUrbanisticos;
import br.com.urbaniza.model.Regiao;

/**
 * RN02 — declividade igual ou superior a 30%: não parcelar, salvo exigências específicas
 * (Lei 6.766/79, art. 3º, parágrafo único, III). Impedimento superável.
 */
public class CriterioDeclividade extends CriterioVedacao {

    public static final String FUNDAMENTO_PADRAO = "Lei 6.766/79, art. 3º, parágrafo único, III";

    private double declividadeLimite = ParametrosUrbanisticos.DECLIVIDADE_LIMITE_PADRAO_PCT;

    public CriterioDeclividade() {
        super(null, null, null, FUNDAMENTO_PADRAO);
    }

    public CriterioDeclividade(Long id, Long regiaoId, String codigoInterno, String fundamentoLegal,
                               double declividadeLimite) {
        super(id, regiaoId, codigoInterno, fundamentoLegal == null ? FUNDAMENTO_PADRAO : fundamentoLegal);
        setDeclividadeLimite(declividadeLimite);
    }

    /** RN02 — atende se a declividade média da região for menor que o limite. */
    @Override
    public boolean avaliar(Regiao regiao) {
        if (regiao == null) {
            throw new IllegalArgumentException("A região avaliada não pode ser nula.");
        }
        return regiao.getDeclividadeMediaPct() < declividadeLimite;
    }

    /** RN15 / E3 — a lei admite exigências específicas: gera APTA_COM_RESTRICAO. */
    @Override
    public boolean isImpedimentoAbsoluto() {
        return false;
    }

    @Override
    public String getTipo() {
        return TIPO_DECLIVIDADE;
    }

    @Override
    public String descreverImpedimento() {
        return "Declividade média >= limite de " + formatarDecimal(declividadeLimite)
                + "% (Lei 6.766/79, art. 3º, p.ú., III)";
    }

    /** Mensagem com a declividade medida, ex.: "Declividade média de 34,00% &gt;= limite de 30,00% (...)". */
    @Override
    public String descreverImpedimento(Regiao regiao) {
        if (regiao == null) {
            return descreverImpedimento();
        }
        return "Declividade média de " + formatarDecimal(regiao.getDeclividadeMediaPct())
                + "% >= limite de " + formatarDecimal(declividadeLimite)
                + "% (Lei 6.766/79, art. 3º, p.ú., III)";
    }

    public double getDeclividadeLimite() {
        return declividadeLimite;
    }

    public final void setDeclividadeLimite(double declividadeLimite) {
        if (!(declividadeLimite > 0)) {
            throw new IllegalArgumentException("O limite de declividade deve ser maior que zero.");
        }
        this.declividadeLimite = declividadeLimite;
    }

    @Override
    public String toString() {
        return "CriterioDeclividade[id=" + getId() + ", regiaoId=" + getRegiaoId() + ", codigoInterno="
                + getCodigoInterno() + ", fundamentoLegal=" + getFundamentoLegal()
                + ", declividadeLimite=" + declividadeLimite + "]";
    }
}
