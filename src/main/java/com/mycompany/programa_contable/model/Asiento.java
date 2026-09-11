package com.mycompany.programa_contable.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Asiento {
    private int id;
    private int numero;
    private String fecha;
    private String concepto;
    private double totalDebe;
    private double totalHaber;
    private int usuarioId;
    private String usuarioNombre;
    private String createdAt;
    private List<DetalleAsiento> detalles = new ArrayList<>();

    public Asiento() {}

    public Asiento(int numero, String fecha, String concepto, int usuarioId) {
        this.numero = numero;
        this.fecha = fecha;
        this.concepto = concepto;
        this.usuarioId = usuarioId;
    }

    public void recalcularTotales() {
        double d = 0.0;
        double h = 0.0;
        for (DetalleAsiento det : detalles) {
            d += det.getDebe();
            h += det.getHaber();
        }
        this.totalDebe = redondear(d);
        this.totalHaber = redondear(h);
    }

    public double getDiferencia() {
        recalcularTotales();
        return redondear(Math.abs(totalDebe - totalHaber));
    }

    /**
     * Validación obligatoria de la Partida Doble:
     * 1. Total Debe == Total Haber (diferencia menor a 0.005)
     * 2. Total Debe > 0 (no se permiten asientos en cero)
     * 3. Mínimo 2 renglones en el detalle
     */
    public boolean isPartidaDobleValida() {
        recalcularTotales();
        if (detalles == null || detalles.size() < 2) {
            return false;
        }
        if (totalDebe <= 0.0 || totalHaber <= 0.0) {
            return false;
        }
        return Math.abs(totalDebe - totalHaber) < 0.005;
    }

    private double redondear(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public double getTotalDebe() { return totalDebe; }
    public void setTotalDebe(double totalDebe) { this.totalDebe = redondear(totalDebe); }

    public double getTotalHaber() { return totalHaber; }
    public void setTotalHaber(double totalHaber) { this.totalHaber = redondear(totalHaber); }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<DetalleAsiento> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleAsiento> detalles) {
        this.detalles = detalles != null ? detalles : new ArrayList<>();
        recalcularTotales();
    }

    public void agregarDetalle(DetalleAsiento det) {
        det.setRenglon(this.detalles.size() + 1);
        this.detalles.add(det);
        recalcularTotales();
    }
}
