package br.com.urbaniza.model;

import br.com.urbaniza.model.enums.ClassificacaoIAV;
import br.com.urbaniza.model.enums.PotencialIlhaCalor;
import br.com.urbaniza.model.equipamento.EquipamentoComunitario;

import java.util.ArrayList;
import java.util.List;

/**
 * Região do município analisada quanto à aptidão ao parcelamento (RF01).
 * Entidade principal do sistema.
 * <ul>
 *   <li>Composição 1:N com {@link Loteamento} (o loteamento não existe sem a região);</li>
 *   <li>Agregação com {@link EquipamentoComunitario} (o equipamento sobrevive sem a região).</li>
 * </ul>
 */
public class Regiao {

    private Long id;
    private String nome;
    private double areaTotalM2;
    private double areaVerdeM2;
    private double areaImpermeavelM2;
    private int populacaoAtual;
    /** [EXT E1] declividade média medida na região, em %. */
    private double declividadeMediaPct;
    private List<Loteamento> loteamentos = new ArrayList<>();
    private List<EquipamentoComunitario> equipamentos = new ArrayList<>();

    public Regiao() {
    }

    public Regiao(Long id, String nome, double areaTotalM2, double areaVerdeM2, double areaImpermeavelM2,
                  int populacaoAtual, double declividadeMediaPct) {
        this.id = id;
        this.nome = nome;
        setAreaTotalM2(areaTotalM2);
        setAreaVerdeM2(areaVerdeM2);
        setAreaImpermeavelM2(areaImpermeavelM2);
        setPopulacaoAtual(populacaoAtual);
        setDeclividadeMediaPct(declividadeMediaPct);
    }

    /**
     * RF04 / RN13 — população projetada = população atual + soma da população estimada
     * dos loteamentos aprovados e ainda não ocupados.
     */
    public int calcularPopulacaoProjetada() {
        int projetada = populacaoAtual;
        for (Loteamento loteamento : loteamentos) {
            if (loteamento.contribuiParaProjecao()) {
                projetada += loteamento.getPopulacaoEstimada();
            }
        }
        return projetada;
    }

