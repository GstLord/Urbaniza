package br.com.urbaniza.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatação de valores para exibição no padrão brasileiro (vírgula decimal, 2 casas).
 * {@link Double#POSITIVE_INFINITY} é exibido como "sem demanda" ou "não aplicável".
 */
public final class Formatador {

    private static final Locale LOCALE_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatador() {
    }

    /** Ex.: 1234.5 -&gt; "1.234,50". */
    public static String decimal(double valor) {
        if (Double.isInfinite(valor)) {
            return "não aplicável";
        }
        return String.format(LOCALE_BR, "%,.2f", valor);
    }

    /** Ex.: 20400 -&gt; "20.400". */
    public static String inteiro(long valor) {
        return String.format(LOCALE_BR, "%,d", valor);
    }

    /** Fração 0..1 em percentual. Ex.: 0.45 -&gt; "45,00%". */
    public static String percentual(double fracao) {
        return decimal(fracao * 100) + "%";
    }

    /** Valor já em percentual. Ex.: 34 -&gt; "34,00%". */
    public static String percentualDireto(double valor) {
        return decimal(valor) + "%";
    }

    /** Índice de atendimento (RN11). Infinito (demanda zero) -&gt; "sem demanda". */
    public static String indiceAtendimento(double indice) {
        return Double.isInfinite(indice) ? "sem demanda" : decimal(indice);
    }

    /** IAV (RN08). Infinito (população projetada zero) -&gt; "não aplicável". */
    public static String iav(double iav) {
        return Double.isInfinite(iav) ? "não aplicável" : decimal(iav) + " m²/hab";
    }

    public static String data(LocalDate data) {
        return data == null ? "—" : data.format(DATA);
    }

    public static String dataHora(LocalDateTime dataHora) {
        return dataHora == null ? "—" : dataHora.format(DATA_HORA);
    }

    public static String simNao(boolean valor) {
        return valor ? "sim" : "não";
    }
}
