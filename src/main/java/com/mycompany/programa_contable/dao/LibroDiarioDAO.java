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

    /**
     * Registra un nuevo asiento contable con atomicidad (transacción ACID)
     * y validación estricta de Partida Doble (Total Debe == Total Haber).
     */
    public boolean registrarAsiento(Asiento asiento) throws IllegalArgumentException, SQLException {
        // -----------------------------------------------------------------
        // PASO 0: CONSULTAR CONFIGURACIÓN Y PARÁMETROS ANTES DE VALIDAR
        // -----------------------------------------------------------------
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

        // -----------------------------------------------------------------
        // PASO 1: DETECTAR SI ES VENTA E INYECTAR AUTOMÁTICAMENTE COSTO DE VENTAS
        // -----------------------------------------------------------------
        boolean esVentaComercial = false;
        double montoTotalVentaDetectado = 0.0;

        for (DetalleAsiento det : asiento.getDetalles()) {
            String cuenta = det.getCuentaCodigo();
            if ("4".equals(cuenta) || "4.1".equals(cuenta) || cuenta.startsWith("4.")) {
                double val = det.getHaber();
                if (val <= 0 && det.getConceptoLinea() != null) {
                    try {
                        val = Double.parseDouble(det.getConceptoLinea().replace("$", "").replace(",", "").trim());
                    } catch (Exception ignored) {}
                }
                if (val > 0) {
                    esVentaComercial = true;
                    montoTotalVentaDetectado = val;
                    break;
                }
            }
        }

        // Si es una venta, calculamos el costo de ventas y agregamos los renglones si no existen
        if (esVentaComercial && montoTotalVentaDetectado > 0) {
            int unidadesVendidas = (int) Math.round(montoTotalVentaDetectado / precioVentaDinamico);
            if (unidadesVendidas <= 0) unidadesVendidas = 1;
            
            double costoTotalVenta = redondear(unidadesVendidas * costoDinamico);

            // Verificamos si el asiento ya tiene la cuenta 5.2 para no duplicarla
            boolean yaTieneCostoVentas = asiento.getDetalles().stream()
                .anyMatch(d -> "5.2".equals(d.getCuentaCodigo()));

            if (!yaTieneCostoVentas && costoTotalVenta > 0) {
                int nuevoRenglon = asiento.getDetalles().size() + 1;
                
                // Renglón a la cuenta 5.2 (Costo de Ventas) al Debe
                asiento.agregarDetalle(new DetalleAsiento(
                    nuevoRenglon++, "5.2", "Costo de ventas", "Costo automático", costoTotalVenta, 0.0
                ));
                
                // Renglón a la cuenta 1.2 (Inventario de Mercaderías) al Haber
                asiento.agregarDetalle(new DetalleAsiento(
                    nuevoRenglon, "1.2", "Inventario de Mercaderías", "Salida automática", 0.0, costoTotalVenta
                ));
                
                // Ajustamos los totales de la partida doble de forma perfectamente simétrica
                asiento.setTotalDebe(redondear(asiento.getTotalDebe() + costoTotalVenta));
                asiento.setTotalHaber(redondear(asiento.getTotalHaber() + costoTotalVenta));
            }
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

            // -----------------------------------------------------------------
            // AUTOMATIZACIÓN KÁRDEX (ENTRADAS Y SALIDAS)
            // -----------------------------------------------------------------
            KardexService kardexService = new KardexService();

            for (DetalleAsiento det : asiento.getDetalles()) {
                String cuenta = det.getCuentaCodigo();
                
                // 1. INVENTARIO INICIAL (Detecta si es 1.2 o si el concepto/parcial trae el valor de inventario)
                boolean esInventarioInicial = ("1.2".equals(cuenta) || "1".equals(cuenta)) && asiento.getNumero() == 1;
                
                if (esInventarioInicial) {
                    // Tomamos el valor del Debe si existe, o del Parcial si la cuenta principal agrupó el monto
                    double valorInventario = det.getDebe();
                    if (valorInventario <= 0 && det.getConceptoLinea() != null) {
                        try {
                            valorInventario = Double.parseDouble(det.getConceptoLinea().replace("$", "").replace(",", "").trim());
                        } catch (Exception ignored) {}
                    }

                    if (valorInventario > 0 && ("1.2".equals(cuenta) || (det.getConceptoLinea() != null && !det.getConceptoLinea().isEmpty()))) {
                        int cantidadInicial = (int) Math.round(valorInventario / costoDinamico);
                        if (cantidadInicial <= 0) cantidadInicial = 1;
                        
                        kardexService.registrarMovimientoConConexión(
                            conn, 1, asiento.getFecha(), "ENTRADA", cantidadInicial, costoDinamico, asientoId
                        );
                        break; 
                    }
                }
                
                // 2. COMPRAS NUEVAS (Afectando la cuenta 5.4 Compras al Debe)
                else if (("5.4".equals(cuenta) || cuenta.startsWith("5.4")) && det.getDebe() > 0) {
                    int cantidadComprada = (int) Math.round(det.getDebe() / costoDinamico);
                    if (cantidadComprada <= 0) cantidadComprada = 1;
                    
                    kardexService.registrarMovimientoConConexión(
                        conn, 1, asiento.getFecha(), "ENTRADA", cantidadComprada, costoDinamico, asientoId
                    );
                    break; 
                }
                
                // 3. VENTAS (Afectando cuentas de ingresos 4.x al Haber)
                else if ("4".equals(cuenta) || "4.1".equals(cuenta) || cuenta.startsWith("4.")) {
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
                            conn, 1, asiento.getFecha(), "SALIDA", unidadesVendidas, costoDinamico, asientoId
                        );
                        break; 
                    }
                }
            }
            // -----------------------------------------------------------------
            conn.commit(); // Se confirma todo de forma atómica
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
