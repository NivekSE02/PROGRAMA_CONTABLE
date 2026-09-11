package com.mycompany.programa_contable.dao;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.Rol;
import com.mycompany.programa_contable.model.Usuario;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public Usuario autenticar(String username, String password) {
        String sql = "SELECT id, username, password, nombre_completo, rol, estado FROM usuarios WHERE username = ? AND password = ? AND estado = 'ACTIVO'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearUsuario(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO] Error en autenticación: " + e.getMessage());
        }
        return null;
    }

    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id, username, password, nombre_completo, rol, estado FROM usuarios ORDER BY id";
        try (Connection conn = dbManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(mapearUsuario(rs));
            }
        } catch (SQLException e) {
            System.err.println("[UsuarioDAO] Error listando usuarios: " + e.getMessage());
        }
        return lista;
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String user = rs.getString("username");
        String pass = rs.getString("password");
        String nombre = rs.getString("nombre_completo");
        String rolStr = rs.getString("rol");
        String estado = rs.getString("estado");

        Rol rol = Rol.ADMINISTRADOR;
        try {
            rol = Rol.valueOf(rolStr.toUpperCase());
        } catch (IllegalArgumentException ignored) {}

        return new Usuario(id, user, pass, nombre, rol, estado);
    }
}
