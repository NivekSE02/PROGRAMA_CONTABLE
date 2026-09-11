package com.mycompany.programa_contable.model;

public class Cuenta {
    private String codigo;
    private String nombre;
    private TipoCuenta tipo;
    private String subtipo;
    private int nivel;
    private NaturalezaCuenta naturaleza;
    private String cuentaPadre;
    private boolean permiteMovimiento;

    public Cuenta() {}

    public Cuenta(String codigo, String nombre, TipoCuenta tipo, String subtipo, int nivel, NaturalezaCuenta naturaleza, String cuentaPadre, boolean permiteMovimiento) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo != null ? tipo : TipoCuenta.desdeCodigo(codigo);
        this.subtipo = subtipo;
        this.nivel = nivel;
        this.naturaleza = naturaleza != null ? naturaleza : this.tipo.getNaturalezaPorDefecto();
        this.cuentaPadre = cuentaPadre;
        this.permiteMovimiento = permiteMovimiento;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
        if (this.tipo == null) {
            this.tipo = TipoCuenta.desdeCodigo(codigo);
        }
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public TipoCuenta getTipo() { return tipo; }
    public void setTipo(TipoCuenta tipo) { this.tipo = tipo; }

    public String getSubtipo() { return subtipo; }
    public void setSubtipo(String subtipo) { this.subtipo = subtipo; }

    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    public NaturalezaCuenta getNaturaleza() { return naturaleza; }
    public void setNaturaleza(NaturalezaCuenta naturaleza) { this.naturaleza = naturaleza; }

    public String getCuentaPadre() { return cuentaPadre; }
    public void setCuentaPadre(String cuentaPadre) { this.cuentaPadre = cuentaPadre; }

    public boolean isPermiteMovimiento() { return permiteMovimiento; }
    public void setPermiteMovimiento(boolean permiteMovimiento) { this.permiteMovimiento = permiteMovimiento; }

    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}
