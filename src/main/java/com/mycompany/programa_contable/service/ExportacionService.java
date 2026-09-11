package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.BalanceGeneralDTO;
import com.mycompany.programa_contable.model.BalanzaComprobacionDTO;
import com.mycompany.programa_contable.model.DetalleAsiento;
import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import com.mycompany.programa_contable.model.MayorCuenta;
import com.mycompany.programa_contable.model.MovimientoMayor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportacionService {

    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static String getEstiloImpresionCSS() {
        return "<style>" +
               "body { font-family: 'Segoe UI', Arial, sans-serif; margin: 40px; color: #1e293b; background: #fff; font-size: 13px; }" +
               ".header { text-align: center; border-bottom: 2px solid #0f172a; padding-bottom: 12px; margin-bottom: 24px; }" +
               ".header h1 { margin: 0 0 4px 0; font-size: 20px; color: #0f172a; text-transform: uppercase; }" +
               ".header h2 { margin: 0 0 4px 0; font-size: 15px; color: #475569; font-weight: normal; }" +
               ".header .meta { font-size: 11px; color: #64748b; margin-top: 6px; }" +
               "table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
               "th { background-color: #f1f5f9; color: #0f172a; font-weight: 600; text-align: left; padding: 8px 10px; border: 1px solid #cbd5e1; font-size: 12px; }" +
               "td { padding: 7px 10px; border: 1px solid #e2e8f0; }" +
               "tr:nth-child(even) { background-color: #f8fafc; }" +
               ".num { text-align: right; font-family: 'Consolas', monospace; }" +
               ".total-row td { font-weight: bold; background-color: #e2e8f0; border-top: 2px solid #0f172a; border-bottom: 2px solid #0f172a; }" +
               ".badge-success { color: #059669; font-weight: bold; }" +
               ".section-title { font-weight: bold; font-size: 14px; color: #1e3a8a; margin: 16px 0 8px 0; border-bottom: 1px solid #cbd5e1; padding-bottom: 4px; }" +
               ".signatures { display: flex; justify-content: space-between; margin-top: 50px; page-break-inside: avoid; }" +
               ".sig-box { text-align: center; width: 28%; border-top: 1px solid #334155; padding-top: 6px; font-size: 12px; }" +
               "@media print { body { margin: 20px; } .no-print { display: none; } }" +
               "</style>";
    }

    public static File exportarBalanceGeneralHTML(BalanceGeneralDTO bg, String nombreEmpresa, File destino) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(destino, StandardCharsets.UTF_8))) {
            pw.println("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Balance General</title>" + getEstiloImpresionCSS() + "</head><body>");
            pw.println("<div class='header'>");
            pw.println("<h1>" + nombreEmpresa + "</h1>");
            pw.println("<h2>BALANCE GENERAL</h2>");
            pw.println("<div class='meta'>Generado el " + LocalDateTime.now().format(FECHA_HORA) + " | Expresado en Dólares de los Estados Unidos de América (USD)</div>");
            pw.println("</div>");

            pw.println("<div class='section-title'>1. ACTIVOS</div>");
            pw.println("<table><thead><tr><th>Código</th><th>Cuenta</th><th class='num'>Parcial</th><th class='num'>Total</th></tr></thead><tbody>");
            pw.println("<tr style='font-weight:600; background:#f1f5f9;'><td colspan='4'>ACTIVO CORRIENTE</td></tr>");
            for (BalanceGeneralDTO.LineaBalance l : bg.getActivosCorrientes()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr style='font-weight:bold;'><td colspan='3'>Total Activo Corriente</td><td class='num'>" + MONEDA.format(bg.getTotalActivoCorriente()) + "</td></tr>");

            pw.println("<tr style='font-weight:600; background:#f1f5f9;'><td colspan='4'>ACTIVO NO CORRIENTE</td></tr>");
            for (BalanceGeneralDTO.LineaBalance l : bg.getActivosNoCorrientes()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr style='font-weight:bold;'><td colspan='3'>Total Activo No Corriente</td><td class='num'>" + MONEDA.format(bg.getTotalActivoNoCorriente()) + "</td></tr>");
            pw.println("<tr class='total-row'><td colspan='3'>TOTAL ACTIVO (CÓDIGO 1)</td><td class='num'>" + MONEDA.format(bg.getTotalActivo()) + "</td></tr>");
            pw.println("</tbody></table>");

            pw.println("<div class='section-title'>2. PASIVOS Y 3. CAPITAL CONTABLE</div>");
            pw.println("<table><thead><tr><th>Código</th><th>Cuenta</th><th class='num'>Parcial</th><th class='num'>Total</th></tr></thead><tbody>");
            pw.println("<tr style='font-weight:600; background:#f1f5f9;'><td colspan='4'>PASIVO CORRIENTE</td></tr>");
            for (BalanceGeneralDTO.LineaBalance l : bg.getPasivosCorrientes()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr style='font-weight:bold;'><td colspan='3'>Total Pasivo Corriente</td><td class='num'>" + MONEDA.format(bg.getTotalPasivoCorriente()) + "</td></tr>");
            pw.println("<tr class='total-row'><td colspan='3'>TOTAL PASIVO (CÓDIGO 2)</td><td class='num'>" + MONEDA.format(bg.getTotalPasivo()) + "</td></tr>");

            pw.println("<tr style='font-weight:600; background:#f1f5f9;'><td colspan='4'>CAPITAL CONTABLE / PATRIMONIO</td></tr>");
            for (BalanceGeneralDTO.LineaBalance l : bg.getCuentasCapital()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr><td>330102</td><td>Utilidad Neta del Presente Ejercicio</td><td class='num'>" + MONEDA.format(bg.getUtilidadDelEjercicio()) + "</td><td></td></tr>");
            pw.println("<tr class='total-row'><td colspan='3'>TOTAL CAPITAL CONTABLE (CÓDIGO 3)</td><td class='num'>" + MONEDA.format(bg.getTotalCapitalContable()) + "</td></tr>");
            pw.println("<tr class='total-row' style='background:#dcfce7;'><td colspan='3'>TOTAL PASIVO + CAPITAL CONTABLE (2 + 3)</td><td class='num'>" + MONEDA.format(bg.getTotalPasivoMasCapital()) + "</td></tr>");
            pw.println("</tbody></table>");

            pw.println("<p style='text-align:center; font-weight:bold; font-size:14px; margin:20px 0; color:" + (bg.isCuadrado() ? "#059669" : "#dc2626") + ";'>");
            pw.println(bg.isCuadrado() ? "✔ BALANCE GENERAL CUADRADO EXACTAMENTE (ACTIVO = PASIVO + CAPITAL)" : "⚠ BALANCE DESCUADRADO (Diferencia: " + MONEDA.format(bg.getDiferencia()) + ")");
            pw.println("</p>");

            pw.println("<div class='signatures'>");
            pw.println("<div class='sig-box'><strong>Licda. María Contadora</strong><br>Contador General</div>");
            pw.println("<div class='sig-box'><strong>Lic. Carlos Auditor</strong><br>Auditor Externo</div>");
            pw.println("<div class='sig-box'><strong>Representante Legal</strong><br>Gerencia General</div>");
            pw.println("</div>");

            pw.println("</body></html>");
        }
        return destino;
    }

    public static File exportarEstadoResultadosHTML(EstadoResultadosDTO er, String nombreEmpresa, File destino) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(destino, StandardCharsets.UTF_8))) {
            pw.println("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Estado de Resultados</title>" + getEstiloImpresionCSS() + "</head><body>");
            pw.println("<div class='header'>");
            pw.println("<h1>" + nombreEmpresa + "</h1>");
            pw.println("<h2>ESTADO DE RESULTADOS (PÉRDIDAS Y GANANCIAS)</h2>");
            pw.println("<div class='meta'>Generado el " + LocalDateTime.now().format(FECHA_HORA) + " | Expresado en USD</div>");
            pw.println("</div>");

            pw.println("<table><thead><tr><th>Código</th><th>Concepto</th><th class='num'>Subtotal</th><th class='num'>Total</th></tr></thead><tbody>");
            pw.println("<tr style='font-weight:600; background:#f1f5f9;'><td colspan='4'>5. INGRESOS DE OPERACIÓN</td></tr>");
            for (EstadoResultadosDTO.LineaReporte l : er.getIngresosOperacion()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr style='font-weight:600; background:#f1f5f9;'><td colspan='4'>(-) 41. COSTO DE VENTAS</td></tr>");
            for (EstadoResultadosDTO.LineaReporte l : er.getCostosVenta()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr class='total-row'><td colspan='3'>UTILIDAD BRUTA</td><td class='num'>" + MONEDA.format(er.getUtilidadBruta()) + "</td></tr>");

            pw.println("<tr style='font-weight:600; background:#f1f5f9;'><td colspan='4'>(-) 42. GASTOS DE ADMINISTRACIÓN</td></tr>");
            for (EstadoResultadosDTO.LineaReporte l : er.getGastosAdministracion()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }

            pw.println("<tr style='font-weight:600; background:#f1f5f9;'><td colspan='4'>(-) 43. GASTOS DE VENTA</td></tr>");
            for (EstadoResultadosDTO.LineaReporte l : er.getGastosVenta()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr class='total-row'><td colspan='3'>UTILIDAD DE OPERACIÓN</td><td class='num'>" + MONEDA.format(er.getUtilidadOperacion()) + "</td></tr>");

            if (!er.getOtrosIngresos().isEmpty()) {
                pw.println("<tr style='font-weight:600; background:#f1f5f9;'><td colspan='4'>(+) OTROS INGRESOS</td></tr>");
                for (EstadoResultadosDTO.LineaReporte l : er.getOtrosIngresos()) {
                    pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
                }
            }

            pw.println("<tr class='total-row' style='background:#dcfce7;'><td colspan='3'>UTILIDAD NETA DEL EJERCICIO (5 INGRESOS - 4 COSTOS/GASTOS)</td><td class='num'>" + MONEDA.format(er.getUtilidadNeta()) + "</td></tr>");
            pw.println("</tbody></table>");

            pw.println("<div class='signatures'>");
            pw.println("<div class='sig-box'><strong>Licda. María Contadora</strong><br>Contador General</div>");
            pw.println("<div class='sig-box'><strong>Lic. Carlos Auditor</strong><br>Auditor Externo</div>");
            pw.println("<div class='sig-box'><strong>Representante Legal</strong><br>Gerencia General</div>");
            pw.println("</div>");

            pw.println("</body></html>");
        }
        return destino;
    }

    public static File exportarBalanzaComprobacionCSV(BalanzaComprobacionDTO balanza, File destino) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(destino, StandardCharsets.UTF_8))) {
            pw.println("Codigo,Cuenta,Tipo,Movimiento_Debe,Movimiento_Haber,Saldo_Deudor,Saldo_Acreedor");
            for (BalanzaComprobacionDTO.Renglon r : balanza.getRenglones()) {
                pw.printf("\"%s\",\"%s\",\"%s\",%.2f,%.2f,%.2f,%.2f%n",
                    r.getCodigo(), r.getNombre().replace("\"", "\"\""), r.getTipo().getNombre(),
                    r.getMovimientoDebe(), r.getMovimientoHaber(), r.getSaldoDeudor(), r.getSaldoAcreedor()
                );
            }
            pw.printf("\"TOTALES\",\"\",\"\",%.2f,%.2f,%.2f,%.2f%n",
                balanza.getTotalMovimientoDebe(), balanza.getTotalMovimientoHaber(),
                balanza.getTotalSaldoDeudor(), balanza.getTotalSaldoAcreedor()
            );
        }
        return destino;
    }

    public static File exportarLibroDiarioCSV(List<Asiento> asientos, File destino) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(destino, StandardCharsets.UTF_8))) {
            pw.println("Asiento_No,Fecha,Concepto_General,Renglon,Codigo_Cuenta,Nombre_Cuenta,Concepto_Linea,Debe,Haber");
            for (Asiento a : asientos) {
                for (DetalleAsiento d : a.getDetalles()) {
                    pw.printf("%d,\"%s\",\"%s\",%d,\"%s\",\"%s\",\"%s\",%.2f,%.2f%n",
                        a.getNumero(), a.getFecha(), a.getConcepto().replace("\"", "\"\""),
                        d.getRenglon(), d.getCuentaCodigo(),
                        (d.getCuentaNombre() != null ? d.getCuentaNombre().replace("\"", "\"\"") : ""),
                        (d.getConceptoLinea() != null ? d.getConceptoLinea().replace("\"", "\"\"") : ""),
                        d.getDebe(), d.getHaber()
                    );
                }
            }
        }
        return destino;
    }
}
