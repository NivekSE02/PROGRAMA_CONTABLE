package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.KardexFilaDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class KardexService {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public double obtenerCostoDeVentasTotal() {
        double costoTotal = 0.0;
        // El costo de ventas neto considera las salidas por ventas (4.1) y
        // revierte su costo cuando el cliente devuelve mercancía (4.2). Las
        // salidas por devolución a proveedor (5.1) no forman parte del costo.
        // SQL Server no permite subconsultas dentro de la expresión de SUM.
        // Por ello se clasifica cada asiento primero y luego se suma el Kárdex.
        String sql = "SELECT COALESCE(SUM(CASE "
                   + "WHEN k.tipo_movimiento = 'SALIDA' AND clasificacion.es_venta = 1 THEN k.costo_total "
                   + "WHEN k.tipo_movimiento = 'ENTRADA' AND clasificacion.es_devolucion_venta = 1 THEN -k.costo_total "
                   + "ELSE 0 END), 0) AS total_salidas "
                   + "FROM kardex k LEFT JOIN ("
                   + " SELECT asiento_id, "
                   + " MAX(CASE WHEN (cuenta_codigo = '4.1' OR cuenta_codigo LIKE '4.1.%') AND haber > 0 THEN 1 ELSE 0 END) AS es_venta, "
                   + " MAX(CASE WHEN (cuenta_codigo = '4.2' OR cuenta_codigo LIKE '4.2.%') AND debe > 0 THEN 1 ELSE 0 END) AS es_devolucion_venta "
                   + " FROM detalle_asiento GROUP BY asiento_id"
                   + ") clasificacion ON clasificacion.asiento_id = k.asiento_id";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                costoTotal = rs.getDouble("total_salidas");
            }
        } catch (Exception e) {
            System.err.println("[KardexService] Error al calcular el costo de ventas: " + e.getMessage());
        }
        return costoTotal;
    }

    /** Obtiene el saldo final reconstruido con los importes originales del diario. */
    public double obtenerInventarioFinal(int productoId) {
        List<KardexFilaDTO> filas = generarReporteKardex(productoId);
        return filas.isEmpty() ? 0.0 : filas.get(filas.size() - 1).getSaldoMonetario();
    }

    public void registrarMovimientoAuto(int productoId, String fecha, String tipoMovimiento, int cantidad, double costoUnitario, int asientoId) {
        String sql = "INSERT INTO kardex (producto_id, fecha, tipo_movimiento, cantidad, costo_unitario, costo_total, asiento_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ps.setString(2, fecha);
            ps.setString(3, tipoMovimiento);
            ps.setInt(4, cantidad);
            ps.setDouble(5, costoUnitario);
            ps.setDouble(6, cantidad * costoUnitario);
            ps.setInt(7, asientoId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[KardexService] Error insertando movimiento: " + e.getMessage());
        }
    }

    public List<KardexFilaDTO> generarReporteKardex(int productoId) {
        List<KardexFilaDTO> reporte = new ArrayList<>();
        int existenciasActuales = 0;
        double saldoActual = 0.0;

        String sql = "SELECT k.fecha, k.tipo_movimiento, k.cantidad, k.costo_unitario, k.costo_total, "
                   + "k.asiento_id, a.concepto, COALESCE((SELECT SUM(CASE "
                   + "WHEN d.cuenta_codigo = '5.1' OR d.cuenta_codigo LIKE '5.1.%' "
                   + "THEN d.haber - d.debe ELSE d.debe - d.haber END) "
                   + "FROM detalle_asiento d WHERE d.asiento_id = k.asiento_id AND ("
                   + "d.cuenta_codigo = '1.2' OR d.cuenta_codigo LIKE '1.2.%' "
                   + "OR d.cuenta_codigo = '5.4' OR d.cuenta_codigo LIKE '5.4.%' "
                   + "OR d.cuenta_codigo = '5.1' OR d.cuenta_codigo LIKE '5.1.%')), 0) AS importe_contable "
                   + "FROM kardex k INNER JOIN asientos a ON k.asiento_id = a.id "
                   + "WHERE k.producto_id = ? ORDER BY k.fecha ASC, k.id ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fecha    = rs.getString("fecha");
                    String concepto = rs.getString("concepto");
                    String tipo     = rs.getString("tipo_movimiento");
                    int cantidad    = rs.getInt("cantidad");
                    double costoU   = rs.getDouble("costo_unitario");
                    double costoT   = rs.getDouble("costo_total");

                    // En aperturas, compras y devoluciones a proveedor, el asiento
                    // es la fuente del importe. Versiones anteriores guardaban
                    // cantidad * costo unitario y acumulaban diferencias de redondeo.
                    double importe = rs.getDouble("importe_contable");
                    if (importe > 0) {
                        costoT = redondear(importe);
                        costoU = costoT / cantidad;
                    }

                    int entrada = 0, salida = 0;
                    if (tipo.equals("ENTRADA")) {
                        entrada = cantidad;
                        existenciasActuales += cantidad;
                        saldoActual += costoT;
                    } else if (tipo.equals("SALIDA")) {
                        salida = cantidad;
                        existenciasActuales -= cantidad;
                        saldoActual -= costoT;
                    }

                    reporte.add(new KardexFilaDTO(fecha, concepto, entrada, salida, existenciasActuales,
                            costoU, costoT, redondear(saldoActual)));
                }
            }
        } catch (Exception e) {
            System.err.println("[KardexService] Error generando reporte: " + e.getMessage());
        }
        return reporte;
    }

    public void registrarMovimientoConConexión(Connection conn, int productoId, String fecha,
            String tipoMovimiento, int cantidad, double importeEntrada,
            boolean usarCostoPromedio, int asientoId) throws Exception {
        int ultimoSaldoCantidad = 0;
        double ultimoSaldoValor = 0.0;

        // SQLite usa LIMIT, SQL Server usa TOP
        String sqlSaldo;
        if (dbManager.getMotorActivo() == DatabaseManager.MotorBD.SQLITE) {
            sqlSaldo = "SELECT saldo_cantidad, saldo_valor FROM kardex WHERE producto_id = ? ORDER BY fecha DESC, id DESC LIMIT 1";
        } else {
            sqlSaldo = "SELECT TOP 1 saldo_cantidad, saldo_valor FROM kardex WHERE producto_id = ? ORDER BY fecha DESC, id DESC";
        }

        try (PreparedStatement psSaldo = conn.prepareStatement(sqlSaldo)) {
            psSaldo.setInt(1, productoId);
            try (ResultSet rs = psSaldo.executeQuery()) {
                if (rs.next()) {
                    ultimoSaldoCantidad = rs.getInt("saldo_cantidad");
                    ultimoSaldoValor    = rs.getDouble("saldo_valor");
                }
            }
        }

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad del movimiento debe ser mayor que cero.");
        }

        double costoUnitario;
        double costoTotalMovimiento;
        if (usarCostoPromedio) {
            if (ultimoSaldoCantidad <= 0 || ultimoSaldoValor < 0) {
                throw new IllegalArgumentException("No hay existencias valorizadas para calcular el costo promedio.");
            }
            costoUnitario = ultimoSaldoValor / ultimoSaldoCantidad;
            costoTotalMovimiento = redondear(cantidad * costoUnitario);
        } else {
            costoTotalMovimiento = redondear(importeEntrada);
            costoUnitario = costoTotalMovimiento / cantidad;
        }

        int nuevoSaldoCantidad;
        double nuevoSaldoValor;

        if ("ENTRADA".equalsIgnoreCase(tipoMovimiento)) {
            nuevoSaldoCantidad = ultimoSaldoCantidad + cantidad;
            nuevoSaldoValor    = redondear(ultimoSaldoValor + costoTotalMovimiento);
        } else {
            if (cantidad > ultimoSaldoCantidad) {
                throw new IllegalArgumentException("La salida supera las existencias disponibles en el Kárdex.");
            }
            nuevoSaldoCantidad = ultimoSaldoCantidad - cantidad;
            nuevoSaldoValor    = redondear(ultimoSaldoValor - costoTotalMovimiento);
        }

        String sql = "INSERT INTO kardex (producto_id, fecha, tipo_movimiento, cantidad, costo_unitario, costo_total, saldo_cantidad, saldo_valor, asiento_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ps.setString(2, fecha);
            ps.setString(3, tipoMovimiento);
            ps.setInt(4, cantidad);
            ps.setDouble(5, costoUnitario);
            ps.setDouble(6, costoTotalMovimiento);
            ps.setInt(7, nuevoSaldoCantidad);
            ps.setDouble(8, nuevoSaldoValor);
            ps.setInt(9, asientoId);
            ps.executeUpdate();
        }
    }

    private double redondear(double val) {
        return java.math.BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
