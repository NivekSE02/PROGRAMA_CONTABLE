package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import com.mycompany.programa_contable.model.NaturalezaCuenta;
import com.mycompany.programa_contable.service.ExportacionService;
import com.mycompany.programa_contable.service.MayorizacionService;
import com.mycompany.programa_contable.model.MayorCuenta;
import com.mycompany.programa_contable.service.ReportesFinancierosService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

public class EstadoResultadosView extends ScrollPane {

    private final ReportesFinancierosService reportesService = new ReportesFinancierosService();
    private final MayorizacionService mayorizacionService = new MayorizacionService();
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    // Anchos de columnas (Concepto | Detalle | Total)
    private static final double COL_CONCEPTO = 320;
    private static final double COL_DETALLE   = 150;
    private static final double COL_TOTAL     = 150;

    private VBox mainContainer;
    private EstadoResultadosDTO estadoActual;
    private List<MayorCuenta> cuentasMayorizadas;
    private List<ExportacionService.FilaEstadoResultados> filasExportacion = new java.util.ArrayList<>();

    public EstadoResultadosView() {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane");
        setStyle("-fx-background-color: #f8fafc; -fx-background: #f8fafc;");

        mainContainer = new VBox(24);
        mainContainer.setPadding(new Insets(28, 32, 32, 32));
        mainContainer.setStyle("-fx-background-color: #f8fafc;");
        setContent(mainContainer);

        cargarDatos();
    }

    public void cargarDatos() {
        mainContainer.getChildren().clear();
        estadoActual = reportesService.generarEstadoResultados();
        cuentasMayorizadas = mayorizacionService.obtenerMayorizacion(true);

        // ── Cabecera ──────────────────────────────────────────────────
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label lblTitulo = new Label("Estado de Resultados");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label lblSub = new Label("Con inventario inicial y final — Promedio ponderado móvil");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblTitulo, lblSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        MenuButton btnImprimir = ExportMenuFactory.crear(this::exportarPDF, this::exportarExcel);

        Button btnRefrescar = new Button("Actualizar");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> cargarDatos());

        topBar.getChildren().addAll(titleBox, btnImprimir, btnRefrescar);

        // ── Obtener valores desde Mayor y Kardex ──────────────────────
        // Ventas (4.1) y devoluciones sobre ventas (4.2)
        double ventasTotales    = getSaldoGrupoNormal("4.1", NaturalezaCuenta.ACREEDORA);
        double devVentas        = getSaldoGrupoNormal("4.2", NaturalezaCuenta.DEUDORA);
        double ventasNetas      = redondear(ventasTotales - devVentas);

        // Inventario inicial (saldo contable de cuenta 1.2)
        double inventarioInicial = getInventarioInicialRegistrado();

        // Compras (5.4) y devoluciones sobre compras (5.1) y gastos de compra (si hay)
        double compras          = getMovimientoGrupo("5.4", true);
        double gastoDeCompra    = 0.0;   // Cuenta específica de gasto de compra (no existe en catálogo actual)
        double comprasTotales   = redondear(compras + gastoDeCompra);
        double devCompras       = getMovimientoGrupo("5.1", false);
        double comprasNetas     = redondear(comprasTotales - devCompras);
        double mercDisponible   = redondear(inventarioInicial + comprasNetas);

        // El costo de ventas sale del costo valorizado de las salidas de venta.
        // El saldo guardado en Kárdex puede provenir de versiones anteriores que
        // valoraron compras por cantidad * costo unitario y no por el asiento.
        double costoVentas      = redondear(getSaldoGrupoNormal("5.2", NaturalezaCuenta.DEUDORA));
        double inventarioFinal  = redondear(getSaldoGrupoNormal("1.2", NaturalezaCuenta.DEUDORA));
        double utilidadBruta    = redondear(ventasNetas - costoVentas);

        // Gastos de operación
        double gastoAdmin       = getSaldoGrupoNormal("6.2", NaturalezaCuenta.DEUDORA);
        double gastoVenta       = getSaldoGrupoNormal("6.3", NaturalezaCuenta.DEUDORA);
        double gastosFinancieros= getSaldoGrupoNormal("6.1", NaturalezaCuenta.DEUDORA);
        double totalGastoOper   = redondear(gastoAdmin + gastoVenta + gastosFinancieros);
        double utilidadOpera    = redondear(utilidadBruta - totalGastoOper);

