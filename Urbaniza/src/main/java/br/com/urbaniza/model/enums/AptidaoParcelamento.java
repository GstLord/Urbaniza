package br.com.urbaniza.model.enums;

/**
 * Resultado da análise de aptidão de uma região ao parcelamento (RF01 / RN15).
 */
public enum AptidaoParcelamento {
    APTA("Apta"),
    APTA_COM_RESTRICAO("Apta com restrição"),
    INAPTA("Inapta");

    private final String descricao;

    AptidaoParcelamento(String descricao) {
        this.descricao = descricao;
    }

    /** @return texto com acentuação para exibição ao usuário */
    public String getDescricao() {
        return descricao;
    }
}
