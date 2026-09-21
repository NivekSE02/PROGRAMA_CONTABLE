package com.mycompany.programa_contable.dao;

import com.mycompany.programa_contable.db.DatabaseManager;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class ConfiguracionDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public Map<String, String> obtenerConfiguracion() {
        Map<String, String> config = new HashMap<>();
        String sql = "SELECT clave, valor FROM configuracion";
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                config.put(rs.getString("clave"), rs.getString("valor"));
            }
        } catch (SQLException e) {
            System.err.println("[ConfiguracionDAO] Error al obtener configuración: " + e.getMessage());
        }
        return config;
    }

    public void guardarConfiguracion(Map<String, String> valores) {
        String sql = "INSERT OR REPLACE INTO configuracion (clave, valor) VALUES (?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, String> entry : valores.entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setString(2, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            System.err.println("[ConfiguracionDAO] Error al guardar configuración: " + e.getMessage());
        }
    }
}
