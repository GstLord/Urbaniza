package br.com.urbaniza.ui;

import br.com.urbaniza.model.Lote;
import br.com.urbaniza.model.Loteamento;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.model.equipamento.EquipamentoComunitario;
import br.com.urbaniza.service.EquipamentoService;
import br.com.urbaniza.service.IndicadoresRegiao;
import br.com.urbaniza.service.RegiaoService;
import br.com.urbaniza.service.SituacaoAtendimento;

import java.util.List;

/**
 * CRUD da entidade principal (Região) + indicadores urbanísticos (RF01, RF03–RF05).
 */
public class MenuRegiao extends MenuCrud<Regiao> {

    private final RegiaoService regiaoService;
    private final EquipamentoService equipamentoService;

    public MenuRegiao(Entrada entrada) {
        this(entrada, new RegiaoService(), new EquipamentoService());
    }

    public MenuRegiao(Entrada entrada, RegiaoService regiaoService, EquipamentoService equipamentoService) {
        super(entrada);
        this.regiaoService = regiaoService;
        this.equipamentoService = equipamentoService;
    }

    @Override
    protected String titulo() {
        return "REGIÕES";
    }

    @Override
    protected List<OpcaoMenu> opcoesExtras() {
        return List.of(new OpcaoMenu("Indicadores da região", this::mostrarIndicadores));
    }

    @Override
    protected String resumir(Regiao r) {
        return String.format("%4d | %-40s | pop. atual %10s | área total %16s m² | declividade %s",
                r.getId(), r.getNome(), Formatador.inteiro(r.getPopulacaoAtual()),
                Formatador.decimal(r.getAreaTotalM2()), Formatador.percentualDireto(r.getDeclividadeMediaPct()));
    }

    @Override
    protected void listar() {
        imprimirLista(regiaoService.listar(), "Nenhuma região cadastrada.");
    }

    @Override
    protected void detalhar() {
        Regiao r = regiaoService.carregarCompleta(entrada.lerId("Id da região"));
        System.out.println("Id: " + r.getId());
        System.out.println("Nome: " + r.getNome());
        System.out.println("Área total: " + Formatador.decimal(r.getAreaTotalM2()) + " m²");
        System.out.println("Área verde: " + Formatador.decimal(r.getAreaVerdeM2()) + " m²");
        System.out.println("Área impermeável: " + Formatador.decimal(r.getAreaImpermeavelM2()) + " m²");
        System.out.println("População atual: " + Formatador.inteiro(r.getPopulacaoAtual()) + " hab");
        System.out.println("Declividade média: " + Formatador.percentualDireto(r.getDeclividadeMediaPct()));
        System.out.println("Loteamentos (" + r.getLoteamentos().size() + "):");
        for (Loteamento l : r.getLoteamentos()) {
            System.out.println("  " + l.getId() + " - " + l.getDenominacao()
                    + " | aprovação: " + (l.isAprovado() ? Formatador.data(l.getDataAprovacao()) : "não aprovado")
                    + " | ocupado: " + Formatador.simNao(l.isOcupado())
                    + " | pop. estimada: " + Formatador.inteiro(l.getPopulacaoEstimada())
                    + " | entra na projeção: " + Formatador.simNao(l.contribuiParaProjecao()));
            for (Lote lote : l.getLotes()) {
                System.out.println("      lote " + lote.getQuadra() + "-" + lote.getNumero() + ": "
                        + Formatador.decimal(lote.getAreaM2()) + " m², frente " + Formatador.decimal(lote.getFrenteM()) + " m");
            }
        }
        System.out.println("Equipamentos comunitários (" + r.getEquipamentos().size() + "):");
        for (EquipamentoComunitario e : r.getEquipamentos()) {
            System.out.println("  " + e.getId() + " - " + e.getTipo() + " " + e.getNome()
                    + " | capacidade instalada: " + Formatador.inteiro(e.getCapacidadeInstalada()));
        }
    }

