package br.com.urbaniza.dao;

import java.util.List;

/**
 * DAO de entidades vinculadas a uma região, com consulta por região.
 *
 * @param <T> tipo da entidade
 */
public interface DaoPorRegiao<T> extends Dao<T> {

    List<T> listarPorRegiao(long regiaoId);
}
