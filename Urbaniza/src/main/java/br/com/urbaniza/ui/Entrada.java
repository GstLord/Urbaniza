package br.com.urbaniza.ui;

import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Wrapper ÚNICO de {@link Scanner} sobre a entrada padrão. Cada leitura repete a pergunta até receber
 * um valor válido. Se a entrada terminar (EOF), lança {@link EntradaEncerradaException}.
 * Nas variantes com "valor atual", ENTER vazio mantém o valor atual (útil nas atualizações).
 */
public class Entrada {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final Scanner scanner;
    private final PrintStream saida;

    /** Lê de System.in usando a codificação do console. */
    public Entrada() {
        this(System.in, System.out, codificacaoEntrada());
    }

    public Entrada(InputStream in, PrintStream saida, Charset codificacao) {
        this.scanner = new Scanner(in, codificacao);
        this.saida = saida;
    }

    /** Lê um texto obrigatório (não vazio). */
    public String lerTexto(String rotulo) {
        while (true) {
            String valor = lerLinha(rotulo + ": ");
            if (!valor.isEmpty()) {
                return valor;
            }
            saida.println("  Valor obrigatório.");
        }
    }

    /** Lê um texto; ENTER vazio mantém o valor atual. */
    public String lerTexto(String rotulo, String atual) {
        String valor = lerLinha(rotulo + " [" + atual + "]: ");
        return valor.isEmpty() ? atual : valor;
    }

    /** Lê um texto opcional; ENTER vazio devolve string vazia. */
    public String lerTextoOpcional(String rotulo) {
        return lerLinha(rotulo + " (opcional): ");
    }

    /** Lê um inteiro no intervalo [min, max]. */
    public int lerInteiro(String rotulo, int min, int max) {
        while (true) {
            Integer valor = converterInteiro(lerLinha(rotulo + ": "), min, max);
            if (valor != null) {
                return valor;
            }
        }
    }

    /** Lê um inteiro no intervalo [min, max]; ENTER vazio mantém o valor atual. */
    public int lerInteiro(String rotulo, int min, int max, int atual) {
        while (true) {
            String texto = lerLinha(rotulo + " [" + atual + "]: ");
            if (texto.isEmpty()) {
                return atual;
            }
            Integer valor = converterInteiro(texto, min, max);
            if (valor != null) {
                return valor;
            }
        }
    }

    /** Lê um número decimal &gt;= min (aceita "1.234,56", "1234,56" e "1234.56"). */
    public double lerDouble(String rotulo, double min) {
        while (true) {
            Double valor = converterDouble(lerLinha(rotulo + ": "), min);
            if (valor != null) {
                return valor;
            }
        }
    }

    /** Lê um número decimal &gt;= min; ENTER vazio mantém o valor atual. */
    public double lerDouble(String rotulo, double min, double atual) {
        while (true) {
            String texto = lerLinha(rotulo + " [" + Formatador.decimal(atual) + "]: ");
            if (texto.isEmpty()) {
                return atual;
            }
            Double valor = converterDouble(texto, min);
            if (valor != null) {
                return valor;
            }
        }
    }

    /** Lê uma data dd/MM/aaaa; ENTER vazio devolve null. */
    public LocalDate lerData(String rotulo) {
        while (true) {
            String texto = lerLinha(rotulo + " (dd/MM/aaaa ou vazio): ");
            if (texto.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(texto, FORMATO_DATA);
            } catch (DateTimeParseException e) {
                saida.println("  Data inválida. Use o formato dd/MM/aaaa (ex.: 10/03/2025).");
            }
        }
    }

    /** Lê uma data obrigatória dd/MM/aaaa. */
    public LocalDate lerDataObrigatoria(String rotulo) {
        while (true) {
            LocalDate data = lerData(rotulo);
            if (data != null) {
                return data;
            }
            saida.println("  Data obrigatória.");
        }
    }

    /** Lê uma resposta s/n (aceita também sim/não). */
    public boolean lerSimNao(String pergunta) {
        while (true) {
            String texto = lerLinha(pergunta + " (s/n): ").toLowerCase(Locale.ROOT);
            switch (texto) {
                case "s", "sim" -> {
                    return true;
                }
                case "n", "nao", "não" -> {
                    return false;
                }
                default -> saida.println("  Responda s ou n.");
            }
        }
    }

    /** Lê uma resposta s/n; ENTER vazio mantém o valor atual. */
    public boolean lerSimNao(String pergunta, boolean atual) {
        while (true) {
            String texto = lerLinha(pergunta + " (s/n) [" + (atual ? "s" : "n") + "]: ").toLowerCase(Locale.ROOT);
            switch (texto) {
                case "" -> {
                    return atual;
                }
                case "s", "sim" -> {
                    return true;
                }
                case "n", "nao", "não" -> {
                    return false;
                }
                default -> saida.println("  Responda s ou n.");
            }
        }
    }

    /** Lê um identificador (inteiro positivo). */
    public long lerId(String rotulo) {
        while (true) {
            String texto = lerLinha(rotulo + ": ");
            try {
                long id = Long.parseLong(texto);
                if (id > 0) {
                    return id;
                }
            } catch (NumberFormatException e) {
                // cai na mensagem abaixo
            }
            saida.println("  Informe um id numérico positivo.");
        }
    }

    private String lerLinha(String prompt) {
        saida.print(prompt);
        saida.flush();
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException | IllegalStateException e) {
            throw new EntradaEncerradaException();
        }
    }

    private Integer converterInteiro(String texto, int min, int max) {
        try {
            int valor = Integer.parseInt(texto.replace(".", ""));
            if (valor >= min && valor <= max) {
                return valor;
            }
        } catch (NumberFormatException e) {
            // cai na mensagem abaixo
        }
        saida.println("  Informe um número inteiro entre " + min + " e " + max + ".");
        return null;
    }

    private Double converterDouble(String texto, double min) {
        try {
            double valor = converterNumeroBrasileiro(texto);
            if (Double.isFinite(valor) && valor >= min) {
                return valor;
            }
        } catch (NumberFormatException e) {
            // cai na mensagem abaixo
        }
        saida.println("  Informe um número maior ou igual a " + Formatador.decimal(min) + " (ex.: 1.234,56).");
        return null;
    }

    /**
     * Converte "1.234,56", "1234,56", "1234.56", "180.000" e "2.000.000" para double.
     * Com vírgula, os pontos são separadores de milhar. Sem vírgula, pontos seguidos de grupos de
     * exatamente 3 dígitos ("180.000") são separadores de milhar; caso contrário o ponto é decimal ("7.5").
     */
    static double converterNumeroBrasileiro(String texto) {
        String t = texto.trim().replace(" ", "");
        if (t.isEmpty()) {
            throw new NumberFormatException("vazio");
        }
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        } else if (t.matches("\\d{1,3}(\\.\\d{3})+")) {
            t = t.replace(".", "");
        }
        return Double.parseDouble(t);
    }

    /** Codificação da entrada: stdin.encoding (JDK 25+), console ou padrão da JVM. */
    private static Charset codificacaoEntrada() {
        String propriedade = System.getProperty("stdin.encoding");
        if (propriedade != null) {
            try {
                return Charset.forName(propriedade);
            } catch (IllegalArgumentException e) {
                // usa as alternativas abaixo
            }
        }
        Console console = System.console();
        return console != null ? console.charset() : Charset.defaultCharset();
    }
}
