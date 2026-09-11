package com.mycompany.programa_contable.model;

public enum Rol {
    ADMINISTRADOR("Administrador", "Acceso total al sistema, catálogo y usuarios"),
    CONTADOR("Contador", "Registro de asientos, mayorización y reportes"),
    AUDITOR("Auditor", "Consulta y revisión de estados financieros (solo lectura)");

    private final String etiqueta;
    private final String descripcion;

    Rol(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
