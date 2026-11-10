package br.com.urbaniza.model.equipamento;

/**
 * Equipamento comunitário (escola ou unidade de saúde) — RF03.
 * Classe ABSTRATA: cada subclasse define, por polimorfismo, como calcular a sua demanda (RN12).
 * Agregação com a região: o equipamento pode existir sem região (regiaoId nulo).
 */
public abstract class EquipamentoComunitario {

    public static final String TIPO_ESCOLA = "ESCOLA";
    public static final String TIPO_UNIDADE_SAUDE = "UNIDADE_SAUDE";

    private Long id;
    private Long regiaoId;
    private String nome;
    private int capacidadeInstalada;

    protected EquipamentoComunitario() {
    }

    protected EquipamentoComunitario(Long id, Long regiaoId, String nome, int capacidadeInstalada) {
        this.id = id;
        this.regiaoId = regiaoId;
        this.nome = nome;
        setCapacidadeInstalada(capacidadeInstalada);
    }

    /**
     * RN12 — demanda do equipamento frente à população informada (implementada pelas subclasses).
     *
     * @param populacao população considerada (a projetada, conforme RN14)
     * @return demanda na mesma unidade de {@link #getCapacidadeInstalada()}
     */
    public abstract int calcularDemanda(int populacao);

    /** @return "ESCOLA" ou "UNIDADE_SAUDE" (coluna discriminadora "tipo") */
    public abstract String getTipo();

    /**
     * RF03 / RN11 — índice de atendimento = capacidade instalada / demanda.
     * Se a demanda for zero, retorna {@link Double#POSITIVE_INFINITY}.
     */
    public double calcularIndiceAtendimento(int populacao) {
        int demanda = calcularDemanda(populacao);
        if (demanda == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (double) capacidadeInstalada / demanda;
    }

    /** RN11 — há déficit quando o índice de atendimento é menor que 1. */
    public boolean possuiDeficit(int populacao) {
        return calcularIndiceAtendimento(populacao) < 1.0;
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

    public String getNome() {
        return nome;
    }

    public final void setNome(String nome) {
        this.nome = nome;
    }

    public int getCapacidadeInstalada() {
        return capacidadeInstalada;
    }

    public final void setCapacidadeInstalada(int capacidadeInstalada) {
        if (capacidadeInstalada < 0) {
            throw new IllegalArgumentException("A capacidade instalada não pode ser negativa.");
        }
        this.capacidadeInstalada = capacidadeInstalada;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + ", regiaoId=" + regiaoId + ", nome=" + nome
                + ", capacidadeInstalada=" + capacidadeInstalada + "]";
    }
}
