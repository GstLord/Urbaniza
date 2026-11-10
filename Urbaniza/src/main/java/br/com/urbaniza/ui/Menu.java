package br.com.urbaniza.ui;

import br.com.urbaniza.exception.PersistenciaException;
import br.com.urbaniza.exception.RegraNegocioException;
import br.com.urbaniza.model.Analista;
import br.com.urbaniza.model.Regiao;
import br.com.urbaniza.service.AnalistaService;
import br.com.urbaniza.service.RegiaoService;

import java.util.List;

/**
 * Base ABSTRATA de todos os menus de console. Implementa o TEMPLATE METHOD {@link #exibir()}:
 * mostra as opções definidas pela subclasse ({@link #opcoes()}), lê a escolha e executa a ação,
 * capturando erros de regra de negócio/persistência sem derrubar o programa.
 * A interface não contém SQL nem regra de negócio: apenas chama os services.
 */
public abstract class Menu {

    protected final Entrada entrada;

    protected Menu(Entrada entrada) {
        this.entrada = entrada;
    }

    /** Título exibido no topo do menu. */
    protected abstract String titulo();

    /** Opções do menu (numeradas a partir de 1; a opção 0 é sempre sair/voltar). */
    protected abstract List<OpcaoMenu> opcoes();

    /** Rótulo da opção 0. */
    protected String rotuloSaida() {
        return "Voltar";
    }

    /** TEMPLATE METHOD — laço de exibição do menu. */
    public final void exibir() {
        while (true) {
            List<OpcaoMenu> opcoes = opcoes();
            System.out.println();
            System.out.println("=== " + titulo() + " ===");
            for (int i = 0; i < opcoes.size(); i++) {
                System.out.println((i + 1) + " - " + opcoes.get(i).rotulo());
            }
            System.out.println("0 - " + rotuloSaida());
            int escolha = entrada.lerInteiro("Escolha uma opção", 0, opcoes.size());
            if (escolha == 0) {
                return;
            }
            executarComSeguranca(opcoes.get(escolha - 1));
        }
    }

    private void executarComSeguranca(OpcaoMenu opcao) {
        try {
            System.out.println();
            System.out.println("--- " + opcao.rotulo() + " ---");
            opcao.acao().run();
        } catch (EntradaEncerradaException e) {
            throw e;
        } catch (RegraNegocioException | PersistenciaException | IllegalArgumentException e) {
            System.out.println("Erro: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Erro inesperado (" + e.getClass().getSimpleName() + "): " + e.getMessage()
                    + ". A operação foi cancelada.");
        }
    }

    /** Lista as regiões cadastradas e pede o id de uma delas (que precisa existir). */
    protected Regiao escolherRegiao(RegiaoService regiaoService) {
        List<Regiao> regioes = regiaoService.listar();
        if (regioes.isEmpty()) {
            throw new RegraNegocioException("Nenhuma região cadastrada. Cadastre uma região primeiro.");
        }
        System.out.println("Regiões cadastradas:");
        for (Regiao r : regioes) {
            System.out.println("  " + r.getId() + " - " + r.getNome());
        }
        return regiaoService.buscar(entrada.lerId("Id da região"));
    }

    /** Lista os analistas cadastrados e pede o id de um deles (que precisa existir). */
    protected Analista escolherAnalista(AnalistaService analistaService) {
        List<Analista> analistas = analistaService.listar();
        if (analistas.isEmpty()) {
            throw new RegraNegocioException("Nenhum analista cadastrado. Cadastre um analista primeiro.");
        }
        System.out.println("Analistas cadastrados:");
        for (Analista a : analistas) {
            System.out.println("  " + a.getId() + " - " + a.getNome() + " (" + a.getMatricula() + ")");
        }
        return analistaService.buscar(entrada.lerId("Id do analista responsável"));
    }

    /** Pede confirmação antes de uma exclusão. */
    protected boolean confirmarExclusao(String descricao) {
        boolean confirmado = entrada.lerSimNao("Confirma a exclusão de " + descricao + "?");
        if (!confirmado) {
            System.out.println("Exclusão cancelada.");
        }
        return confirmado;
    }
}
