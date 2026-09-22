package com.mycompany.programa_contable.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class MayorCuenta {
    private String codigo;
    private String nombre;
    private TipoCuenta tipo;
    private NaturalezaCuenta naturaleza;
    private double totalDebe;
    private double totalHaber;
    private double saldoDeudor;
    private double saldoAcreedor;
    private int nivel;
    private boolean permiteMovimiento;
    private String cuentaPadre;
    private String subtipo;
    private List<MovimientoMayor> movimientos = new ArrayList<>();

    public MayorCuenta(String codigo, String nombre, TipoCuenta tipo, NaturalezaCuenta naturaleza, int nivel, boolean permiteMovimiento, String cuentaPadre) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.naturaleza = naturaleza;
        this.nivel = nivel;
        this.permiteMovimiento = permiteMovimiento;
        this.cuentaPadre = cuentaPadre;
    }

    public void agregarMovimiento(int asientoNum, String fecha, String concepto, double debe, double haber) {
        double d = redondear(debe);
        double h = redondear(haber);
        this.totalDebe = redondear(this.totalDebe + d);
        this.totalHaber = redondear(this.totalHaber + h);

        double saldoActual;
        if (naturaleza == NaturalezaCuenta.DEUDORA) {
            saldoActual = redondear(this.totalDebe - this.totalHaber);
        } else {
            saldoActual = redondear(this.totalHaber - this.totalDebe);
        }

        this.movimientos.add(new MovimientoMayor(asientoNum, fecha, concepto, d, h, saldoActual));
        calcularSaldosFinales();
    }

    public void calcularSaldosFinales() {
        double diff = redondear(totalDebe - totalHaber);
        if (diff > 0) {
            this.saldoDeudor = diff;
            this.saldoAcreedor = 0.0;
        } else if (diff < 0) {
            this.saldoDeudor = 0.0;
            this.saldoAcreedor = Math.abs(diff);
        } else {
            this.saldoDeudor = 0.0;
            this.saldoAcreedor = 0.0;
        }
    }

    public double getSaldoNeto() {
        if (naturaleza == NaturalezaCuenta.DEUDORA) {
            return redondear(totalDebe - totalHaber);
        } else {
            return redondear(totalHaber - totalDebe);
        }
    }

    private double redondear(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public TipoCuenta getTipo() { return tipo; }
    public NaturalezaCuenta getNaturaleza() { return naturaleza; }
    public double getTotalDebe() { return totalDebe; }
    public double getTotalHaber() { return totalHaber; }
    public double getSaldoDeudor() { return saldoDeudor; }
    public double getSaldoAcreedor() { return saldoAcreedor; }
    public int getNivel() { return nivel; }
    public boolean isPermiteMovimiento() { return permiteMovimiento; }
    public String getCuentaPadre() { return cuentaPadre; }
    public String getSubtipo() { return subtipo; }
    public void setSubtipo(String subtipo) { this.subtipo = subtipo; }
    public List<MovimientoMayor> getMovimientos() { return movimientos; }

    public String getCuentaDisplay() {
        return codigo + " - " + nombre;
    }
}
