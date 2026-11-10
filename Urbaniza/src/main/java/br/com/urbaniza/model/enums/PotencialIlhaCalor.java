package br.com.urbaniza.model.enums;

/**
 * Potencial qualitativo de formação de ilha de calor (RN10).
 */
public enum PotencialIlhaCalor {
    BAIXO("Baixo"),
    MEDIO("Médio"),
    ALTO("Alto");

    private final String descricao;

    PotencialIlhaCalor(String descricao) {
        this.descricao = descricao;
    }

    /** @return texto com acentuação para exibição ao usuário */
    public String getDescricao() {
        return descricao;
    }
}
