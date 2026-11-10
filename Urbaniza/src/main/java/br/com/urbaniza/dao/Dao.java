package br.com.urbaniza.dao;

import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de acesso a dados (E5). Operações comuns a todas as entidades persistidas.
 *
 * @param <T> tipo da entidade
 */
public interface Dao<T> {

    /** Grava a entidade e devolve-a com o id gerado pelo banco. */
    T inserir(T entidade);

    Optional<T> buscarPorId(long id);

    List<T> listarTodos();

    void excluir(long id);
}
