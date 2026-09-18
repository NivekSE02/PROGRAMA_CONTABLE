package com.mycompany.programa_contable.model;

public class Producto {
    private int id;
    private String codigo;
    private String nombre;
    private double precioVenta;

    public Producto(int id, String codigo, String nombre, double precioVenta) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.precioVenta = precioVenta;
    }

    public int getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public double getPrecioVenta() { return precioVenta; }
}