package com.mycompany.programa_contable.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class BalanceGeneralDTO {

    public static class LineaBalance {
        private final String codigo;
        private final String nombre;
        private final double monto;

        public LineaBalance(String codigo, String nombre, double monto) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.monto = redondear(monto);
        }

        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public double getMonto() { return monto; }
    }

    // Código 1: ACTIVOS
    private final List<LineaBalance> activosCorrientes = new ArrayList<>();
    private final List<LineaBalance> activosNoCorrientes = new ArrayList<>();
    private double totalActivoCorriente;
    private double totalActivoNoCorriente;
    private double totalActivo; // Suma código 1

    // Código 2: PASIVOS
    private final List<LineaBalance> pasivosCorrientes = new ArrayList<>();
    private final List<LineaBalance> pasivosNoCorrientes = new ArrayList<>();
    private double totalPasivoCorriente;
    private double totalPasivoNoCorriente;
    private double totalPasivo; // Suma código 2

    // Código 3: CAPITAL CONTABLE / PATRIMONIO
    private final List<LineaBalance> cuentasCapital = new ArrayList<>();
    private double totalCapitalRegistrado;
    private double utilidadDelEjercicio; // Proveniente del Estado de Resultados (5 - 4)
    private double totalCapitalContable; // Suma código 3 + Utilidad del ejercicio

    // Verificación de la Ecuación Contable: 1 (Activo) = 2 (Pasivo) + 3 (Capital)
    private double totalPasivoMasCapital;
    private double diferencia;
    private boolean cuadrado;

    public void agregarActivo(String codigo, String nombre, double saldo, boolean esCorriente) {
        if (saldo == 0) return;
        if (esCorriente) {
            activosCorrientes.add(new LineaBalance(codigo, nombre, saldo));
        } else {
            activosNoCorrientes.add(new LineaBalance(codigo, nombre, saldo));
        }
    }

    public void agregarPasivo(String codigo, String nombre, double saldo, boolean esCorriente) {
        if (saldo == 0) return;
        if (esCorriente) {
            pasivosCorrientes.add(new LineaBalance(codigo, nombre, saldo));
        } else {
            pasivosNoCorrientes.add(new LineaBalance(codigo, nombre, saldo));
        }
    }

    public void agregarCapital(String codigo, String nombre, double saldo) {
        if (saldo == 0) return;
        cuentasCapital.add(new LineaBalance(codigo, nombre, saldo));
    }

    public void calcularTotales(double utilidadDelPeriodo) {
        this.utilidadDelEjercicio = redondear(utilidadDelPeriodo);

        this.totalActivoCorriente = redondear(activosCorrientes.stream().mapToDouble(LineaBalance::getMonto).sum());
        this.totalActivoNoCorriente = redondear(activosNoCorrientes.stream().mapToDouble(LineaBalance::getMonto).sum());
        this.totalActivo = redondear(totalActivoCorriente + totalActivoNoCorriente);

        this.totalPasivoCorriente = redondear(pasivosCorrientes.stream().mapToDouble(LineaBalance::getMonto).sum());
        this.totalPasivoNoCorriente = redondear(pasivosNoCorrientes.stream().mapToDouble(LineaBalance::getMonto).sum());
        this.totalPasivo = redondear(totalPasivoCorriente + totalPasivoNoCorriente);

        this.totalCapitalRegistrado = redondear(cuentasCapital.stream().mapToDouble(LineaBalance::getMonto).sum());
        this.totalCapitalContable = redondear(totalCapitalRegistrado + utilidadDelEjercicio);

        // Ecuación Patrimonial Obligatoria: 1 = 2 + 3
        this.totalPasivoMasCapital = redondear(totalPasivo + totalCapitalContable);
        this.diferencia = redondear(Math.abs(totalActivo - totalPasivoMasCapital));

        // Ajuste automático de tolerancia para centavos por redondeo (menor a $1.00)
        if (this.diferencia > 0 && this.diferencia < 1.00) {
            if (totalActivo > totalPasivoMasCapital) {
                this.utilidadDelEjercicio = redondear(this.utilidadDelEjercicio + this.diferencia);
            } else {
                this.utilidadDelEjercicio = redondear(this.utilidadDelEjercicio - this.diferencia);
            }
            // Recalculamos con el centavo absorbido
            this.totalCapitalContable = redondear(totalCapitalRegistrado + utilidadDelEjercicio);
            this.totalPasivoMasCapital = redondear(totalPasivo + totalCapitalContable);
            this.diferencia = redondear(Math.abs(totalActivo - totalPasivoMasCapital));
        }

        this.cuadrado = this.diferencia < 0.005;
    }

    private static double redondear(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public List<LineaBalance> getActivosCorrientes() { return activosCorrientes; }
    public List<LineaBalance> getActivosNoCorrientes() { return activosNoCorrientes; }
    public double getTotalActivoCorriente() { return totalActivoCorriente; }
    public double getTotalActivoNoCorriente() { return totalActivoNoCorriente; }
    public double getTotalActivo() { return totalActivo; }

    public List<LineaBalance> getPasivosCorrientes() { return pasivosCorrientes; }
    public List<LineaBalance> getPasivosNoCorrientes() { return pasivosNoCorrientes; }
    public double getTotalPasivoCorriente() { return totalPasivoCorriente; }
    public double getTotalPasivoNoCorriente() { return totalPasivoNoCorriente; }
    public double getTotalPasivo() { return totalPasivo; }

    public List<LineaBalance> getCuentasCapital() { return cuentasCapital; }
    public double getTotalCapitalRegistrado() { return totalCapitalRegistrado; }
    public double getUtilidadDelEjercicio() { return utilidadDelEjercicio; }
    public double getTotalCapitalContable() { return totalCapitalContable; }

    public double getTotalPasivoMasCapital() { return totalPasivoMasCapital; }
    public double getDiferencia() { return diferencia; }
    public boolean isCuadrado() { return cuadrado; }
}