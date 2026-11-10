package br.com.urbaniza.service;

import br.com.urbaniza.dao.CrudDao;
import br.com.urbaniza.dao.DaoPorRegiao;
import br.com.urbaniza.dao.EquipamentoComunitarioDao;
import br.com.urbaniza.dao.LoteamentoDao;
import br.com.urbaniza.dao.RegiaoDao;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.Loteamento;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.model.equipamento.EquipamentoComunitario;

import java.util.List;

/**
 * Regras e casos de uso da entidade principal {@link Regiao}: CRUD completo (RF01),
 * carga completa (loteamentos + lotes + equipamentos) e indicadores (RF04, RF05, RN08–RN10).
 */
public class RegiaoService {

    private static final int TAMANHO_MAXIMO_NOME = 150;

    private final CrudDao<Regiao> regiaoDao;
    private final DaoPorRegiao<Loteamento> loteamentoDao;
    private final DaoPorRegiao<EquipamentoComunitario> equipamentoDao;

    /** Construtor padrão com os DAOs JDBC reais. */
    public RegiaoService() {
        this(new RegiaoDao(), new LoteamentoDao(), new EquipamentoComunitarioDao());
    }

    /** Construtor com injeção dos DAOs (permite testar com DAOs falsos). */
    public RegiaoService(CrudDao<Regiao> regiaoDao, DaoPorRegiao<Loteamento> loteamentoDao,
                         DaoPorRegiao<EquipamentoComunitario> equipamentoDao) {
        this.regiaoDao = regiaoDao;
        this.loteamentoDao = loteamentoDao;
        this.equipamentoDao = equipamentoDao;
    }

    /** Create — cadastra uma nova região. */
    public Regiao criar(Regiao regiao) {
        validar(regiao);
        return regiaoDao.inserir(regiao);
    }

    /** Read — busca uma região (dados básicos) ou lança exceção se não existir. */
    public Regiao buscar(long id) {
        return regiaoDao.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Região " + id + " não cadastrada."));
    }

    /** Read — lista todas as regiões (dados básicos). */
    public List<Regiao> listar() {
        return regiaoDao.listarTodos();
    }

    /** Update — atualiza os dados de uma região existente. */
    public void atualizar(Regiao regiao) {
        Validacao.exigirId(regiao.getId(), "da região");
        buscar(regiao.getId());
        validar(regiao);
        regiaoDao.atualizar(regiao);
    }

    /**
     * Delete — exclui a região. Loteamentos, lotes e critérios são removidos em cascata;
     * equipamentos ficam sem região (agregação). Bloqueado se houver análises emitidas (RN16).
     */
    public void excluir(long id) {
        buscar(id);
        regiaoDao.excluir(id);
    }

    /** Carrega a região com loteamentos (e seus lotes) e equipamentos comunitários. */
    public Regiao carregarCompleta(long id) {
        Regiao regiao = buscar(id);
        regiao.setLoteamentos(loteamentoDao.listarPorRegiao(id));
        regiao.setEquipamentos(equipamentoDao.listarPorRegiao(id));
        return regiao;
    }

    /** RF04 / RF05 / RN08–RN10 / RN13 / RN14 — indicadores urbanísticos da região. */
    public IndicadoresRegiao calcularIndicadores(long id) {
        return calcularIndicadores(carregarCompleta(id));
    }

    /** Indicadores de uma região já carregada por completo. */
    public static IndicadoresRegiao calcularIndicadores(Regiao regiao) {
        return new IndicadoresRegiao(
                regiao.getPopulacaoAtual(),
                regiao.calcularPopulacaoProjetada(),
                regiao.calcularIAV(),
                regiao.classificarIAV(),
                regiao.calcularTaxaImpermeabilizacao(),
                regiao.classificarIlhaDeCalor());
    }

    private static void validar(Regiao regiao) {
        if (regiao == null) {
            throw new RegraNegocioException("Informe os dados da região.");
        }
        regiao.setNome(Validacao.textoObrigatorio(regiao.getNome(), "o nome da região", TAMANHO_MAXIMO_NOME));
        if (!(regiao.getAreaTotalM2() > 0)) {
            throw new RegraNegocioException("A área total da região deve ser maior que zero.");
        }
        if (regiao.getAreaVerdeM2() + regiao.getAreaImpermeavelM2() > regiao.getAreaTotalM2()) {
            throw new RegraNegocioException(
                    "A soma das áreas verde e impermeável não pode ser maior que a área total da região.");
        }
    }
}
