package br.com.urbaniza.service;

import br.com.urbaniza.dao.AnalistaDao;
import br.com.urbaniza.dao.CrudDao;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.Analista;

import java.util.List;

/**
 * Cadastro dos analistas responsáveis pelas análises (RF07 / RN16).
 */
public class AnalistaService {

    private static final int TAMANHO_MAXIMO_NOME = 150;
    private static final int TAMANHO_MAXIMO_MATRICULA = 30;
    private static final int TAMANHO_MAXIMO_SETOR = 80;

    private final CrudDao<Analista> analistaDao;

    /** Construtor padrão com o DAO JDBC real. */
    public AnalistaService() {
        this(new AnalistaDao());
    }

    /** Construtor com injeção do DAO (permite testar com DAO falso). */
    public AnalistaService(CrudDao<Analista> analistaDao) {
        this.analistaDao = analistaDao;
    }

    public Analista inserir(Analista analista) {
        validar(analista);
        return analistaDao.inserir(analista);
    }

    public Analista buscar(long id) {
        return analistaDao.buscarPorId(id)
                .orElseThrow(() -> new RegraNegocioException("Analista " + id + " não cadastrado."));
    }

    public List<Analista> listar() {
        return analistaDao.listarTodos();
    }

    public void atualizar(Analista analista) {
        Validacao.exigirId(analista.getId(), "do analista");
        buscar(analista.getId());
        validar(analista);
        analistaDao.atualizar(analista);
    }

    /** Exclui o analista; bloqueado se ele for responsável por análises emitidas (RN16). */
    public void excluir(long id) {
        buscar(id);
        analistaDao.excluir(id);
    }

    private static void validar(Analista analista) {
        if (analista == null) {
            throw new RegraNegocioException("Informe os dados do analista.");
        }
        analista.setNome(Validacao.textoObrigatorio(analista.getNome(), "o nome do analista", TAMANHO_MAXIMO_NOME));
        analista.setMatricula(Validacao.textoObrigatorio(analista.getMatricula(), "a matrícula",
                TAMANHO_MAXIMO_MATRICULA));
        analista.setSetor(Validacao.textoObrigatorio(analista.getSetor(), "o setor", TAMANHO_MAXIMO_SETOR));
    }
}
