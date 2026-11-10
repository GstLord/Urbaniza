package br.com.urbaniza.ui;

import java.util.List;

/**
 * Menu principal da aplicação.
 */
public class MenuPrincipal extends Menu {

    public MenuPrincipal(Entrada entrada) {
        super(entrada);
    }

    @Override
    protected String titulo() {
        return "URBANIZA — MENU PRINCIPAL";
    }

    @Override
    protected String rotuloSaida() {
        return "Sair";
    }

    @Override
    protected List<OpcaoMenu> opcoes() {
        return List.of(
                new OpcaoMenu("Regiões", () -> new MenuRegiao(entrada).exibir()),
                new OpcaoMenu("Loteamentos", () -> new MenuLoteamento(entrada).exibir()),
                new OpcaoMenu("Equipamentos comunitários", () -> new MenuEquipamento(entrada).exibir()),
                new OpcaoMenu("Analistas", () -> new MenuAnalista(entrada).exibir()),
                new OpcaoMenu("Critérios de vedação", () -> new MenuCriterio(entrada).exibir()),
                new OpcaoMenu("Análises de parcelamento", () -> new MenuAnalise(entrada).exibir()));
    }
}
