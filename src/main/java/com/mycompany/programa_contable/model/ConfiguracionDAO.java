package com.mycompany.programa_contable.model;

import com.mycompany.programa_contable.db.DatabaseManager;
import java.sql.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ConfiguracionDAO {
    private static final String CLAVE_TASA_IVA = "tasa_iva";
    private static final String CLAVE_MODALIDAD_IVA = "modalidad_iva";
    private static final double TASA_IVA_POR_DEFECTO = 0.13;
    public static final String IVA_INCLUIDO = "INCLUIDO";
    public static final String IVA_MAS_IVA = "MAS_IVA";
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

    /** Devuelve la tasa vigente para asientos nuevos; 0.13 conserva el valor histórico por defecto. */
    public double obtenerTasaIva() {
        String valor = obtenerConfiguracion().get(CLAVE_TASA_IVA);
        if (valor == null || valor.isBlank()) return TASA_IVA_POR_DEFECTO;
        try {
            double tasa = Double.parseDouble(valor);
            return tasa >= 0.0 && tasa <= 1.0 ? tasa : TASA_IVA_POR_DEFECTO;
        } catch (NumberFormatException e) {
            return TASA_IVA_POR_DEFECTO;
        }
    }

    /** Guarda la tasa como decimal (por ejemplo, 0.13 para 13 %). */
    public void guardarTasaIva(double tasa) throws SQLException {
        if (tasa < 0.0 || tasa > 1.0) {
            throw new IllegalArgumentException("La tasa de IVA debe estar entre 0 % y 100 %.");
        }
        String valor = BigDecimal.valueOf(tasa).stripTrailingZeros().toPlainString();
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE configuracion SET valor = ? WHERE clave = ?")) {
                update.setString(1, valor);
                update.setString(2, CLAVE_TASA_IVA);
                int actualizadas = update.executeUpdate();
                if (actualizadas == 0) {
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO configuracion (clave, valor) VALUES (?, ?)")) {
                        insert.setString(1, CLAVE_TASA_IVA);
                        insert.setString(2, valor);
                        insert.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /** Modalidad vigente solo para renglones de asientos que se agreguen desde este momento. */
    public String obtenerModalidadIva() {
        String modalidad = obtenerConfiguracion().get(CLAVE_MODALIDAD_IVA);
        return IVA_MAS_IVA.equals(modalidad) ? IVA_MAS_IVA : IVA_INCLUIDO;
    }

    public void guardarModalidadIva(String modalidad) throws SQLException {
        if (!IVA_INCLUIDO.equals(modalidad) && !IVA_MAS_IVA.equals(modalidad)) {
            throw new IllegalArgumentException("Modalidad de IVA inválida.");
        }
        guardarValor(CLAVE_MODALIDAD_IVA, modalidad);
    }

    private void guardarValor(String clave, String valor) throws SQLException {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement update = conn.prepareStatement(
                    "UPDATE configuracion SET valor = ? WHERE clave = ?")) {
                update.setString(1, valor);
                update.setString(2, clave);
                int actualizadas = update.executeUpdate();
                if (actualizadas == 0) {
                    try (PreparedStatement insert = conn.prepareStatement(
                            "INSERT INTO configuracion (clave, valor) VALUES (?, ?)")) {
                        insert.setString(1, clave);
                        insert.setString(2, valor);
                        insert.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
