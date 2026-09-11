package com.mycompany.programa_contable.model;

public class Usuario {
    private int id;
    private String username;
    private String password;
    private String nombreCompleto;
    private Rol rol;
    private String estado;

    public Usuario() {}

    public Usuario(int id, String username, String password, String nombreCompleto, Rol rol, String estado) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nombreCompleto = nombreCompleto;
        this.rol = rol;
        this.estado = estado;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() {
        return nombreCompleto + " (" + (rol != null ? rol.getEtiqueta() : "") + ")";
    }
}
