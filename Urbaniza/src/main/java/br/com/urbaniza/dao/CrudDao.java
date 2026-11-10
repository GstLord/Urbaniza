package br.com.urbaniza.dao;

/**
 * DAO com CRUD completo (Create, Read, Update, Delete).
 * A análise de parcelamento NÃO implementa esta interface: análise emitida é imutável (RN16).
 *
 * @param <T> tipo da entidade
 */
public interface CrudDao<T> extends Dao<T> {

    void atualizar(T entidade);
}
