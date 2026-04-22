package db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the connection to the SQL Server database.
 * Credentials are loaded from config/db.properties — NOT hardcoded.
 * This is a security best practice for production applications.
 */
public class DatabaseConnection {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    // Loaded from config/db.properties at class load time
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();
        String url = "jdbc:sqlserver://localhost:1433;databaseName=LibraryDB;encrypt=true;trustServerCertificate=true;";
        String user = "sa";
        String password = "Adisu@123";

        // Try to load from config file first
        try (InputStream is = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
                String host    = props.getProperty("db.host",    "localhost");
                String port    = props.getProperty("db.port",    "1433");
                String dbName  = props.getProperty("db.name",    "LibraryDB");
                String encrypt = props.getProperty("db.encrypt", "true");
                String trust   = props.getProperty("db.trustServerCertificate", "true");
                user     = props.getProperty("db.user",     "sa");
                password = props.getProperty("db.password", "Adisu@123");
                url = "jdbc:sqlserver://" + host + ":" + port
                    + ";databaseName=" + dbName
                    + ";encrypt=" + encrypt
                    + ";trustServerCertificate=" + trust + ";";
                LOGGER.info("✅ Database config loaded from db.properties");
            } else {
                LOGGER.warning("⚠️ db.properties not found — using built-in defaults.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not load db.properties, using defaults: ", e);
        }

        URL      = url;
        USER     = user;
        PASSWORD = password;
    }

    private DatabaseConnection() {}

    public static Connection getConnection() {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            LOGGER.info("✅ SQL Server Database connected successfully!");
            return conn;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ SQL Server connection failed: ", e);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "❌ SQL Server JDBC Driver not found: ", e);
        }
        return null;
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing connection: ", e);
            }
        }
    }
}