package br.com.urbaniza.ui;

import br.com.urbaniza.model.ParametrosUrbanisticos;
import br.com.urbaniza.model.criterio.CriterioAlagamento;
import br.com.urbaniza.model.criterio.CriterioDeclividade;
import br.com.urbaniza.model.criterio.CriterioGeologico;
import br.com.urbaniza.model.criterio.CriterioPreservacao;
import br.com.urbaniza.model.criterio.CriterioVedacao;
import br.com.urbaniza.service.CriterioVedacaoService;
import br.com.urbaniza.service.RegiaoService;

import java.util.Locale;

/**
 * CRUD dos critérios de vedação legal de cada região (RF01, RN01–RN04).
 */
public class MenuCriterio extends MenuCrud<CriterioVedacao> {

    private static final int TIPO_ALAGAMENTO = 1;
    private static final int TIPO_DECLIVIDADE = 2;
    private static final int TIPO_GEOLOGICO = 3;
    private static final int TIPO_PRESERVACAO = 4;
    private static final String SEPARADOR_LAUDO = ": ";

    private final CriterioVedacaoService criterioService;
    private final RegiaoService regiaoService;

    public MenuCriterio(Entrada entrada) {
        this(entrada, new CriterioVedacaoService(), new RegiaoService());
    }

    public MenuCriterio(Entrada entrada, CriterioVedacaoService criterioService, RegiaoService regiaoService) {
        super(entrada);
        this.criterioService = criterioService;
        this.regiaoService = regiaoService;
    }

    @Override
    protected String titulo() {
        return "CRITÉRIOS DE VEDAÇÃO";
    }

    @Override
    protected String resumir(CriterioVedacao c) {
        return String.format("%4d | região %3d | %-15s | %-12s | %s",
                c.getId(), c.getRegiaoId(), c.getCodigoInterno(), c.getTipo(), descreverParametro(c));
    }

    @Override
    protected void listar() {
        imprimirLista(criterioService.listar(), "Nenhum critério cadastrado.");
    }

    @Override
    protected void detalhar() {
        CriterioVedacao c = criterioService.buscar(entrada.lerId("Id do critério"));
        System.out.println("Id: " + c.getId());
        System.out.println("Região: " + c.getRegiaoId() + " - " + regiaoService.buscar(c.getRegiaoId()).getNome());
        System.out.println("Código interno: " + c.getCodigoInterno());
        System.out.println("Tipo: " + c.getTipo());
        System.out.println("Fundamento legal: " + c.getFundamentoLegal());
        System.out.println("Parâmetro: " + descreverParametro(c));
        System.out.println("Impedimento absoluto (gera INAPTA): " + Formatador.simNao(c.isImpedimentoAbsoluto()));
        System.out.println("Descrição do impedimento: " + c.descreverImpedimento());
    }

    @Override
    protected void cadastrar() {
        long regiaoId = escolherRegiao(regiaoService).getId();
        System.out.println("Tipo: 1 - Alagamento | 2 - Declividade | 3 - Geológico | 4 - Preservação");
        int tipo = entrada.lerInteiro("Tipo", TIPO_ALAGAMENTO, TIPO_PRESERVACAO);
        CriterioVedacao c = switch (tipo) {
            case TIPO_ALAGAMENTO -> new CriterioAlagamento();
            case TIPO_DECLIVIDADE -> new CriterioDeclividade();
            case TIPO_GEOLOGICO -> new CriterioGeologico();
            default -> new CriterioPreservacao();
        };
        c.setRegiaoId(regiaoId);
        c.setCodigoInterno(entrada.lerTexto("Código interno (ex.: ALG-NORTE-01)"));
        c.setFundamentoLegal(entrada.lerTexto("Fundamento legal", c.getFundamentoLegal()));
        lerParametro(c, false);
        criterioService.inserir(c);
        System.out.println("Critério cadastrado com id " + c.getId() + ".");
    }

    @Override
    protected void atualizar() {
        CriterioVedacao c = criterioService.buscar(entrada.lerId("Id do critério"));
        System.out.println("Tipo: " + c.getTipo() + " (o tipo não pode ser alterado). ENTER mantém o valor atual.");
        c.setCodigoInterno(entrada.lerTexto("Código interno", c.getCodigoInterno()));
        c.setFundamentoLegal(entrada.lerTexto("Fundamento legal", c.getFundamentoLegal()));
        lerParametro(c, true);
        criterioService.atualizar(c);
        System.out.println("Critério atualizado.");
    }

    @Override
    protected void excluir() {
        CriterioVedacao c = criterioService.buscar(entrada.lerId("Id do critério"));
        if (confirmarExclusao("\"" + c.getCodigoInterno() + "\"")) {
            criterioService.excluir(c.getId());
            System.out.println("Critério excluído.");
        }
    }

    private void lerParametro(CriterioVedacao criterio, boolean atualizacao) {
        switch (criterio) {
            case CriterioAlagamento c -> c.setSaneamentoExecutado(atualizacao
                    ? entrada.lerSimNao("O saneamento já foi executado?", c.isSaneamentoExecutado())
                    : entrada.lerSimNao("O saneamento já foi executado?"));
            case CriterioDeclividade c -> c.setDeclividadeLimite(lerLimite(atualizacao
                    ? c.getDeclividadeLimite() : ParametrosUrbanisticos.DECLIVIDADE_LIMITE_PADRAO_PCT));
            case CriterioGeologico c -> c.setLaudoGeotecnico(lerLaudo(atualizacao ? c.getLaudoGeotecnico() : null));
            case CriterioPreservacao c -> c.setAreaProtegida(atualizacao
                    ? entrada.lerSimNao("A área é de preservação ecológica (protegida)?", c.isAreaProtegida())
                    : entrada.lerSimNao("A área é de preservação ecológica (protegida)?"));
            default -> throw new IllegalArgumentException("Tipo de critério não suportado: " + criterio.getTipo());
        }
    }

    private double lerLimite(double atual) {
        while (true) {
            double limite = entrada.lerDouble("Limite de declividade (%)", 0, atual);
            if (limite > 0) {
                return limite;
            }
            System.out.println("  O limite deve ser maior que zero.");
        }
    }

    /** Monta "ADEQUADO: resumo" ou "INADEQUADO: resumo" a partir das respostas (RN03). */
    private String lerLaudo(String atual) {
        if (atual != null) {
            System.out.println("Laudo atual: " + atual);
            if (!entrada.lerSimNao("Alterar o laudo?", false)) {
                return atual;
            }
        }
        boolean adequado = entrada.lerSimNao("O laudo indica terreno adequado?");
        String resumo = entrada.lerTextoOpcional("Resumo do laudo");
        String prefixo = adequado ? CriterioGeologico.LAUDO_ADEQUADO : CriterioGeologico.LAUDO_INADEQUADO;
        return resumo.isEmpty() ? prefixo : prefixo + SEPARADOR_LAUDO + resumo;
    }

    private static String descreverParametro(CriterioVedacao criterio) {
        return switch (criterio) {
            case CriterioAlagamento c -> "saneamento executado: " + Formatador.simNao(c.isSaneamentoExecutado());
            case CriterioDeclividade c -> "limite: " + Formatador.percentualDireto(c.getDeclividadeLimite());
            case CriterioGeologico c -> "laudo: " + c.getLaudoGeotecnico();
            case CriterioPreservacao c -> "área protegida: " + Formatador.simNao(c.isAreaProtegida());
            default -> criterio.getTipo().toLowerCase(Locale.ROOT);
        };
    }
}
