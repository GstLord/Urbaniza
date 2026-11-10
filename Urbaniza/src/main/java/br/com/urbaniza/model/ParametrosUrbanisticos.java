package br.com.urbaniza.model;

/**
 * Parâmetros urbanísticos centralizados. Nenhuma regra usa números mágicos:
 * todas referenciam estas constantes.
 */
public final class ParametrosUrbanisticos {

    /** RN05 — área mínima do lote (Lei 6.766/79, art. 4º, II). */
    public static final double LOTE_AREA_MINIMA_M2 = 125.0;

    /** RN05 — frente mínima do lote (Lei 6.766/79, art. 4º, II). */
    public static final double LOTE_FRENTE_MINIMA_M = 5.0;

    /** RN02 — declividade a partir da qual não se parcela (Lei 6.766/79, art. 3º, p.ú., III). */
    public static final double DECLIVIDADE_LIMITE_PADRAO_PCT = 30.0;

    /** RN08 — IAV acima deste valor é ADEQUADO. */
    public static final double IAV_LIMITE_ADEQUADO = 12.0;

    /** RN08 — IAV abaixo deste valor é DEFICITARIO. */
    public static final double IAV_LIMITE_MINIMO = 9.0;

    /** RN10 / E6 — cobertura vegetal a partir da qual o potencial de ilha de calor é BAIXO. */
    public static final double COBERTURA_VEGETAL_RISCO_BAIXO = 0.30;

    /** RN10 / E6 — cobertura vegetal a partir da qual o potencial de ilha de calor é MEDIO. */
    public static final double COBERTURA_VEGETAL_RISCO_MEDIO = 0.15;

    private ParametrosUrbanisticos() {
    }
}
