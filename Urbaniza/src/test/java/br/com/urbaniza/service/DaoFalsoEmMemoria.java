package br.com.urbaniza.service;

import br.com.urbaniza.dao.CrudDaoPorRegiao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * DAO FALSO em memória, usado SOMENTE nos testes de serviço (a aplicação real persiste no PostgreSQL).
 * Implementa as interfaces de DAO para que os serviços possam ser testados sem banco.
 *
 * @param <T> tipo da entidade
 */
class DaoFalsoEmMemoria<T> implements CrudDaoPorRegiao<T> {

    private final Map<Long, T> registros = new LinkedHashMap<>();
    private final Function<T, Long> obterId;
    private final BiConsumer<T, Long> definirId;
    private final Function<T, Long> obterRegiaoId;
    private final List<T> inseridos = new ArrayList<>();
    private final List<T> atualizados = new ArrayList<>();
    private long proximoId = 1000;

    DaoFalsoEmMemoria(Function<T, Long> obterId, BiConsumer<T, Long> definirId, Function<T, Long> obterRegiaoId) {
        this.obterId = obterId;
        this.definirId = definirId;
        this.obterRegiaoId = obterRegiaoId;
    }

    /** Pré-carrega um registro que já "existe no banco" (mantém o id informado). */
    void carregar(T entidade) {
        registros.put(obterId.apply(entidade), entidade);
    }

    List<T> getInseridos() {
        return inseridos;
    }

    List<T> getAtualizados() {
        return atualizados;
    }

    @Override
    public T inserir(T entidade) {
        definirId.accept(entidade, proximoId++);
        registros.put(obterId.apply(entidade), entidade);
        inseridos.add(entidade);
        return entidade;
    }

    @Override
    public Optional<T> buscarPorId(long id) {
        return Optional.ofNullable(registros.get(id));
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(registros.values());
    }

    @Override
    public void excluir(long id) {
        registros.remove(id);
    }

    @Override
    public void atualizar(T entidade) {
        registros.put(obterId.apply(entidade), entidade);
        atualizados.add(entidade);
    }

    @Override
    public List<T> listarPorRegiao(long regiaoId) {
        List<T> resultado = new ArrayList<>();
        for (T entidade : registros.values()) {
            if (Objects.equals(obterRegiaoId.apply(entidade), regiaoId)) {
                resultado.add(entidade);
            }
        }
        return resultado;
    }
}
