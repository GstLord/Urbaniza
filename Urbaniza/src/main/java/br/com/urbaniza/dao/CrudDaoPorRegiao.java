package br.com.urbaniza.dao;

/**
 * DAO com CRUD completo para entidades vinculadas a uma região.
 *
 * @param <T> tipo da entidade
 */
public interface CrudDaoPorRegiao<T> extends CrudDao<T>, DaoPorRegiao<T> {
}
