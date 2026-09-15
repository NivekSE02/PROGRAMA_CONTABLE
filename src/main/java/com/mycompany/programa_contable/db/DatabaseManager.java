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

/**
 * Gestor de persistencia con soporte para:
 * 1. Microsoft SQL Server (Base de Datos Real Corporativa: contabilidad_db)
 * 2. SQLite (Soporte portable local automático)
 */
public class DatabaseManager {

    public enum MotorBD {
        SQL_SERVER("Microsoft SQL Server (contabilidad_db)"),
        SQLITE("SQLite Embebido (contabilidad.db)");

        private final String etiqueta;
        MotorBD(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
    }

    private static DatabaseManager instance;

    // Configuración SQL Server por defecto
    private String mssqlHost = "localhost";
    private int mssqlPort = 1433;
    private String mssqlDatabase = "Sistema_Contable";
    private String mssqlUser = "conta_user";
    private String mssqlPassword = "Conta2026*!";

    private MotorBD motorActivo = MotorBD.SQL_SERVER;

    private DatabaseManager() {
        // Directly use SQL Server; no fallback to SQLite
        // Connection will be attempted on first getConnection call
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    // Removed fallback logic; the manager now assumes SQL Server is available.
// If connection fails, an exception will be propagated.


    public Connection getConnection() throws SQLException {
        // Always connect to SQL Server
        return crearConexionSQLServer(mssqlHost, mssqlPort, mssqlDatabase, mssqlUser, mssqlPassword);
    }

    private Connection crearConexionSQLServer(String host, int port, String db, String user, String pass) throws SQLException {
        String url =
            "jdbc:sqlserver://localhost:1433;" +
            "databaseName=Sistema_Contable;" +
            "encrypt=true;" +
            "trustServerCertificate=true;" +
            "loginTimeout=5;";

        return DriverManager.getConnection(
            url,
            "conta_user",
            "Conta2026*!"
        );
    }

    public boolean probarYCambiarConexionSQLServer(String host, int port, String db, String user, String pass) {
        try (Connection conn = crearConexionSQLServer(host, port, db, user, pass)) {
            if (conn != null && !conn.isClosed()) {
                this.mssqlHost = host;
                this.mssqlPort = port;
                this.mssqlDatabase = db;
                this.mssqlUser = user;
                this.mssqlPassword = pass;
                this.motorActivo = MotorBD.SQL_SERVER;
                initDatabase(conn);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Prueba de conexión SQL Server falló: " + e.getMessage());
        }
        return false;
    }

    public synchronized void initDatabase() {
        // Reset database to load only the minimal catalog
        resetDatabase();
    }

    private synchronized void initDatabase(Connection conn) {
        try {
            if (!tablesExist(conn)) {
                System.out.println("[DatabaseManager] Creando tablas en " + motorActivo.getEtiqueta() + "...");
                ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "schema_sqlserver.sql" : "schema.sql");
                ejecutarScript(conn, "data.sql");
                System.out.println("[DatabaseManager] Base de datos inicializada exitosamente.");
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error al inicializar tablas: " + e.getMessage());
        }
    }

    public synchronized void resetDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("[DatabaseManager] Restableciendo base de datos a estado original...");
            ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "schema_sqlserver.sql" : "schema.sql");
            ejecutarScript(conn, "data.sql");
            
            // Add default user
            try (Statement s = conn.createStatement()) {
                if (motorActivo == MotorBD.SQL_SERVER) {
                    s.execute("IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id = 1) BEGIN SET IDENTITY_INSERT usuarios ON; INSERT INTO usuarios (id, username, password, nombre_completo, rol, estado) VALUES (1, 'admin', 'admin', 'Administrador del Sistema', 'ADMINISTRADOR', 'ACTIVO'); SET IDENTITY_INSERT usuarios OFF; END");
                }
            }
            
            System.out.println("[DatabaseManager] Base de datos restablecida con éxito.");
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error al reiniciar base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean tablesExist(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            if (motorActivo == MotorBD.SQL_SERVER) {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='cuentas'")) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            } else {
                try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='cuentas'")) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
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
                if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                    continue;
                }
                sb.append(line).append("\n");
            }
            reader.close();

            String[] statements = sb.toString().split(";");
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    String cleanSql = sql.trim();
                    if (!cleanSql.isEmpty()) {
                        try {
                            stmt.execute(cleanSql);
                        } catch (SQLException ex) {
                            // Ignorar errores menores en scripts de reinicio (como drop table inexistente)
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error ejecutando script " + scriptName + ": " + e.getMessage());
        }
    }

    public MotorBD getMotorActivo() { return motorActivo; }
    public String getMssqlHost() { return mssqlHost; }
    public int getMssqlPort() { return mssqlPort; }
    public String getMssqlDatabase() { return mssqlDatabase; }
    public String getMssqlUser() { return mssqlUser; }
}
