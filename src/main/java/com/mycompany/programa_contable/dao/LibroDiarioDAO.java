package com.mycompany.programa_contable.dao;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.DetalleAsiento;
import com.mycompany.programa_contable.service.KardexService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    public List<Asiento> listarAsientos(String fechaDesde, String fechaHasta) {
        List<Asiento> lista = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT a.id, a.numero, a.fecha, a.concepto, a.total_debe, a.total_haber, a.created_at " +
            "FROM asientos a " +
            "WHERE 1=1 "
        );

        if (fechaDesde != null && !fechaDesde.isEmpty()) {
            sb.append("AND a.fecha >= ? ");
        }
        if (fechaHasta != null && !fechaHasta.isEmpty()) {
            sb.append("AND a.fecha <= ? ");
        }
        sb.append("ORDER BY a.fecha DESC, a.numero DESC");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            int parametro = 1;
            if (fechaDesde != null && !fechaDesde.isEmpty()) ps.setString(parametro++, fechaDesde);
            if (fechaHasta != null && !fechaHasta.isEmpty()) ps.setString(parametro, fechaHasta);

            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Asiento as = new Asiento();
                as.setId(rs.getInt("id"));
                as.setNumero(rs.getInt("numero"));
                as.setFecha(rs.getString("fecha"));
                as.setConcepto(rs.getString("concepto"));
                as.setTotalDebe(rs.getDouble("total_debe"));
                as.setTotalHaber(rs.getDouble("total_haber"));
                as.setCreatedAt(rs.getString("created_at"));
                lista.add(as);
            }
            }
            cargarDetallesLote(conn, lista);
        } catch (SQLException e) {
            System.err.println("[LibroDiarioDAO] Error listando asientos: " + e.getMessage());
        }
        return lista;
    }

    private void cargarDetallesLote(Connection conn, List<Asiento> asientos) throws SQLException {
        final int tamanoLote = 500;
        for (int inicio = 0; inicio < asientos.size(); inicio += tamanoLote) {
            int fin = Math.min(inicio + tamanoLote, asientos.size());
            List<Asiento> lote = asientos.subList(inicio, fin);
            Map<Integer, Asiento> porId = new HashMap<>();
            for (Asiento asiento : lote) porId.put(asiento.getId(), asiento);

            String marcadores = String.join(",", Collections.nCopies(lote.size(), "?"));
            String sql = "SELECT d.id, d.asiento_id, d.renglon, d.cuenta_codigo, c.nombre AS cuenta_nombre, "
                    + "d.concepto_linea, d.debe, d.haber FROM detalle_asiento d "
                    + "LEFT JOIN cuentas c ON d.cuenta_codigo = c.codigo "
                    + "WHERE d.asiento_id IN (" + marcadores + ") ORDER BY d.asiento_id, d.renglon";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < lote.size(); i++) ps.setInt(i + 1, lote.get(i).getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Asiento asiento = porId.get(rs.getInt("asiento_id"));
                        if (asiento == null) continue;
                        DetalleAsiento detalle = new DetalleAsiento();
                        detalle.setId(rs.getInt("id"));
                        detalle.setAsientoId(rs.getInt("asiento_id"));
                        detalle.setRenglon(rs.getInt("renglon"));
                        detalle.setCuentaCodigo(rs.getString("cuenta_codigo"));
                        detalle.setCuentaNombre(rs.getString("cuenta_nombre"));
                        detalle.setConceptoLinea(rs.getString("concepto_linea"));
                        detalle.setDebe(rs.getDouble("debe"));
                        detalle.setHaber(rs.getDouble("haber"));
                        asiento.getDetalles().add(detalle);
                    }
                }
            }
        }
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

    public boolean registrarAsiento(Asiento asiento) throws IllegalArgumentException, SQLException {
        double costoDinamico = 0.0;
        double precioVentaDinamico = 0.0;
        
        String sqlProd = "SELECT costo_compra, precio_venta FROM productos WHERE id = 1";
        try (Connection connCheck = dbManager.getConnection();
             Statement stC = connCheck.createStatement(); 
             ResultSet rsC = stC.executeQuery(sqlProd)) {
            if (rsC.next()) {
                costoDinamico = rsC.getDouble("costo_compra");
                precioVentaDinamico = rsC.getDouble("precio_venta");
            }
        } catch (Exception e) {
            throw new SQLException("Error crítico al consultar los parámetros del producto en la base de datos: " + e.getMessage());
        }

        if (costoDinamico <= 0 || precioVentaDinamico <= 0) {
            throw new SQLException("Error crítico de configuración: El costo de compra y/o el precio de venta en la tabla productos no están configurados o son inválidos.");
        }


        // Validación obligatoria requerida por la guía:
        if (!asiento.isPartidaDobleValida()) {
            throw new IllegalArgumentException(
                "Validación Obligatoria Fallida: El asiento no cumple la Partida Doble. " +
                "Debe: $" + String.format("%.2f", asiento.getTotalDebe()) +
                ", Haber: $" + String.format("%.2f", asiento.getTotalHaber()) +
                ", Diferencia: $" + String.format("%.2f", asiento.getDiferencia())
            );
        }

        String sqlAsiento = "INSERT INTO asientos (numero, fecha, concepto, total_debe, total_haber) VALUES (?, ?, ?, ?, ?)";
        String sqlDetalle = "INSERT INTO detalle_asiento (asiento_id, renglon, cuenta_codigo, concepto_linea, debe, haber) VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);

            int asientoId = 0;
            try (PreparedStatement psAsiento = conn.prepareStatement(sqlAsiento, Statement.RETURN_GENERATED_KEYS)) {
                psAsiento.setInt(1, asiento.getNumero());
                psAsiento.setString(2, asiento.getFecha());
                psAsiento.setString(3, asiento.getConcepto());
                psAsiento.setDouble(4, asiento.getTotalDebe());
                psAsiento.setDouble(5, asiento.getTotalHaber());

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

            KardexService kardexService = new KardexService();

            for (DetalleAsiento det : asiento.getDetalles()) {
                String cuenta = det.getCuentaCodigo();

                // Una línea de la cuenta de inventario (1.2 o una subcuenta)
                // con saldo deudor representa una entrada valorizada al kardex.
                // La UI guarda el importe en 1.2 y la subcuenta solo como parcial.
                boolean esInventarioInicial = esSubcuentaDe(cuenta, "1.2") && det.getDebe() > 0;
                
                if (esInventarioInicial) {
                    // Tomamos el valor del Debe si existe, o del Parcial si la cuenta principal agrupó el monto
                    double valorInventario = det.getDebe();
                    if (valorInventario <= 0 && det.getConceptoLinea() != null) {
                        try {
                            valorInventario = Double.parseDouble(det.getConceptoLinea().replace("$", "").replace(",", "").trim());
                        } catch (Exception ignored) {}
                    }

                    if (valorInventario > 0) {
                        int cantidadInicial = (int) Math.round(valorInventario / costoDinamico);
                        if (cantidadInicial <= 0) cantidadInicial = 1;
                        
                        kardexService.registrarMovimientoConConexión(
                            conn, 1, asiento.getFecha(), "ENTRADA", cantidadInicial,
                            valorInventario, false, asientoId
                        );
                        break; 
                    }
                }
                
                // Compras: cuenta 5.4 al Debe
                else if (esSubcuentaDe(cuenta, "5.4") && det.getDebe() > 0) {
                    int cantidadComprada = (int) Math.round(det.getDebe() / costoDinamico);
                    if (cantidadComprada <= 0) cantidadComprada = 1;
                    
                    kardexService.registrarMovimientoConConexión(
                        conn, 1, asiento.getFecha(), "ENTRADA", cantidadComprada,
                        det.getDebe(), false, asientoId
                    );
                    break; 
                }
                
                // Devolución sobre compras: la mercancía vuelve al proveedor.
                else if (esSubcuentaDe(cuenta, "5.1") && det.getHaber() > 0) {
                    int cantidadDevuelta = (int) Math.round(det.getHaber() / costoDinamico);
                    if (cantidadDevuelta <= 0) cantidadDevuelta = 1;
                    kardexService.registrarMovimientoConConexión(
                        conn, 1, asiento.getFecha(), "SALIDA", cantidadDevuelta,
                        0, true, asientoId
                    );
                    break;
                }

                // Devolución sobre ventas: el cliente devuelve mercancía a bodega.
                else if (esSubcuentaDe(cuenta, "4.2") && det.getDebe() > 0) {
                    int cantidadDevuelta = (int) Math.round(det.getDebe() / precioVentaDinamico);
                    if (cantidadDevuelta <= 0) cantidadDevuelta = 1;
                    kardexService.registrarMovimientoConConexión(
                        conn, 1, asiento.getFecha(), "ENTRADA", cantidadDevuelta,
                        0, true, asientoId
                    );
                    break;
                }

                // Ventas reales: únicamente la cuenta 4.1 acreditada genera costo de venta.
                else if (esSubcuentaDe(cuenta, "4.1") && det.getHaber() > 0) {
                    double montoVenta = det.getHaber();
                    if (montoVenta <= 0 && det.getConceptoLinea() != null) {
                        try {
                            montoVenta = Double.parseDouble(det.getConceptoLinea().replace("$", "").replace(",", "").trim());
                        } catch (Exception ignored) {}
                    }

                    if (montoVenta > 0) {
                        int unidadesVendidas = (int) Math.round(montoVenta / precioVentaDinamico);
                        if (unidadesVendidas <= 0) {
                            unidadesVendidas = 1;
                        }
                        
                        kardexService.registrarMovimientoConConexión(
                            conn, 1, asiento.getFecha(), "SALIDA", unidadesVendidas,
                            0, true, asientoId
                        );
                        break; 
                    }
                }
            }
            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            System.err.println("[LibroDiarioDAO] Error al registrar asiento y actualizar kárdex: " + e.getMessage());
            throw new SQLException(e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {}
            }
        }
    }

    private double redondear(double val) {
        return java.math.BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private boolean esSubcuentaDe(String codigo, String codigoGrupo) {
        return codigo.equals(codigoGrupo) || codigo.startsWith(codigoGrupo + ".");
    }

    public boolean eliminarAsiento(int id) {
        String sqlKardex = "DELETE FROM kardex WHERE asiento_id = ?";
        String sqlAsiento = "DELETE FROM asientos WHERE id = ?";
        
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            
            // 1. Limpiamos el movimiento asociado en el Kárdex si existe
            try (PreparedStatement psK = conn.prepareStatement(sqlKardex)) {
                psK.setInt(1, id);
                psK.executeUpdate();
            }
            
            // 2. Eliminamos el asiento (los detalles se borran en cascada por la BD)
            boolean eliminado;
            try (PreparedStatement psA = conn.prepareStatement(sqlAsiento)) {
                psA.setInt(1, id);
                eliminado = psA.executeUpdate() > 0;
            }
            
            conn.commit();
            return eliminado;
        } catch (SQLException e) {
            System.err.println("[LibroDiarioDAO] Error eliminando asiento y su kárdex: " + e.getMessage());
            return false;
        }
    }
}
