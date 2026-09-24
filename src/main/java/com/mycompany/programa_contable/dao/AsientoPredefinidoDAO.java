package com.mycompany.programa_contable.dao;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.DetalleAsiento;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Persiste plantillas independientes de los asientos contabilizados. */
public class AsientoPredefinidoDAO {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public AsientoPredefinidoDAO() { asegurarEsquema(); }

    private void asegurarEsquema() {
        String tipoTexto = db.getMotorActivo() == DatabaseManager.MotorBD.SQL_SERVER ? "NVARCHAR(255)" : "TEXT";
        String tipoCuenta = db.getMotorActivo() == DatabaseManager.MotorBD.SQL_SERVER ? "NVARCHAR(50)" : "TEXT";
        try (Connection c = db.getConnection(); Statement s = c.createStatement()) {
            if (!tablaExiste(c, "asientos_predefinidos"))
                s.executeUpdate("CREATE TABLE asientos_predefinidos (nombre " + tipoTexto + " PRIMARY KEY, concepto " + tipoTexto + ")");
            if (!tablaExiste(c, "detalle_asiento_predefinido"))
                s.executeUpdate("CREATE TABLE detalle_asiento_predefinido (nombre " + tipoTexto + " NOT NULL, renglon INT NOT NULL, cuenta_codigo " + tipoCuenta + " NOT NULL, concepto_linea " + tipoTexto + ", lado " + (db.getMotorActivo() == DatabaseManager.MotorBD.SQL_SERVER ? "NVARCHAR(10)" : "TEXT") + " NOT NULL, PRIMARY KEY (nombre, renglon), FOREIGN KEY (nombre) REFERENCES asientos_predefinidos(nombre) ON DELETE CASCADE, FOREIGN KEY (cuenta_codigo) REFERENCES cuentas(codigo))");
            s.executeUpdate("UPDATE detalle_asiento_predefinido SET concepto_linea = ''");
        } catch (SQLException e) {
            System.err.println("[AsientoPredefinidoDAO] No se pudo inicializar almacenamiento: " + e.getMessage());
        }
    }

    private boolean tablaExiste(Connection c, String tabla) throws SQLException {
        if (db.getMotorActivo() == DatabaseManager.MotorBD.SQL_SERVER) {
            try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?")) { p.setString(1, tabla); try (ResultSet r=p.executeQuery()) { return r.next(); } }
        }
        try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) { p.setString(1, tabla); try (ResultSet r=p.executeQuery()) { return r.next(); } }
    }

    public List<String> listarNombres() {
        List<String> nombres = new ArrayList<>();
        try (Connection c = db.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT nombre FROM asientos_predefinidos ORDER BY nombre")) {
            while (r.next()) nombres.add(r.getString(1));
        } catch (SQLException e) { System.err.println("[AsientoPredefinidoDAO] " + e.getMessage()); }
        return nombres;
    }

    public String cargarConcepto(String nombre) {
        try (Connection c = db.getConnection(); PreparedStatement p = c.prepareStatement("SELECT concepto FROM asientos_predefinidos WHERE nombre = ?")) {
            p.setString(1, nombre); try (ResultSet r = p.executeQuery()) { if (r.next()) return r.getString(1); }
        } catch (SQLException e) { System.err.println("[AsientoPredefinidoDAO] " + e.getMessage()); }
        return "";
    }

    public List<DetalleAsiento> cargarDetalles(String nombre) {
        List<DetalleAsiento> filas = new ArrayList<>();
        String sql = "SELECT d.renglon,d.cuenta_codigo,c.nombre,d.concepto_linea,d.lado FROM detalle_asiento_predefinido d LEFT JOIN cuentas c ON c.codigo=d.cuenta_codigo WHERE d.nombre=? ORDER BY d.renglon";
        try (Connection c = db.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, nombre); try (ResultSet r = p.executeQuery()) { while (r.next()) filas.add(new DetalleAsiento(r.getInt(1), r.getString(2), r.getString(3), r.getString(4), "DEBE".equals(r.getString(5)) ? -1 : 0, "HABER".equals(r.getString(5)) ? -1 : 0)); }
        } catch (SQLException e) { System.err.println("[AsientoPredefinidoDAO] " + e.getMessage()); }
        return filas;
    }

    public boolean guardar(String nombre, String concepto, List<DetalleAsiento> filas) throws SQLException {
        try (Connection c = db.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("DELETE FROM asientos_predefinidos WHERE nombre=?")) { p.setString(1,nombre); p.executeUpdate(); }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO asientos_predefinidos(nombre,concepto) VALUES(?,?)")) { p.setString(1,nombre); p.setString(2,concepto); p.executeUpdate(); }
                String sql = "INSERT INTO detalle_asiento_predefinido(nombre,renglon,cuenta_codigo,concepto_linea,lado) VALUES(?,?,?,?,?)";
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    int row=1; String ultimoLado = "DEBE";
                    for (DetalleAsiento d: filas) {
                        if (d.getCuentaCodigo()==null || d.getCuentaCodigo().isBlank()) continue;
                        if (d.getDebe()>0) ultimoLado = "DEBE";
                        else if (d.getHaber()>0) ultimoLado = "HABER";
                        p.setString(1,nombre); p.setInt(2,row++); p.setString(3,d.getCuentaCodigo()); p.setString(4, "");
                        p.setString(5, ultimoLado); p.addBatch();
                    }
                    p.executeBatch();
                }
                c.commit(); return true;
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public boolean eliminar(String nombre) {
        try (Connection c = db.getConnection(); PreparedStatement p = c.prepareStatement("DELETE FROM asientos_predefinidos WHERE nombre=?")) { p.setString(1,nombre); return p.executeUpdate()>0; }
        catch (SQLException e) { System.err.println("[AsientoPredefinidoDAO] " + e.getMessage()); return false; }
    }
}
