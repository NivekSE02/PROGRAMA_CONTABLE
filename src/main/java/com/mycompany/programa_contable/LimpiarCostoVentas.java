package com.mycompany.programa_contable;

import java.sql.Connection;
import java.sql.Statement;
import com.mycompany.programa_contable.db.DatabaseManager;

public class LimpiarCostoVentas {
    public static void main(String[] args) {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Iniciando limpieza de base de datos...");
            
            // Eliminar las filas de 5.2 (Costo de ventas) inyectadas
            int eliminadosCosto = stmt.executeUpdate("DELETE FROM detalle_asiento WHERE cuenta_codigo = '5.2'");
            System.out.println("Filas de Costo de Ventas eliminadas: " + eliminadosCosto);
            
            // Eliminar las filas de 1.2 (Inventario) inyectadas (que tienen Salida automatica en concepto o están al Haber sin ser compras iniciales)
            int eliminadosInventario = stmt.executeUpdate("DELETE FROM detalle_asiento WHERE cuenta_codigo = '1.2' AND haber > 0");
            System.out.println("Filas de Salida de Inventario eliminadas: " + eliminadosInventario);
            
            System.out.println("¡Limpieza completada! El sistema ya no tiene los asientos duplicados de Costo de Ventas.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
