package br.com.urbaniza.service;

import br.com.urbaniza.dao.CrudDao;
import br.com.urbaniza.dao.CrudDaoPorRegiao;
import br.com.urbaniza.dao.DaoPorRegiao;
import br.com.urbaniza.dao.EquipamentoComunitarioDao;
import br.com.urbaniza.dao.LoteamentoDao;
import br.com.urbaniza.dao.RegiaoDao;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.Loteamento;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.model.equipamento.EquipamentoComunitario;
import br.com.urbaniza.model.equipamento.Escola;
import br.com.urbaniza.model.equipamento.UnidadeSaude;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Regras dos equipamentos comunitários (RF03). O índice de atendimento usa a população
 * PROJETADA da região (RN14); equipamento sem região não tem índice ("não aplicável").
 */
public class EquipamentoService {

    private static final int TAMANHO_MAXIMO_NOME = 150;
    private static final int TAMANHO_MAXIMO_DESCRICAO = 50;

    private final CrudDaoPorRegiao<EquipamentoComunitario> equipamentoDao;
    private final CrudDao<Regiao> regiaoDao;
    private final RegiaoService regiaoService;

    /** Construtor padrão com os DAOs JDBC reais. */
    public EquipamentoService() {
        this(new EquipamentoComunitarioDao(), new RegiaoDao(), new LoteamentoDao());
    }

    /** Construtor com injeção dos DAOs (permite testar com DAOs falsos). */
    public EquipamentoService(CrudDaoPorRegiao<EquipamentoComunitario> equipamentoDao, CrudDao<Regiao> regiaoDao,
                              DaoPorRegiao<Loteamento> loteamentoDao) {
        this.equipamentoDao = equipamentoDao;
        this.regiaoDao = regiaoDao;
        this.regiaoService = new RegiaoService(regiaoDao, loteamentoDao, equipamentoDao);
    }

    public EquipamentoComunitario inserir(EquipamentoComunitario equipamento) {
        validar(equipamento);
        return equipamentoDao.inserir(equipamento);
    }

    public EquipamentoComunitario buscar(long id) {
        return equipamentoDao.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Equipamento " + id + " não cadastrado."));
    }

    public List<EquipamentoComunitario> listar() {
        return equipamentoDao.listarTodos();
    }

    public List<EquipamentoComunitario> listarPorRegiao(long regiaoId) {
        return equipamentoDao.listarPorRegiao(regiaoId);
    }

    public void atualizar(EquipamentoComunitario equipamento) {
        Validacao.exigirId(equipamento.getId(), "do equipamento");
        buscar(equipamento.getId());
        validar(equipamento);
        equipamentoDao.atualizar(equipamento);
    }

    public void excluir(long id) {
        buscar(id);
        equipamentoDao.excluir(id);
    }

    /**
     * RF03 / RN11 / RN14 — situação de atendimento do equipamento frente à população projetada da sua região.
     *
     * @return vazio quando o equipamento não está associado a uma região ("não aplicável")
     */
    public Optional<SituacaoAtendimento> calcularAtendimento(long equipamentoId) {
        EquipamentoComunitario equipamento = buscar(equipamentoId);
        if (equipamento.getRegiaoId() == null) {
            return Optional.empty();
        }
        Regiao regiao = regiaoService.carregarCompleta(equipamento.getRegiaoId());
        return Optional.of(SituacaoAtendimento.calcular(equipamento, regiao.calcularPopulacaoProjetada()));
    }

    /** RF03 / RN11 / RN14 — situação de atendimento de todos os equipamentos de uma região. */
    public List<SituacaoAtendimento> calcularAtendimentoDaRegiao(long regiaoId) {
        Regiao regiao = regiaoService.carregarCompleta(regiaoId);
        int populacaoProjetada = regiao.calcularPopulacaoProjetada();
        List<SituacaoAtendimento> situacoes = new ArrayList<>();
        for (EquipamentoComunitario equipamento : regiao.getEquipamentos()) {
            situacoes.add(SituacaoAtendimento.calcular(equipamento, populacaoProjetada));
        }
        return situacoes;
    }

    private void validar(EquipamentoComunitario equipamento) {
        if (equipamento == null) {
            throw new RegraNegocioException("Informe os dados do equipamento.");
        }
        equipamento.setNome(Validacao.textoObrigatorio(equipamento.getNome(), "o nome do equipamento",
                TAMANHO_MAXIMO_NOME));
        if (equipamento.getRegiaoId() != null && regiaoDao.buscarPorId(equipamento.getRegiaoId()).isEmpty()) {
            throw new RegraNegocioException("Região " + equipamento.getRegiaoId() + " não cadastrada.");
        }
        switch (equipamento) {
            case Escola escola -> {
                escola.setNivelEnsino(Validacao.textoObrigatorio(escola.getNivelEnsino(), "o nível de ensino",
                        TAMANHO_MAXIMO_DESCRICAO));
                escola.setPercentualIdadeEscolar(escola.getPercentualIdadeEscolar());
            }
            case UnidadeSaude unidade -> {
                unidade.setTipoUnidade(Validacao.textoObrigatorio(unidade.getTipoUnidade(), "o tipo da unidade",
                        TAMANHO_MAXIMO_DESCRICAO));
                unidade.setHabitantesPorEquipe(unidade.getHabitantesPorEquipe());
            }
            default -> throw new RegraNegocioException("Tipo de equipamento não suportado: " + equipamento.getTipo());
        }
    }
}
