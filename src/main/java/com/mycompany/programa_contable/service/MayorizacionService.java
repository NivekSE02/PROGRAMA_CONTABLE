package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.Cuenta;
import com.mycompany.programa_contable.model.MayorCuenta;
import com.mycompany.programa_contable.model.NaturalezaCuenta;
import com.mycompany.programa_contable.model.TipoCuenta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de Mayorización Automática en Tiempo Real:
 * Consolida automáticamente los débitos y créditos para actualizar el saldo
 * (Deudor / Acreedor) de cada cuenta del Catálogo, sin cálculos manuales.
 */
public class MayorizacionService {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    /**
     * Obtiene la consolidación en tiempo real de todas las cuentas que tienen movimientos.
     */
    public List<MayorCuenta> obtenerMayorizacionCompleta() {
        return obtenerMayorizacion(false);
    }

    /**
     * Obtiene la mayorización incluyendo o no cuentas sin movimiento.
     */
    public List<MayorCuenta> obtenerMayorizacion(boolean incluirCuentasSinMovimiento) {
        Map<String, MayorCuenta> mapaMayor = new LinkedHashMap<>();

        // 1. Cargar todas las cuentas del catálogo ordenadas por código
        String sqlCuentas = "SELECT codigo, nombre, tipo, naturaleza, nivel, permite_movimiento, cuenta_padre FROM cuentas ORDER BY codigo ASC";
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sqlCuentas)) {
            while (rs.next()) {
                String cod = rs.getString("codigo");
                String nom = rs.getString("nombre");
                String tipStr = rs.getString("tipo");
                String natStr = rs.getString("naturaleza");

                TipoCuenta tipo;
                try {
                    tipo = TipoCuenta.valueOf(tipStr);
                } catch (Exception e) {
                    tipo = TipoCuenta.desdeCodigo(cod);
                }

                NaturalezaCuenta nat;
                try {
                    nat = NaturalezaCuenta.valueOf(natStr);
                } catch (Exception e) {
                    nat = tipo.getNaturalezaPorDefecto();
                }

                int nivel = rs.getInt("nivel");
                boolean permiteMov = rs.getInt("permite_movimiento") == 1;
                String padre = rs.getString("cuenta_padre");

                mapaMayor.put(cod, new MayorCuenta(cod, nom, tipo, nat, nivel, permiteMov, padre));
            }
        } catch (SQLException e) {
            System.err.println("[MayorizacionService] Error al cargar catálogo: " + e.getMessage());
        }

        // 2. Consolidar en tiempo real todos los movimientos desde detalle_asiento
        String sqlMovs = "SELECT d.cuenta_codigo, a.numero as asiento_num, a.fecha, " +
                         "COALESCE(d.concepto_linea, a.concepto) as concepto, d.debe, d.haber " +
                         "FROM detalle_asiento d " +
                         "INNER JOIN asientos a ON d.asiento_id = a.id " +
                         "ORDER BY a.fecha ASC, a.numero ASC, d.renglon ASC";

        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sqlMovs)) {
            while (rs.next()) {
                String ctaCod = rs.getString("cuenta_codigo");
                int num = rs.getInt("asiento_num");
                String fec = rs.getString("fecha");
                String conc = rs.getString("concepto");
                double d = rs.getDouble("debe");
                double h = rs.getDouble("haber");

                MayorCuenta mayor = mapaMayor.get(ctaCod);
                if (mayor != null) {
                    mayor.agregarMovimiento(num, fec, conc, d, h);
                }
            }
        } catch (SQLException e) {
            System.err.println("[MayorizacionService] Error al consolidar movimientos: " + e.getMessage());
        }

        List<MayorCuenta> resultado = new ArrayList<>();
        for (MayorCuenta m : mapaMayor.values()) {
            if (incluirCuentasSinMovimiento || (m.getTotalDebe() > 0 || m.getTotalHaber() > 0)) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    public List<MayorCuenta> obtenerMayorizacionParaCuentasT() {
        Map<String, MayorCuenta> mapaMayor = new LinkedHashMap<>();

        String sqlCuentas = "SELECT codigo, nombre, tipo, naturaleza, nivel, permite_movimiento, cuenta_padre FROM cuentas ORDER BY codigo ASC";
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sqlCuentas)) {
            while (rs.next()) {
                String cod = rs.getString("codigo");
                String nom = rs.getString("nombre");
                String tipStr = rs.getString("tipo");
                String natStr = rs.getString("naturaleza");

                TipoCuenta tipo;
                try {
                    tipo = TipoCuenta.valueOf(tipStr);
                } catch (Exception e) {
                    tipo = TipoCuenta.desdeCodigo(cod);
                }

                NaturalezaCuenta nat;
                try {
                    nat = NaturalezaCuenta.valueOf(natStr);
                } catch (Exception e) {
                    nat = tipo.getNaturalezaPorDefecto();
                }

                int nivel = rs.getInt("nivel");
                boolean permiteMov = rs.getInt("permite_movimiento") == 1;
                String padre = rs.getString("cuenta_padre");

                mapaMayor.put(cod, new MayorCuenta(cod, nom, tipo, nat, nivel, permiteMov, padre));
            }
        } catch (SQLException e) {
            System.err.println("[MayorizacionService] Error al cargar catálogo para T: " + e.getMessage());
        }

        String sqlMovs = "SELECT d.cuenta_codigo, a.numero as asiento_num, a.fecha, " +
                         "COALESCE(d.concepto_linea, a.concepto) as concepto, d.debe, d.haber " +
                         "FROM detalle_asiento d " +
                         "INNER JOIN asientos a ON d.asiento_id = a.id " +
                         "ORDER BY a.fecha ASC, a.numero ASC, d.renglon ASC";

        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sqlMovs)) {
            while (rs.next()) {
                String ctaCod = rs.getString("cuenta_codigo");
                int num = rs.getInt("asiento_num");
                String fec = rs.getString("fecha");
                String conc = rs.getString("concepto");
                double d = rs.getDouble("debe");
                double h = rs.getDouble("haber");

                String currentCod = ctaCod;
                // Aplicamos el roll-up recursivo solo para las Cuentas T
                while (currentCod != null) {
                    MayorCuenta mayor = mapaMayor.get(currentCod);
                    if (mayor != null) {
                        mayor.agregarMovimiento(num, fec, conc, d, h);
                        currentCod = mayor.getCuentaPadre();
                    } else {
                        currentCod = null;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[MayorizacionService] Error al consolidar T: " + e.getMessage());
        }

        List<MayorCuenta> resultado = new ArrayList<>();
        for (MayorCuenta m : mapaMayor.values()) {
            // Solo cuentas de Nivel 2 con movimientos
            if (m.getNivel() == 2 && (m.getTotalDebe() > 0 || m.getTotalHaber() > 0)) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    /**
     * Obtiene la cuenta T o mayor detallado de una cuenta específica.
     */
    public MayorCuenta obtenerMayorDeCuenta(String codigoCuenta) {
        String sqlCuenta = "SELECT codigo, nombre, tipo, naturaleza, nivel, permite_movimiento, cuenta_padre FROM cuentas WHERE codigo = ?";
        MayorCuenta mayor = null;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlCuenta)) {
            ps.setString(1, codigoCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String cod = rs.getString("codigo");
                    String nom = rs.getString("nombre");
                    
                    TipoCuenta tipo;
                    try {
                        tipo = TipoCuenta.valueOf(rs.getString("tipo"));
                    } catch (Exception e) {
                        tipo = TipoCuenta.desdeCodigo(cod);
                    }
                    
                    NaturalezaCuenta nat;
                    try {
                        nat = NaturalezaCuenta.valueOf(rs.getString("naturaleza"));
                    } catch (Exception e) {
                        nat = tipo.getNaturalezaPorDefecto();
                    }
                    
                    int nivel = rs.getInt("nivel");
                    boolean permiteMov = rs.getInt("permite_movimiento") == 1;
                    String padre = rs.getString("cuenta_padre");
                    
                    mayor = new MayorCuenta(cod, nom, tipo, nat, nivel, permiteMov, padre);
                }
            }
        } catch (SQLException e) {
            System.err.println("[MayorizacionService] Error al buscar cuenta para mayor: " + e.getMessage());
        }

        if (mayor == null) return null;

        String sqlMovs = "SELECT a.numero as asiento_num, a.fecha, " +
                         "COALESCE(d.concepto_linea, a.concepto) as concepto, d.debe, d.haber " +
                         "FROM detalle_asiento d " +
                         "INNER JOIN asientos a ON d.asiento_id = a.id " +
                         "WHERE d.cuenta_codigo = ? " +
                         "ORDER BY a.fecha ASC, a.numero ASC, d.renglon ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlMovs)) {
            ps.setString(1, codigoCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mayor.agregarMovimiento(
                        rs.getInt("asiento_num"),
                        rs.getString("fecha"),
                        rs.getString("concepto"),
                        rs.getDouble("debe"),
                        rs.getDouble("haber")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[MayorizacionService] Error cargando movimientos de cuenta " + codigoCuenta + ": " + e.getMessage());
        }

        return mayor;
    }
}
