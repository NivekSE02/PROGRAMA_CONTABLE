package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.model.Rol;
import com.mycompany.programa_contable.model.Usuario;

public class SessionManager {

    private static SessionManager instance;
    private Usuario usuarioActual;

    private SessionManager() {
        // Por defecto puede iniciar como administrador para desarrollo/demostración
        this.usuarioActual = new Usuario(1, "admin", "admin123", "Lic. Kevin Administrador", Rol.ADMINISTRADOR, "ACTIVO");
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public void setUsuarioActual(Usuario usuarioActual) {
        this.usuarioActual = usuarioActual;
    }

    public boolean tieneRol(Rol rolRequerido) {
        if (usuarioActual == null) return false;
        if (usuarioActual.getRol() == Rol.ADMINISTRADOR) return true; // Administrador tiene acceso a todo
        return usuarioActual.getRol() == rolRequerido;
    }

    public boolean puedeRegistrarAsientos() {
        if (usuarioActual == null) return false;
        return usuarioActual.getRol() == Rol.ADMINISTRADOR || usuarioActual.getRol() == Rol.CONTADOR;
    }

    public boolean puedeModificarCatalogo() {
        if (usuarioActual == null) return false;
        return usuarioActual.getRol() == Rol.ADMINISTRADOR;
    }

    public void cerrarSesion() {
        this.usuarioActual = null;
    }
}
