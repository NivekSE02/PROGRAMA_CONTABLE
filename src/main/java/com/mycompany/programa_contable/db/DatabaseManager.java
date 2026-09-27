package com.mycompany.programa_contable.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private MotorBD motorActivo = MotorBD.SQLITE;

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
        Path databasePath = obtenerRutaSQLite();
        try {
            Files.createDirectories(databasePath.getParent());
            migrarBaseLegadaSiExiste(databasePath);
        } catch (Exception e) {
            throw new SQLException("No se pudo preparar el almacenamiento local: " + databasePath, e);
        }
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    private Path obtenerRutaSQLite() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path dataDirectory = localAppData != null && !localAppData.isBlank()
                ? Path.of(localAppData, "ContaNoPortable")
                : Path.of(System.getProperty("user.home"), ".contanoportable");
        return dataDirectory.resolve("contabilidad.db").toAbsolutePath();
    }

    private void migrarBaseLegadaSiExiste(Path databasePath) throws Exception {
        if (Files.exists(databasePath)) return;
        Path legacyDatabase = Path.of("contabilidad.db").toAbsolutePath();
        if (Files.isRegularFile(legacyDatabase) && !legacyDatabase.equals(databasePath)) {
            Files.copy(legacyDatabase, databasePath);
            System.out.println("[DatabaseManager] Base existente migrada a: " + databasePath);
        }
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
            throw new IllegalStateException("No se pudo abrir o preparar la base de datos local.", e);
        }
    }

    private synchronized void initDatabase(Connection conn) {
        try {
            if (!tablesExist(conn)) {
                System.out.println("[DatabaseManager] Creando tablas en " + motorActivo.getEtiqueta() + "...");
                ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "database/schema_sqlserver.sql" : "database/schema.sql");
                ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "database/data_sqlserver.sql" : "database/data.sql");
                System.out.println("[DatabaseManager] Base de datos inicializada.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo inicializar el esquema de " + motorActivo.getEtiqueta() + ".", e);
        }
    }

    public synchronized void resetDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("[DatabaseManager] Restableciendo base de datos...");
            ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "database/schema_sqlserver.sql" : "database/schema.sql");
            ejecutarScript(conn, motorActivo == MotorBD.SQL_SERVER ? "database/data_sqlserver.sql" : "database/data.sql");
            System.out.println("[DatabaseManager] Base de datos restablecida.");
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error al restablecer base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean tablesExist(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            String sql = motorActivo == MotorBD.SQL_SERVER
                ? "SELECT count(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME IN ('cuentas','productos','asientos','detalle_asiento','kardex','configuracion')"
                : "SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('cuentas','productos','asientos','detalle_asiento','kardex','configuracion')";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1) == 6;
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    private void ejecutarScript(Connection conn, String scriptName) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(scriptName)) {
            if (input == null) {
                throw new IllegalStateException("No se encontró el recurso requerido: " + scriptName);
            }
            StringBuilder script = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (!trimmed.startsWith("--") && !trimmed.isEmpty()) {
                        script.append(line).append('\n');
                    }
                }
            }
            try (Statement stmt = conn.createStatement()) {
                for (String sql : script.toString().split(";")) {
                    String cleanSql = sql.trim();
                    if (!cleanSql.isEmpty()) stmt.execute(cleanSql);
                }
            }
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
