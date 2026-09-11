package com.mycompany.programa_contable.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DetalleAsiento {
    private int id;
    private int asientoId;
    private int renglon;
    private String cuentaCodigo;
    private String cuentaNombre;
    private String conceptoLinea;
    private double debe;
    private double haber;

    public DetalleAsiento() {}

    public DetalleAsiento(int renglon, String cuentaCodigo, String cuentaNombre, String conceptoLinea, double debe, double haber) {
        this.renglon = renglon;
        this.cuentaCodigo = cuentaCodigo;
        this.cuentaNombre = cuentaNombre;
        this.conceptoLinea = conceptoLinea;
        this.debe = redondear(debe);
        this.haber = redondear(haber);
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getAsientoId() { return asientoId; }
    public void setAsientoId(int asientoId) { this.asientoId = asientoId; }

    public int getRenglon() { return renglon; }
    public void setRenglon(int renglon) { this.renglon = renglon; }

    public String getCuentaCodigo() { return cuentaCodigo; }
    public void setCuentaCodigo(String cuentaCodigo) { this.cuentaCodigo = cuentaCodigo; }

    public String getCuentaNombre() { return cuentaNombre; }
    public void setCuentaNombre(String cuentaNombre) { this.cuentaNombre = cuentaNombre; }

    public String getConceptoLinea() { return conceptoLinea; }
    public void setConceptoLinea(String conceptoLinea) { this.conceptoLinea = conceptoLinea; }

    public double getDebe() { return debe; }
    public void setDebe(double debe) { this.debe = redondear(debe); }

    public double getHaber() { return haber; }
    public void setHaber(double haber) { this.haber = redondear(haber); }

    public String getCuentaDisplay() {
        if (cuentaCodigo == null) return "";
        return (cuentaNombre != null && !cuentaNombre.isEmpty())
                ? cuentaCodigo + " - " + cuentaNombre
                : cuentaCodigo;
    }
}
