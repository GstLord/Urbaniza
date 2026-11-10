package br.com.urbaniza.service;

import br.com.urbaniza.dao.CrudDao;
import br.com.urbaniza.dao.CrudDaoPorRegiao;
import br.com.urbaniza.dao.CriterioVedacaoDao;
import br.com.urbaniza.dao.RegiaoDao;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.model.criterio.CriterioGeologico;
import br.com.urbaniza.model.criterio.CriterioVedacao;

import java.util.List;
import java.util.Locale;

/**
 * Cadastro dos critérios de vedação de cada região (RF01, RN01–RN04, E2).
 * Valida que a região existe e que o laudo geológico começa com ADEQUADO ou INADEQUADO.
 */
public class CriterioVedacaoService {

    private static final int TAMANHO_MAXIMO_CODIGO = 30;
    private static final int TAMANHO_MAXIMO_FUNDAMENTO = 150;

    private final CrudDaoPorRegiao<CriterioVedacao> criterioDao;
    private final CrudDao<Regiao> regiaoDao;

    /** Construtor padrão com os DAOs JDBC reais. */
    public CriterioVedacaoService() {
        this(new CriterioVedacaoDao(), new RegiaoDao());
    }

    /** Construtor com injeção dos DAOs (permite testar com DAOs falsos). */
    public CriterioVedacaoService(CrudDaoPorRegiao<CriterioVedacao> criterioDao, CrudDao<Regiao> regiaoDao) {
        this.criterioDao = criterioDao;
        this.regiaoDao = regiaoDao;
    }

    public CriterioVedacao inserir(CriterioVedacao criterio) {
        validar(criterio);
        return criterioDao.inserir(criterio);
    }

    public CriterioVedacao buscar(long id) {
        return criterioDao.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Critério " + id + " não cadastrado."));
    }

    public List<CriterioVedacao> listar() {
        return criterioDao.listarTodos();
    }

    public List<CriterioVedacao> listarPorRegiao(long regiaoId) {
        return criterioDao.listarPorRegiao(regiaoId);
    }

    public void atualizar(CriterioVedacao criterio) {
        Validacao.exigirId(criterio.getId(), "do critério");
        buscar(criterio.getId());
        validar(criterio);
        criterioDao.atualizar(criterio);
    }

    /** Exclui o critério; bloqueado se ele já foi usado em análise emitida (RN16). */
    public void excluir(long id) {
        buscar(id);
        criterioDao.excluir(id);
    }

    private void validar(CriterioVedacao criterio) {
        if (criterio == null) {
            throw new RegraNegocioException("Informe os dados do critério.");
        }
        if (criterio.getRegiaoId() == null || regiaoDao.buscarPorId(criterio.getRegiaoId()).isEmpty()) {
            throw new RegraNegocioException("Região " + criterio.getRegiaoId()
                    + " não cadastrada. Cadastre a região antes do critério de vedação.");
        }
        criterio.setCodigoInterno(Validacao.textoObrigatorio(criterio.getCodigoInterno(), "o código interno",
                TAMANHO_MAXIMO_CODIGO));
        criterio.setFundamentoLegal(Validacao.textoObrigatorio(criterio.getFundamentoLegal(), "o fundamento legal",
                TAMANHO_MAXIMO_FUNDAMENTO));
        if (criterio instanceof CriterioGeologico geologico) {
            geologico.setLaudoGeotecnico(normalizarLaudo(geologico.getLaudoGeotecnico()));
        }
    }

    /**
     * RN03 — o laudo deve começar com ADEQUADO ou INADEQUADO (formato exigido pelo banco).
     * Aceita o prefixo em minúsculas e o grava em maiúsculas.
     */
    static String normalizarLaudo(String laudo) {
        String texto = Validacao.textoObrigatorio(laudo, "o laudo geotécnico", TAMANHO_MAXIMO_FUNDAMENTO);
        String maiusculo = texto.toUpperCase(Locale.ROOT);
        for (String prefixo : new String[] {CriterioGeologico.LAUDO_INADEQUADO, CriterioGeologico.LAUDO_ADEQUADO}) {
            if (maiusculo.startsWith(prefixo)) {
                return prefixo + texto.substring(prefixo.length());
            }
        }
        throw new RegraNegocioException("O laudo geotécnico deve começar com \"ADEQUADO\" ou \"INADEQUADO\" "
                + "(ex.: \"INADEQUADO: encosta com risco de deslizamento\").");
    }
}
