package com.mycompany.programa_contable.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BalanzaComprobacionDTO {

    public static class Renglon {
        private final String codigo;
        private final String nombre;
        private final TipoCuenta tipo;
        private final double movimientoDebe;
        private final double movimientoHaber;
        private final double saldoDeudor;
        private final double saldoAcreedor;

        public Renglon(String codigo, String nombre, TipoCuenta tipo, double movimientoDebe, double movimientoHaber, double saldoDeudor, double saldoAcreedor) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.tipo = tipo;
            this.movimientoDebe = redondear(movimientoDebe);
            this.movimientoHaber = redondear(movimientoHaber);
            this.saldoDeudor = redondear(saldoDeudor);
            this.saldoAcreedor = redondear(saldoAcreedor);
        }

        private static double redondear(double val) {
            return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public TipoCuenta getTipo() { return tipo; }
        public double getMovimientoDebe() { return movimientoDebe; }
        public double getMovimientoHaber() { return movimientoHaber; }
        public double getSaldoDeudor() { return saldoDeudor; }
        public double getSaldoAcreedor() { return saldoAcreedor; }
    }

    private final List<Renglon> renglones = new ArrayList<>();
    private double totalMovimientoDebe;
    private double totalMovimientoHaber;
    private double totalSaldoDeudor;
    private double totalSaldoAcreedor;

    public void agregarRenglon(Renglon r) {
        renglones.add(r);
        totalMovimientoDebe = redondear(totalMovimientoDebe + r.getMovimientoDebe());
        totalMovimientoHaber = redondear(totalMovimientoHaber + r.getMovimientoHaber());
        totalSaldoDeudor = redondear(totalSaldoDeudor + r.getSaldoDeudor());
        totalSaldoAcreedor = redondear(totalSaldoAcreedor + r.getSaldoAcreedor());
    }

    public boolean isCuadrada() {
        boolean movimientosCuadran = Math.abs(totalMovimientoDebe - totalMovimientoHaber) < 0.005;
        boolean saldosCuadran = Math.abs(totalSaldoDeudor - totalSaldoAcreedor) < 0.005;
        return movimientosCuadran && saldosCuadran && totalMovimientoDebe > 0;
    }

    private static double redondear(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public List<Renglon> getRenglones() { return renglones; }
    public double getTotalMovimientoDebe() { return totalMovimientoDebe; }
    public double getTotalMovimientoHaber() { return totalMovimientoHaber; }
    public double getTotalSaldoDeudor() { return totalSaldoDeudor; }
    public double getTotalSaldoAcreedor() { return totalSaldoAcreedor; }
}
