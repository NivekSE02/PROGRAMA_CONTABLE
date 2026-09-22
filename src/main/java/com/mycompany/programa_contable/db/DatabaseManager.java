package com.mycompany.programa_contable.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    public enum MotorBD {
        SQL_SERVER("Microsoft SQL Server (Sistema_Contable)"),
        SQLITE("SQLite (contabilidad.db)");

        private final String etiqueta;
        MotorBD(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
    }

    private static DatabaseManager instance;

    private String mssqlHost     = "localhost";
    private int    mssqlPort     = 1433;
    private String mssqlDatabase = "Sistema_Contable";
    private String mssqlUser     = "conta_user";
    private String mssqlPassword = "Conta2026*!";

    // Cambiar a SQL_SERVER para usar la base de datos corporativa en desarrollo
    private MotorBD motorActivo = MotorBD.SQL_SERVER;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (motorActivo == MotorBD.SQLITE) {
            return crearConexionSQLite();
        }
        return crearConexionSQLServer(mssqlHost, mssqlPort, mssqlDatabase, mssqlUser, mssqlPassword);
    }

    private Connection crearConexionSQLite() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:contabilidad.db");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private Connection crearConexionSQLServer(String host, int port, String db, String user, String pass) throws SQLException {
        String url = "jdbc:sqlserver://" + host + ":" + port + ";"
                   + "databaseName=" + db + ";"
                   + "encrypt=true;"
                   + "trustServerCertificate=true;"
                   + "loginTimeout=5;";
        return DriverManager.getConnection(url, user, pass);
    }

    public boolean probarYCambiarConexionSQLServer(String host, int port, String db, String user, String pass) {
        try (Connection conn = crearConexionSQLServer(host, port, db, user, pass)) {
            if (conn != null && !conn.isClosed()) {
                this.mssqlHost     = host;
                this.mssqlPort     = port;
                this.mssqlDatabase = db;
                this.mssqlUser     = user;
                this.mssqlPassword = pass;
                this.motorActivo   = MotorBD.SQL_SERVER;
                initDatabase(conn);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Prueba de conexión SQL Server falló: " + e.getMessage());
        }
        return false;
    }

    public synchronized void initDatabase() {
        try (Connection conn = getConnection()) {
            initDatabase(conn);
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error al inicializar base de datos: " + e.getMessage());
        }
    }

    private synchronized void initDatabase(Connection conn) {
        try {
            if (!tablesExist(conn)) {
                System.out.println("[DatabaseManager] Creando tablas en " + motorActivo.getEtiqueta() + "...");
                ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "schema_sqlserver.sql" : "schema.sql");
                ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "data_sqlserver.sql" : "data.sql");
                System.out.println("[DatabaseManager] Base de datos inicializada.");
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error al inicializar tablas: " + e.getMessage());
        }
    }

    public synchronized void resetDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("[DatabaseManager] Restableciendo base de datos...");
            ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "schema_sqlserver.sql" : "schema.sql");
            ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "data_sqlserver.sql" : "data.sql");
            System.out.println("[DatabaseManager] Base de datos restablecida.");
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error al restablecer base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean tablesExist(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String sql = motorActivo == MotorBD.SQL_SERVER
                ? "SELECT count(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='cuentas'"
                : "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='cuentas'";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    private void ejecutarScript(Connection conn, String scriptName) {
        try {
            BufferedReader reader = null;
            File file = new File(scriptName);
            if (file.exists()) {
                reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
            } else {
                InputStream is = getClass().getClassLoader().getResourceAsStream(scriptName);
                if (is != null) {
                    reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                }
            }

            if (reader == null) {
                System.err.println("[DatabaseManager] No se encontró el script: " + scriptName);
                return;
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.isEmpty()) continue;
                sb.append(line).append("\n");
            }
            reader.close();

            try (Statement stmt = conn.createStatement()) {
                for (String sql : sb.toString().split(";")) {
                    String cleanSql = sql.trim();
                    if (!cleanSql.isEmpty()) {
                        try {
                            stmt.execute(cleanSql);
                        } catch (SQLException ex) {
                            // Continuar ante errores no críticos (DROP en BD vacía, etc.)
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error ejecutando script " + scriptName + ": " + e.getMessage());
        }
    }

    public MotorBD getMotorActivo()  { return motorActivo; }
    public void activarSQLite()      { this.motorActivo = MotorBD.SQLITE; }
    public void activarSQLServer()   { this.motorActivo = MotorBD.SQL_SERVER; }
    public String getMssqlHost()     { return mssqlHost; }
    public int    getMssqlPort()     { return mssqlPort; }
    public String getMssqlDatabase() { return mssqlDatabase; }
    public String getMssqlUser()     { return mssqlUser; }
}
