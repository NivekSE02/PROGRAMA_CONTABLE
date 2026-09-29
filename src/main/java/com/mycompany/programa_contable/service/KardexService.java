package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.KardexFilaDTO;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
                   + "k.asiento_id, a.concepto "
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
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad del movimiento debe ser mayor que cero.");
        if (!"ENTRADA".equalsIgnoreCase(tipoMovimiento) && !"SALIDA".equalsIgnoreCase(tipoMovimiento)) {
            throw new IllegalArgumentException("Tipo de movimiento inválido: " + tipoMovimiento);
        }
        if (importeEntrada < 0) throw new IllegalArgumentException("El importe no puede ser negativo.");

        // Las salidas y las devoluciones de clientes se valorizan en el recálculo.
        double costoInicial = "ENTRADA".equalsIgnoreCase(tipoMovimiento) && !usarCostoPromedio
                ? redondear(importeEntrada) : 0.0;
        String sql = "INSERT INTO kardex (producto_id, fecha, tipo_movimiento, cantidad, costo_unitario, costo_total, saldo_cantidad, saldo_valor, asiento_id) "
                   + "VALUES (?, ?, ?, ?, ?, ?, 0, 0, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ps.setString(2, fecha);
            ps.setString(3, tipoMovimiento.toUpperCase(java.util.Locale.ROOT));
            ps.setInt(4, cantidad);
            ps.setDouble(5, costoInicial / cantidad);
            ps.setDouble(6, costoInicial);
            ps.setInt(7, asientoId);
            ps.executeUpdate();
        }
        recalcularKardex(conn, productoId);
    }

    /** Revalora el producto en orden cronológico usando promedio ponderado móvil. */
    public void recalcularKardex(Connection conn, int productoId) throws SQLException {
        String sql = "SELECT k.id, k.tipo_movimiento, k.cantidad, k.costo_unitario, k.costo_total, "
                + "COALESCE(d.importe_entrada, 0) AS importe_entrada, "
                + "COALESCE(d.devolucion_venta, 0) AS devolucion_venta, "
                + "COALESCE(d.importe_devolucion_compra, 0) AS importe_devolucion_compra, "
                + "COALESCE(d.devolucion_compra, 0) AS devolucion_compra "
                + "FROM kardex k LEFT JOIN (SELECT asiento_id, "
                + "SUM(CASE WHEN cuenta_codigo = '1.2' OR cuenta_codigo LIKE '1.2.%' "
                + "OR cuenta_codigo = '5.4' OR cuenta_codigo LIKE '5.4.%' THEN debe - haber ELSE 0 END) AS importe_entrada, "
                + "MAX(CASE WHEN (cuenta_codigo = '4.2' OR cuenta_codigo LIKE '4.2.%') AND debe > 0 THEN 1 ELSE 0 END) AS devolucion_venta, "
                + "SUM(CASE WHEN cuenta_codigo = '5.1' OR cuenta_codigo LIKE '5.1.%' THEN haber ELSE 0 END) AS importe_devolucion_compra, "
                + "MAX(CASE WHEN (cuenta_codigo = '5.1' OR cuenta_codigo LIKE '5.1.%') AND haber > 0 THEN 1 ELSE 0 END) AS devolucion_compra "
                + "FROM detalle_asiento GROUP BY asiento_id) d ON d.asiento_id = k.asiento_id "
                + "WHERE k.producto_id = ? ORDER BY k.fecha ASC, k.id ASC";
        String update = "UPDATE kardex SET costo_unitario = ?, costo_total = ?, saldo_cantidad = ?, saldo_valor = ? WHERE id = ?";
        List<MovimientoRecalculo> movimientos = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) movimientos.add(new MovimientoRecalculo(rs.getInt("id"),
                        rs.getString("tipo_movimiento"), rs.getInt("cantidad"), rs.getDouble("costo_unitario"),
                        rs.getDouble("costo_total"), rs.getDouble("importe_entrada"),
                        rs.getInt("devolucion_venta") == 1, rs.getDouble("importe_devolucion_compra"),
                        rs.getInt("devolucion_compra") == 1));
            }
        }

        int saldoCantidad = 0;
        double saldoValor = 0;
        double costoUltimaSalida = 0;
        try (PreparedStatement ps = conn.prepareStatement(update)) {
            for (MovimientoRecalculo movimiento : movimientos) {
                int cantidad = movimiento.cantidad;
                double costoUnitario;
                double costoTotal;
                if ("ENTRADA".equalsIgnoreCase(movimiento.tipo)) {
                    if (movimiento.devolucionVenta) {
                        costoUnitario = saldoCantidad > 0 ? saldoValor / saldoCantidad
                                : (costoUltimaSalida > 0 ? costoUltimaSalida : movimiento.costoUnitario);
                        if (costoUnitario <= 0) throw new SQLException("No se puede valorizar la devolución: falta costo histórico.");
                        costoTotal = redondear(costoUnitario * cantidad);
                    } else {
                        costoTotal = movimiento.importeEntrada > 0
                                ? redondear(movimiento.importeEntrada) : redondear(movimiento.costoTotal);
                        if (costoTotal < 0) throw new SQLException("El costo de una entrada no puede ser negativo.");
                        costoUnitario = costoTotal / cantidad;
                    }
                    saldoCantidad += cantidad;
                    saldoValor = redondear(saldoValor + costoTotal);
                } else {
                    if (cantidad > saldoCantidad) {
                        throw new SQLException("El movimiento " + movimiento.id + " dejaría existencias negativas; revise las fechas o los asientos relacionados.");
                    }
                    if (movimiento.devolucionCompra) {
                        costoTotal = movimiento.importeDevolucionCompra > 0
                                ? redondear(movimiento.importeDevolucionCompra) : redondear(movimiento.costoTotal);
                        if (costoTotal > saldoValor + 0.005) {
                            throw new SQLException("La devolucion al proveedor superaria el valor disponible del inventario.");
                        }
                        costoUnitario = costoTotal / cantidad;
                    } else {
                        costoUnitario = saldoValor / saldoCantidad;
                        costoTotal = cantidad == saldoCantidad ? saldoValor : redondear(cantidad * costoUnitario);
                    }
                    costoUltimaSalida = costoUnitario;
                    saldoCantidad -= cantidad;
                    saldoValor = saldoCantidad == 0 ? 0 : redondear(saldoValor - costoTotal);
                }
                ps.setDouble(1, costoUnitario);
                ps.setDouble(2, costoTotal);
                ps.setInt(3, saldoCantidad);
                ps.setDouble(4, saldoValor);
                ps.setInt(5, movimiento.id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static final class MovimientoRecalculo {
        final int id;
        final String tipo;
        final int cantidad;
        final double costoUnitario;
        final double costoTotal;
        final double importeEntrada;
        final boolean devolucionVenta;
        final double importeDevolucionCompra;
        final boolean devolucionCompra;
        MovimientoRecalculo(int id, String tipo, int cantidad, double costoUnitario,
                double costoTotal, double importeEntrada, boolean devolucionVenta,
                double importeDevolucionCompra, boolean devolucionCompra) {
            this.id = id; this.tipo = tipo; this.cantidad = cantidad; this.costoUnitario = costoUnitario;
            this.costoTotal = costoTotal; this.importeEntrada = importeEntrada; this.devolucionVenta = devolucionVenta;
            this.importeDevolucionCompra = importeDevolucionCompra; this.devolucionCompra = devolucionCompra;
        }
    }

    private double redondear(double val) {
        return java.math.BigDecimal.valueOf(val).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
