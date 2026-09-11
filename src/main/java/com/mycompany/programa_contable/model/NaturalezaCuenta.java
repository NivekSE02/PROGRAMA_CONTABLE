package com.mycompany.programa_contable.model;

public enum NaturalezaCuenta {
    DEUDORA("Deudora", "Aumenta al Debe, disminuye al Haber"),
    ACREEDORA("Acreedora", "Aumenta al Haber, disminuye al Debe");

    private final String etiqueta;
    private final String explicacion;

    NaturalezaCuenta(String etiqueta, String explicacion) {
        this.etiqueta = etiqueta;
        this.explicacion = explicacion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getExplicacion() {
        return explicacion;
    }
}
