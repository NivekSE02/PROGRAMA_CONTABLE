package com.mycompany.programa_contable.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class EstadoResultadosDTO {

    public static class LineaReporte {

        private final String codigo;
        private final String nombre;
        private final double monto;

        public LineaReporte(String codigo, String nombre, double monto) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.monto = redondear(monto);
        }

        public String getCodigo() {
            return codigo;
        }

        public String getNombre() {
            return nombre;
        }

        public double getMonto() {
            return monto;
        }
    }

    private final List<LineaReporte> ingresosOperacion = new ArrayList<>();
    private final List<LineaReporte> otrosIngresos = new ArrayList<>();

    private final List<LineaReporte> devolucionesVentas = new ArrayList<>();

    private final List<LineaReporte> costosVenta = new ArrayList<>();
    private final List<LineaReporte> devolucionesCompras = new ArrayList<>();

    private final List<LineaReporte> gastosAdministracion = new ArrayList<>();
    private final List<LineaReporte> gastosVenta = new ArrayList<>();
    private final List<LineaReporte> gastosFinancieros = new ArrayList<>();

    private double totalIngresos;
    private double totalDevolucionesVentas;

    private double totalCostos;
    private double totalDevolucionesCompras;

    private double totalGastosOperacion;
    private double totalGastosFinancieros;
    private double totalCostosYGastos;

    private double utilidadBruta;
    private double utilidadOperacion;
    private double utilidadNeta;

    public void agregarIngreso(String codigo, String nombre, double monto, boolean esOperacional) {
        if (monto == 0) {
            return;
        }

        if (esOperacional) {
            ingresosOperacion.add(new LineaReporte(codigo, nombre, monto));
        } else {
            otrosIngresos.add(new LineaReporte(codigo, nombre, monto));
        }
    }

    public void agregarDevolucionVenta(String codigo, String nombre, double monto) {
        if (monto == 0) {
            return;
        }

        devolucionesVentas.add(new LineaReporte(codigo, nombre, monto));
    }

    public void agregarCosto(String codigo, String nombre, double monto) {
        if (monto == 0) {
            return;
        }

        costosVenta.add(new LineaReporte(codigo, nombre, monto));
    }

    public void agregarDevolucionCompra(String codigo, String nombre, double monto) {
        if (monto == 0) {
            return;
        }

        devolucionesCompras.add(new LineaReporte(codigo, nombre, monto));
    }

    public void agregarGastoAdministracion(String codigo, String nombre, double monto) {
        if (monto == 0) {
            return;
        }

        gastosAdministracion.add(new LineaReporte(codigo, nombre, monto));
    }

    public void agregarGastoVenta(String codigo, String nombre, double monto) {
        if (monto == 0) {
            return;
        }

        gastosVenta.add(new LineaReporte(codigo, nombre, monto));
    }

    public void agregarGastoFinanciero(String codigo, String nombre, double monto) {
        if (monto == 0) {
            return;
        }

        gastosFinancieros.add(new LineaReporte(codigo, nombre, monto));
    }

    public void calcularTotales() {

        // INGRESOS
        double ingresos = ingresosOperacion.stream()
                .mapToDouble(LineaReporte::getMonto)
                .sum();

        double otrosIngresosMonto = otrosIngresos.stream()
                .mapToDouble(LineaReporte::getMonto)
                .sum();

        double devolucionesVentasMonto = devolucionesVentas.stream()
                .mapToDouble(LineaReporte::getMonto)
                .sum();

        this.totalDevolucionesVentas = redondear(devolucionesVentasMonto);

        this.totalIngresos = redondear(
                ingresos
                + otrosIngresosMonto
                - devolucionesVentasMonto
        );

        // COSTOS
        double costos = costosVenta.stream()
                .mapToDouble(LineaReporte::getMonto)
                .sum();

        double devolucionesComprasMonto = devolucionesCompras.stream()
                .mapToDouble(LineaReporte::getMonto)
                .sum();

        this.totalCostos = redondear(costos);
        this.totalDevolucionesCompras = redondear(devolucionesComprasMonto);

        double costosNetos = redondear(
                costos - devolucionesComprasMonto
        );

        // UTILIDAD BRUTA
        this.utilidadBruta = redondear(
                totalIngresos - costosNetos
        );

        // GASTOS DE OPERACIÓN
        double gastosAdmin = gastosAdministracion.stream()
                .mapToDouble(LineaReporte::getMonto)
                .sum();

        // CORRECCIÓN: Cambiado de 'gastosVenta' a 'sumaGastosVenta' para evitar conflicto con la lista
        double sumaGastosVenta = gastosVenta.stream()
                .mapToDouble(LineaReporte::getMonto)
                .sum();

        double gastosFinancierosMonto = gastosFinancieros.stream()
                .mapToDouble(LineaReporte::getMonto)
                .sum();

        this.totalGastosOperacion = redondear(
                gastosAdmin + sumaGastosVenta
        );

        this.totalGastosFinancieros = redondear(
                gastosFinancierosMonto
        );

        // UTILIDAD OPERATIVA
        this.utilidadOperacion = redondear(
                utilidadBruta - totalGastosOperacion
        );

        // TOTAL DE COSTOS Y GASTOS
        this.totalCostosYGastos = redondear(
                costosNetos
                + totalGastosOperacion
                + totalGastosFinancieros
        );

        // UTILIDAD NETA
        this.utilidadNeta = redondear(
                totalIngresos - totalCostosYGastos
        );
    }
    private static double redondear(double val) {
        return BigDecimal.valueOf(val)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public List<LineaReporte> getIngresosOperacion() {
        return ingresosOperacion;
    }

    public List<LineaReporte> getOtrosIngresos() {
        return otrosIngresos;
    }

    public List<LineaReporte> getDevolucionesVentas() {
        return devolucionesVentas;
    }

    public List<LineaReporte> getCostosVenta() {
        return costosVenta;
    }

    public List<LineaReporte> getDevolucionesCompras() {
        return devolucionesCompras;
    }

    public List<LineaReporte> getGastosAdministracion() {
        return gastosAdministracion;
    }

    public List<LineaReporte> getGastosVenta() {
        return gastosVenta;
    }

    public List<LineaReporte> getGastosFinancieros() {
        return gastosFinancieros;
    }

    public double getTotalIngresos() {
        return totalIngresos;
    }

    public double getTotalDevolucionesVentas() {
        return totalDevolucionesVentas;
    }

    public double getTotalCostos() {
        return totalCostos;
    }

    public double getTotalDevolucionesCompras() {
        return totalDevolucionesCompras;
    }

    public double getTotalGastosOperacion() {
        return totalGastosOperacion;
    }

    public double getTotalGastosFinancieros() {
        return totalGastosFinancieros;
    }

    public double getTotalCostosYGastos() {
        return totalCostosYGastos;
    }

    public double getUtilidadBruta() {
        return utilidadBruta;
    }

    public double getUtilidadOperacion() {
        return utilidadOperacion;
    }

    public double getUtilidadNeta() {
        return utilidadNeta;
    }
}