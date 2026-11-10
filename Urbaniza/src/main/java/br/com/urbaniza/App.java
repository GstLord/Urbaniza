package br.com.urbaniza;

import br.com.urbaniza.config.ConnectionFactory;
import br.com.urbaniza.ui.Entrada;
import br.com.urbaniza.ui.EntradaEncerradaException;
import br.com.urbaniza.ui.MenuPrincipal;

/**
 * Ponto de entrada da aplicação Urbaniza: testa a conexão com o banco e abre o menu principal.
 * Nenhum stack trace é mostrado ao usuário.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        System.out.println("Urbaniza — Sistema de Controle de Urbanização por Regiões");
        if (!ConnectionFactory.testarConexao()) {
            System.out.println("Conexão com o banco: FALHOU — não foi possível acessar o PostgreSQL.");
            System.out.println("Suba o banco: cd Urbaniza/database && docker compose up -d");
            System.exit(1);
        }
        System.out.println("Conexão com o banco: OK");
        try {
            new MenuPrincipal(new Entrada()).exibir();
            System.out.println("Até logo!");
        } catch (EntradaEncerradaException e) {
            System.out.println();
            System.out.println("Entrada encerrada.");
        } catch (RuntimeException e) {
            System.out.println("Erro inesperado: " + e.getMessage());
            System.exit(1);
        }
    }
}
