package com.mycompany.programa_contable.model;

/**
 * Clasificación principal de las cuentas según su código.
 *
 * 1 = ACTIVO
 * 2 = PASIVO
 * 3 = PATRIMONIO
 * 4 = INGRESOS
 * 5 = COSTOS
 * 6 = GASTOS
 * 7 = CUENTAS DE ORDEN
 */
public enum TipoCuenta {

    ACTIVO(
        "1",
        "Activo",
        "Bienes y derechos de la empresa",
        NaturalezaCuenta.DEUDORA
    ),

    PASIVO(
        "2",
        "Pasivo",
        "Obligaciones y deudas con terceros",
        NaturalezaCuenta.ACREEDORA
    ),

    PATRIMONIO(
        "3",
        "Patrimonio",
        "Aportes de los propietarios y resultados acumulados",
        NaturalezaCuenta.ACREEDORA
    ),

    INGRESO(
        "4",
        "Ingresos",
        "Ingresos obtenidos por las operaciones de la empresa",
        NaturalezaCuenta.ACREEDORA
    ),

    COSTO(
        "5",
        "Costos",
        "Costo relacionado con los bienes o servicios vendidos",
        NaturalezaCuenta.DEUDORA
    ),

    GASTO(
        "6",
        "Gastos",
        "Gastos necesarios para la operación de la empresa",
        NaturalezaCuenta.DEUDORA
    ),

    ORDEN(
        "7",
        "Cuentas de Orden",
        "Registro de valores contingentes y de control",
        NaturalezaCuenta.DEUDORA
    );

    private final String digito;
    private final String nombre;
    private final String descripcion;
    private final NaturalezaCuenta naturalezaPorDefecto;

    TipoCuenta(
        String digito,
        String nombre,
        String descripcion,
        NaturalezaCuenta naturalezaPorDefecto
    ) {
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
     * Determina el tipo de cuenta a partir del primer dígito del código.
     */
    public static TipoCuenta desdeCodigo(String codigo) {

        if (codigo == null || codigo.trim().isEmpty()) {
            return ACTIVO;
        }

        char primerDigito = codigo.trim().charAt(0);

        return switch (primerDigito) {
            case '1' -> ACTIVO;
            case '2' -> PASIVO;
            case '3' -> PATRIMONIO;
            case '4' -> INGRESO;
            case '5' -> COSTO;
            case '6' -> GASTO;
            case '7' -> ORDEN;
            default -> ACTIVO;
        };
    }
}
