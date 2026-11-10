package br.com.urbaniza.ui;

import br.com.urbaniza.model.AnaliseParcelamento;
import br.com.urbaniza.model.Analista;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.model.ResultadoCriterio;
import br.com.urbaniza.service.AnaliseService;
import br.com.urbaniza.service.AnalistaService;
import br.com.urbaniza.service.RegiaoService;

import java.util.List;

/**
 * Emissão e consulta das análises de parcelamento (RF01, RF07). A análise emitida é imutável (RN16):
 * não há opção de atualizar.
 */
public class MenuAnalise extends Menu {

    private final AnaliseService analiseService;
    private final RegiaoService regiaoService;
    private final AnalistaService analistaService;

    public MenuAnalise(Entrada entrada) {
        this(entrada, new AnaliseService(), new RegiaoService(), new AnalistaService());
    }

    public MenuAnalise(Entrada entrada, AnaliseService analiseService, RegiaoService regiaoService,
                       AnalistaService analistaService) {
        super(entrada);
        this.analiseService = analiseService;
        this.regiaoService = regiaoService;
        this.analistaService = analistaService;
    }

    @Override
    protected String titulo() {
        return "ANÁLISES DE PARCELAMENTO";
    }

    @Override
    protected List<OpcaoMenu> opcoes() {
        return List.of(
                new OpcaoMenu("Emitir análise", this::emitir),
                new OpcaoMenu("Listar", this::listar),
                new OpcaoMenu("Detalhar", this::detalhar),
                new OpcaoMenu("Listar por região", this::listarPorRegiao),
                new OpcaoMenu("Excluir", this::excluir));
    }

    private void emitir() {
        Regiao regiao = escolherRegiao(regiaoService);
        Analista analista = escolherAnalista(analistaService);
        AnaliseParcelamento analise = analiseService.emitir(regiao.getId(), analista.getId());
        System.out.println("Análise emitida e gravada com id " + analise.getId() + ".");
        System.out.println();
        System.out.println(analise.getParecerTextual());
    }

    private void listar() {
        imprimir(analiseService.listar());
    }

    private void listarPorRegiao() {
        Regiao regiao = escolherRegiao(regiaoService);
        imprimir(analiseService.listarPorRegiao(regiao.getId()));
    }

    private void detalhar() {
        AnaliseParcelamento a = analiseService.buscar(entrada.lerId("Id da análise"));
        System.out.println("Análise " + a.getId() + " — região " + a.getRegiao().getNome()
                + " — " + a.getResultado().getDescricao() + " (" + a.getResultado().name() + ")");
        System.out.println("IAV apurado: " + (a.getIndiceAreaVerdeApurado() == null ? "não aplicável"
                : Formatador.decimal(a.getIndiceAreaVerdeApurado()) + " m²/hab"));
        System.out.println();
        System.out.println(a.getParecerTextual());
        System.out.println();
        System.out.println("Resultados por critério (registrados em analise_criterio):");
        for (ResultadoCriterio rc : a.getResultados()) {
            System.out.println("  " + rc.criterio().getCodigoInterno() + " [" + rc.criterio().getTipo() + "]: "
                    + (rc.atendido() ? "atendido" : "NÃO atendido — " + rc.descricaoImpedimento()));
        }
    }

    private void excluir() {
        AnaliseParcelamento a = analiseService.buscar(entrada.lerId("Id da análise"));
        System.out.println("Atenção: a análise é o registro de rastreabilidade (RN16) do parecer emitido.");
        if (confirmarExclusao("análise " + a.getId() + " (" + a.getRegiao().getNome() + ", "
                + Formatador.dataHora(a.getDataEmissao()) + ")")) {
            analiseService.excluir(a.getId());
            System.out.println("Análise excluída.");
        }
    }

    private static void imprimir(List<AnaliseParcelamento> analises) {
        if (analises.isEmpty()) {
            System.out.println("Nenhuma análise emitida.");
            return;
        }
        for (AnaliseParcelamento a : analises) {
            System.out.println(String.format("%4d | %s | %-35s | %-18s | %-20s | %s",
                    a.getId(), Formatador.dataHora(a.getDataEmissao()), a.getRegiao().getNome(),
                    a.getAnalista().getNome(), a.getResultado().name(), a.getVersaoDados()));
        }
        System.out.println("Total: " + analises.size());
    }
}
