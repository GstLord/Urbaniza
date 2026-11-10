package br.com.urbaniza.ui;

/**
 * Uma opção de menu: rótulo exibido e ação executada.
 *
 * @param rotulo texto da opção
 * @param acao   ação executada quando a opção é escolhida
 */
public record OpcaoMenu(String rotulo, Runnable acao) {
}
