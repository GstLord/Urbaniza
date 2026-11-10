package br.com.urbaniza.model;

import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.criterio.CriterioVedacao;
import br.com.urbaniza.model.enums.AptidaoParcelamento;
import br.com.urbaniza.model.enums.ClassificacaoIAV;
import br.com.urbaniza.model.enums.PotencialIlhaCalor;
import br.com.urbaniza.model.equipamento.EquipamentoComunitario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * Análise de aptidão de uma região ao parcelamento (RF01, RF07).
 * Toda análise emitida registra data, responsável e versão dos dados (RN16) e é imutável depois de gravada.
 */
public class AnaliseParcelamento {

    /** Frase obrigatória presente em todo parecer. */
    public static final String AVISO_INDICADORES = "Indicadores estimados a partir dos dados cadastrados; "
            + "não substituem o parecer técnico nem constituem previsão certificada.";

    public static final String MENSAGEM_SEM_CRITERIOS =
            "Cadastre ao menos um critério de vedação para a região antes de emitir a análise";

    private static final String PREFIXO_VERSAO = "v-";
    private static final int TAMANHO_HASH_VERSAO = 12;
    private static final Locale LOCALE_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String LINHA = "------------------------------------------------------------";

    private Long id;
    private Regiao regiao;
    private Analista analista;
    private LocalDateTime dataEmissao;
    private String versaoDados;
    private AptidaoParcelamento resultado;
    private Double indiceAreaVerdeApurado;
    private String parecerTextual;
    private List<CriterioVedacao> criterios = new ArrayList<>();
    private List<ResultadoCriterio> resultados = new ArrayList<>();

    public AnaliseParcelamento() {
    }

    public AnaliseParcelamento(Regiao regiao, Analista analista, List<CriterioVedacao> criterios) {
        this.regiao = regiao;
        this.analista = analista;
        setCriterios(criterios);
    }

    public AnaliseParcelamento(Long id, Regiao regiao, Analista analista, LocalDateTime dataEmissao,
                               String versaoDados, AptidaoParcelamento resultado, Double indiceAreaVerdeApurado,
                               String parecerTextual, List<CriterioVedacao> criterios,
                               List<ResultadoCriterio> resultados) {
        this.id = id;
        this.regiao = regiao;
        this.analista = analista;
        this.dataEmissao = dataEmissao;
        this.versaoDados = versaoDados;
        this.resultado = resultado;
        this.indiceAreaVerdeApurado = indiceAreaVerdeApurado;
        this.parecerTextual = parecerTextual;
        setCriterios(criterios);
        setResultados(resultados);
    }

    /**
     * RF01 / RF07 / RN14 / RN15 / RN16 — executa a análise: registra data, versão dos dados,
     * resultado de aptidão, IAV apurado e o parecer textual completo.
     *
     * @throws RegraNegocioException se faltar região, analista ou critério
     */
    public void executar() {
        if (regiao == null) {
            throw new RegraNegocioException("Informe a região a ser analisada.");
        }
        if (analista == null) {
            throw new RegraNegocioException("Informe o analista responsável pela análise (RN16).");
        }
        if (criterios == null || criterios.isEmpty()) {
            throw new RegraNegocioException(MENSAGEM_SEM_CRITERIOS);
        }
        dataEmissao = LocalDateTime.now().withNano(0);
        versaoDados = calcularVersaoDados();
        resultado = avaliarAptidao();
        double iav = regiao.calcularIAV();
        indiceAreaVerdeApurado = Double.isInfinite(iav) ? null : iav;
        parecerTextual = montarParecer();
    }

    /**
     * RN15 — avalia POLIMORFICAMENTE cada critério de vedação (criterio.avaliar(regiao)) e
     * preenche os resultados. Reprovação em critério de impedimento absoluto =&gt; INAPTA;
     * reprovação apenas em critérios superáveis =&gt; APTA_COM_RESTRICAO; nenhuma =&gt; APTA.
     */
    public AptidaoParcelamento avaliarAptidao() {
        if (regiao == null) {
            throw new RegraNegocioException("Informe a região a ser analisada.");
        }
        resultados = new ArrayList<>();
        boolean reprovadoAbsoluto = false;
        boolean reprovadoSuperavel = false;
        for (CriterioVedacao criterio : criterios) {
            boolean atendido = criterio.avaliar(regiao);
            String descricao = atendido ? null : criterio.descreverImpedimento(regiao);
            resultados.add(new ResultadoCriterio(criterio, atendido, descricao));
            if (!atendido) {
                if (criterio.isImpedimentoAbsoluto()) {
                    reprovadoAbsoluto = true;
                } else {
                    reprovadoSuperavel = true;
                }
            }
        }
        if (reprovadoAbsoluto) {
            return AptidaoParcelamento.INAPTA;
        }
        if (reprovadoSuperavel) {
            return AptidaoParcelamento.APTA_COM_RESTRICAO;
        }
        return AptidaoParcelamento.APTA;
    }

