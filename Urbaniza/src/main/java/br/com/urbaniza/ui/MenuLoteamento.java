package br.com.urbaniza.ui;

import br.com.urbaniza.model.Lote;
import br.com.urbaniza.model.Loteamento;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.service.LoteamentoService;
import br.com.urbaniza.service.RegiaoService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRUD de loteamentos com seus lotes (RF02, RF06) + aprovar e marcar como ocupado (RN13).
 */
public class MenuLoteamento extends MenuCrud<Loteamento> {

    private static final int MAXIMO_LOTES_POR_CADASTRO = 500;

    private final LoteamentoService loteamentoService;
    private final RegiaoService regiaoService;

    public MenuLoteamento(Entrada entrada) {
        this(entrada, new LoteamentoService(), new RegiaoService());
    }

    public MenuLoteamento(Entrada entrada, LoteamentoService loteamentoService, RegiaoService regiaoService) {
        super(entrada);
        this.loteamentoService = loteamentoService;
        this.regiaoService = regiaoService;
    }

    @Override
    protected String titulo() {
        return "LOTEAMENTOS";
    }

    @Override
    protected List<OpcaoMenu> opcoesExtras() {
        return List.of(
                new OpcaoMenu("Aprovar loteamento", this::aprovar),
                new OpcaoMenu("Marcar como ocupado", this::marcarComoOcupado));
    }

    @Override
    protected String resumir(Loteamento l) {
        return String.format("%4d | região %3d | %-30s | aprovação %-12s | ocupado %-3s | pop. est. %8s | lotes %d",
                l.getId(), l.getRegiaoId(), l.getDenominacao(),
                l.isAprovado() ? Formatador.data(l.getDataAprovacao()) : "não aprovado",
                Formatador.simNao(l.isOcupado()), Formatador.inteiro(l.getPopulacaoEstimada()), l.getLotes().size());
    }

    @Override
    protected void listar() {
        imprimirLista(loteamentoService.listar(), "Nenhum loteamento cadastrado.");
    }

    @Override
    protected void detalhar() {
        Loteamento l = loteamentoService.buscar(entrada.lerId("Id do loteamento"));
        Map<Long, String> nomes = nomesDasRegioes();
        System.out.println("Id: " + l.getId());
        System.out.println("Região: " + l.getRegiaoId() + " - " + nomes.getOrDefault(l.getRegiaoId(), "?"));
        System.out.println("Denominação: " + l.getDenominacao());
        System.out.println("Data de aprovação: " + (l.isAprovado() ? Formatador.data(l.getDataAprovacao()) : "não aprovado"));
        System.out.println("Ocupado: " + Formatador.simNao(l.isOcupado()));
        System.out.println("Área da gleba: " + Formatador.decimal(l.getAreaGlebaM2()) + " m²");
        System.out.println("Área loteável (soma dos lotes): " + Formatador.decimal(l.calcularAreaLoteavel()) + " m²");
        System.out.println("População estimada: " + Formatador.inteiro(l.getPopulacaoEstimada()) + " hab");
        System.out.println("Entra na população projetada (RN13): " + Formatador.simNao(l.contribuiParaProjecao()));
        System.out.println("Lotes atendem às dimensões mínimas (RN05): " + Formatador.simNao(l.validarDimensoesLotes()));
        System.out.println("Lotes (" + l.getLotes().size() + "):");
        for (Lote lote : l.getLotes()) {
            System.out.println("  quadra " + lote.getQuadra() + ", número " + lote.getNumero() + ": "
                    + Formatador.decimal(lote.getAreaM2()) + " m², frente " + Formatador.decimal(lote.getFrenteM()) + " m");
        }
    }

    @Override
    protected void cadastrar() {
        Regiao regiao = escolherRegiao(regiaoService);
        Loteamento l = new Loteamento();
        l.setRegiaoId(regiao.getId());
        l.setDenominacao(entrada.lerTexto("Denominação"));
        l.setDataAprovacao(entrada.lerData("Data de aprovação (vazio = ainda não aprovado)"));
        l.setAreaGlebaM2(entrada.lerDouble("Área da gleba (m²)", 0));
        l.setPopulacaoEstimada(entrada.lerInteiro("População estimada (hab)", 0, Integer.MAX_VALUE));
        l.setOcupado(l.isAprovado() && entrada.lerSimNao("O loteamento já está ocupado?"));
        l.setLotes(lerLotes());
        loteamentoService.inserir(l);
        System.out.println("Loteamento cadastrado com id " + l.getId() + " (" + l.getLotes().size() + " lotes).");
    }

    @Override
    protected void atualizar() {
        Loteamento l = loteamentoService.buscar(entrada.lerId("Id do loteamento"));
        System.out.println("Pressione ENTER para manter o valor atual.");
        l.setDenominacao(entrada.lerTexto("Denominação", l.getDenominacao()));
        l.setAreaGlebaM2(entrada.lerDouble("Área da gleba (m²)", 0, l.getAreaGlebaM2()));
        l.setPopulacaoEstimada(entrada.lerInteiro("População estimada (hab)", 0, Integer.MAX_VALUE,
                l.getPopulacaoEstimada()));
        if (entrada.lerSimNao("Deseja substituir os " + l.getLotes().size() + " lotes atuais?", false)) {
            l.setLotes(lerLotes());
        }
        loteamentoService.atualizar(l);
        System.out.println("Loteamento atualizado. (Use as opções \"Aprovar\" e \"Marcar como ocupado\" para a situação.)");
    }

    @Override
    protected void excluir() {
        Loteamento l = loteamentoService.buscar(entrada.lerId("Id do loteamento"));
        if (confirmarExclusao("\"" + l.getDenominacao() + "\" e de seus " + l.getLotes().size() + " lotes")) {
            loteamentoService.excluir(l.getId());
            System.out.println("Loteamento excluído.");
        }
    }

    private void aprovar() {
        long id = entrada.lerId("Id do loteamento");
        LocalDate data = entrada.lerDataObrigatoria("Data de aprovação");
        loteamentoService.aprovar(id, data);
        System.out.println("Loteamento aprovado em " + Formatador.data(data) + ".");
    }

    private void marcarComoOcupado() {
        long id = entrada.lerId("Id do loteamento");
        loteamentoService.marcarComoOcupado(id);
        System.out.println("Loteamento marcado como ocupado (deixa de entrar na população projetada).");
    }

    private List<Lote> lerLotes() {
        int quantidade = entrada.lerInteiro("Quantos lotes", 1, MAXIMO_LOTES_POR_CADASTRO);
        List<Lote> lotes = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            System.out.println("Lote " + i + " de " + quantidade + ":");
            Lote lote = new Lote();
            lote.setQuadra(entrada.lerTexto("  Quadra"));
            lote.setNumero(entrada.lerTexto("  Número"));
            lote.setAreaM2(entrada.lerDouble("  Área (m²)", 0));
            lote.setFrenteM(entrada.lerDouble("  Frente (m)", 0));
            lotes.add(lote);
        }
        return lotes;
    }

    private Map<Long, String> nomesDasRegioes() {
        Map<Long, String> nomes = new HashMap<>();
        for (Regiao r : regiaoService.listar()) {
            nomes.put(r.getId(), r.getNome());
        }
        return nomes;
    }
}
