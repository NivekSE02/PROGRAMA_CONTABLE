package com.mycompany.programa_contable.model;

public class MovimientoMayor {
    private int asientoNumero;
    private String fecha;
    private String concepto;
    private double debe;
    private double haber;
    private double saldoAcumulado;

    public MovimientoMayor(int asientoNumero, String fecha, String concepto, double debe, double haber, double saldoAcumulado) {
        this.asientoNumero = asientoNumero;
        this.fecha = fecha;
        this.concepto = concepto;
        this.debe = debe;
        this.haber = haber;
        this.saldoAcumulado = saldoAcumulado;
    }

    public int getAsientoNumero() { return asientoNumero; }
    public String getFecha() { return fecha; }
    public String getConcepto() { return concepto; }
    public double getDebe() { return debe; }
    public double getHaber() { return haber; }
    public double getSaldoAcumulado() { return saldoAcumulado; }
}
