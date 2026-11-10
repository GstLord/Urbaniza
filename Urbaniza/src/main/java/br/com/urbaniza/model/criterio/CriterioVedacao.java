package br.com.urbaniza.model.criterio;

import br.com.urbaniza.model.Regiao;

import java.util.Locale;

/**
 * Critério de vedação legal ao parcelamento (Lei 6.766/79, art. 3º, parágrafo único) — RF01.
 * Classe ABSTRATA: cada subclasse avalia a região de forma própria (polimorfismo — RN01 a RN04)
 * e informa se o seu impedimento é absoluto (E3 — operacionaliza a RN15).
 */
public abstract class CriterioVedacao {

    public static final String TIPO_ALAGAMENTO = "ALAGAMENTO";
    public static final String TIPO_DECLIVIDADE = "DECLIVIDADE";
    public static final String TIPO_GEOLOGICO = "GEOLOGICO";
    public static final String TIPO_PRESERVACAO = "PRESERVACAO";

    /** Locale usado nas mensagens exibidas ao usuário (vírgula decimal). */
    protected static final Locale LOCALE_BR = Locale.of("pt", "BR");

    private Long id;
    /** [EXT E2] região à qual o critério se aplica. */
    private Long regiaoId;
    private String codigoInterno;
    private String fundamentoLegal;

    protected CriterioVedacao() {
    }

    protected CriterioVedacao(Long id, Long regiaoId, String codigoInterno, String fundamentoLegal) {
        this.id = id;
        this.regiaoId = regiaoId;
        this.codigoInterno = codigoInterno;
        this.fundamentoLegal = fundamentoLegal;
    }

    /**
     * RN01–RN04 — avalia a região frente ao critério.
     *
     * @return TRUE se a região atende ao critério (não há impedimento)
     */
    public abstract boolean avaliar(Regiao regiao);

    /**
     * E3 / RN15 — indica se a reprovação neste critério torna a região INAPTA
     * (true) ou apenas APTA_COM_RESTRICAO (false, impedimento superável).
     */
    public abstract boolean isImpedimentoAbsoluto();

    /** @return "ALAGAMENTO", "DECLIVIDADE", "GEOLOGICO" ou "PRESERVACAO" (coluna discriminadora "tipo") */
    public abstract String getTipo();

    /**
     * Texto padrão do impedimento. As subclasses sobrescrevem com mensagem específica.
     */
    public String descreverImpedimento() {
        return "Impedimento legal identificado pelo critério " + codigoInterno + " (" + fundamentoLegal + ")";
    }

    /**
     * Descrição do impedimento considerando os dados medidos da região avaliada.
     * Por padrão delega para {@link #descreverImpedimento()}; subclasses que dependem
     * de um valor da região (ex.: declividade) sobrescrevem.
     */
    public String descreverImpedimento(Regiao regiao) {
        return descreverImpedimento();
    }

    /** Formata um número com 2 casas decimais no padrão brasileiro. */
    protected static String formatarDecimal(double valor) {
        return String.format(LOCALE_BR, "%,.2f", valor);
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

    public String getCodigoInterno() {
        return codigoInterno;
    }

    public final void setCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
    }

    public String getFundamentoLegal() {
        return fundamentoLegal;
    }

    public final void setFundamentoLegal(String fundamentoLegal) {
        this.fundamentoLegal = fundamentoLegal;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + ", regiaoId=" + regiaoId + ", codigoInterno="
                + codigoInterno + ", fundamentoLegal=" + fundamentoLegal + "]";
    }
}
