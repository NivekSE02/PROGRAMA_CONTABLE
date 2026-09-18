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

    /**
     * Calcula automáticamente el Costo de Ventas total leyendo 
     * todas las salidas del kárdex en la base de datos.
     */
    public double obtenerCostoDeVentasTotal() {
        double costoTotal = 0.0;
        String sql = "SELECT SUM(costo_total) as total_salidas FROM kardex WHERE tipo_movimiento = 'SALIDA'";
        
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
    
    // -------------------------------------------------------------------------
    // 1. AUTOMATIZACIÓN: Llamar a este método cuando guardes un Asiento nuevo
    // -------------------------------------------------------------------------
    public void registrarMovimientoAuto(int productoId, String fecha, String tipoMovimiento, int cantidad, double costoUnitario, int asientoId) {
        String sql = "INSERT INTO kardex (producto_id, fecha, tipo_movimiento, cantidad, costo_unitario, costo_total, asiento_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, productoId);
            ps.setString(2, fecha);
            ps.setString(3, tipoMovimiento); // "ENTRADA" o "SALIDA"
            ps.setInt(4, cantidad);
            ps.setDouble(5, costoUnitario);
            ps.setDouble(6, cantidad * costoUnitario); // Calcula el total solo
            ps.setInt(7, asientoId);
            
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[KardexService] Error insertando movimiento: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 2. LA VISTA: Genera las filas calculando existencias y saldos como en Excel
    // -------------------------------------------------------------------------
    public List<KardexFilaDTO> generarReporteKardex(int productoId) {
        List<KardexFilaDTO> reporte = new ArrayList<>();
        int existenciasActuales = 0;
        double saldoActual = 0.0;

        String sql = "SELECT k.fecha, k.tipo_movimiento, k.cantidad, k.costo_unitario, k.costo_total, a.concepto " +
                     "FROM kardex k INNER JOIN asientos a ON k.asiento_id = a.id " +
                     "WHERE k.producto_id = ? ORDER BY k.fecha ASC, k.id ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, productoId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fecha = rs.getString("fecha");
                    String concepto = rs.getString("concepto");
                    String tipo = rs.getString("tipo_movimiento");
                    int cantidad = rs.getInt("cantidad");
                    double costoU = rs.getDouble("costo_unitario");
                    double costoT = rs.getDouble("costo_total");

                    int entrada = 0, salida = 0;

                    // La matemática automática del Kárdex
                    if (tipo.equals("ENTRADA")) {
                        entrada = cantidad;
                        existenciasActuales += cantidad;
                        saldoActual += costoT;
                    } else if (tipo.equals("SALIDA")) {
                        salida = cantidad;
                        existenciasActuales -= cantidad;
                        saldoActual -= costoT;
                    }

                    reporte.add(new KardexFilaDTO(fecha, concepto, entrada, salida, existenciasActuales, costoU, costoT, saldoActual));
                }
            }
        } catch (Exception e) {
            System.err.println("[KardexService] Error generando reporte: " + e.getMessage());
        }
        
        return reporte;
    }
}