package br.com.urbaniza.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Loteamento vinculado obrigatoriamente a uma região (RF02 / RN17).
 * Composição 1:N com {@link Lote}.
 */
public class Loteamento {

    private Long id;
    private Long regiaoId;
    private String denominacao;
    private LocalDate dataAprovacao;
    private double areaGlebaM2;
    private int populacaoEstimada;
    private boolean ocupado;
    private List<Lote> lotes = new ArrayList<>();

    public Loteamento() {
    }

    public Loteamento(Long id, Long regiaoId, String denominacao, LocalDate dataAprovacao,
                      double areaGlebaM2, int populacaoEstimada, boolean ocupado, List<Lote> lotes) {
        this.id = id;
        this.regiaoId = regiaoId;
        this.denominacao = denominacao;
        this.dataAprovacao = dataAprovacao;
        setAreaGlebaM2(areaGlebaM2);
        setPopulacaoEstimada(populacaoEstimada);
        this.ocupado = ocupado;
        setLotes(lotes);
    }

    /** RN13 — o loteamento está aprovado quando possui data de aprovação. */
    public boolean isAprovado() {
        return dataAprovacao != null;
    }

    /** RN13 — só loteamentos APROVADOS e AINDA NÃO OCUPADOS entram na população projetada. */
    public boolean contribuiParaProjecao() {
        return isAprovado() && !ocupado;
    }

    /** Soma das áreas de todos os lotes (m²). */
    public double calcularAreaLoteavel() {
        double total = 0.0;
        for (Lote lote : lotes) {
            total += lote.getAreaM2();
        }
        return total;
    }

    /** RF06 / RN05 — todos os lotes atendem às dimensões mínimas. */
    public boolean validarDimensoesLotes() {
        for (Lote lote : lotes) {
            if (!lote.atendeDimensoesMinimas()) {
                return false;
            }
        }
        return true;
    }

    /** Composição — adiciona um lote ao loteamento. */
    public void adicionarLote(Lote lote) {
        if (lote == null) {
            throw new IllegalArgumentException("O lote não pode ser nulo.");
        }
        lotes.add(lote);
    }

    public Long getId() {
        return id;
    }

    public final void setId(Long id) {
        this.id = id;
    }

    public Long getRegiaoId() {
        return regiaoId;
    }

    public final void setRegiaoId(Long regiaoId) {
        this.regiaoId = regiaoId;
    }

    public String getDenominacao() {
        return denominacao;
    }

    public final void setDenominacao(String denominacao) {
        this.denominacao = denominacao;
    }

    public LocalDate getDataAprovacao() {
        return dataAprovacao;
    }

    public final void setDataAprovacao(LocalDate dataAprovacao) {
        this.dataAprovacao = dataAprovacao;
    }

    public double getAreaGlebaM2() {
        return areaGlebaM2;
    }

    public final void setAreaGlebaM2(double areaGlebaM2) {
        if (areaGlebaM2 < 0 || Double.isNaN(areaGlebaM2)) {
            throw new IllegalArgumentException("A área da gleba não pode ser negativa.");
        }
        this.areaGlebaM2 = areaGlebaM2;
    }

    public int getPopulacaoEstimada() {
        return populacaoEstimada;
    }

    public final void setPopulacaoEstimada(int populacaoEstimada) {
        if (populacaoEstimada < 0) {
            throw new IllegalArgumentException("A população estimada não pode ser negativa.");
        }
        this.populacaoEstimada = populacaoEstimada;
    }

    public boolean isOcupado() {
        return ocupado;
    }

    public final void setOcupado(boolean ocupado) {
        this.ocupado = ocupado;
    }

    public List<Lote> getLotes() {
        return lotes;
    }

    public final void setLotes(List<Lote> lotes) {
        this.lotes = lotes == null ? new ArrayList<>() : new ArrayList<>(lotes);
    }

    @Override
    public String toString() {
        return "Loteamento[id=" + id + ", regiaoId=" + regiaoId + ", denominacao=" + denominacao
                + ", dataAprovacao=" + dataAprovacao + ", areaGlebaM2=" + areaGlebaM2
                + ", populacaoEstimada=" + populacaoEstimada + ", ocupado=" + ocupado
                + ", lotes=" + lotes.size() + "]";
    }
}
