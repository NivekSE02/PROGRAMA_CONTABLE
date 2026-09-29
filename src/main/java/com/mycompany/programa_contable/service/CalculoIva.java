package com.mycompany.programa_contable.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Desglosa un importe gravado para los asientos del Libro Diario. */
public final class CalculoIva {

    public record Desglose(double base, double impuesto) {}

    private CalculoIva() {}

    public static Desglose desglosar(double importe, double tasa, boolean ivaIncluido) {
        if (!Double.isFinite(importe) || importe < 0 || !Double.isFinite(tasa) || tasa < 0) {
            throw new IllegalArgumentException("El importe y la tasa de IVA deben ser valores no negativos.");
        }
        BigDecimal total = BigDecimal.valueOf(importe);
        BigDecimal tasaDecimal = BigDecimal.valueOf(tasa);
        BigDecimal base = ivaIncluido
                ? total.divide(BigDecimal.ONE.add(tasaDecimal), 2, RoundingMode.HALF_UP)
                : total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal impuesto = ivaIncluido
                ? total.subtract(base).setScale(2, RoundingMode.HALF_UP)
                : base.multiply(tasaDecimal).setScale(2, RoundingMode.HALF_UP);
        return new Desglose(base.doubleValue(), impuesto.doubleValue());
    }
}