        filasExportacion = new java.util.ArrayList<>();
        agregarSeccionExportacion("5 Ventas");
        agregarDetalleExportacion("Ventas totales", ventasTotales);
        agregarDetalleExportacion("(-) Dev sobre ventas", devVentas);
        agregarTotalExportacion("Ventas netas", ventasNetas, false);
        agregarSeccionExportacion("4 Costos");
        agregarDetalleExportacion("Inventario inicial", inventarioInicial);
        agregarDetalleExportacion("(+) Compras", compras);
        agregarDetalleExportacion("(+) Gasto de compra", gastoDeCompra);
        agregarDetalleExportacion("Compras totales", comprasTotales);
        agregarDetalleExportacion("(-) Dev sobre compras", devCompras);
        agregarDetalleExportacion("Compras netas", comprasNetas);
        agregarDetalleExportacion("Mercancía disponible", mercDisponible);
        agregarDetalleExportacion("(-) Inventario final", inventarioFinal);
        agregarTotalExportacion("Costo ventas", costoVentas, false);
        agregarTotalExportacion("Utilidad bruta", utilidadBruta, false);
        agregarSeccionExportacion("Gasto operación");
        agregarDetalleExportacion("Gasto administrativo", gastoAdmin);
        agregarDetalleExportacion("Gasto ventas", gastoVenta);
        agregarDetalleExportacion("Gastos financieros", gastosFinancieros);
        agregarTotalExportacion("Total Gasto operación", totalGastoOper, false);
        agregarTotalExportacion("Utilidad operacional", utilidadOpera, true);

        // ── Construir la tabla fila a fila ────────────────────────────
        VBox tabla = new VBox(0);
        tabla.getStyleClass().add("card");
        tabla.setPadding(new Insets(0));

        // Encabezado de columnas
        tabla.getChildren().add(filaEncabezado());

        // ══ SECCIÓN VENTAS ══
        tabla.getChildren().add(filaSeccion("5 Ventas"));
        tabla.getChildren().add(filaDetalle("Ventas totales",        ventasTotales,  true,  false, false));
        tabla.getChildren().add(filaDetalle("(-) Dev sobre ventas",  devVentas,      true,  false, false));
        tabla.getChildren().add(filaTotal  ("Ventas netas",          ventasNetas,           false, false));

        // ══ SECCIÓN COSTOS ══
        tabla.getChildren().add(filaSeccion("4 Costos"));
        tabla.getChildren().add(filaDetalle("Inventario inicial",    inventarioInicial, true, false, false));
        tabla.getChildren().add(filaDetalle("(+) Compras",           compras,        true,  false, false));
        tabla.getChildren().add(filaDetalle("(+) Gasto de compra",   gastoDeCompra,  true,  false, false));
        tabla.getChildren().add(filaDetalle("Compras totales",       comprasTotales, true,  false, false));
        tabla.getChildren().add(filaDetalle("(-) Dev sobre compras", devCompras,     true,  false, false));
        tabla.getChildren().add(filaDetalle("Compras netas",         comprasNetas,   true,  false, false));
        tabla.getChildren().add(filaDetalle("Mercancía disponible",  mercDisponible, true,  false, false));
        tabla.getChildren().add(filaDetalle("(-) Inventario final",  inventarioFinal,true,  false, false));
        tabla.getChildren().add(filaTotal  ("Costo ventas",          costoVentas,           false, false));
        tabla.getChildren().add(filaTotal  ("Utilidad bruta",        utilidadBruta,         false, false));

        // ══ SECCIÓN GASTOS DE OPERACIÓN ══
        tabla.getChildren().add(filaSeccion("Gasto operación"));
        tabla.getChildren().add(filaDetalle("Gasto administrativo",  gastoAdmin,     true,  false, false));
        tabla.getChildren().add(filaDetalle("Gasto ventas",          gastoVenta,     true,  false, false));
        tabla.getChildren().add(filaDetalle("Gastos financieros",    gastosFinancieros, true, false, false));
        tabla.getChildren().add(filaTotal  ("Total Gasto operación", totalGastoOper,        false, false));

        // ══ UTILIDAD OPERACIONAL (resaltada en amarillo) ══
        tabla.getChildren().add(filaTotal  ("Utilidad operacional",  utilidadOpera,         true,  true));

