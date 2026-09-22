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
        String sqlUpdate = "UPDATE configuracion SET valor = ? WHERE clave = ?";
        String sqlInsert = "INSERT INTO configuracion (clave, valor) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM configuracion WHERE clave = ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement psU = conn.prepareStatement(sqlUpdate);
             PreparedStatement psI = conn.prepareStatement(sqlInsert)) {
            for (Map.Entry<String, String> entry : valores.entrySet()) {
                psU.setString(1, entry.getValue());
                psU.setString(2, entry.getKey());
                psU.addBatch();
                psI.setString(1, entry.getKey());
                psI.setString(2, entry.getValue());
                psI.setString(3, entry.getKey());
                psI.addBatch();
            }
            psU.executeBatch();
            psI.executeBatch();
        } catch (SQLException e) {
            System.err.println("[ConfiguracionDAO] Error al guardar configuración: " + e.getMessage());
        }
    }
}
