package br.com.urbaniza.model.equipamento;

/**
 * Escola — capacidade instalada = número de vagas.
 * RN12: a demanda é a população em idade escolar.
 */
public class Escola extends EquipamentoComunitario {

    private double percentualIdadeEscolar;
    private String nivelEnsino;

    public Escola() {
    }

    public Escola(Long id, Long regiaoId, String nome, int vagas, double percentualIdadeEscolar, String nivelEnsino) {
        super(id, regiaoId, nome, vagas);
        setPercentualIdadeEscolar(percentualIdadeEscolar);
        this.nivelEnsino = nivelEnsino;
    }

    /** RN12 — demanda = teto(população × percentual em idade escolar / 100). */
    @Override
    public int calcularDemanda(int populacao) {
        return (int) Math.ceil(populacao * percentualIdadeEscolar / 100.0);
    }

    @Override
    public String getTipo() {
        return TIPO_ESCOLA;
    }

    public double getPercentualIdadeEscolar() {
        return percentualIdadeEscolar;
    }

    public final void setPercentualIdadeEscolar(double percentualIdadeEscolar) {
        if (!(percentualIdadeEscolar > 0 && percentualIdadeEscolar <= 100)) {
            throw new IllegalArgumentException("O percentual em idade escolar deve estar no intervalo (0, 100].");
        }
        this.percentualIdadeEscolar = percentualIdadeEscolar;
    }

    public String getNivelEnsino() {
        return nivelEnsino;
    }

    public final void setNivelEnsino(String nivelEnsino) {
        this.nivelEnsino = nivelEnsino;
    }

    @Override
    public String toString() {
        return "Escola[id=" + getId() + ", regiaoId=" + getRegiaoId() + ", nome=" + getNome()
                + ", vagas=" + getCapacidadeInstalada() + ", percentualIdadeEscolar=" + percentualIdadeEscolar
                + ", nivelEnsino=" + nivelEnsino + "]";
    }
}