        mainContainer.getChildren().addAll(topBar, tabla);
    }

    private void agregarSeccionExportacion(String texto) {
        filasExportacion.add(new ExportacionService.FilaEstadoResultados(texto, null, null, true, false));
    }

    private void agregarDetalleExportacion(String texto, double monto) {
        filasExportacion.add(new ExportacionService.FilaEstadoResultados(texto, monto, null, false, false));
    }

    private void agregarTotalExportacion(String texto, double monto, boolean resaltada) {
        filasExportacion.add(new ExportacionService.FilaEstadoResultados(texto, null, monto, false, resaltada));
    }

    // ── Fábrica de filas ──────────────────────────────────────────────

    /** Encabezado de tabla: Concepto | Detalle | Total */
    private HBox filaEncabezado() {
        HBox row = new HBox(0);
        row.setStyle("-fx-background-color: #1e3a8a; -fx-background-radius: 8px 8px 0 0;");
        row.setPadding(new Insets(8, 12, 8, 12));

        Label c = celda("Concepto", COL_CONCEPTO, true, "#ffffff", true);
        Label d = celda("Detalle",  COL_DETALLE,  true, "#ffffff", true);
        Label t = celda("Total",    COL_TOTAL,    true, "#ffffff", true);
        row.getChildren().addAll(c, d, t);
        return row;
    }

    /** Fila de sección (header de grupo, fondo azul oscuro) */
    private HBox filaSeccion(String titulo) {
        HBox row = new HBox(0);
        row.setStyle("-fx-background-color: #1e3a8a;");
        row.setPadding(new Insets(5, 12, 5, 12));

        Label c = celda(titulo, COL_CONCEPTO + COL_DETALLE + COL_TOTAL, true, "#ffffff", false);
        row.getChildren().add(c);
        return row;
    }

    /**
     * Fila de detalle: monto en columna "Detalle", columna "Total" vacía.
     * @param mostrarCero si false, muestra igualmente el valor (el Excel siempre muestra)
     */
    private HBox filaDetalle(String concepto, double monto, boolean mostrarCero,
                             boolean esUtilidad, boolean esAmarillo) {
        HBox row = crearFilaBase(esAmarillo);
        String bg = row.getStyle();

        Label c = celda(concepto,              COL_CONCEPTO, false, "#0f172a", false);
        Label d = celda(MONEDA.format(monto),  COL_DETALLE,  false, "#374151", true);
        Label t = celda("",                    COL_TOTAL,    false, "#0f172a", true);

        row.getChildren().addAll(c, d, t);
        separadorVertical(row);
        return row;
    }

    /**
     * Fila de total: columna "Detalle" vacía, monto en columna "Total".
     * @param esNegrita si el texto del concepto va en negrita
     * @param esAmarillo si la fila tiene fondo amarillo (utilidad operacional)
     */
    private HBox filaTotal(String concepto, double monto, boolean esNegrita,
                           boolean esAmarillo) {
        HBox row = crearFilaBase(esAmarillo);

        String colorTexto = esAmarillo ? "#92400e" : "#0f172a";
        String colorMonto = esAmarillo ? "#78350f" : "#1e3a8a";

        Label c = celda(concepto,             COL_CONCEPTO, esNegrita || esAmarillo, colorTexto, false);
        Label d = celda("",                   COL_DETALLE,  false, colorTexto, true);
        Label t = celda(MONEDA.format(monto), COL_TOTAL,    true,  colorMonto, true);

        row.getChildren().addAll(c, d, t);
        return row;
    }

    private HBox crearFilaBase(boolean esAmarillo) {
        HBox row = new HBox(0);
        if (esAmarillo) {
            row.setStyle("-fx-background-color: #fef08a; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        } else {
            row.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        }
        row.setPadding(new Insets(6, 12, 6, 12));

        // Hover effect
        row.setOnMouseEntered(e -> {
            if (!esAmarillo)
                row.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        });
        row.setOnMouseExited(e -> {
            if (!esAmarillo)
                row.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
        });
        return row;
    }

    /** Crear un Label-celda con ancho fijo */
    private Label celda(String texto, double ancho, boolean negrita, String color, boolean alineaDerecha) {
        Label lbl = new Label(texto);
        lbl.setMinWidth(ancho);
        lbl.setPrefWidth(ancho);
        lbl.setMaxWidth(ancho);
        String align = alineaDerecha ? "-fx-alignment: CENTER-RIGHT;" : "-fx-alignment: CENTER-LEFT;";
        String weight = negrita ? "-fx-font-weight: bold;" : "";
        lbl.setStyle(align + weight + "-fx-text-fill: " + color + "; -fx-font-family: 'Consolas', monospace;");
        return lbl;
    }

    /** Añade líneas verticales de separación entre celdas */
    private void separadorVertical(HBox row) {
        // Se logra con el padding y border ya definido en la celda
    }

    // ── Utilidades ────────────────────────────────────────────────────

    private double getMovimientoGrupo(String prefijo, boolean debe) {
        return cuentasMayorizadas.stream()
                .filter(m -> m.getCodigo().equals(prefijo) || m.getCodigo().startsWith(prefijo + "."))
                .mapToDouble(m -> debe ? m.getTotalDebe() : m.getTotalHaber())
                .sum();
    }

    private double getInventarioInicialRegistrado() {
        return cuentasMayorizadas.stream()
                .filter(m -> m.getCodigo().equals("1.2") || m.getCodigo().startsWith("1.2."))
                .flatMap(m -> m.getMovimientos().stream())
                .filter(m -> m.getConcepto() == null || !m.getConcepto().startsWith("AUTO_KARDEX_"))
                .mapToDouble(m -> m.getDebe() - m.getHaber())
                .sum();
    }

    /** Suma saldos por naturaleza normal dentro del grupo, incluidos padres con movimientos directos. */
    private double getSaldoGrupoNormal(String prefijo, NaturalezaCuenta naturalezaNormal) {
        return cuentasMayorizadas.stream()
                // El mayor conserva los movimientos por cuenta, no consolida las
                // cuentas padre. Por eso se incluyen solo cuentas con movimientos
                // propios: la cuenta padre no duplica el saldo de sus subcuentas.
                .filter(m -> m.getTotalDebe() != 0 || m.getTotalHaber() != 0)
                .filter(m -> m.getCodigo().equals(prefijo) || m.getCodigo().startsWith(prefijo + "."))
                .mapToDouble(m -> m.getNaturaleza() == naturalezaNormal
                        ? m.getSaldoNeto() : -m.getSaldoNeto())
                .sum();
    }

    private double redondear(double val) {
        return java.math.BigDecimal.valueOf(val)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void exportarPDF() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Estado de Resultados a PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
        fc.setInitialFileName("Estado_Resultados_" + LocalDate.now() + ".pdf");
        File dest = fc.showSaveDialog(getScene().getWindow());
        if (dest == null) return;
        try {
            java.util.List<String> rows = new java.util.ArrayList<>();
            rows.add("Concepto | Detalle | Total");
            for (ExportacionService.FilaEstadoResultados fila : filasExportacion) {
                if (fila.isSeccion()) rows.add(fila.getConcepto());
                else rows.add(fila.getConcepto()+" | "+(fila.getDetalle() == null ? "" : MONEDA.format(fila.getDetalle()))+" | "+(fila.getTotal() == null ? "" : MONEDA.format(fila.getTotal())));
            }
            rows.add("");
            rows.add("Nota: Estado preparado con base en el libro diario y la valuación de inventario por promedio ponderado móvil.");
            rows.add("");
            rows.add("FIRMAS:");
            dest = ExportacionService.exportarPDF(ExportacionService.obtenerNombreEmpresa(), "Estado de Resultados", rows, dest);
            ExportMenuFactory.ofrecerAbrir(dest, "PDF");
        } catch (Exception ex) { new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait(); }
    }

    private void exportarHTML() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Reporte de Estado de Resultados");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Página Web Imprimible (*.html)", "*.html"));
        fc.setInitialFileName("Estado_Resultados_" + LocalDate.now() + ".html");
        File dest = fc.showSaveDialog(getScene().getWindow());
        if (dest != null) {
            try {
                ExportacionService.exportarEstadoResultadosHTML(estadoActual, ExportacionService.obtenerNombreEmpresa(), dest);
                Alert a = new Alert(Alert.AlertType.INFORMATION,
                        "Reporte formal generado. ¿Desea abrirlo en el navegador?",
                        ButtonType.YES, ButtonType.NO);
                a.setTitle("Exportación Exitosa");
                a.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.YES && Desktop.isDesktopSupported()) {
                        try { Desktop.getDesktop().open(dest); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
            }
        }
    }

    private void exportarExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Reporte en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("Estado_Resultados_" + LocalDate.now() + ".xlsx");
        File dest = fc.showSaveDialog(getScene().getWindow());
        if (dest != null) {
            try {
                File archivo = ExportacionService.exportarEstadoResultadosExcel(ExportacionService.obtenerNombreEmpresa(), filasExportacion, dest);
                Alert a = new Alert(Alert.AlertType.INFORMATION,
                        "Libro de Excel generado. ¿Desea abrirlo ahora?",
                        ButtonType.YES, ButtonType.NO);
                a.setTitle("Exportación a Excel Exitosa");
                a.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.YES && Desktop.isDesktopSupported()) {
                        try { Desktop.getDesktop().open(archivo); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage()).showAndWait();
            }
        }
    }
}