    /**
     * RN11 / RN14 / RN08 / RN10 — lista os déficits da região calculados sobre a população PROJETADA:
     * equipamentos com índice de atendimento &lt; 1, IAV deficitário e potencial de ilha de calor alto.
     */
    public List<String> listarDeficits() {
        List<String> deficits = new ArrayList<>();
        if (regiao == null) {
            return deficits;
        }
        int populacaoProjetada = regiao.calcularPopulacaoProjetada();
        for (EquipamentoComunitario equipamento : ordenarPorId(regiao.getEquipamentos(), EquipamentoComunitario::getId)) {
            if (equipamento.possuiDeficit(populacaoProjetada)) {
                deficits.add(equipamento.getTipo() + " " + equipamento.getNome() + ": índice "
                        + formatarDecimal(equipamento.calcularIndiceAtendimento(populacaoProjetada)) + " — déficit");
            }
        }
        if (regiao.classificarIAV() == ClassificacaoIAV.DEFICITARIO) {
            deficits.add("Índice de Área Verde: " + formatarDecimal(regiao.calcularIAV()) + " m²/hab — "
                    + ClassificacaoIAV.DEFICITARIO.getDescricao());
        }
        if (regiao.classificarIlhaDeCalor() == PotencialIlhaCalor.ALTO) {
            deficits.add("Potencial de ilha de calor " + PotencialIlhaCalor.ALTO.getDescricao()
                    + " (cobertura vegetal de " + formatarDecimal(regiao.calcularCoberturaVegetal() * 100) + "%)");
        }
        return deficits;
    }

