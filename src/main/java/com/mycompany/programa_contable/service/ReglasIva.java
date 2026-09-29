package com.mycompany.programa_contable.service;

/** Clasificación pura de cuentas que reciben automatización de IVA en el diario. */
public final class ReglasIva {

    private ReglasIva() {}

    public static boolean esCuentaDeVenta(String codigo) {
        return esCuentaDeGrupo(codigo, "4.1");
    }

    public static boolean esDevolucionDeVenta(String codigo) {
        return esCuentaDeGrupo(codigo, "4.2");
    }

    public static boolean esDevolucionDeCompra(String codigo) {
        return esCuentaDeGrupo(codigo, "5.1");
    }

    public static boolean esCuentaConCreditoFiscal(String codigo) {
        return esCuentaDeGrupo(codigo, "5.4")
                || esCuentaDeGrupo(codigo, "1.6")
                || esCuentaDeGrupo(codigo, "1.7")
                || esCuentaDeGrupo(codigo, "6.1")
                || esCuentaDeGrupo(codigo, "6.2")
                || esCuentaDeGrupo(codigo, "6.3");
    }

    private static boolean esCuentaDeGrupo(String codigo, String grupo) {
        return codigo != null && (codigo.equals(grupo) || codigo.startsWith(grupo + "."));
    }
}
