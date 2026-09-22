package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.BalanceGeneralDTO;
import com.mycompany.programa_contable.model.BalanzaComprobacionDTO;
import com.mycompany.programa_contable.model.DetalleAsiento;
import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
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
               "body { font-family: 'Inter', 'Segoe UI', Arial, sans-serif; margin: 40px; color: #1e293b; background: #f8fafc; font-size: 13px; line-height: 1.5; }" +
               ".container { max-width: 1000px; margin: 0 auto; background: #fff; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06); }" +
               ".header { text-align: center; border-bottom: 3px solid #3b82f6; padding-bottom: 16px; margin-bottom: 32px; }" +
               ".header h1 { margin: 0 0 8px 0; font-size: 24px; color: #0f172a; text-transform: uppercase; font-weight: 800; letter-spacing: 0.5px; }" +
               ".header h2 { margin: 0 0 8px 0; font-size: 16px; color: #3b82f6; font-weight: 600; }" +
               ".header .meta { font-size: 12px; color: #64748b; font-weight: 500; }" +
               "table { width: 100%; border-collapse: separate; border-spacing: 0; margin-bottom: 24px; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; }" +
               "th { background-color: #f1f5f9; color: #334155; font-weight: 700; text-align: left; padding: 12px 16px; border-bottom: 2px solid #cbd5e1; font-size: 13px; text-transform: uppercase; letter-spacing: 0.5px; }" +
               "td { padding: 10px 16px; border-bottom: 1px solid #f1f5f9; color: #475569; }" +
               "tr:last-child td { border-bottom: none; }" +
               "tr:hover { background-color: #f8fafc; transition: all 0.2s ease; }" +
               ".num { text-align: right; font-family: 'JetBrains Mono', 'Consolas', monospace; font-weight: 500; }" +
               ".total-row td { font-weight: 700; background-color: #eff6ff; color: #1d4ed8; border-top: 2px solid #bfdbfe; border-bottom: 2px solid #bfdbfe; }" +
               ".badge-success { color: #059669; font-weight: bold; padding: 4px 8px; background: #d1fae5; border-radius: 4px; }" +
               ".badge-error { color: #dc2626; font-weight: bold; padding: 4px 8px; background: #fee2e2; border-radius: 4px; }" +
               ".section-title { font-weight: 700; font-size: 15px; color: #0f172a; margin: 24px 0 12px 0; display: flex; align-items: center; }" +
               ".section-title::before { content: ''; display: inline-block; width: 4px; height: 16px; background: #3b82f6; margin-right: 8px; border-radius: 2px; }" +
               ".signatures { display: flex; justify-content: space-between; margin-top: 60px; page-break-inside: avoid; gap: 20px; }" +
               ".sig-box { text-align: center; flex: 1; border-top: 2px solid #cbd5e1; padding-top: 12px; font-size: 13px; color: #475569; font-weight: 500; }" +
               "@media print { body { margin: 0; background: #fff; } .container { box-shadow: none; padding: 0; max-width: 100%; } .no-print { display: none; } }" +
               "</style>";
    }

    public static File exportarBalanceGeneralHTML(BalanceGeneralDTO bg, String nombreEmpresa, File destino) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(destino, StandardCharsets.UTF_8))) {
            pw.println("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Balance General</title>" + getEstiloImpresionCSS() + "</head><body><div class='container'>");
            pw.println("<div class='header'>");
            pw.println("<h1>" + nombreEmpresa + "</h1>");
            pw.println("<h2>BALANCE GENERAL</h2>");
            pw.println("<div class='meta'>Generado el " + LocalDateTime.now().format(FECHA_HORA) + " | Expresado en Dólares (USD)</div>");
            pw.println("</div>");

            pw.println("<div class='section-title'>ACTIVOS</div>");
            pw.println("<table><thead><tr><th>Código</th><th>Cuenta</th><th class='num'>Parcial</th><th class='num'>Total</th></tr></thead><tbody>");
            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>ACTIVO CORRIENTE</strong></td></tr>");
            for (BalanceGeneralDTO.LineaBalance l : bg.getActivosCorrientes()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr><td colspan='3' style='font-weight:600; text-align:right;'>Total Activo Corriente</td><td class='num' style='font-weight:600;'>" + MONEDA.format(bg.getTotalActivoCorriente()) + "</td></tr>");

            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>ACTIVO NO CORRIENTE</strong></td></tr>");
            for (BalanceGeneralDTO.LineaBalance l : bg.getActivosNoCorrientes()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr><td colspan='3' style='font-weight:600; text-align:right;'>Total Activo No Corriente</td><td class='num' style='font-weight:600;'>" + MONEDA.format(bg.getTotalActivoNoCorriente()) + "</td></tr>");
            pw.println("<tr class='total-row'><td colspan='3'>TOTAL ACTIVO</td><td class='num'>" + MONEDA.format(bg.getTotalActivo()) + "</td></tr>");
            pw.println("</tbody></table>");

            pw.println("<div class='section-title'>PASIVOS Y CAPITAL CONTABLE</div>");
            pw.println("<table><thead><tr><th>Código</th><th>Cuenta</th><th class='num'>Parcial</th><th class='num'>Total</th></tr></thead><tbody>");
            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>PASIVO CORRIENTE</strong></td></tr>");
            for (BalanceGeneralDTO.LineaBalance l : bg.getPasivosCorrientes()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr><td colspan='3' style='font-weight:600; text-align:right;'>Total Pasivo Corriente</td><td class='num' style='font-weight:600;'>" + MONEDA.format(bg.getTotalPasivoCorriente()) + "</td></tr>");
            pw.println("<tr class='total-row' style='background:#f1f5f9; color:#0f172a;'><td colspan='3'>TOTAL PASIVO</td><td class='num'>" + MONEDA.format(bg.getTotalPasivo()) + "</td></tr>");

            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>CAPITAL CONTABLE / PATRIMONIO</strong></td></tr>");
            for (BalanceGeneralDTO.LineaBalance l : bg.getCuentasCapital()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr><td>330102</td><td>Utilidad Neta del Presente Ejercicio</td><td class='num'>" + MONEDA.format(bg.getUtilidadDelEjercicio()) + "</td><td></td></tr>");
            pw.println("<tr class='total-row' style='background:#f1f5f9; color:#0f172a;'><td colspan='3'>TOTAL CAPITAL CONTABLE</td><td class='num'>" + MONEDA.format(bg.getTotalCapitalContable()) + "</td></tr>");
            pw.println("<tr class='total-row'><td colspan='3'>TOTAL PASIVO + CAPITAL CONTABLE</td><td class='num'>" + MONEDA.format(bg.getTotalPasivoMasCapital()) + "</td></tr>");
            pw.println("</tbody></table>");

            pw.println("<div style='text-align:center; margin:30px 0;'>");
            if (bg.isCuadrado()) {
                pw.println("<span class='badge-success'>✔ BALANCE GENERAL CUADRADO EXACTAMENTE</span>");
            } else {
                pw.println("<span class='badge-error'>⚠ BALANCE DESCUADRADO (Diferencia: " + MONEDA.format(bg.getDiferencia()) + ")</span>");
            }
            pw.println("</div>");

            pw.println("<div class='signatures'>");
            pw.println("<div class='sig-box'>Contador General</div>");
            pw.println("<div class='sig-box'>Auditor Externo</div>");
            pw.println("<div class='sig-box'>Representante Legal<br><span style='font-size:11px; color:#94a3b8;'>Gerencia General</span></div>");
            pw.println("</div>");

            pw.println("</div></body></html>");
        }
        return destino;
    }

    public static File exportarEstadoResultadosHTML(EstadoResultadosDTO er, String nombreEmpresa, File destino) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(destino, StandardCharsets.UTF_8))) {
            pw.println("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Estado de Resultados</title>" + getEstiloImpresionCSS() + "</head><body><div class='container'>");
            pw.println("<div class='header'>");
            pw.println("<h1>" + nombreEmpresa + "</h1>");
            pw.println("<h2>ESTADO DE RESULTADOS (PÉRDIDAS Y GANANCIAS)</h2>");
            pw.println("<div class='meta'>Generado el " + LocalDateTime.now().format(FECHA_HORA) + " | Expresado en USD</div>");
            pw.println("</div>");

            pw.println("<table><thead><tr><th>Código</th><th>Concepto</th><th class='num'>Subtotal</th><th class='num'>Total</th></tr></thead><tbody>");
            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>5. INGRESOS DE OPERACIÓN</strong></td></tr>");
            for (EstadoResultadosDTO.LineaReporte l : er.getIngresosOperacion()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>(-) 41. COSTO DE VENTAS</strong></td></tr>");
            for (EstadoResultadosDTO.LineaReporte l : er.getCostosVenta()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr class='total-row' style='background:#f1f5f9; color:#0f172a;'><td colspan='3'>UTILIDAD BRUTA</td><td class='num'>" + MONEDA.format(er.getUtilidadBruta()) + "</td></tr>");

            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>(-) 42. GASTOS DE ADMINISTRACIÓN</strong></td></tr>");
            for (EstadoResultadosDTO.LineaReporte l : er.getGastosAdministracion()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }

            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>(-) 43. GASTOS DE VENTA</strong></td></tr>");
            for (EstadoResultadosDTO.LineaReporte l : er.getGastosVenta()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr class='total-row' style='background:#fef3c7; color:#b45309; border-top-color:#fcd34d; border-bottom-color:#fcd34d;'><td colspan='3'>UTILIDAD DE OPERACIÓN</td><td class='num'>" + MONEDA.format(er.getUtilidadOperacion()) + "</td></tr>");

            if (!er.getOtrosIngresos().isEmpty()) {
                pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>(+) OTROS INGRESOS</strong></td></tr>");
                for (EstadoResultadosDTO.LineaReporte l : er.getOtrosIngresos()) {
                    pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
                }
            }

            pw.println("<tr class='total-row'><td colspan='3'>UTILIDAD NETA DEL EJERCICIO</td><td class='num'>" + MONEDA.format(er.getUtilidadNeta()) + "</td></tr>");
            pw.println("</tbody></table>");

            pw.println("<div class='signatures'>");
            pw.println("<div class='sig-box'>Contador General</div>");
            pw.println("<div class='sig-box'>Auditor Externo</div>");
            pw.println("<div class='sig-box'>Representante Legal<br><span style='font-size:11px; color:#94a3b8;'>Gerencia General</span></div>");
            pw.println("</div>");

            pw.println("</div></body></html>");
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

    /* ====================================================================================
       MÉTODOS PARA EXPORTAR A EXCEL (.XLSX) USANDO APACHE POI
       ==================================================================================== */

    private static void crearEstilosExcel(Workbook wb, CellStyle styleHeader, CellStyle styleBold, CellStyle styleCurrency, CellStyle styleCurrencyBold, CellStyle styleTitle, CellStyle styleSubtitle) {
        Font fontBold = wb.createFont();
        fontBold.setBold(true);

        Font fontHeader = wb.createFont();
        fontHeader.setBold(true);
        fontHeader.setColor(IndexedColors.WHITE.getIndex());

        styleHeader.setFont(fontHeader);
        styleHeader.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleHeader.setAlignment(HorizontalAlignment.CENTER);
        styleHeader.setBorderBottom(BorderStyle.THIN);
        styleHeader.setBorderTop(BorderStyle.THIN);
        styleHeader.setBorderRight(BorderStyle.THIN);
        styleHeader.setBorderLeft(BorderStyle.THIN);

        styleBold.setFont(fontBold);
        
        DataFormat format = wb.createDataFormat();
        styleCurrency.setDataFormat(format.getFormat("$#,##0.00"));
        styleCurrencyBold.setFont(fontBold);
        styleCurrencyBold.setDataFormat(format.getFormat("$#,##0.00"));
        
        Font fontTitle = wb.createFont();
        fontTitle.setBold(true);
        fontTitle.setFontHeightInPoints((short) 16);
        styleTitle.setFont(fontTitle);
        styleTitle.setAlignment(HorizontalAlignment.CENTER);
        
        Font fontSubtitle = wb.createFont();
        fontSubtitle.setFontHeightInPoints((short) 12);
        fontSubtitle.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        styleSubtitle.setFont(fontSubtitle);
        styleSubtitle.setAlignment(HorizontalAlignment.CENTER);
    }

    public static File exportarBalanceGeneralExcel(BalanceGeneralDTO bg, String nombreEmpresa, File destino) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Balance General");

            CellStyle styleHeader = wb.createCellStyle();
            CellStyle styleBold = wb.createCellStyle();
            CellStyle styleCurrency = wb.createCellStyle();
            CellStyle styleCurrencyBold = wb.createCellStyle();
            CellStyle styleTitle = wb.createCellStyle();
            CellStyle styleSubtitle = wb.createCellStyle();

            crearEstilosExcel(wb, styleHeader, styleBold, styleCurrency, styleCurrencyBold, styleTitle, styleSubtitle);
            
            CellStyle styleRowTotal = wb.createCellStyle();
            styleRowTotal.cloneStyleFrom(styleCurrencyBold);
            styleRowTotal.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            styleRowTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleRowTotal.setBorderTop(BorderStyle.THIN);
            styleRowTotal.setBorderBottom(BorderStyle.THIN);
            
            CellStyle styleRowTotalLabel = wb.createCellStyle();
            styleRowTotalLabel.cloneStyleFrom(styleBold);
            styleRowTotalLabel.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            styleRowTotalLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleRowTotalLabel.setBorderTop(BorderStyle.THIN);
            styleRowTotalLabel.setBorderBottom(BorderStyle.THIN);

            int rowIdx = 0;
            Row rowTitle = sheet.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue(nombreEmpresa);
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            
            Row rowSubtitle1 = sheet.createRow(rowIdx++);
            Cell cellSubtitle1 = rowSubtitle1.createCell(0);
            cellSubtitle1.setCellValue("BALANCE GENERAL");
            cellSubtitle1.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));
            
            Row rowSubtitle2 = sheet.createRow(rowIdx++);
            Cell cellSubtitle2 = rowSubtitle2.createCell(0);
            cellSubtitle2.setCellValue("Generado el " + LocalDateTime.now().format(FECHA_HORA) + " | Expresado en USD");
            cellSubtitle2.setCellStyle(styleSubtitle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 3));

            rowIdx++; // empty row

            // Headers
            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Código", "Cuenta", "Parcial", "Total"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }

            // Activos
            Row rowSec = sheet.createRow(rowIdx++);
            Cell cellSec = rowSec.createCell(1);
            cellSec.setCellValue("ACTIVOS");
            cellSec.setCellStyle(styleBold);
            
            Row rowSecC = sheet.createRow(rowIdx++);
            Cell cellSecC = rowSecC.createCell(1);
            cellSecC.setCellValue("ACTIVO CORRIENTE");
            cellSecC.setCellStyle(styleBold);

            for (BalanceGeneralDTO.LineaBalance l : bg.getActivosCorrientes()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getCodigo());
                row.createCell(1).setCellValue(l.getNombre());
                Cell c = row.createCell(2);
                c.setCellValue(l.getMonto());
                c.setCellStyle(styleCurrency);
            }
            Row rowTotAC = sheet.createRow(rowIdx++);
            Cell cellTotACLabel = rowTotAC.createCell(1);
            cellTotACLabel.setCellValue("Total Activo Corriente");
            cellTotACLabel.setCellStyle(styleBold);
            Cell cellTotAC = rowTotAC.createCell(3);
            cellTotAC.setCellValue(bg.getTotalActivoCorriente());
            cellTotAC.setCellStyle(styleCurrencyBold);

            // Activo No Corriente
            Row rowSecNC = sheet.createRow(rowIdx++);
            Cell cellSecNC = rowSecNC.createCell(1);
            cellSecNC.setCellValue("ACTIVO NO CORRIENTE");
            cellSecNC.setCellStyle(styleBold);
            for (BalanceGeneralDTO.LineaBalance l : bg.getActivosNoCorrientes()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getCodigo());
                row.createCell(1).setCellValue(l.getNombre());
                Cell c = row.createCell(2);
                c.setCellValue(l.getMonto());
                c.setCellStyle(styleCurrency);
            }
            Row rowTotANC = sheet.createRow(rowIdx++);
            Cell cellTotANCLabel = rowTotANC.createCell(1);
            cellTotANCLabel.setCellValue("Total Activo No Corriente");
            cellTotANCLabel.setCellStyle(styleBold);
            Cell cellTotANC = rowTotANC.createCell(3);
            cellTotANC.setCellValue(bg.getTotalActivoNoCorriente());
            cellTotANC.setCellStyle(styleCurrencyBold);
            
            Row rowTotA = sheet.createRow(rowIdx++);
            Cell cellTotALabel = rowTotA.createCell(1);
            cellTotALabel.setCellValue("TOTAL ACTIVO");
            cellTotALabel.setCellStyle(styleRowTotalLabel);
            Cell cellTotA = rowTotA.createCell(3);
            cellTotA.setCellValue(bg.getTotalActivo());
            cellTotA.setCellStyle(styleRowTotal);
            
            rowIdx++; // Empty row
            
            // Pasivos
            Row rowSecP = sheet.createRow(rowIdx++);
            Cell cellSecP = rowSecP.createCell(1);
            cellSecP.setCellValue("PASIVOS Y CAPITAL CONTABLE");
            cellSecP.setCellStyle(styleBold);
            
            Row rowSecPC = sheet.createRow(rowIdx++);
            Cell cellSecPC = rowSecPC.createCell(1);
            cellSecPC.setCellValue("PASIVO CORRIENTE");
            cellSecPC.setCellStyle(styleBold);
            for (BalanceGeneralDTO.LineaBalance l : bg.getPasivosCorrientes()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getCodigo());
                row.createCell(1).setCellValue(l.getNombre());
                Cell c = row.createCell(2);
                c.setCellValue(l.getMonto());
                c.setCellStyle(styleCurrency);
            }
            Row rowTotPC = sheet.createRow(rowIdx++);
            Cell cellTotPCLabel = rowTotPC.createCell(1);
            cellTotPCLabel.setCellValue("Total Pasivo Corriente");
            cellTotPCLabel.setCellStyle(styleBold);
            Cell cellTotPC = rowTotPC.createCell(3);
            cellTotPC.setCellValue(bg.getTotalPasivoCorriente());
            cellTotPC.setCellStyle(styleCurrencyBold);

            Row rowTotPasivo = sheet.createRow(rowIdx++);
            Cell cellTotPasivoLabel = rowTotPasivo.createCell(1);
            cellTotPasivoLabel.setCellValue("TOTAL PASIVO");
            cellTotPasivoLabel.setCellStyle(styleBold);
            Cell cellTotPas = rowTotPasivo.createCell(3);
            cellTotPas.setCellValue(bg.getTotalPasivo());
            cellTotPas.setCellStyle(styleCurrencyBold);
            
            // Capital
            Row rowSecCap = sheet.createRow(rowIdx++);
            Cell cellSecCap = rowSecCap.createCell(1);
            cellSecCap.setCellValue("CAPITAL CONTABLE / PATRIMONIO");
            cellSecCap.setCellStyle(styleBold);
            for (BalanceGeneralDTO.LineaBalance l : bg.getCuentasCapital()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getCodigo());
                row.createCell(1).setCellValue(l.getNombre());
                Cell c = row.createCell(2);
                c.setCellValue(l.getMonto());
                c.setCellStyle(styleCurrency);
            }
            Row rowUtilidad = sheet.createRow(rowIdx++);
            rowUtilidad.createCell(0).setCellValue("330102");
            rowUtilidad.createCell(1).setCellValue("Utilidad Neta del Presente Ejercicio");
            Cell cellUtilidad = rowUtilidad.createCell(2);
            cellUtilidad.setCellValue(bg.getUtilidadDelEjercicio());
            cellUtilidad.setCellStyle(styleCurrency);
            
            Row rowTotCap = sheet.createRow(rowIdx++);
            Cell cellTotCapLabel = rowTotCap.createCell(1);
            cellTotCapLabel.setCellValue("TOTAL CAPITAL CONTABLE");
            cellTotCapLabel.setCellStyle(styleBold);
            Cell cellTotCap = rowTotCap.createCell(3);
            cellTotCap.setCellValue(bg.getTotalCapitalContable());
            cellTotCap.setCellStyle(styleCurrencyBold);
            
            Row rowTotPCap = sheet.createRow(rowIdx++);
            Cell cellTotPCapLabel = rowTotPCap.createCell(1);
            cellTotPCapLabel.setCellValue("TOTAL PASIVO + CAPITAL CONTABLE");
            cellTotPCapLabel.setCellStyle(styleRowTotalLabel);
            Cell cellTotPCap = rowTotPCap.createCell(3);
            cellTotPCap.setCellValue(bg.getTotalPasivoMasCapital());
            cellTotPCap.setCellStyle(styleRowTotal);

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
        return destino;
    }

    public static File exportarEstadoResultadosExcel(EstadoResultadosDTO er, String nombreEmpresa, File destino) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Estado Resultados");

            CellStyle styleHeader = wb.createCellStyle();
            CellStyle styleBold = wb.createCellStyle();
            CellStyle styleCurrency = wb.createCellStyle();
            CellStyle styleCurrencyBold = wb.createCellStyle();
            CellStyle styleTitle = wb.createCellStyle();
            CellStyle styleSubtitle = wb.createCellStyle();

            crearEstilosExcel(wb, styleHeader, styleBold, styleCurrency, styleCurrencyBold, styleTitle, styleSubtitle);
            
            CellStyle styleRowTotal = wb.createCellStyle();
            styleRowTotal.cloneStyleFrom(styleCurrencyBold);
            styleRowTotal.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            styleRowTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleRowTotal.setBorderTop(BorderStyle.THIN);
            styleRowTotal.setBorderBottom(BorderStyle.THIN);
            
            CellStyle styleRowTotalLabel = wb.createCellStyle();
            styleRowTotalLabel.cloneStyleFrom(styleBold);
            styleRowTotalLabel.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            styleRowTotalLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleRowTotalLabel.setBorderTop(BorderStyle.THIN);
            styleRowTotalLabel.setBorderBottom(BorderStyle.THIN);

            int rowIdx = 0;
            Row rowTitle = sheet.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue(nombreEmpresa);
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            
            Row rowSubtitle1 = sheet.createRow(rowIdx++);
            Cell cellSubtitle1 = rowSubtitle1.createCell(0);
            cellSubtitle1.setCellValue("ESTADO DE RESULTADOS");
            cellSubtitle1.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));
            
            Row rowSubtitle2 = sheet.createRow(rowIdx++);
            Cell cellSubtitle2 = rowSubtitle2.createCell(0);
            cellSubtitle2.setCellValue("Generado el " + LocalDateTime.now().format(FECHA_HORA) + " | Expresado en USD");
            cellSubtitle2.setCellStyle(styleSubtitle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 3));

            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Código", "Concepto", "Subtotal", "Total"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }

            // Ingresos
            Row rowSec = sheet.createRow(rowIdx++);
            Cell cellSec = rowSec.createCell(1);
            cellSec.setCellValue("5. INGRESOS DE OPERACIÓN");
            cellSec.setCellStyle(styleBold);
            for (EstadoResultadosDTO.LineaReporte l : er.getIngresosOperacion()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getCodigo());
                row.createCell(1).setCellValue(l.getNombre());
                Cell c = row.createCell(2);
                c.setCellValue(l.getMonto());
                c.setCellStyle(styleCurrency);
            }

            // Costos
            Row rowSecC = sheet.createRow(rowIdx++);
            Cell cellSecC = rowSecC.createCell(1);
            cellSecC.setCellValue("(-) 41. COSTO DE VENTAS");
            cellSecC.setCellStyle(styleBold);
            for (EstadoResultadosDTO.LineaReporte l : er.getCostosVenta()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getCodigo());
                row.createCell(1).setCellValue(l.getNombre());
                Cell c = row.createCell(2);
                c.setCellValue(l.getMonto());
                c.setCellStyle(styleCurrency);
            }
            
            Row rowBruta = sheet.createRow(rowIdx++);
            Cell cellBrutaLabel = rowBruta.createCell(1);
            cellBrutaLabel.setCellValue("UTILIDAD BRUTA");
            cellBrutaLabel.setCellStyle(styleBold);
            Cell cellBruta = rowBruta.createCell(3);
            cellBruta.setCellValue(er.getUtilidadBruta());
            cellBruta.setCellStyle(styleCurrencyBold);

            // Gastos Adm
            Row rowSecA = sheet.createRow(rowIdx++);
            Cell cellSecA = rowSecA.createCell(1);
            cellSecA.setCellValue("(-) 42. GASTOS DE ADMINISTRACIÓN");
            cellSecA.setCellStyle(styleBold);
            for (EstadoResultadosDTO.LineaReporte l : er.getGastosAdministracion()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getCodigo());
                row.createCell(1).setCellValue(l.getNombre());
                Cell c = row.createCell(2);
                c.setCellValue(l.getMonto());
                c.setCellStyle(styleCurrency);
            }
            
            // Gastos Venta
            Row rowSecV = sheet.createRow(rowIdx++);
            Cell cellSecV = rowSecV.createCell(1);
            cellSecV.setCellValue("(-) 43. GASTOS DE VENTA");
            cellSecV.setCellStyle(styleBold);
            for (EstadoResultadosDTO.LineaReporte l : er.getGastosVenta()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getCodigo());
                row.createCell(1).setCellValue(l.getNombre());
                Cell c = row.createCell(2);
                c.setCellValue(l.getMonto());
                c.setCellStyle(styleCurrency);
            }
            
            Row rowOper = sheet.createRow(rowIdx++);
            Cell cellOperLabel = rowOper.createCell(1);
            cellOperLabel.setCellValue("UTILIDAD DE OPERACIÓN");
            cellOperLabel.setCellStyle(styleBold);
            Cell cellOper = rowOper.createCell(3);
            cellOper.setCellValue(er.getUtilidadOperacion());
            cellOper.setCellStyle(styleCurrencyBold);
            
            if (!er.getOtrosIngresos().isEmpty()) {
                Row rowSecO = sheet.createRow(rowIdx++);
                Cell cellSecO = rowSecO.createCell(1);
                cellSecO.setCellValue("(+) OTROS INGRESOS");
                cellSecO.setCellStyle(styleBold);
                for (EstadoResultadosDTO.LineaReporte l : er.getOtrosIngresos()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(l.getCodigo());
                    row.createCell(1).setCellValue(l.getNombre());
                    Cell c = row.createCell(2);
                    c.setCellValue(l.getMonto());
                    c.setCellStyle(styleCurrency);
                }
            }
            
            Row rowNeta = sheet.createRow(rowIdx++);
            Cell cellNetaLabel = rowNeta.createCell(1);
            cellNetaLabel.setCellValue("UTILIDAD NETA DEL EJERCICIO");
            cellNetaLabel.setCellStyle(styleRowTotalLabel);
            Cell cellNeta = rowNeta.createCell(3);
            cellNeta.setCellValue(er.getUtilidadNeta());
            cellNeta.setCellStyle(styleRowTotal);

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
        return destino;
    }

    public static File exportarBalanzaComprobacionExcel(BalanzaComprobacionDTO balanza, File destino) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Balanza Comprobación");

            CellStyle styleHeader = wb.createCellStyle();
            CellStyle styleBold = wb.createCellStyle();
            CellStyle styleCurrency = wb.createCellStyle();
            CellStyle styleCurrencyBold = wb.createCellStyle();
            CellStyle styleTitle = wb.createCellStyle();
            CellStyle styleSubtitle = wb.createCellStyle();

            crearEstilosExcel(wb, styleHeader, styleBold, styleCurrency, styleCurrencyBold, styleTitle, styleSubtitle);

            int rowIdx = 0;
            Row rowTitle = sheet.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("BALANZA DE COMPROBACIÓN");
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Código", "Cuenta", "Tipo", "Mov. Debe", "Mov. Haber", "Saldo Deudor", "Saldo Acreedor"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }

            for (BalanzaComprobacionDTO.Renglon r : balanza.getRenglones()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getCodigo());
                row.createCell(1).setCellValue(r.getNombre());
                row.createCell(2).setCellValue(r.getTipo().getNombre());
                
                Cell c3 = row.createCell(3); c3.setCellValue(r.getMovimientoDebe()); c3.setCellStyle(styleCurrency);
                Cell c4 = row.createCell(4); c4.setCellValue(r.getMovimientoHaber()); c4.setCellStyle(styleCurrency);
                Cell c5 = row.createCell(5); c5.setCellValue(r.getSaldoDeudor()); c5.setCellStyle(styleCurrency);
                Cell c6 = row.createCell(6); c6.setCellValue(r.getSaldoAcreedor()); c6.setCellStyle(styleCurrency);
            }
            
            Row rowTotales = sheet.createRow(rowIdx++);
            Cell cellTLabel = rowTotales.createCell(1);
            cellTLabel.setCellValue("TOTALES");
            cellTLabel.setCellStyle(styleBold);
            
            Cell t3 = rowTotales.createCell(3); t3.setCellValue(balanza.getTotalMovimientoDebe()); t3.setCellStyle(styleCurrencyBold);
            Cell t4 = rowTotales.createCell(4); t4.setCellValue(balanza.getTotalMovimientoHaber()); t4.setCellStyle(styleCurrencyBold);
            Cell t5 = rowTotales.createCell(5); t5.setCellValue(balanza.getTotalSaldoDeudor()); t5.setCellStyle(styleCurrencyBold);
            Cell t6 = rowTotales.createCell(6); t6.setCellValue(balanza.getTotalSaldoAcreedor()); t6.setCellStyle(styleCurrencyBold);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
        return destino;
    }

    public static File exportarLibroDiarioExcel(List<Asiento> asientos, File destino) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Libro Diario");

            CellStyle styleHeader = wb.createCellStyle();
            CellStyle styleBold = wb.createCellStyle();
            CellStyle styleCurrency = wb.createCellStyle();
            CellStyle styleCurrencyBold = wb.createCellStyle();
            CellStyle styleTitle = wb.createCellStyle();
            CellStyle styleSubtitle = wb.createCellStyle();

            crearEstilosExcel(wb, styleHeader, styleBold, styleCurrency, styleCurrencyBold, styleTitle, styleSubtitle);

            int rowIdx = 0;
            Row rowTitle = sheet.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("LIBRO DIARIO");
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Asiento No.", "Fecha", "Concepto Gral.", "Código", "Cuenta", "Debe", "Haber"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }

            for (Asiento a : asientos) {
                for (DetalleAsiento d : a.getDetalles()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(a.getNumero());
                    row.createCell(1).setCellValue(a.getFecha().toString());
                    row.createCell(2).setCellValue(a.getConcepto());
                    row.createCell(3).setCellValue(d.getCuentaCodigo());
                    row.createCell(4).setCellValue(d.getCuentaNombre());
                    
                    Cell c5 = row.createCell(5);
                    if(d.getDebe() > 0) c5.setCellValue(d.getDebe());
                    c5.setCellStyle(styleCurrency);
                    
                    Cell c6 = row.createCell(6);
                    if(d.getHaber() > 0) c6.setCellValue(d.getHaber());
                    c6.setCellStyle(styleCurrency);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
        return destino;
    }
    
    public static File exportarKardexExcel(List<com.mycompany.programa_contable.model.KardexFilaDTO> reporteKardex, String nombreProducto, File destino) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Kárdex");

            CellStyle styleHeader = wb.createCellStyle();
            CellStyle styleBold = wb.createCellStyle();
            CellStyle styleCurrency = wb.createCellStyle();
            CellStyle styleCurrencyBold = wb.createCellStyle();
            CellStyle styleTitle = wb.createCellStyle();
            CellStyle styleSubtitle = wb.createCellStyle();

            crearEstilosExcel(wb, styleHeader, styleBold, styleCurrency, styleCurrencyBold, styleTitle, styleSubtitle);

            int rowIdx = 0;
            Row rowTitle = sheet.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("KÁRDEX DE INVENTARIO - MÉTODO PEPS");
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
            
            Row rowSubtitle = sheet.createRow(rowIdx++);
            Cell cellSubtitle = rowSubtitle.createCell(0);
            cellSubtitle.setCellValue("Producto: " + nombreProducto + " | Generado el " + LocalDateTime.now().format(FECHA_HORA));
            cellSubtitle.setCellStyle(styleSubtitle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));

            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Fecha", "Concepto", "Entrada (U)", "Salida (U)", "Existencias", "Costo Unit.", "Total Mov.", "Saldo Bodega ($)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }

            for (com.mycompany.programa_contable.model.KardexFilaDTO f : reporteKardex) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(f.getFecha());
                row.createCell(1).setCellValue(f.getConcepto());
                
                Cell c2 = row.createCell(2);
                if (f.getEntrada() > 0) c2.setCellValue(f.getEntrada());
                
                Cell c3 = row.createCell(3);
                if (f.getSalida() > 0) c3.setCellValue(f.getSalida());
                
                row.createCell(4).setCellValue(f.getExistencias());
                
                Cell c5 = row.createCell(5); c5.setCellValue(f.getCostoUnitario()); c5.setCellStyle(styleCurrency);
                Cell c6 = row.createCell(6); c6.setCellValue(f.getCostoTotal()); c6.setCellStyle(styleCurrency);
                Cell c7 = row.createCell(7); c7.setCellValue(f.getSaldoMonetario()); c7.setCellStyle(styleCurrencyBold);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
        return destino;
    }
}
