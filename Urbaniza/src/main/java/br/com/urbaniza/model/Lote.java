package br.com.urbaniza.model;

/**
 * Lote de um loteamento (composição: o lote só existe dentro do loteamento).
 * RF06 / RN05 — dimensões mínimas.
 */
public class Lote {

    private Long id;
    private String quadra;
    private String numero;
    private double areaM2;
    private double frenteM;

    public Lote() {
    }

    public Lote(Long id, String quadra, String numero, double areaM2, double frenteM) {
        this.id = id;
        setQuadra(quadra);
        setNumero(numero);
        setAreaM2(areaM2);
        setFrenteM(frenteM);
    }

    /**
     * RF06 / RN05 — o lote atende às dimensões mínimas da Lei 6.766/79, art. 4º, II
     * (área &gt;= 125 m² e frente &gt;= 5 m).
     */
    public boolean atendeDimensoesMinimas() {
        return areaM2 >= ParametrosUrbanisticos.LOTE_AREA_MINIMA_M2
                && frenteM >= ParametrosUrbanisticos.LOTE_FRENTE_MINIMA_M;
    }

    public Long getId() {
        return id;
    }

    public final void setId(Long id) {
        this.id = id;
    }

    public String getQuadra() {
        return quadra;
    }

    public final void setQuadra(String quadra) {
        this.quadra = quadra;
    }

    public String getNumero() {
        return numero;
    }

    public final void setNumero(String numero) {
        this.numero = numero;
    }

    public double getAreaM2() {
        return areaM2;
    }

    public final void setAreaM2(double areaM2) {
        if (areaM2 < 0 || Double.isNaN(areaM2)) {
            throw new IllegalArgumentException("A área do lote não pode ser negativa.");
        }
        this.areaM2 = areaM2;
    }

    public double getFrenteM() {
        return frenteM;
    }

    public final void setFrenteM(double frenteM) {
        if (frenteM < 0 || Double.isNaN(frenteM)) {
            throw new IllegalArgumentException("A frente do lote não pode ser negativa.");
        }
        this.frenteM = frenteM;
    }

    @Override
    public String toString() {
        return "Lote[id=" + id + ", quadra=" + quadra + ", numero=" + numero
                + ", areaM2=" + areaM2 + ", frenteM=" + frenteM + "]";
    }
}
