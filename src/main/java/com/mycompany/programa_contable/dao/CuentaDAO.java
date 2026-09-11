package com.mycompany.programa_contable.dao;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.Cuenta;
import com.mycompany.programa_contable.model.NaturalezaCuenta;
import com.mycompany.programa_contable.model.TipoCuenta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CuentaDAO {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public List<Cuenta> listarTodas() {
        List<Cuenta> lista = new ArrayList<>();
        String sql = "SELECT codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento FROM cuentas ORDER BY codigo ASC";
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapearCuenta(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CuentaDAO] Error listando cuentas: " + e.getMessage());
        }
        return lista;
    }

    public List<Cuenta> listarPermitenMovimiento() {
        List<Cuenta> lista = new ArrayList<>();
        String sql = "SELECT codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento FROM cuentas WHERE permite_movimiento = 1 ORDER BY codigo ASC";
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapearCuenta(rs));
            }
        } catch (SQLException e) {
            System.err.println("[CuentaDAO] Error listando cuentas con movimiento: " + e.getMessage());
        }
        return lista;
    }

    public Cuenta buscarPorCodigo(String codigo) {
        String sql = "SELECT codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento FROM cuentas WHERE codigo = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCuenta(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[CuentaDAO] Error buscando cuenta: " + e.getMessage());
        }
        return null;
    }

    public List<Cuenta> buscar(String filtro) {
        List<Cuenta> lista = new ArrayList<>();
        String sql = "SELECT codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento FROM cuentas " +
                     "WHERE codigo LIKE ? OR LOWER(nombre) LIKE ? ORDER BY codigo ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String f = "%" + (filtro != null ? filtro.trim().toLowerCase() : "") + "%";
            ps.setString(1, f);
            ps.setString(2, f);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearCuenta(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[CuentaDAO] Error filtrando cuentas: " + e.getMessage());
        }
        return lista;
    }

    public boolean insertar(Cuenta c) {
        String sql = "INSERT INTO cuentas (codigo, nombre, tipo, subtipo, nivel, naturaleza, cuenta_padre, permite_movimiento) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCodigo());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getTipo().name());
            ps.setString(4, c.getSubtipo());
            ps.setInt(5, c.getNivel());
            ps.setString(6, c.getNaturaleza().name());
            ps.setString(7, c.getCuentaPadre());
            ps.setInt(8, c.isPermiteMovimiento() ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CuentaDAO] Error insertando cuenta: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Cuenta c) {
        String sql = "UPDATE cuentas SET nombre = ?, tipo = ?, subtipo = ?, nivel = ?, naturaleza = ?, cuenta_padre = ?, permite_movimiento = ? WHERE codigo = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getTipo().name());
            ps.setString(3, c.getSubtipo());
            ps.setInt(4, c.getNivel());
            ps.setString(6, c.getCuentaPadre());
            ps.setInt(7, c.isPermiteMovimiento() ? 1 : 0);
            ps.setString(8, c.getCodigo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CuentaDAO] Error actualizando cuenta: " + e.getMessage());
            return false;
        }
    }

    public boolean tieneMovimientos(String codigo) {
        String sql = "SELECT COUNT(*) FROM detalle_asiento WHERE cuenta_codigo = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[CuentaDAO] Error verificando movimientos: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminar(String codigo) {
        if (tieneMovimientos(codigo)) {
            return false;
        }
        String sql = "DELETE FROM cuentas WHERE codigo = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[CuentaDAO] Error eliminando cuenta: " + e.getMessage());
            return false;
        }
    }

    private Cuenta mapearCuenta(ResultSet rs) throws SQLException {
        String codigo = rs.getString("codigo");
        String nombre = rs.getString("nombre");
        String tipoStr = rs.getString("tipo");
        String subtipo = rs.getString("subtipo");
        int nivel = rs.getInt("nivel");
        String natStr = rs.getString("naturaleza");
        String padre = rs.getString("cuenta_padre");
        boolean permiteMov = rs.getInt("permite_movimiento") == 1;

        TipoCuenta tipo;
        try {
            tipo = TipoCuenta.valueOf(tipoStr);
        } catch (Exception e) {
            tipo = TipoCuenta.desdeCodigo(codigo);
        }

        NaturalezaCuenta nat;
        try {
            nat = NaturalezaCuenta.valueOf(natStr);
        } catch (Exception e) {
            nat = tipo.getNaturalezaPorDefecto();
        }

        return new Cuenta(codigo, nombre, tipo, subtipo, nivel, nat, padre, permiteMov);
    }
}
