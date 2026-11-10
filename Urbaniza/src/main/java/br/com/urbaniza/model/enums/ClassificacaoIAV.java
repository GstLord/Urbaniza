package br.com.urbaniza.model.enums;

/**
 * Classificação do Índice de Área Verde por Habitante (RF05 / RN08).
 */
public enum ClassificacaoIAV {
    ADEQUADO("Adequado"),
    ACEITAVEL("Aceitável"),
    DEFICITARIO("Deficitário");

    private final String descricao;

    ClassificacaoIAV(String descricao) {
        this.descricao = descricao;
    }

    /** @return texto com acentuação para exibição ao usuário */
    public String getDescricao() {
        return descricao;
    }
}