    /**
     * RF07 / RN16 — versão dos dados: "v-" + 12 primeiros caracteres hexadecimais do SHA-256 de um
     * texto canônico (Locale.ROOT) com os dados da região, loteamentos e lotes, equipamentos e
     * critérios, todos ordenados por id. Mesmos dados =&gt; mesma versão.
     */
    String calcularVersaoDados() {
        String canonico = montarTextoCanonico();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonico.getBytes(StandardCharsets.UTF_8));
            return PREFIXO_VERSAO + HexFormat.of().formatHex(hash).substring(0, TAMANHO_HASH_VERSAO);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponível na JVM.", e);
        }
    }

    private String montarTextoCanonico() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "REGIAO|%s|%s|%.6f|%.6f|%.6f|%d|%.6f%n",
                regiao.getId(), regiao.getNome(), regiao.getAreaTotalM2(), regiao.getAreaVerdeM2(),
                regiao.getAreaImpermeavelM2(), regiao.getPopulacaoAtual(), regiao.getDeclividadeMediaPct()));
        for (Loteamento loteamento : ordenarPorId(regiao.getLoteamentos(), Loteamento::getId)) {
            sb.append(String.format(Locale.ROOT, "LOTEAMENTO|%s|%s|%s|%.6f|%d|%b%n",
                    loteamento.getId(), loteamento.getDenominacao(), loteamento.getDataAprovacao(),
                    loteamento.getAreaGlebaM2(), loteamento.getPopulacaoEstimada(), loteamento.isOcupado()));
            for (Lote lote : ordenarPorId(loteamento.getLotes(), Lote::getId)) {
                sb.append(String.format(Locale.ROOT, "LOTE|%s|%s|%s|%.6f|%.6f%n",
                        lote.getId(), lote.getQuadra(), lote.getNumero(), lote.getAreaM2(), lote.getFrenteM()));
            }
        }
        for (EquipamentoComunitario equipamento : ordenarPorId(regiao.getEquipamentos(), EquipamentoComunitario::getId)) {
            sb.append("EQUIPAMENTO|").append(equipamento.getTipo()).append('|').append(equipamento).append('\n');
        }
        for (CriterioVedacao criterio : ordenarPorId(criterios, CriterioVedacao::getId)) {
            sb.append("CRITERIO|").append(criterio.getTipo()).append('|').append(criterio).append('\n');
        }
        return sb.toString();
    }

    /** Ordena por id (nulos por último), mantendo a ordem de inserção entre iguais. */
    private static <T> List<T> ordenarPorId(List<T> itens, Function<T, Long> extratorId) {
        List<T> copia = new ArrayList<>(itens);
        copia.sort(Comparator.comparing(extratorId, Comparator.nullsLast(Comparator.naturalOrder())));
        return copia;
    }

    private String montarParecer() {
        int populacaoProjetada = regiao.calcularPopulacaoProjetada();
        double iav = regiao.calcularIAV();
        StringBuilder sb = new StringBuilder();
        sb.append("PARECER DE APTIDÃO AO PARCELAMENTO DO SOLO (Lei 6.766/1979)\n");
        sb.append(LINHA).append('\n');
        sb.append("Região: ").append(regiao.getNome()).append(" (id ").append(regiao.getId()).append(")\n");
        sb.append("Analista responsável: ").append(analista.getNome())
                .append(" — matrícula ").append(analista.getMatricula())
                .append(" — ").append(analista.getSetor()).append('\n');
        sb.append("Data de emissão: ").append(dataEmissao.format(FORMATO_DATA_HORA)).append('\n');
        sb.append("Versão dos dados: ").append(versaoDados).append('\n');
        sb.append("Resultado: ").append(resultado.getDescricao()).append(" (").append(resultado.name()).append(")\n");
        sb.append(LINHA).append('\n');
        sb.append("Critérios de vedação avaliados:\n");
        for (ResultadoCriterio rc : resultados) {
            CriterioVedacao c = rc.criterio();
            sb.append("  - ").append(c.getCodigoInterno()).append(" [").append(c.getTipo()).append("] ");
            if (rc.atendido()) {
                sb.append("ATENDIDO\n");
            } else {
                sb.append("IMPEDIMENTO ").append(c.isImpedimentoAbsoluto() ? "ABSOLUTO" : "SUPERÁVEL")
                        .append(": ").append(rc.descricaoImpedimento()).append('\n');
            }
        }
        sb.append(LINHA).append('\n');
        sb.append("Indicadores urbanísticos (calculados sobre a população projetada — RN14):\n");
        sb.append("  População atual: ").append(formatarInteiro(regiao.getPopulacaoAtual())).append(" hab\n");
        sb.append("  População projetada: ").append(formatarInteiro(populacaoProjetada)).append(" hab\n");
        sb.append("  Índice de Área Verde (IAV): ");
        if (Double.isInfinite(iav)) {
            sb.append("não aplicável (população projetada igual a zero)");
        } else {
            sb.append(formatarDecimal(iav)).append(" m²/hab");
        }
        sb.append(" — ").append(regiao.classificarIAV().getDescricao()).append('\n');
        sb.append("  Taxa de impermeabilização: ")
                .append(formatarDecimal(regiao.calcularTaxaImpermeabilizacao() * 100)).append("%\n");
        sb.append("  Potencial de ilha de calor: ").append(regiao.classificarIlhaDeCalor().getDescricao())
                .append(" (cobertura vegetal de ").append(formatarDecimal(regiao.calcularCoberturaVegetal() * 100))
                .append("%)\n");
        sb.append("Déficits identificados:\n");
        List<String> deficits = listarDeficits();
        if (deficits.isEmpty()) {
            sb.append("  Nenhum déficit identificado.\n");
        } else {
            for (String deficit : deficits) {
                sb.append("  - ").append(deficit).append('\n');
            }
        }
        sb.append(LINHA).append('\n');
        sb.append(AVISO_INDICADORES);
        return sb.toString();
    }

    private static String formatarDecimal(double valor) {
        return String.format(LOCALE_BR, "%,.2f", valor);
    }

    private static String formatarInteiro(int valor) {
        return String.format(LOCALE_BR, "%,d", valor);
    }

    public Long getId() {
        return id;
    }

    public final void setId(Long id) {
        this.id = id;
    }

    public Regiao getRegiao() {
        return regiao;
    }

    public final void setRegiao(Regiao regiao) {
        this.regiao = regiao;
    }

    public Analista getAnalista() {
        return analista;
    }

    public final void setAnalista(Analista analista) {
        this.analista = analista;
    }

    public LocalDateTime getDataEmissao() {
        return dataEmissao;
    }

    public final void setDataEmissao(LocalDateTime dataEmissao) {
        this.dataEmissao = dataEmissao;
    }

    public String getVersaoDados() {
        return versaoDados;
    }

    public final void setVersaoDados(String versaoDados) {
        this.versaoDados = versaoDados;
    }

    public AptidaoParcelamento getResultado() {
        return resultado;
    }

    public final void setResultado(AptidaoParcelamento resultado) {
        this.resultado = resultado;
    }

    public Double getIndiceAreaVerdeApurado() {
        return indiceAreaVerdeApurado;
    }

    public final void setIndiceAreaVerdeApurado(Double indiceAreaVerdeApurado) {
        this.indiceAreaVerdeApurado = indiceAreaVerdeApurado;
    }

    public String getParecerTextual() {
        return parecerTextual;
    }

    public final void setParecerTextual(String parecerTextual) {
        this.parecerTextual = parecerTextual;
    }

    public List<CriterioVedacao> getCriterios() {
        return criterios;
    }

    public final void setCriterios(List<CriterioVedacao> criterios) {
        this.criterios = criterios == null ? new ArrayList<>() : new ArrayList<>(criterios);
    }

    public List<ResultadoCriterio> getResultados() {
        return resultados;
    }

    public final void setResultados(List<ResultadoCriterio> resultados) {
        this.resultados = resultados == null ? new ArrayList<>() : new ArrayList<>(resultados);
    }

    @Override
    public String toString() {
        return "AnaliseParcelamento[id=" + id + ", regiao=" + (regiao == null ? null : regiao.getNome())
                + ", analista=" + (analista == null ? null : analista.getNome()) + ", dataEmissao=" + dataEmissao
                + ", versaoDados=" + versaoDados + ", resultado=" + resultado
                + ", indiceAreaVerdeApurado=" + indiceAreaVerdeApurado + ", criterios=" + criterios.size() + "]";
    }
}
