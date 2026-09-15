package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import com.mycompany.programa_contable.service.ExportacionService;
import com.mycompany.programa_contable.service.ReportesFinancierosService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;

public class EstadoResultadosView extends ScrollPane {

    private final ReportesFinancierosService reportesService = new ReportesFinancierosService();
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    private VBox mainContainer;
    private EstadoResultadosDTO estadoActual;

    public EstadoResultadosView() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");

        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(24));
        setContent(mainContainer);

        cargarDatos();
    }

    public void cargarDatos() {
        mainContainer.getChildren().clear();
        estadoActual = reportesService.generarEstadoResultados();

        // 1. Cabecera institucional
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblInst = new Label("UNIVERSIDAD CATÓLICA DE EL SALVADOR - EMPRESA PRÁCTICA S.A. DE C.V.");
        lblInst.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
        Label lblTitulo = new Label("ESTADO DE RESULTADOS AUTOMÁTICO (PÉRDIDAS Y GANANCIAS)");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label lblSub = new Label("Clasificación por Dígitos: Código 4 (Ingresos) - Código 5/6 (Costos y Gastos) = Utilidad");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblInst, lblTitulo, lblSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button btnImprimir = new Button("📄 Exportar Reporte Formal e Imprimir");
        btnImprimir.getStyleClass().add("btn-primary");
        btnImprimir.setOnAction(e -> exportarHTML());

        Button btnRefrescar = new Button("🔄 Actualizar");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> cargarDatos());

        topBar.getChildren().addAll(titleBox, btnImprimir, btnRefrescar);

        // 2. Banner de Fórmula Obligatoria: 4 - 5/6 = Utilidad
        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(14, 20, 14, 20));
        boolean esGanancia = estadoActual.getUtilidadNeta() >= 0;

        if (esGanancia) {
            banner.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 10px; -fx-border-color: #86efac; -fx-border-radius: 10px;");
            Label lblCheck = new Label("✔ RESULTADO POSITIVO (UTILIDAD NETA DEL EJERCICIO):");
            lblCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #15803d;");
            Label lblFormula = new Label("INGRESOS CÓDIGO 4 (" + MONEDA.format(estadoActual.getTotalIngresos()) +
                                         ")  -  COSTOS/GASTOS CÓDIGO 5/6 (" + MONEDA.format(estadoActual.getTotalCostosYGastos()) +
                                         ")  =  UTILIDAD NETA: " + MONEDA.format(estadoActual.getUtilidadNeta()));
            lblFormula.setStyle("-fx-font-weight: bold; -fx-text-fill: #166534; -fx-font-family: 'Consolas', monospace;");
            banner.getChildren().addAll(lblCheck, lblFormula);
        } else {
            banner.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 10px; -fx-border-color: #fca5a5; -fx-border-radius: 10px;");
            Label lblErr = new Label("⚠ RESULTADO NEGATIVO (PÉRDIDA DEL EJERCICIO):");
            lblErr.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #b91c1c;");
            Label lblFormula = new Label("Pérdida Neta: " + MONEDA.format(estadoActual.getUtilidadNeta()));
            lblFormula.setStyle("-fx-font-weight: bold; -fx-text-fill: #991b1b;");
            banner.getChildren().addAll(lblErr, lblFormula);
        }

        // 3. Tarjeta central con el Estado de Resultados Estructurado
        VBox cardReporte = new VBox(14);
        cardReporte.getStyleClass().add("card");

        // Sección 1: Ingresos de Operación (Código 4)
        Label lblIngTitle = new Label("4. INGRESOS DE OPERACIÓN");
        lblIngTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #047857;");
        TableView<EstadoResultadosDTO.LineaReporte> tblIng = crearTablaLineas(estadoActual.getIngresosOperacion());

        // Sección 2: Costo de Ventas (Código 51)
        Label lblCosTitle = new Label("(-) 51. COSTO DE VENTAS / COMPRAS");
        lblCosTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #9a3412;");
        TableView<EstadoResultadosDTO.LineaReporte> tblCos = crearTablaLineas(estadoActual.getCostosVenta());

        // Subtotal: Utilidad Bruta
        HBox rowBruta = crearFilaSubtotal("(=) UTILIDAD BRUTA EN VENTAS:", estadoActual.getUtilidadBruta(), "#1e3a8a", false);

        // Sección 3: Gastos de Operación (6)
        Label lblGasAdmTitle = new Label("(-) 6. GASTOS DE OPERACIÓN Y ADMINISTRACIÓN");
        lblGasAdmTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        TableView<EstadoResultadosDTO.LineaReporte> tblGasAdm = crearTablaLineas(estadoActual.getGastosAdministracion());

        Label lblGasVenTitle = new Label("(-) GASTOS DE COMERCIALIZACIÓN Y VENTA");
        lblGasVenTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        TableView<EstadoResultadosDTO.LineaReporte> tblGasVen = crearTablaLineas(estadoActual.getGastosVenta());

        // Subtotal: Utilidad de Operación
        HBox rowOperacion = crearFilaSubtotal("(=) UTILIDAD DE OPERACIÓN:", estadoActual.getUtilidadOperacion(), "#1e3a8a", false);

        // Otros ingresos o gastos si existen
        VBox otrosBox = new VBox(6);
        if (!estadoActual.getOtrosIngresos().isEmpty()) {
            otrosBox.getChildren().addAll(new Label("(+) Otros Ingresos"), crearTablaLineas(estadoActual.getOtrosIngresos()));
        }
        if (!estadoActual.getGastosFinancieros().isEmpty()) {
            otrosBox.getChildren().addAll(new Label("(-) Gastos Financieros"), crearTablaLineas(estadoActual.getGastosFinancieros()));
        }

        // Fila Final: Utilidad Neta del Ejercicio
        HBox rowNeta = crearFilaSubtotal("(=) UTILIDAD NETA DEL EJERCICIO (4 INGRESOS - 5/6 COSTOS/GASTOS):", estadoActual.getUtilidadNeta(), "#15803d", true);

        cardReporte.getChildren().addAll(
            lblIngTitle, tblIng,
            lblCosTitle, tblCos,
            rowBruta,
            new Separator(),
            lblGasAdmTitle, tblGasAdm,
            lblGasVenTitle, tblGasVen,
            rowOperacion,
            otrosBox,
            new Separator(),
            rowNeta
        );

        mainContainer.getChildren().addAll(topBar, banner, cardReporte);
    }

    private TableView<EstadoResultadosDTO.LineaReporte> crearTablaLineas(java.util.List<EstadoResultadosDTO.LineaReporte> lineas) {
        TableView<EstadoResultadosDTO.LineaReporte> tbl = new TableView<>();
        tbl.setPrefHeight(Math.max(80, (lineas.size() + 1) * 35));

        TableColumn<EstadoResultadosDTO.LineaReporte, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCod.setPrefWidth(100);

        TableColumn<EstadoResultadosDTO.LineaReporte, String> colNom = new TableColumn<>("Cuenta");
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colNom.setPrefWidth(450);

        TableColumn<EstadoResultadosDTO.LineaReporte, String> colMon = new TableColumn<>("Monto ($)");
        colMon.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getMonto())));
        colMon.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colMon.setPrefWidth(160);

        tbl.getColumns().addAll(colCod, colNom, colMon);
        tbl.setItems(FXCollections.observableArrayList(lineas));
        return tbl;
    }

    private HBox crearFilaSubtotal(String etiqueta, double valor, String colorHex, boolean esFinal) {
        HBox box = new HBox(12);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10, 16, 10, 16));

        if (esFinal) {
            box.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 8px; -fx-border-color: #86efac; -fx-border-radius: 8px;");
        } else {
            box.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");
        }

        Label lblE = new Label(etiqueta);
        lblE.setStyle("-fx-font-weight: bold; -fx-font-size: " + (esFinal ? "15px" : "13px") + "; -fx-text-fill: " + colorHex + ";");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label lblV = new Label(MONEDA.format(valor));
        lblV.setStyle("-fx-font-weight: bold; -fx-font-size: " + (esFinal ? "18px" : "15px") + "; -fx-font-family: 'Consolas', monospace; -fx-text-fill: " + colorHex + ";");

        box.getChildren().addAll(lblE, sp, lblV);
        return box;
    }

    private void exportarHTML() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Reporte de Estado de Resultados");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Página Web Imprimible (*.html)", "*.html"));
        fc.setInitialFileName("Estado_Resultados_" + LocalDate.now() + ".html");
        File dest = fc.showSaveDialog(getScene().getWindow());
        if (dest != null) {
            try {
                ExportacionService.exportarEstadoResultadosHTML(estadoActual, "UNIVERSIDAD CATÓLICA DE EL SALVADOR - EMPRESA PRÁCTICA S.A. DE C.V.", dest);
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Reporte formal generado con éxito. ¿Desea abrirlo en su navegador para imprimir o guardar como PDF?", ButtonType.YES, ButtonType.NO);
                a.setTitle("Exportación Formal Exitosa");
                a.showAndWait().ifPresent(resp -> {
                    if (resp == ButtonType.YES && Desktop.isDesktopSupported()) {
                        try { Desktop.getDesktop().open(dest); } catch (Exception ignored) {}
                    }
                });
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
                a.showAndWait();
            }
        }
    }
}
