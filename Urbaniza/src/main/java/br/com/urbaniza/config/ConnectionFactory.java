package br.com.urbaniza.config;

import br.com.urbaniza.exception.PersistenciaException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fábrica de conexões JDBC com o PostgreSQL.
 * <p>
 * Ordem de prioridade da configuração:
 * <ol>
 *   <li>variáveis de ambiente URBANIZA_DB_URL, URBANIZA_DB_USER, URBANIZA_DB_PASSWORD;</li>
 *   <li>arquivo db.properties do classpath (chaves db.url, db.user, db.password).</li>
 * </ol>
 */
public final class ConnectionFactory {

    private static final String ARQUIVO_PROPRIEDADES = "db.properties";

    private static final String ENV_URL = "URBANIZA_DB_URL";
    private static final String ENV_USER = "URBANIZA_DB_USER";
    private static final String ENV_PASSWORD = "URBANIZA_DB_PASSWORD";

    private static final String PROP_URL = "db.url";
    private static final String PROP_USER = "db.user";
    private static final String PROP_PASSWORD = "db.password";

    private static final int TIMEOUT_LOGIN_SEGUNDOS = 5;

    /** Propriedades carregadas uma única vez (lazy, thread-safe via holder). */
    private static final class PropriedadesHolder {
        private static final Properties PROPRIEDADES = carregarPropriedades();
    }

    private ConnectionFactory() {
    }

    /**
     * Abre uma nova conexão com o banco.
     *
     * @return conexão aberta (o chamador deve fechá-la, de preferência com try-with-resources)
     * @throws PersistenciaException se não for possível conectar
     */
    public static Connection getConnection() {
        String url = obterValor(ENV_URL, PROP_URL);
        String usuario = obterValor(ENV_USER, PROP_USER);
        String senha = obterValor(ENV_PASSWORD, PROP_PASSWORD);
        if (url == null || url.isBlank()) {
            throw new PersistenciaException("URL do banco não configurada (defina " + ENV_URL
                    + " ou a chave " + PROP_URL + " em " + ARQUIVO_PROPRIEDADES + ").");
        }
        try {
            DriverManager.setLoginTimeout(TIMEOUT_LOGIN_SEGUNDOS);
            return DriverManager.getConnection(url, usuario, senha);
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível conectar ao banco de dados em " + url
                    + ": " + e.getMessage(), e);
        }
    }

    /**
     * Testa a conexão abrindo-a e executando "SELECT 1". Nunca lança exceção.
     *
     * @return true se o banco respondeu corretamente
     */
    public static boolean testarConexao() {
        try (Connection conexao = getConnection();
             PreparedStatement ps = conexao.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException | RuntimeException e) {
            return false;
        }
    }

    private static String obterValor(String variavelAmbiente, String chavePropriedade) {
        String valor = System.getenv(variavelAmbiente);
        if (valor != null && !valor.isBlank()) {
            return valor;
        }
        return PropriedadesHolder.PROPRIEDADES.getProperty(chavePropriedade);
    }

    private static Properties carregarPropriedades() {
        Properties propriedades = new Properties();
        try (InputStream entrada = ConnectionFactory.class.getClassLoader()
                .getResourceAsStream(ARQUIVO_PROPRIEDADES)) {
            if (entrada != null) {
                propriedades.load(entrada);
            }
        } catch (IOException e) {
            throw new PersistenciaException("Falha ao ler " + ARQUIVO_PROPRIEDADES + ": " + e.getMessage(), e);
        }
        return propriedades;
    }
}
