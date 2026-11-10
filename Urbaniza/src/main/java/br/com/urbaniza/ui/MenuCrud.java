package br.com.urbaniza.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu ABSTRATO de CRUD. O template method {@link #exibir()} (herdado de {@link Menu}) mostra as opções
 * Listar, Detalhar, Cadastrar, Atualizar, Excluir (+ opções extras da subclasse) e Voltar, delegando
 * para os métodos abstratos implementados (com @Override) por cada menu concreto.
 *
 * @param <T> entidade gerenciada pelo menu
 */
public abstract class MenuCrud<T> extends Menu {

    protected MenuCrud(Entrada entrada) {
        super(entrada);
    }

    @Override
    protected final List<OpcaoMenu> opcoes() {
        List<OpcaoMenu> opcoes = new ArrayList<>();
        opcoes.add(new OpcaoMenu("Listar", this::listar));
        opcoes.add(new OpcaoMenu("Detalhar", this::detalhar));
        opcoes.add(new OpcaoMenu("Cadastrar", this::cadastrar));
        opcoes.add(new OpcaoMenu("Atualizar", this::atualizar));
        opcoes.add(new OpcaoMenu("Excluir", this::excluir));
        opcoes.addAll(opcoesExtras());
        return opcoes;
    }

    /** Opções adicionais específicas do menu (por padrão, nenhuma). */
    protected List<OpcaoMenu> opcoesExtras() {
        return List.of();
    }

    /** Read — lista todos os registros. */
    protected abstract void listar();

    /** Read — mostra todos os dados de um registro. */
    protected abstract void detalhar();

    /** Create — cadastra um novo registro. */
    protected abstract void cadastrar();

    /** Update — altera um registro existente. */
    protected abstract void atualizar();

    /** Delete — exclui um registro (sempre com confirmação). */
    protected abstract void excluir();

    /** Linha-resumo de um registro, usada nas listagens. */
    protected abstract String resumir(T item);

    /** Imprime uma lista de registros, uma linha por item. */
    protected void imprimirLista(List<T> itens, String mensagemVazia) {
        if (itens.isEmpty()) {
            System.out.println(mensagemVazia);
            return;
        }
        for (T item : itens) {
            System.out.println(resumir(item));
        }
        System.out.println("Total: " + itens.size());
    }
}
