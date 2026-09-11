package com.mycompany.programa_contable.dao;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.DetalleAsiento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class LibroDiarioDAO {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public int obtenerSiguienteNumeroAsiento() {
        String sql = "SELECT COALESCE(MAX(numero), 0) + 1 FROM asientos";
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[LibroDiarioDAO] Error obteniendo número de asiento: " + e.getMessage());
        }
        return 1;
    }

    /**
     * Registra un nuevo asiento contable con atomicidad (transacción ACID)
     * y validación estricta de Partida Doble (Total Debe == Total Haber).
     */
    public boolean registrarAsiento(Asiento asiento) throws IllegalArgumentException, SQLException {
        // Validación obligatoria requerida por la guía:
        if (!asiento.isPartidaDobleValida()) {
            throw new IllegalArgumentException(
                "Validación Obligatoria Fallida: El asiento no cumple la Partida Doble. " +
                "Debe: $" + String.format("%.2f", asiento.getTotalDebe()) +
                ", Haber: $" + String.format("%.2f", asiento.getTotalHaber()) +
                ", Diferencia: $" + String.format("%.2f", asiento.getDiferencia())
            );
        }

        String sqlAsiento = "INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber, usuario_id) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_asiento (asiento_id, renglon, cuenta_codigo, concepto_linea, debe, haber) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Transacción atómica

            int asientoId = 0;
            try (PreparedStatement psAsiento = conn.prepareStatement(sqlAsiento, Statement.RETURN_GENERATED_KEYS)) {
                psAsiento.setInt(1, asiento.getNumero());
                psAsiento.setString(2, asiento.getFecha());
                psAsiento.setString(3, asiento.getConcepto());
                psAsiento.setDouble(4, asiento.getTotalDebe());
                psAsiento.setDouble(5, asiento.getTotalHaber());
                if (asiento.getUsuarioId() > 0) {
                    psAsiento.setInt(6, asiento.getUsuarioId());
                } else {
                    psAsiento.setNull(6, java.sql.Types.INTEGER);
                }

                psAsiento.executeUpdate();
                try (ResultSet rsKeys = psAsiento.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        asientoId = rsKeys.getInt(1);
                        asiento.setId(asientoId);
                    }
                }
            }

            if (asientoId <= 0) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement psDet = conn.prepareStatement(sqlDetalle)) {
                int renglon = 1;
                for (DetalleAsiento det : asiento.getDetalles()) {
                    psDet.setInt(1, asientoId);
                    psDet.setInt(2, renglon++);
                    psDet.setString(3, det.getCuentaCodigo());
                    psDet.setString(4, det.getConceptoLinea() != null ? det.getConceptoLinea() : asiento.getConcepto());
                    psDet.setDouble(5, det.getDebe());
                    psDet.setDouble(6, det.getHaber());
                    psDet.addBatch();
                }
                psDet.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            System.err.println("[LibroDiarioDAO] Error al registrar asiento: " + e.getMessage());
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {}
            }
        }
    }

    public List<Asiento> listarAsientos(String fechaDesde, String fechaHasta) {
        List<Asiento> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT a.id, a.numero, a.fecha, a.concepto, a.total_debe, a.total_haber, a.usuario_id, " +
            "u.nombre_completo as usuario_nombre, a.created_at " +
            "FROM asientos a " +
            "LEFT JOIN usuarios u ON a.usuario_id = u.id " +
            "WHERE 1=1 "
        );

        if (fechaDesde != null && !fechaDesde.isEmpty()) {
            sb.append("AND a.fecha >= '").append(fechaDesde).append("' ");
        }
        if (fechaHasta != null && !fechaHasta.isEmpty()) {
            sb.append("AND a.fecha <= '").append(fechaHasta).append("' ");
        }
        sb.append("ORDER BY a.numero DESC");

        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sb.toString())) {
            while (rs.next()) {
                Asiento as = new Asiento();
                as.setId(rs.getInt("id"));
                as.setNumero(rs.getInt("numero"));
                as.setFecha(rs.getString("fecha"));
                as.setConcepto(rs.getString("concepto"));
                as.setTotalDebe(rs.getDouble("total_debe"));
                as.setTotalHaber(rs.getDouble("total_haber"));
                as.setUsuarioId(rs.getInt("usuario_id"));
                as.setUsuarioNombre(rs.getString("usuario_nombre"));
                as.setCreatedAt(rs.getString("created_at"));
                as.setDetalles(cargarDetalles(conn, as.getId()));
                lista.add(as);
            }
        } catch (SQLException e) {
            System.err.println("[LibroDiarioDAO] Error listando asientos: " + e.getMessage());
        }
        return lista;
    }

    public List<DetalleAsiento> cargarDetalles(Connection conn, int asientoId) throws SQLException {
        List<DetalleAsiento> detalles = new ArrayList<>();
        String sql = "SELECT d.id, d.asiento_id, d.renglon, d.cuenta_codigo, c.nombre as cuenta_nombre, " +
                     "d.concepto_linea, d.debe, d.haber " +
                     "FROM detalle_asiento d " +
                     "LEFT JOIN cuentas c ON d.cuenta_codigo = c.codigo " +
                     "WHERE d.asiento_id = ? ORDER BY d.renglon ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, asientoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleAsiento det = new DetalleAsiento();
                    det.setId(rs.getInt("id"));
                    det.setAsientoId(rs.getInt("asiento_id"));
                    det.setRenglon(rs.getInt("renglon"));
                    det.setCuentaCodigo(rs.getString("cuenta_codigo"));
                    det.setCuentaNombre(rs.getString("cuenta_nombre"));
                    det.setConceptoLinea(rs.getString("concepto_linea"));
                    det.setDebe(rs.getDouble("debe"));
                    det.setHaber(rs.getDouble("haber"));
                    detalles.add(det);
                }
            }
        }
        return detalles;
    }

    public boolean eliminarAsiento(int id) {
        String sql = "DELETE FROM asientos WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[LibroDiarioDAO] Error eliminando asiento: " + e.getMessage());
            return false;
        }
    }
}
