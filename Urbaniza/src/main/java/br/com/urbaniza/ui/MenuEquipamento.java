package br.com.urbaniza.ui;

import br.com.urbaniza.model.equipamento.EquipamentoComunitario;
import br.com.urbaniza.model.equipamento.Escola;
import br.com.urbaniza.model.equipamento.UnidadeSaude;
import br.com.urbaniza.service.EquipamentoService;
import br.com.urbaniza.service.RegiaoService;
import br.com.urbaniza.service.SituacaoAtendimento;

import java.util.List;
import java.util.Optional;

/**
 * CRUD de equipamentos comunitários — escolas e unidades de saúde (RF03).
 */
public class MenuEquipamento extends MenuCrud<EquipamentoComunitario> {

    private static final int TIPO_ESCOLA = 1;
    private static final int TIPO_UNIDADE_SAUDE = 2;

    private final EquipamentoService equipamentoService;
    private final RegiaoService regiaoService;

    public MenuEquipamento(Entrada entrada) {
        this(entrada, new EquipamentoService(), new RegiaoService());
    }

    public MenuEquipamento(Entrada entrada, EquipamentoService equipamentoService, RegiaoService regiaoService) {
        super(entrada);
        this.equipamentoService = equipamentoService;
        this.regiaoService = regiaoService;
    }

    @Override
    protected String titulo() {
        return "EQUIPAMENTOS COMUNITÁRIOS";
    }

    @Override
    protected String resumir(EquipamentoComunitario e) {
        return String.format("%4d | %-13s | %-35s | capacidade %6s | região %s",
                e.getId(), e.getTipo(), e.getNome(), Formatador.inteiro(e.getCapacidadeInstalada()),
                e.getRegiaoId() == null ? "sem região" : e.getRegiaoId().toString());
    }

    @Override
    protected void listar() {
        imprimirLista(equipamentoService.listar(), "Nenhum equipamento cadastrado.");
    }

    @Override
    protected void detalhar() {
        EquipamentoComunitario e = equipamentoService.buscar(entrada.lerId("Id do equipamento"));
        System.out.println("Id: " + e.getId());
        System.out.println("Tipo: " + e.getTipo());
        System.out.println("Nome: " + e.getNome());
        System.out.println("Região: " + (e.getRegiaoId() == null ? "sem região"
                : e.getRegiaoId() + " - " + regiaoService.buscar(e.getRegiaoId()).getNome()));
        switch (e) {
            case Escola escola -> {
                System.out.println("Vagas (capacidade instalada): " + Formatador.inteiro(escola.getCapacidadeInstalada()));
                System.out.println("Percentual da população em idade escolar: "
                        + Formatador.percentualDireto(escola.getPercentualIdadeEscolar()));
                System.out.println("Nível de ensino: " + escola.getNivelEnsino());
            }
            case UnidadeSaude unidade -> {
                System.out.println("Equipes instaladas (capacidade): " + Formatador.inteiro(unidade.getCapacidadeInstalada()));
                System.out.println("Habitantes por equipe: " + Formatador.inteiro(unidade.getHabitantesPorEquipe()));
                System.out.println("Tipo de unidade: " + unidade.getTipoUnidade());
            }
            default -> System.out.println("Capacidade instalada: " + Formatador.inteiro(e.getCapacidadeInstalada()));
        }
        Optional<SituacaoAtendimento> situacao = equipamentoService.calcularAtendimento(e.getId());
        if (situacao.isEmpty()) {
            System.out.println("Índice de atendimento: não aplicável (equipamento sem região)");
        } else {
            SituacaoAtendimento s = situacao.get();
            System.out.println("População projetada da região: " + Formatador.inteiro(s.populacaoProjetada()) + " hab");
            System.out.println("Demanda: " + Formatador.inteiro(s.demanda()));
            System.out.println("Índice de atendimento (RN11): " + Formatador.indiceAtendimento(s.indice())
                    + (s.deficit() ? " — DÉFICIT" : " — sem déficit"));
        }
    }

