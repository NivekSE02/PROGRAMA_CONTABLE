package com.mycompany.programa_contable.model;

public class KardexFilaDTO {
    private String fecha;
    private String concepto;
    private int entrada;
    private int salida;
    private int existencias; // Calculado automático
    private double costoUnitario;
    private double costoTotal;
    private double saldoMonetario; // Calculado automático

    public KardexFilaDTO(String fecha, String concepto, int entrada, int salida, int existencias, double costoUnitario, double costoTotal, double saldoMonetario) {
        this.fecha = fecha;
        this.concepto = concepto;
        this.entrada = entrada;
        this.salida = salida;
        this.existencias = existencias;
        this.costoUnitario = costoUnitario;
        this.costoTotal = costoTotal;
        this.saldoMonetario = saldoMonetario;
    }

    // Genera los Getters para todos los atributos aquí (getFecha(), getConcepto(), etc.)
    public String getFecha() { return fecha; }
    public String getConcepto() { return concepto; }
    public int getEntrada() { return entrada; }
    public int getSalida() { return salida; }
    public int getExistencias() { return existencias; }
    public double getCostoUnitario() { return costoUnitario; }
    public double getCostoTotal() { return costoTotal; }
    public double getSaldoMonetario() { return saldoMonetario; }
}