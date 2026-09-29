package com.mycompany.programa_contable.service;

import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.BalanceGeneralDTO;
import com.mycompany.programa_contable.model.BalanzaComprobacionDTO;
import com.mycompany.programa_contable.model.DetalleAsiento;
import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import com.mycompany.programa_contable.model.ConfiguracionDAO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class ExportacionService {

    public static final class FilaEstadoResultados {
        private final String concepto;
        private final Double detalle;
        private final Double total;
        private final boolean seccion;
        private final boolean resaltada;

        public FilaEstadoResultados(String concepto, Double detalle, Double total, boolean seccion, boolean resaltada) {
            this.concepto = concepto;
            this.detalle = detalle;
            this.total = total;
            this.seccion = seccion;
            this.resaltada = resaltada;
        }
        public String getConcepto() { return concepto; }
        public Double getDetalle() { return detalle; }
        public Double getTotal() { return total; }
        public boolean isSeccion() { return seccion; }
        public boolean isResaltada() { return resaltada; }
    }

    /** Genera PDF sencillo y autónomo para que todos los reportes compartan formato. */
    public static File exportarPDF(String titulo, List<String> lineas, File destino) throws IOException {
        return exportarPDF(obtenerNombreEmpresa(), titulo, lineas, destino);
    }

    public static String obtenerNombreEmpresa() {
        return new ConfiguracionDAO().obtenerNombreEmpresa();
    }

    public static File exportarPDF(String nombreEmpresa, String titulo, List<String> lineas, File destino) throws IOException {
        destino = asegurarExtension(destino, ".pdf");
        List<String> rows = new ArrayList<>();
        for (String linea : lineas) if (linea != null && !linea.trim().isEmpty()) rows.add(linea.trim());
        List<List<String>> pages = new ArrayList<>();
        String tableHeader = !rows.isEmpty() && rows.get(0).contains("|") ? rows.get(0) : null;
        int cursor = 0;
        while (cursor < rows.size()) {
            List<String> page = new ArrayList<>();
            if (cursor > 0 && tableHeader != null) page.add(tableHeader);
            int pageLimit = tableHeader == null ? 25 : (cursor == 0 ? 24 : 23);
            int end = Math.min(rows.size(), cursor + pageLimit);
            page.addAll(rows.subList(cursor, end));
            pages.add(page);
            cursor = end;
        }
        if (pages.isEmpty()) pages.add(new ArrayList<>());
        List<byte[]> objects = new ArrayList<>();
        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder("<< /Type /Pages /Kids [");
        for (int i = 0; i < pages.size(); i++) kids.append(5 + i * 2).append(" 0 R ");
        kids.append("] /Count ").append(pages.size()).append(" >>");
        objects.add(bytes(kids.toString()));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));
        for (int i = 0; i < pages.size(); i++) {
            int pageId = 5 + i * 2, contentId = pageId + 1;
            objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents " + contentId + " 0 R >>"));
            StringBuilder stream = new StringBuilder();
            stream.append("0.22 0.25 0.31 rg BT /F2 15 Tf 40 554 Td (").append(pdfEscape(nombreEmpresa)).append(") Tj ET\n");
            stream.append("0.12 0.23 0.54 rg BT /F2 12 Tf 40 535 Td (").append(pdfEscape(titulo)).append(") Tj ET\n");
            stream.append("0.45 0.49 0.55 rg BT /F1 8 Tf 40 521 Td (Generado el ")
                    .append(pdfEscape(LocalDateTime.now().format(FECHA_HORA))).append(") Tj ET\n");
            stream.append("0.55 0.12 0.22 RG 40 512 m 802 512 l S\n");
            float y = 498;
            for (String row : pages.get(i)) {
                String[] cells = row.split("\\s*\\|\\s*", -1);
                if (cells.length > 1) {
                    boolean isHeader = isPdfTableHeader(cells);
                    float rowHeight = isHeader ? 19 : 14;
                    float colWidth = 762f / cells.length;
                    if (isHeader) {
                        stream.append("0.55 0.12 0.22 rg 40 ").append(fmt(y-rowHeight+3)).append(" 762 ").append(fmt(rowHeight)).append(" re f\n");
                        stream.append("1 1 1 rg\n");
                    } else {
                        if ((i + (int)(y * 10)) % 2 == 0) stream.append("0.96 0.97 0.98 rg 40 ").append(fmt(y-rowHeight+3)).append(" 762 ").append(fmt(rowHeight)).append(" re f\n");
                        stream.append("0.82 0.85 0.89 RG 0.45 w 40 ").append(fmt(y-rowHeight+3)).append(" 762 ").append(fmt(rowHeight)).append(" re S\n");
                        stream.append("0.16 0.19 0.24 rg\n");
                    }
                    stream.append("BT /").append(isHeader ? "F2" : "F1").append(" ").append(cells.length >= 7 ? "7" : "8").append(" Tf\n");
                    for (int c = 0; c < cells.length; c++) {
                        float x = 44 + c * colWidth;
                        int max = Math.max(5, (int)(colWidth / (cells.length >= 7 ? 4.1 : 4.6)));
                        String cell = cells[c].trim();
                        if (cell.length() > max) cell = cell.substring(0, max - 1) + "…";
                        stream.append("1 0 0 1 ").append(fmt(x)).append(" ").append(fmt(y-10)).append(" Tm (").append(pdfEscape(cell)).append(") Tj\n");
                    }
                    stream.append("ET\n");
                    y -= rowHeight;
                } else if (row.startsWith("Nota:")) {
                    stream.append("0.40 0.44 0.50 rg BT /F1 8 Tf 42 ").append(fmt(y)).append(" Td (").append(pdfEscape(row.substring(5).trim())).append(") Tj ET\n");
                    y -= 15;
                } else if (row.startsWith("FIRMAS:")) {
                    stream.append("0.72 0.75 0.79 RG 0.7 w 55 ").append(fmt(y-4)).append(" m 350 ").append(fmt(y-4)).append(" l S 455 ").append(fmt(y-4)).append(" m 750 ").append(fmt(y-4)).append(" l S\n");
                    stream.append("0.25 0.29 0.35 rg BT /F1 8 Tf 155 ").append(fmt(y-17)).append(" Td (Contador general) Tj ET\n");
                    stream.append("0.25 0.29 0.35 rg BT /F1 8 Tf 545 ").append(fmt(y-17)).append(" Td (Representante legal) Tj ET\n");
                    y -= 32;
                } else {
                    stream.append("0.12 0.23 0.54 rg 40 ").append(fmt(y-14)).append(" 762 17 re f\n");
                    stream.append("1 1 1 rg BT /F2 9 Tf 50 ").append(fmt(y-9)).append(" Td (").append(pdfEscape(row)).append(") Tj ET\n");
                    y -= 17;
                }
            }
            stream.append("0.45 0.49 0.55 rg BT /F1 8 Tf 720 22 Td (Página ").append(i+1).append(" de ").append(pages.size()).append(") Tj ET\n");
            byte[] data = stream.toString().getBytes(java.nio.charset.Charset.forName("windows-1252"));
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            obj.write(bytes("<< /Length " + data.length + " >>\nstream\n")); obj.write(data); obj.write(bytes("\nendstream"));
            objects.add(obj.toByteArray());
        }
        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n%".getBytes(StandardCharsets.ISO_8859_1));
        pdf.write(new byte[]{(byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, 10});
        List<Integer> offsets = new ArrayList<>(); offsets.add(0);
        for (int i = 0; i < objects.size(); i++) { offsets.add(pdf.size()); pdf.write(bytes((i + 1) + " 0 obj\n")); pdf.write(objects.get(i)); pdf.write(bytes("\nendobj\n")); }
        int xref = pdf.size(); pdf.write(bytes("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (int i = 1; i < offsets.size(); i++) pdf.write(bytes(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i))));
        pdf.write(bytes("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF"));
        try (FileOutputStream out = new FileOutputStream(destino)) { pdf.writeTo(out); }
        return destino;
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.ISO_8859_1); }
    private static String fmt(float value) { return String.format(Locale.ROOT, "%.2f", value); }
    private static boolean isPdfTableHeader(String[] cells) {
        String first = cells[0].trim().toLowerCase(Locale.ROOT);
        return first.equals("fecha") || first.equals("código") || first.equals("codigo")
                || first.equals("asiento") || first.equals("cuenta") || first.equals("concepto");
    }
    private static String pdfEscape(String value) { return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replaceAll("[\\r\\n]", " "); }

    public static File asegurarExtension(File file, String extension) {
        if (file == null || file.getName().toLowerCase(Locale.ROOT).endsWith(extension)) return file;
        return new File(file.getParentFile(), file.getName() + extension);
    }

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
            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>PASIVO NO CORRIENTE</strong></td></tr>");
            for (BalanceGeneralDTO.LineaBalance l : bg.getPasivosNoCorrientes()) {
                pw.println("<tr><td>" + l.getCodigo() + "</td><td>" + l.getNombre() + "</td><td class='num'>" + MONEDA.format(l.getMonto()) + "</td><td></td></tr>");
            }
            pw.println("<tr><td colspan='3' style='font-weight:600; text-align:right;'>Total Pasivo No Corriente</td><td class='num' style='font-weight:600;'>" + MONEDA.format(bg.getTotalPasivoNoCorriente()) + "</td></tr>");
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
            pw.println("<tr style='background:#f8fafc;'><td colspan='4'><strong style='color:#334155;'>(-) GASTOS FINANCIEROS</strong></td></tr>");
            for (EstadoResultadosDTO.LineaReporte l : er.getGastosFinancieros()) {
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
        destino = asegurarExtension(destino, ".xlsx");
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

            Row rowSecPNC = sheet.createRow(rowIdx++);
            Cell cellSecPNC = rowSecPNC.createCell(1);
            cellSecPNC.setCellValue("PASIVO NO CORRIENTE");
            cellSecPNC.setCellStyle(styleBold);
            for (BalanceGeneralDTO.LineaBalance l : bg.getPasivosNoCorrientes()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(l.getCodigo());
                row.createCell(1).setCellValue(l.getNombre());
                Cell c = row.createCell(2);
                c.setCellValue(l.getMonto());
                c.setCellStyle(styleCurrency);
            }
            Row rowTotPNC = sheet.createRow(rowIdx++);
            Cell cellTotPNCLabel = rowTotPNC.createCell(1);
            cellTotPNCLabel.setCellValue("Total Pasivo No Corriente");
            cellTotPNCLabel.setCellStyle(styleBold);
            Cell cellTotPNC = rowTotPNC.createCell(3);
            cellTotPNC.setCellValue(bg.getTotalPasivoNoCorriente());
            cellTotPNC.setCellStyle(styleCurrencyBold);

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
        List<FilaEstadoResultados> filas = crearFilasEstadoResultados(er);
        return exportarEstadoResultadosExcel(nombreEmpresa, filas, destino);
    }

    private static List<FilaEstadoResultados> crearFilasEstadoResultados(EstadoResultadosDTO er) {
        List<FilaEstadoResultados> filas = new ArrayList<>();
        filas.add(new FilaEstadoResultados("5 Ventas", null, null, true, false));
        agregarDetalles(filas, "Ventas", er.getIngresosOperacion());
        filas.add(new FilaEstadoResultados("Ventas netas", null, er.getTotalIngresos(), false, false));
        filas.add(new FilaEstadoResultados("4 Costos", null, null, true, false));
        agregarDetalles(filas, "Costo", er.getCostosVenta());
        filas.add(new FilaEstadoResultados("Costo ventas", null, er.getTotalCostos(), false, false));
        filas.add(new FilaEstadoResultados("Utilidad bruta", null, er.getUtilidadBruta(), false, false));
        filas.add(new FilaEstadoResultados("Gasto operación", null, null, true, false));
        agregarDetalles(filas, "Gasto administrativo", er.getGastosAdministracion());
        agregarDetalles(filas, "Gasto ventas", er.getGastosVenta());
        agregarDetalles(filas, "Gasto financiero", er.getGastosFinancieros());
        filas.add(new FilaEstadoResultados("Total gasto operación", null, er.getTotalGastosOperacion(), false, false));
        filas.add(new FilaEstadoResultados("Utilidad operacional", null, er.getUtilidadOperacion(), false, true));
        filas.add(new FilaEstadoResultados("UTILIDAD NETA DEL EJERCICIO", null, er.getUtilidadNeta(), false, false));
        return filas;
    }

    private static void agregarDetalles(List<FilaEstadoResultados> destino, String prefijo,
            List<EstadoResultadosDTO.LineaReporte> lineas) {
        for (EstadoResultadosDTO.LineaReporte linea : lineas) {
            destino.add(new FilaEstadoResultados(prefijo + " · " + linea.getNombre(), linea.getMonto(), null, false, false));
        }
    }

    public static File exportarEstadoResultadosExcel(String nombreEmpresa, List<FilaEstadoResultados> filas, File destino) throws IOException {
        destino = asegurarExtension(destino, ".xlsx");
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

            Font sectionFont = wb.createFont(); sectionFont.setBold(true); sectionFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle styleSection = wb.createCellStyle(); styleSection.setFont(sectionFont);
            styleSection.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); styleSection.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            styleSection.setVerticalAlignment(VerticalAlignment.CENTER);
            Font finalFont = wb.createFont(); finalFont.setBold(true); finalFont.setColor(IndexedColors.DARK_RED.getIndex());
            CellStyle styleFinal = wb.createCellStyle(); styleFinal.cloneStyleFrom(styleRowTotal);
            styleFinal.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex()); styleFinal.setFillPattern(FillPatternType.SOLID_FOREGROUND); styleFinal.setFont(finalFont);
            CellStyle styleSectionCell = wb.createCellStyle(); styleSectionCell.cloneStyleFrom(styleSection);
            CellStyle styleDetailTotal = wb.createCellStyle(); styleDetailTotal.cloneStyleFrom(styleCurrency);
            styleDetailTotal.setBorderBottom(BorderStyle.THIN);
            styleDetailTotal.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            CellStyle styleDetailLabel = wb.createCellStyle(); styleDetailLabel.cloneStyleFrom(styleDetailTotal);

            int rowIdx = 0;
            Row rowTitle = sheet.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue(nombreEmpresa);
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
            
            Row rowSubtitle1 = sheet.createRow(rowIdx++);
            Cell cellSubtitle1 = rowSubtitle1.createCell(0);
            cellSubtitle1.setCellValue("ESTADO DE RESULTADOS");
            cellSubtitle1.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));
            
            Row rowSubtitle2 = sheet.createRow(rowIdx++);
            Cell cellSubtitle2 = rowSubtitle2.createCell(0);
            cellSubtitle2.setCellValue("Generado el " + LocalDateTime.now().format(FECHA_HORA) + " | Expresado en USD");
            cellSubtitle2.setCellStyle(styleSubtitle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 2));

            rowIdx++;

            Row headerRow = sheet.createRow(rowIdx++);
            String[] headers = {"Concepto", "Detalle", "Total"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styleHeader);
            }

            for (FilaEstadoResultados fila : filas) {
                Row row = sheet.createRow(rowIdx++);
                if (fila.isSeccion()) {
                    Cell cell = row.createCell(0); cell.setCellValue(fila.getConcepto()); cell.setCellStyle(styleSection);
                    for (int col = 1; col < 3; col++) row.createCell(col).setCellStyle(styleSectionCell);
                    sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, 2));
                    row.setHeightInPoints(22);
                    continue;
                }
                Cell concept = row.createCell(0); concept.setCellValue(fila.getConcepto());
                Cell detail = row.createCell(1); Cell total = row.createCell(2);
                concept.setCellStyle(fila.isResaltada() ? styleRowTotalLabel : styleDetailLabel);
                detail.setCellStyle(styleDetailTotal); total.setCellStyle(styleDetailTotal);
                if (fila.getDetalle() != null) { detail.setCellValue(fila.getDetalle()); detail.setCellStyle(styleDetailTotal); }
                if (fila.getTotal() != null) { total.setCellValue(fila.getTotal()); total.setCellStyle(fila.isResaltada() ? styleFinal : styleRowTotal); }
                if (fila.isResaltada()) { concept.setCellStyle(styleRowTotalLabel); detail.setCellStyle(styleRowTotalLabel); total.setCellStyle(styleFinal); }
                else if (fila.getTotal() != null) concept.setCellStyle(styleBold);
            }

            rowIdx++;
            Row footer = sheet.createRow(rowIdx++);
            Cell footerCell = footer.createCell(0);
            footerCell.setCellValue("Este estado financiero se preparó con base en los registros contables y el promedio ponderado móvil aplicado al inventario.");
            footerCell.setCellStyle(styleSubtitle);
            sheet.addMergedRegion(new CellRangeAddress(footer.getRowNum(), footer.getRowNum(), 0, 2));
            rowIdx++;
            Row signatures = sheet.createRow(rowIdx++);
            signatures.createCell(0).setCellValue("__________________________");
            signatures.createCell(2).setCellValue("__________________________");
            Row signLabels = sheet.createRow(rowIdx);
            signLabels.createCell(0).setCellValue("Contador general");
            signLabels.createCell(2).setCellValue("Representante legal");

            sheet.setColumnWidth(0, 42 * 256); sheet.setColumnWidth(1, 24 * 256); sheet.setColumnWidth(2, 24 * 256);
            sheet.createFreezePane(0, 5);
            sheet.setDisplayGridlines(false);
            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.setFitToPage(true);
            sheet.setRepeatingRows(CellRangeAddress.valueOf("5:5"));

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                wb.write(fos);
            }
        }
        return destino;
    }

    public static File exportarBalanzaComprobacionExcel(BalanzaComprobacionDTO balanza, File destino) throws IOException {
        return exportarBalanzaComprobacionExcel(balanza, obtenerNombreEmpresa(), destino);
    }

    public static File exportarBalanzaComprobacionExcel(BalanzaComprobacionDTO balanza, String nombreEmpresa, File destino) throws IOException {
        destino = asegurarExtension(destino, ".xlsx");
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
            Row rowCompany = sheet.createRow(rowIdx++);
            Cell companyCell = rowCompany.createCell(0);
            companyCell.setCellValue(nombreEmpresa);
            companyCell.setCellStyle(styleSubtitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            Row rowTitle = sheet.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("BALANZA DE COMPROBACIÓN");
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

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
        return exportarLibroDiarioExcel(asientos, obtenerNombreEmpresa(), destino);
    }

    public static File exportarLibroDiarioExcel(List<Asiento> asientos, String nombreEmpresa, File destino) throws IOException {
        destino = asegurarExtension(destino, ".xlsx");
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
            Row rowCompany = sheet.createRow(rowIdx++);
            Cell companyCell = rowCompany.createCell(0);
            companyCell.setCellValue(nombreEmpresa);
            companyCell.setCellStyle(styleSubtitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            Row rowTitle = sheet.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("LIBRO DIARIO");
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

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
        return exportarKardexExcel(reporteKardex, nombreProducto, obtenerNombreEmpresa(), destino);
    }

    public static File exportarKardexExcel(List<com.mycompany.programa_contable.model.KardexFilaDTO> reporteKardex, String nombreProducto, String nombreEmpresa, File destino) throws IOException {
        destino = asegurarExtension(destino, ".xlsx");
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
            Row rowCompany = sheet.createRow(rowIdx++);
            Cell companyCell = rowCompany.createCell(0);
            companyCell.setCellValue(nombreEmpresa);
            companyCell.setCellStyle(styleSubtitle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
            Row rowTitle = sheet.createRow(rowIdx++);
            Cell cellTitle = rowTitle.createCell(0);
            cellTitle.setCellValue("KÁRDEX DE INVENTARIO - PROMEDIO PONDERADO MÓVIL");
            cellTitle.setCellStyle(styleTitle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 7));
            
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
