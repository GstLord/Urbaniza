package br.com.urbaniza.service;

import br.com.urbaniza.dao.AnaliseParcelamentoDao;
import br.com.urbaniza.dao.AnalistaDao;
import br.com.urbaniza.dao.CrudDao;
import br.com.urbaniza.dao.CriterioVedacaoDao;
import br.com.urbaniza.dao.DaoPorRegiao;
import br.com.urbaniza.dao.EquipamentoComunitarioDao;
import br.com.urbaniza.dao.LoteamentoDao;
import br.com.urbaniza.dao.RegiaoDao;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.AnaliseParcelamento;
import br.com.urbaniza.model.Analista;
import br.com.urbaniza.model.Loteamento;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.model.criterio.CriterioVedacao;
import br.com.urbaniza.model.equipamento.EquipamentoComunitario;

import java.util.List;

/**
 * Emissão e consulta das análises de aptidão ao parcelamento (RF01, RF07, RN14–RN16).
 * A análise emitida é imutável: não há operação de atualização.
 */
public class AnaliseService {

    private final DaoPorRegiao<AnaliseParcelamento> analiseDao;
    private final CrudDao<Analista> analistaDao;
    private final DaoPorRegiao<CriterioVedacao> criterioDao;
    private final RegiaoService regiaoService;

    /** Construtor padrão com os DAOs JDBC reais. */
    public AnaliseService() {
        this(new AnaliseParcelamentoDao(), new RegiaoDao(), new LoteamentoDao(), new EquipamentoComunitarioDao(),
                new AnalistaDao(), new CriterioVedacaoDao());
    }

    /** Construtor com injeção dos DAOs (permite testar com DAOs falsos). */
    public AnaliseService(DaoPorRegiao<AnaliseParcelamento> analiseDao, CrudDao<Regiao> regiaoDao,
                          DaoPorRegiao<Loteamento> loteamentoDao,
                          DaoPorRegiao<EquipamentoComunitario> equipamentoDao,
                          CrudDao<Analista> analistaDao, DaoPorRegiao<CriterioVedacao> criterioDao) {
        this.analiseDao = analiseDao;
        this.analistaDao = analistaDao;
        this.criterioDao = criterioDao;
        this.regiaoService = new RegiaoService(regiaoDao, loteamentoDao, equipamentoDao);
    }

    /**
     * RF01 / RF07 / RN16 — emite a análise: carrega a região completa (loteamentos, lotes e equipamentos),
     * o analista e os critérios da região; executa a análise e grava-a no banco.
     *
     * @return a análise gravada, já com id
     */
    public AnaliseParcelamento emitir(long regiaoId, long analistaId) {
        Regiao regiao = regiaoService.carregarCompleta(regiaoId);
        Analista analista = analistaDao.buscarPorId(analistaId)
                .orElseThrow(() -> new RegraNegocioException("Analista " + analistaId + " não cadastrado."));
        List<CriterioVedacao> criterios = criterioDao.listarPorRegiao(regiaoId);
        AnaliseParcelamento analise = new AnaliseParcelamento(regiao, analista, criterios);
        analise.executar();
        return analiseDao.inserir(analise);
    }

    public AnaliseParcelamento buscar(long id) {
        return analiseDao.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Análise " + id + " não encontrada."));
    }

    public List<AnaliseParcelamento> listar() {
        return analiseDao.listarTodos();
    }

    public List<AnaliseParcelamento> listarPorRegiao(long regiaoId) {
        return analiseDao.listarPorRegiao(regiaoId);
    }

    public void excluir(long id) {
        buscar(id);
        analiseDao.excluir(id);
    }
}