    @Override
    protected void cadastrar() {
        System.out.println("Tipo: 1 - Escola | 2 - Unidade de saúde");
        int tipo = entrada.lerInteiro("Tipo", TIPO_ESCOLA, TIPO_UNIDADE_SAUDE);
        EquipamentoComunitario e = tipo == TIPO_ESCOLA ? new Escola() : new UnidadeSaude();
        e.setNome(entrada.lerTexto("Nome"));
        if (entrada.lerSimNao("Vincular a uma região?")) {
            e.setRegiaoId(escolherRegiao(regiaoService).getId());
        }
        lerCamposEspecificos(e, false);
        equipamentoService.inserir(e);
        System.out.println("Equipamento cadastrado com id " + e.getId() + ".");
    }

    @Override
    protected void atualizar() {
        EquipamentoComunitario e = equipamentoService.buscar(entrada.lerId("Id do equipamento"));
        System.out.println("Tipo: " + e.getTipo() + " (o tipo não pode ser alterado). ENTER mantém o valor atual.");
        e.setNome(entrada.lerTexto("Nome", e.getNome()));
        String regiaoAtual = e.getRegiaoId() == null ? "sem região" : "região " + e.getRegiaoId();
        if (entrada.lerSimNao("Alterar a região (atual: " + regiaoAtual + ")?", false)) {
            e.setRegiaoId(entrada.lerSimNao("Vincular a uma região?") ? escolherRegiao(regiaoService).getId() : null);
        }
        lerCamposEspecificos(e, true);
        equipamentoService.atualizar(e);
        System.out.println("Equipamento atualizado.");
    }

    @Override
    protected void excluir() {
        EquipamentoComunitario e = equipamentoService.buscar(entrada.lerId("Id do equipamento"));
        if (confirmarExclusao("\"" + e.getNome() + "\"")) {
            equipamentoService.excluir(e.getId());
            System.out.println("Equipamento excluído.");
        }
    }

    private void lerCamposEspecificos(EquipamentoComunitario e, boolean atualizacao) {
        switch (e) {
            case Escola escola -> {
                escola.setCapacidadeInstalada(atualizacao
                        ? entrada.lerInteiro("Vagas", 0, Integer.MAX_VALUE, escola.getCapacidadeInstalada())
                        : entrada.lerInteiro("Vagas", 0, Integer.MAX_VALUE));
                escola.setPercentualIdadeEscolar(lerPercentual(atualizacao ? escola.getPercentualIdadeEscolar() : null));
                escola.setNivelEnsino(atualizacao
                        ? entrada.lerTexto("Nível de ensino", escola.getNivelEnsino())
                        : entrada.lerTexto("Nível de ensino (ex.: Fundamental, Médio)"));
            }
            case UnidadeSaude unidade -> {
                unidade.setCapacidadeInstalada(atualizacao
                        ? entrada.lerInteiro("Equipes instaladas", 0, Integer.MAX_VALUE, unidade.getCapacidadeInstalada())
                        : entrada.lerInteiro("Equipes instaladas", 0, Integer.MAX_VALUE));
                unidade.setHabitantesPorEquipe(atualizacao
                        ? entrada.lerInteiro("Habitantes por equipe", 1, Integer.MAX_VALUE, unidade.getHabitantesPorEquipe())
                        : entrada.lerInteiro("Habitantes por equipe", 1, Integer.MAX_VALUE));
                unidade.setTipoUnidade(atualizacao
                        ? entrada.lerTexto("Tipo de unidade", unidade.getTipoUnidade())
                        : entrada.lerTexto("Tipo de unidade (ex.: UBS)"));
            }
            default -> throw new IllegalArgumentException("Tipo de equipamento não suportado: " + e.getTipo());
        }
    }

    /** Percentual da população em idade escolar no intervalo (0, 100]. */
    private double lerPercentual(Double atual) {
        while (true) {
            double valor = atual == null
                    ? entrada.lerDouble("Percentual da população em idade escolar (%)", 0)
                    : entrada.lerDouble("Percentual da população em idade escolar (%)", 0, atual);
            if (valor > 0 && valor <= 100) {
                return valor;
            }
            System.out.println("  O percentual deve ser maior que 0 e no máximo 100.");
        }
    }
}
