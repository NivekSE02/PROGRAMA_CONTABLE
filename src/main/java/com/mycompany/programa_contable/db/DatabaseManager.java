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
 * Gestor de persistencia SQLite para el Sistema Contable de UNICAES.
 * Inicializa automáticamente las tablas y los datos desde schema.sql y data.sql.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:contabilidad.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public synchronized void initDatabase() {
        try (Connection conn = getConnection()) {
            if (!tablesExist(conn)) {
                System.out.println("[DatabaseManager] Inicializando tablas y datos por primera vez...");
                ejecutarScript(conn, "schema.sql");
                ejecutarScript(conn, "data.sql");
                System.out.println("[DatabaseManager] Base de datos configurada exitosamente.");
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error al inicializar base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void resetDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("[DatabaseManager] Reiniciando base de datos a estado original...");
            ejecutarScript(conn, "schema.sql");
            ejecutarScript(conn, "data.sql");
            System.out.println("[DatabaseManager] Base de datos reiniciada con éxito.");
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error al reiniciar base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean tablesExist(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='cuentas'")) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
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
                System.err.println("[DatabaseManager] No se pudo encontrar el script: " + scriptName);
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
                        stmt.execute(cleanSql);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Error ejecutando script " + scriptName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