    /**
     * RF05 / RN08 / RN14 — Índice de Área Verde por Habitante, calculado sobre a
     * população PROJETADA. Retorna {@link Double#POSITIVE_INFINITY} se ela for zero.
     */
    public double calcularIAV() {
        int populacaoProjetada = calcularPopulacaoProjetada();
        if (populacaoProjetada == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return areaVerdeM2 / populacaoProjetada;
    }

    /**
     * RF05 / RN08 — &gt; 12 ADEQUADO; &gt;= 9 e &lt;= 12 ACEITAVEL; &lt; 9 DEFICITARIO
     * (POSITIVE_INFINITY é ADEQUADO).
     */
    public ClassificacaoIAV classificarIAV() {
        double iav = calcularIAV();
        if (iav > ParametrosUrbanisticos.IAV_LIMITE_ADEQUADO) {
            return ClassificacaoIAV.ADEQUADO;
        }
        if (iav >= ParametrosUrbanisticos.IAV_LIMITE_MINIMO) {
            return ClassificacaoIAV.ACEITAVEL;
        }
        return ClassificacaoIAV.DEFICITARIO;
    }

    /** RN09 — taxa de impermeabilização = área impermeável / área total (0..1). */
    public double calcularTaxaImpermeabilizacao() {
        if (areaTotalM2 <= 0) {
            return 0.0;
        }
        return areaImpermeavelM2 / areaTotalM2;
    }

    /**
     * RN10 / E6 — potencial de ilha de calor pela cobertura vegetal (área verde / área total):
     * &gt;= 30% BAIXO; &gt;= 15% MEDIO; &lt; 15% ALTO.
     */
    public PotencialIlhaCalor classificarIlhaDeCalor() {
        double cobertura = calcularCoberturaVegetal();
        if (cobertura >= ParametrosUrbanisticos.COBERTURA_VEGETAL_RISCO_BAIXO) {
            return PotencialIlhaCalor.BAIXO;
        }
        if (cobertura >= ParametrosUrbanisticos.COBERTURA_VEGETAL_RISCO_MEDIO) {
            return PotencialIlhaCalor.MEDIO;
        }
        return PotencialIlhaCalor.ALTO;
    }

    /** RN10 — proporção de cobertura vegetal (área verde / área total, 0..1). */
    public double calcularCoberturaVegetal() {
        if (areaTotalM2 <= 0) {
            return 0.0;
        }
        return areaVerdeM2 / areaTotalM2;
    }

    /** Composição — adiciona o loteamento e o vincula a esta região (RN17). */
    public void adicionarLoteamento(Loteamento loteamento) {
        if (loteamento == null) {
            throw new IllegalArgumentException("O loteamento não pode ser nulo.");
        }
        loteamento.setRegiaoId(id);
        loteamentos.add(loteamento);
    }

    /** Agregação — associa um equipamento comunitário a esta região. */
    public void adicionarEquipamento(EquipamentoComunitario equipamento) {
        if (equipamento == null) {
            throw new IllegalArgumentException("O equipamento não pode ser nulo.");
        }
        equipamento.setRegiaoId(id);
        equipamentos.add(equipamento);
    }

    public Long getId() {
        return id;
    }

    public final void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public final void setNome(String nome) {
        this.nome = nome;
    }

    public double getAreaTotalM2() {
        return areaTotalM2;
    }

    public final void setAreaTotalM2(double areaTotalM2) {
        if (areaTotalM2 <= 0 || Double.isNaN(areaTotalM2)) {
            throw new IllegalArgumentException("A área total da região deve ser maior que zero.");
        }
        this.areaTotalM2 = areaTotalM2;
    }

    public double getAreaVerdeM2() {
        return areaVerdeM2;
    }

    public final void setAreaVerdeM2(double areaVerdeM2) {
        if (areaVerdeM2 < 0 || Double.isNaN(areaVerdeM2)) {
            throw new IllegalArgumentException("A área verde não pode ser negativa.");
        }
        this.areaVerdeM2 = areaVerdeM2;
    }

    public double getAreaImpermeavelM2() {
        return areaImpermeavelM2;
    }

    public final void setAreaImpermeavelM2(double areaImpermeavelM2) {
        if (areaImpermeavelM2 < 0 || Double.isNaN(areaImpermeavelM2)) {
            throw new IllegalArgumentException("A área impermeável não pode ser negativa.");
        }
        this.areaImpermeavelM2 = areaImpermeavelM2;
    }

    public int getPopulacaoAtual() {
        return populacaoAtual;
    }

    public final void setPopulacaoAtual(int populacaoAtual) {
        if (populacaoAtual < 0) {
            throw new IllegalArgumentException("A população atual não pode ser negativa.");
        }
        this.populacaoAtual = populacaoAtual;
    }

    public double getDeclividadeMediaPct() {
        return declividadeMediaPct;
    }

    public final void setDeclividadeMediaPct(double declividadeMediaPct) {
        if (declividadeMediaPct < 0 || Double.isNaN(declividadeMediaPct)) {
            throw new IllegalArgumentException("A declividade média não pode ser negativa.");
        }
        this.declividadeMediaPct = declividadeMediaPct;
    }

    public List<Loteamento> getLoteamentos() {
        return loteamentos;
    }

    public final void setLoteamentos(List<Loteamento> loteamentos) {
        this.loteamentos = loteamentos == null ? new ArrayList<>() : new ArrayList<>(loteamentos);
    }

    public List<EquipamentoComunitario> getEquipamentos() {
        return equipamentos;
    }

    public final void setEquipamentos(List<EquipamentoComunitario> equipamentos) {
        this.equipamentos = equipamentos == null ? new ArrayList<>() : new ArrayList<>(equipamentos);
    }

    @Override
    public String toString() {
        return "Regiao[id=" + id + ", nome=" + nome + ", areaTotalM2=" + areaTotalM2
                + ", areaVerdeM2=" + areaVerdeM2 + ", areaImpermeavelM2=" + areaImpermeavelM2
                + ", populacaoAtual=" + populacaoAtual + ", declividadeMediaPct=" + declividadeMediaPct
                + ", loteamentos=" + loteamentos.size() + ", equipamentos=" + equipamentos.size() + "]";
    }
}
