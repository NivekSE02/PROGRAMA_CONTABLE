package com.mycompany.programa_contable.dao;

import com.mycompany.programa_contable.db.DatabaseManager;
import java.sql.*;

public class ProductoDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public double obtenerCostoCompraActual() {
        String sql = "SELECT COALESCE(costo_compra, 8.85) FROM productos WHERE id = 1";
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error obteniendo costo: " + e.getMessage());
        }
        return 8.85; // Valor por defecto
    }

    public void actualizarProductoConfig(String nombre, double costo, double precio) {
        String sql = "UPDATE productos SET nombre = ?, costo_compra = ?, precio_venta = ? WHERE id = 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre);
            ps.setDouble(2, costo);
            ps.setDouble(3, precio);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ProductoDAO] Error actualizando producto: " + e.getMessage());
        }
    }
}