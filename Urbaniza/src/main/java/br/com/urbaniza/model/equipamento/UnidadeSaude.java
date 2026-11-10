package br.com.urbaniza.model.equipamento;

/**
 * Unidade de saúde — capacidade instalada = número de equipes instaladas.
 * RN12: a demanda é calculada sobre a população total.
 */
public class UnidadeSaude extends EquipamentoComunitario {

    private int habitantesPorEquipe;
    private String tipoUnidade;

    public UnidadeSaude() {
    }

    public UnidadeSaude(Long id, Long regiaoId, String nome, int equipes, int habitantesPorEquipe, String tipoUnidade) {
        super(id, regiaoId, nome, equipes);
        setHabitantesPorEquipe(habitantesPorEquipe);
        this.tipoUnidade = tipoUnidade;
    }

    /** RN12 — demanda (equipes necessárias) = teto(população / habitantes por equipe). */
    @Override
    public int calcularDemanda(int populacao) {
        if (habitantesPorEquipe <= 0) {
            throw new IllegalStateException("Habitantes por equipe não informado.");
        }
        return (int) Math.ceil((double) populacao / habitantesPorEquipe);
    }

    @Override
    public String getTipo() {
        return TIPO_UNIDADE_SAUDE;
    }

    public int getHabitantesPorEquipe() {
        return habitantesPorEquipe;
    }

    public final void setHabitantesPorEquipe(int habitantesPorEquipe) {
        if (habitantesPorEquipe <= 0) {
            throw new IllegalArgumentException("Habitantes por equipe deve ser maior que zero.");
        }
        this.habitantesPorEquipe = habitantesPorEquipe;
    }

    public String getTipoUnidade() {
        return tipoUnidade;
    }

    public final void setTipoUnidade(String tipoUnidade) {
        this.tipoUnidade = tipoUnidade;
    }

    @Override
    public String toString() {
        return "UnidadeSaude[id=" + getId() + ", regiaoId=" + getRegiaoId() + ", nome=" + getNome()
                + ", equipes=" + getCapacidadeInstalada() + ", habitantesPorEquipe=" + habitantesPorEquipe
                + ", tipoUnidade=" + tipoUnidade + "]";
    }
}