    @Override
    protected void cadastrar() {
        Regiao r = new Regiao();
        r.setNome(entrada.lerTexto("Nome da região"));
        r.setAreaTotalM2(lerAreaTotal());
        r.setAreaVerdeM2(entrada.lerDouble("Área verde (m²)", 0));
        r.setAreaImpermeavelM2(entrada.lerDouble("Área impermeável (m²)", 0));
        r.setPopulacaoAtual(entrada.lerInteiro("População atual (hab)", 0, Integer.MAX_VALUE));
        r.setDeclividadeMediaPct(entrada.lerDouble("Declividade média (%)", 0));
        regiaoService.criar(r);
        System.out.println("Região cadastrada com id " + r.getId() + ".");
    }

    @Override
    protected void atualizar() {
        Regiao r = regiaoService.buscar(entrada.lerId("Id da região"));
        System.out.println("Pressione ENTER para manter o valor atual.");
        r.setNome(entrada.lerTexto("Nome da região", r.getNome()));
        r.setAreaTotalM2(entrada.lerDouble("Área total (m²)", 0.01, r.getAreaTotalM2()));
        r.setAreaVerdeM2(entrada.lerDouble("Área verde (m²)", 0, r.getAreaVerdeM2()));
        r.setAreaImpermeavelM2(entrada.lerDouble("Área impermeável (m²)", 0, r.getAreaImpermeavelM2()));
        r.setPopulacaoAtual(entrada.lerInteiro("População atual (hab)", 0, Integer.MAX_VALUE, r.getPopulacaoAtual()));
        r.setDeclividadeMediaPct(entrada.lerDouble("Declividade média (%)", 0, r.getDeclividadeMediaPct()));
        regiaoService.atualizar(r);
        System.out.println("Região atualizada.");
    }

    @Override
    protected void excluir() {
        Regiao r = regiaoService.buscar(entrada.lerId("Id da região"));
        System.out.println("Atenção: loteamentos, lotes e critérios da região também serão excluídos;"
                + " os equipamentos ficarão sem região.");
        if (confirmarExclusao("\"" + r.getNome() + "\"")) {
            regiaoService.excluir(r.getId());
            System.out.println("Região excluída.");
        }
    }

    /** RF03 / RF04 / RF05 — indicadores calculados sobre a população projetada (RN14). */
    private void mostrarIndicadores() {
        long id = escolherRegiao(regiaoService).getId();
        Regiao r = regiaoService.carregarCompleta(id);
        IndicadoresRegiao ind = RegiaoService.calcularIndicadores(r);
        System.out.println("Indicadores — " + r.getNome());
        System.out.println("  População atual: " + Formatador.inteiro(ind.populacaoAtual()) + " hab");
        System.out.println("  População projetada (RN13): " + Formatador.inteiro(ind.populacaoProjetada()) + " hab");
        System.out.println("  IAV (RN08): " + Formatador.iav(ind.iav()) + " — " + ind.classificacaoIav().getDescricao());
        System.out.println("  Taxa de impermeabilização (RN09): " + Formatador.percentual(ind.taxaImpermeabilizacao()));
        System.out.println("  Potencial de ilha de calor (RN10): " + ind.potencialIlhaCalor().getDescricao()
                + " (cobertura vegetal " + Formatador.percentual(r.calcularCoberturaVegetal()) + ")");
        System.out.println("  Atendimento dos equipamentos (RN11/RN12, população projetada):");
        List<SituacaoAtendimento> situacoes = equipamentoService.calcularAtendimentoDaRegiao(id);
        if (situacoes.isEmpty()) {
            System.out.println("    Nenhum equipamento associado à região.");
        }
        for (SituacaoAtendimento s : situacoes) {
            System.out.println("    " + s.equipamento().getTipo() + " " + s.equipamento().getNome()
                    + ": capacidade " + Formatador.inteiro(s.equipamento().getCapacidadeInstalada())
                    + ", demanda " + Formatador.inteiro(s.demanda())
                    + ", índice " + Formatador.indiceAtendimento(s.indice())
                    + (s.deficit() ? " — DÉFICIT" : " — sem déficit"));
        }
    }

    private double lerAreaTotal() {
        while (true) {
            double area = entrada.lerDouble("Área total (m²)", 0);
            if (area > 0) {
                return area;
            }
            System.out.println("  A área total deve ser maior que zero.");
        }
    }
}
