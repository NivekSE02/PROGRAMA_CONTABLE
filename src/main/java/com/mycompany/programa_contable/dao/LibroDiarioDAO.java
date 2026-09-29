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
                    insertarAjusteCompra(conn, asientoId, det.getDebe(), false, asiento.getConcepto());
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
                    insertarAjusteCompra(conn, asientoId, det.getHaber(), true, asiento.getConcepto());
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
                    insertarAsientoCostoMovimiento(conn, asientoId, asiento.getConcepto(), cantidadDevuelta,
                            obtenerCostoMovimiento(conn, asientoId, "ENTRADA"), false);
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
                        insertarAsientoCostoMovimiento(conn, asientoId, asiento.getConcepto(), unidadesVendidas,
                                obtenerCostoMovimiento(conn, asientoId, "SALIDA"), true);
                        break; 
                    }
                }
            }
            sincronizarCostosKardexEnDiario(conn, 1);
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

    private void insertarAjusteCompra(Connection conn, int asientoId, double importe,
            boolean esDevolucionProveedor, String concepto) throws SQLException {
        if (importe <= 0) throw new SQLException("El importe de inventario debe ser mayor que cero.");
        int renglon;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(MAX(renglon), 0) + 1 FROM detalle_asiento WHERE asiento_id = ?")) {
            ps.setInt(1, asientoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("No se pudo asignar el renglÃ³n al ajuste de compra.");
                renglon = rs.getInt(1);
            }
        }
        String cuentaCompra = esDevolucionProveedor ? "5.1" : "5.4";
        String conceptoCompra = esDevolucionProveedor
                ? "AUTO_KARDEX_DEV_COMPRA: Ajuste por devoluciÃ³n: " + concepto
                : "AUTO_KARDEX_COMPRA: ReclasificaciÃ³n a inventario: " + concepto;
        String sql = "INSERT INTO detalle_asiento (asiento_id, renglon, cuenta_codigo, concepto_linea, debe, haber) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, asientoId);
            ps.setInt(2, renglon);
            ps.setString(3, "1.2");
            ps.setString(4, conceptoCompra);
            ps.setDouble(5, esDevolucionProveedor ? 0 : importe);
            ps.setDouble(6, esDevolucionProveedor ? importe : 0);
            ps.addBatch();
            ps.setInt(2, renglon + 1);
            ps.setString(3, cuentaCompra);
            ps.setString(4, conceptoCompra);
            ps.setDouble(5, esDevolucionProveedor ? importe : 0);
            ps.setDouble(6, esDevolucionProveedor ? 0 : importe);
            ps.addBatch();
            ps.executeBatch();
        }
        actualizarTotalesAsiento(conn, asientoId);
    }

    private void actualizarTotalesAsiento(Connection conn, int asientoId) throws SQLException {
        double debe;
        double haber;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(debe), 0), COALESCE(SUM(haber), 0) FROM detalle_asiento WHERE asiento_id = ?")) {
            ps.setInt(1, asientoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("No se pudieron calcular los totales del asiento.");
                debe = rs.getDouble(1);
                haber = rs.getDouble(2);
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE asientos SET total_debe = ?, total_haber = ? WHERE id = ?")) {
            ps.setDouble(1, debe);
            ps.setDouble(2, haber);
            ps.setInt(3, asientoId);
            ps.executeUpdate();
        }
    }

    private double obtenerCostoMovimiento(Connection conn, int asientoId, String tipo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT costo_total FROM kardex WHERE asiento_id = ? AND tipo_movimiento = ?")) {
            ps.setInt(1, asientoId);
            ps.setString(2, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
                throw new SQLException("No se encontro el costo del movimiento en el Kardex.");
            }
        }
    }

    private void insertarAsientoCostoMovimiento(Connection conn, int asientoId,
            String concepto, int cantidad, double costo, boolean esVenta) throws SQLException {
        if (costo <= 0) throw new SQLException("El costo del movimiento debe ser mayor que cero.");
        int renglon;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(MAX(renglon), 0) + 1 FROM detalle_asiento WHERE asiento_id = ?")) {
            ps.setInt(1, asientoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("No se pudo asignar el renglon al costo de inventario.");
                renglon = rs.getInt(1);
            }
        }
        String sql = "INSERT INTO detalle_asiento (asiento_id, renglon, cuenta_codigo, concepto_linea, debe, haber) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, asientoId);
            ps.setInt(2, renglon);
            ps.setString(3, "5.2");
            ps.setString(4, "AUTO_KARDEX_COGS: " + (esVenta ? "Costo de " : "Reversion por devolucion de ") + cantidad + " unidades: " + concepto);
            ps.setDouble(5, esVenta ? costo : 0);
            ps.setDouble(6, esVenta ? 0 : costo);
            ps.addBatch();
            ps.setInt(2, renglon + 1);
            ps.setString(3, "1.2");
            ps.setString(4, "AUTO_KARDEX_INV: " + (esVenta ? "Salida de inventario por venta: " : "Ingreso por devolucion de venta: ") + concepto);
            ps.setDouble(5, esVenta ? 0 : costo);
            ps.setDouble(6, esVenta ? costo : 0);
            ps.addBatch();
            ps.executeBatch();
        }
    }
    /** Mantiene los asientos automáticos alineados si una compra retroactiva revalora ventas. */
    private void sincronizarCostosKardexEnDiario(Connection conn, int productoId) throws SQLException {
        String movimientos = "SELECT k.asiento_id, k.costo_total, "
                + "CASE WHEN k.tipo_movimiento = 'ENTRADA' THEN 1 ELSE 0 END AS es_devolucion "
                + "FROM kardex k WHERE k.producto_id = ? AND ("
                + "(k.tipo_movimiento = 'SALIDA' AND EXISTS (SELECT 1 FROM detalle_asiento d "
                + "WHERE d.asiento_id = k.asiento_id AND (d.cuenta_codigo = '4.1' OR d.cuenta_codigo LIKE '4.1.%') AND d.haber > 0)) "
                + "OR (k.tipo_movimiento = 'ENTRADA' AND EXISTS (SELECT 1 FROM detalle_asiento d "
                + "WHERE d.asiento_id = k.asiento_id AND (d.cuenta_codigo = '4.2' OR d.cuenta_codigo LIKE '4.2.%') AND d.debe > 0)))";
        Map<Integer, double[]> asientos = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(movimientos)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int asientoId = rs.getInt("asiento_id");
                    double costo = rs.getDouble("costo_total");
                    asientos.put(asientoId, new double[]{costo, rs.getInt("es_devolucion")});
                }
            }
        }
        for (Map.Entry<Integer, double[]> movimiento : asientos.entrySet()) {
            int asientoId = movimiento.getKey();
            double costo = movimiento.getValue()[0];
            boolean esDevolucion = movimiento.getValue()[1] == 1;
            actualizarLineaAutomatica(conn, asientoId, "5.2", "AUTO_KARDEX_COGS:%", costo, !esDevolucion);
            actualizarLineaAutomatica(conn, asientoId, "1.2", "AUTO_KARDEX_INV:%", costo, esDevolucion);
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(debe), 0), COALESCE(SUM(haber), 0) FROM detalle_asiento WHERE asiento_id = ?")) {
                ps.setInt(1, asientoId);
                double totalDebe;
                double totalHaber;
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) continue;
                    totalDebe = rs.getDouble(1);
                    totalHaber = rs.getDouble(2);
                }
                try (PreparedStatement update = conn.prepareStatement(
                        "UPDATE asientos SET total_debe = ?, total_haber = ? WHERE id = ?")) {
                    update.setDouble(1, totalDebe);
                    update.setDouble(2, totalHaber);
                    update.setInt(3, asientoId);
                    update.executeUpdate();
                }
            }
        }
    }

    private void actualizarLineaAutomatica(Connection conn, int asientoId, String cuenta,
            String marcador, double costo, boolean debe) throws SQLException {
        String sql = "UPDATE detalle_asiento SET debe = ?, haber = ? WHERE asiento_id = ? "
                + "AND cuenta_codigo = ? AND concepto_linea LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, debe ? costo : 0);
            ps.setDouble(2, debe ? 0 : costo);
            ps.setInt(3, asientoId);
            ps.setString(4, cuenta);
            ps.setString(5, marcador);
            ps.executeUpdate();
        }
    }

    /** Completa asientos antiguos que ya tienen KÃ¡rdex pero aÃºn no tenÃ­an los ajustes automÃ¡ticos. */
    public void completarAsientosKardexExistentes() {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Integer> productos = new ArrayList<>();
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT DISTINCT producto_id FROM kardex")) {
                    while (rs.next()) productos.add(rs.getInt(1));
                }
                KardexService kardex = new KardexService();
                for (int productoId : productos) kardex.recalcularKardex(conn, productoId);

                String sql = "SELECT k.asiento_id, k.tipo_movimiento, k.cantidad, k.costo_total, a.concepto, "
                        + "COALESCE((SELECT SUM(d.debe - d.haber) FROM detalle_asiento d WHERE d.asiento_id = k.asiento_id AND d.cuenta_codigo = '5.4'), 0) AS compra, "
                        + "COALESCE((SELECT SUM(d.haber - d.debe) FROM detalle_asiento d WHERE d.asiento_id = k.asiento_id AND d.cuenta_codigo = '5.1'), 0) AS devolucion_compra, "
                        + "CASE WHEN EXISTS (SELECT 1 FROM detalle_asiento d WHERE d.asiento_id = k.asiento_id AND (d.cuenta_codigo = '4.1' OR d.cuenta_codigo LIKE '4.1.%') AND d.haber > 0) THEN 1 ELSE 0 END AS es_venta, "
                        + "CASE WHEN EXISTS (SELECT 1 FROM detalle_asiento d WHERE d.asiento_id = k.asiento_id AND (d.cuenta_codigo = '4.2' OR d.cuenta_codigo LIKE '4.2.%') AND d.debe > 0) THEN 1 ELSE 0 END AS es_devolucion_venta "
                        + "FROM kardex k JOIN asientos a ON a.id = k.asiento_id ORDER BY k.fecha, k.id";
                List<MovimientoKardexDiario> movimientos = new ArrayList<>();
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) movimientos.add(new MovimientoKardexDiario(rs.getInt("asiento_id"),
                            rs.getString("tipo_movimiento"), rs.getInt("cantidad"), rs.getDouble("costo_total"),
                            rs.getString("concepto"), rs.getDouble("compra"), rs.getDouble("devolucion_compra"),
                            rs.getInt("es_venta") == 1, rs.getInt("es_devolucion_venta") == 1));
                }

                for (MovimientoKardexDiario movimiento : movimientos) {
                    if (movimiento.esVenta && "SALIDA".equalsIgnoreCase(movimiento.tipo)
                            && !tieneMarcador(conn, movimiento.asientoId, "AUTO_KARDEX_COGS:%")
                            && !tieneCuenta(conn, movimiento.asientoId, "5.2") && !tieneCuenta(conn, movimiento.asientoId, "1.2")) {
                        insertarAsientoCostoMovimiento(conn, movimiento.asientoId, movimiento.concepto,
                                movimiento.cantidad, movimiento.costo, true);
                    } else if (movimiento.esDevolucionVenta && "ENTRADA".equalsIgnoreCase(movimiento.tipo)
                            && !tieneMarcador(conn, movimiento.asientoId, "AUTO_KARDEX_COGS:%")
                            && !tieneCuenta(conn, movimiento.asientoId, "5.2") && !tieneCuenta(conn, movimiento.asientoId, "1.2")) {
                        insertarAsientoCostoMovimiento(conn, movimiento.asientoId, movimiento.concepto,
                                movimiento.cantidad, movimiento.costo, false);
                    } else if (movimiento.compra > 0
                            && !tieneMarcador(conn, movimiento.asientoId, "AUTO_KARDEX_COMPRA:%")
                            && !tieneCuenta(conn, movimiento.asientoId, "1.2")) {
                        insertarAjusteCompra(conn, movimiento.asientoId, movimiento.compra, false, movimiento.concepto);
                    } else if (movimiento.devolucionCompra > 0
                            && !tieneMarcador(conn, movimiento.asientoId, "AUTO_KARDEX_DEV_COMPRA:%")
                            && !tieneCuenta(conn, movimiento.asientoId, "1.2")) {
                        insertarAjusteCompra(conn, movimiento.asientoId, movimiento.devolucionCompra, true, movimiento.concepto);
                    }
                }
                for (int productoId : productos) sincronizarCostosKardexEnDiario(conn, productoId);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new IllegalStateException("No se pudieron conciliar los asientos existentes con el KÃ¡rdex; no se guardaron cambios.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron conciliar los asientos existentes con el KÃ¡rdex.", e);
        }
    }

    private boolean tieneMarcador(Connection conn, int asientoId, String marcador) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM detalle_asiento WHERE asiento_id = ? AND concepto_linea LIKE ?")) {
            ps.setInt(1, asientoId);
            ps.setString(2, marcador);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private boolean tieneCuenta(Connection conn, int asientoId, String cuenta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM detalle_asiento WHERE asiento_id = ? AND cuenta_codigo = ?")) {
            ps.setInt(1, asientoId);
            ps.setString(2, cuenta);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static final class MovimientoKardexDiario {
        final int asientoId;
        final String tipo;
        final int cantidad;
        final double costo;
        final String concepto;
        final double compra;
        final double devolucionCompra;
        final boolean esVenta;
        final boolean esDevolucionVenta;
        MovimientoKardexDiario(int asientoId, String tipo, int cantidad, double costo, String concepto,
                double compra, double devolucionCompra, boolean esVenta, boolean esDevolucionVenta) {
            this.asientoId = asientoId; this.tipo = tipo; this.cantidad = cantidad; this.costo = costo;
            this.concepto = concepto; this.compra = compra; this.devolucionCompra = devolucionCompra;
            this.esVenta = esVenta; this.esDevolucionVenta = esDevolucionVenta;
        }
    }

    private double redondear(double val) {
        return java.math.BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }

    private boolean esSubcuentaDe(String codigo, String codigoGrupo) {
        return codigo.equals(codigoGrupo) || codigo.startsWith(codigoGrupo + ".");
    }

    public boolean eliminarAsiento(int id) throws SQLException {
        String sqlKardex = "DELETE FROM kardex WHERE asiento_id = ?";
        String sqlAsiento = "DELETE FROM asientos WHERE id = ?";
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<Integer> productos = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT DISTINCT producto_id FROM kardex WHERE asiento_id = ?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) productos.add(rs.getInt(1));
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(sqlKardex)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                int filasEliminadas;
                try (PreparedStatement ps = conn.prepareStatement(sqlAsiento)) {
                    ps.setInt(1, id);
                    filasEliminadas = ps.executeUpdate();
                }
                if (filasEliminadas == 0) {
                    conn.rollback();
                    return false;
                }
                KardexService service = new KardexService();
                for (int productoId : productos) service.recalcularKardex(conn, productoId);
                for (int productoId : productos) sincronizarCostosKardexEnDiario(conn, productoId);
                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof SQLException sqlException) throw sqlException;
                throw new SQLException("No se pudo eliminar el asiento ni recalcular el Kárdex.", e);
            }
        }
    }
}
