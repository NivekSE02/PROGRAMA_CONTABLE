package com.mycompany.programa_contable.model;

public class MovimientoKardex {
    private int id;
    private int productoId;
    private String fecha;
    private String tipoMovimiento; // "ENTRADA" o "SALIDA"
    private int cantidad;
    private double costoUnitario;
    private double costoTotal;

    public MovimientoKardex(int id, int productoId, String fecha, String tipoMovimiento, int cantidad, double costoUnitario, double costoTotal) {
        this.id = id;
        this.productoId = productoId;
        this.fecha = fecha;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.costoTotal = costoTotal;
    }

    public int getId() { return id; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public int getCantidad() { return cantidad; }
    public double getCostoTotal() { return costoTotal; }
}