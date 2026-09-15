package com.mycompany.programa_contable.model;

/**
 * Clasificación obligatoria por dígito según los requerimientos:
 * 1 = ACTIVO (Balance General)
 * 2 = PASIVO (Balance General)
 * 3 = CAPITAL CONTABLE (Balance General)
 * 4 = COSTOS Y GASTOS (Estado de Resultados)
 * 5 = INGRESOS (Estado de Resultados)
 */
public enum TipoCuenta {
    ACTIVO("1", "Activo", "Recursos y derechos de la empresa", NaturalezaCuenta.DEUDORA),
    PASIVO("2", "Pasivo", "Obligaciones y deudas con terceros", NaturalezaCuenta.ACREEDORA),
    CAPITAL("3", "Capital Contable / Patrimonio", "Aporte de socios y resultados", NaturalezaCuenta.ACREEDORA),
    COSTO_GASTO("4", "Costos y Gastos / Cuentas de Resultado Deudoras", "Erogaciones para la operatividad", NaturalezaCuenta.DEUDORA),
    INGRESO("5", "Ingresos / Cuentas de Resultado Acreedoras", "Entradas económicas por ventas o servicios", NaturalezaCuenta.ACREEDORA),
    CIERRE("6", "Cuenta de Cierre / Liquidadora", "Liquidación de pérdidas y ganancias", NaturalezaCuenta.ACREEDORA),
    ORDEN("7", "Cuentas de Orden", "Registro de valores contingentes y control", NaturalezaCuenta.DEUDORA);

    private final String digito;
    private final String nombre;
    private final String descripcion;
    private final NaturalezaCuenta naturalezaPorDefecto;

    TipoCuenta(String digito, String nombre, String descripcion, NaturalezaCuenta naturalezaPorDefecto) {
        this.digito = digito;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.naturalezaPorDefecto = naturalezaPorDefecto;
    }

    public String getDigito() {
        return digito;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public NaturalezaCuenta getNaturalezaPorDefecto() {
        return naturalezaPorDefecto;
    }

    /**
     * Clasificación dinámica por dígito según el catálogo de UNICAES:
     * 1 -> ACTIVO
     * 2 -> PASIVO
     * 3 -> CAPITAL / PATRIMONIO
     * 4 -> COSTOS Y GASTOS (RESULTADO DEUDORAS)
     * 5 -> INGRESOS (RESULTADO ACREEDORAS)
     * 6 -> CUENTA DE CIERRE
     * 7 -> CUENTAS DE ORDEN
     */
    public static TipoCuenta desdeCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            return ACTIVO;
        }
        char primerDigito = codigo.trim().charAt(0);
        return switch (primerDigito) {
            case '1' -> ACTIVO;
            case '2' -> PASIVO;
            case '3' -> CAPITAL;
            case '4' -> COSTO_GASTO;
            case '5' -> INGRESO;
            case '6' -> CIERRE;
            case '7' -> ORDEN;
            default -> ACTIVO;
        };
    }
}
