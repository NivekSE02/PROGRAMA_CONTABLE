package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.model.BalanceGeneralDTO;
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

public class BalanceGeneralView extends ScrollPane {

    private final ReportesFinancierosService reportesService = new ReportesFinancierosService();
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    private VBox mainContainer;
    private BalanceGeneralDTO balanceActual;

    public BalanceGeneralView() {
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
        balanceActual = reportesService.generarBalanceGeneral();

        // 1. Cabecera
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label lblTitulo = new Label("Balance General");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label lblSub = new Label("Código 1 (Activo) = Código 2 (Pasivo) + Código 3 (Capital Contable)");
        lblSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblTitulo, lblSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        MenuButton btnImprimir = new MenuButton("Exportar Reporte");
        btnImprimir.getStyleClass().add("btn-primary");
        btnImprimir.getStyleClass().add("export-button");
        btnImprimir.setStyle("-fx-text-fill: white;");
        MenuItem mnuHtml = new MenuItem("Exportar a HTML");
        mnuHtml.getStyleClass().add("export-menu-item");
        mnuHtml.setOnAction(e -> exportarHTML());
        MenuItem mnuExcel = new MenuItem("Exportar a Excel (.xlsx)");
        mnuExcel.getStyleClass().add("export-menu-item");
        mnuExcel.setOnAction(e -> exportarExcel());
        btnImprimir.getItems().addAll(mnuHtml, mnuExcel);

        Button btnRefrescar = new Button("Actualizar");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> cargarDatos());
        
        Button btnAyuda = new Button("Ayuda");
        btnAyuda.getStyleClass().add("btn-secondary");
        btnAyuda.setOnAction(e -> {
            String msg = "";
            if (balanceActual.isCuadrado()) {
                msg = "ECUACIÓN CONTABLE VERIFICADA (CÓDIGO 1 = 2 + 3):\nACTIVO: " + MONEDA.format(balanceActual.getTotalActivo()) +
                      "  ==  PASIVO (" + MONEDA.format(balanceActual.getTotalPasivo()) +
                      ") + CAPITAL (" + MONEDA.format(balanceActual.getTotalCapitalContable()) +
                      ") = " + MONEDA.format(balanceActual.getTotalPasivoMasCapital());
            } else {
                msg = "ATENCIÓN: Descuadre de " + MONEDA.format(balanceActual.getDiferencia());
            }
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setHeaderText("Ecuación Contable");
            a.showAndWait();
        });

        topBar.getChildren().addAll(titleBox, btnAyuda, btnImprimir, btnRefrescar);


        // 3. Dos Columnas: Izquierda = Activos, Derecha = Pasivos y Capital
        HBox columnas = new HBox(20);

        // Columna Izquierda: Activos
        VBox colActivos = new VBox(12);
        colActivos.getStyleClass().add("card");
        HBox.setHgrow(colActivos, Priority.ALWAYS);

        Label lblActHeader = new Label("1. ACTIVOS");
        lblActHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");

        TableView<BalanceGeneralDTO.LineaBalance> tblActCte = crearTablaLineas(balanceActual.getActivosCorrientes(), "Activo Corriente");
        Label lblTotActCte = new Label("Total Activo Corriente: " + MONEDA.format(balanceActual.getTotalActivoCorriente()));
        lblTotActCte.setStyle("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-text-fill: #475569;");
        lblTotActCte.setMaxWidth(Double.MAX_VALUE);

        TableView<BalanceGeneralDTO.LineaBalance> tblActNoCte = crearTablaLineas(balanceActual.getActivosNoCorrientes(), "Activo No Corriente");
        Label lblTotActNoCte = new Label("Total Activo No Corriente: " + MONEDA.format(balanceActual.getTotalActivoNoCorriente()));
        lblTotActNoCte.setStyle("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-text-fill: #475569;");
        lblTotActNoCte.setMaxWidth(Double.MAX_VALUE);

        HBox totActBox = new HBox(10);
        totActBox.setAlignment(Pos.CENTER_LEFT);
        totActBox.setPadding(new Insets(10, 14, 10, 14));
        totActBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px;");
        Label lblTotActTitle = new Label("TOTAL ACTIVO (CÓDIGO 1):");
        lblTotActTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Label lblTotActVal = new Label(MONEDA.format(balanceActual.getTotalActivo()));
        lblTotActVal.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: 'Consolas', monospace; -fx-text-fill: #1e3a8a;");
        totActBox.getChildren().addAll(lblTotActTitle, sp1, lblTotActVal);

        colActivos.getChildren().addAll(lblActHeader, new Label("Activo Corriente"), tblActCte, lblTotActCte, new Separator(),
            new Label("Activo No Corriente"), tblActNoCte, lblTotActNoCte, new Separator(), totActBox);

        // Columna Derecha: Pasivos y Capital Contable
        VBox colPasivoCapital = new VBox(12);
        colPasivoCapital.getStyleClass().add("card");
        HBox.setHgrow(colPasivoCapital, Priority.ALWAYS);

        Label lblPasHeader = new Label("2. PASIVOS");
        lblPasHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #9a3412;");

        TableView<BalanceGeneralDTO.LineaBalance> tblPasCte = crearTablaLineas(balanceActual.getPasivosCorrientes(), "Pasivo Corriente");
        Label lblTotPasCte = new Label("Total Pasivo Corriente: " + MONEDA.format(balanceActual.getTotalPasivoCorriente()));
        lblTotPasCte.setStyle("-fx-font-weight: bold; -fx-alignment: CENTER-RIGHT; -fx-text-fill: #475569;");
        lblTotPasCte.setMaxWidth(Double.MAX_VALUE);

        HBox totPasBox = new HBox(10);
        totPasBox.setAlignment(Pos.CENTER_LEFT);
        totPasBox.setPadding(new Insets(8, 12, 8, 12));
        totPasBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8px;");
        Label lblTotPasTitle = new Label("TOTAL PASIVO (CÓDIGO 2):");
        lblTotPasTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        Label lblTotPasVal = new Label(MONEDA.format(balanceActual.getTotalPasivo()));
        lblTotPasVal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-font-family: 'Consolas', monospace; -fx-text-fill: #9a3412;");
        totPasBox.getChildren().addAll(lblTotPasTitle, sp2, lblTotPasVal);

        Label lblCapHeader = new Label("3. CAPITAL CONTABLE / PATRIMONIO");
        lblCapHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #047857;");

        TableView<BalanceGeneralDTO.LineaBalance> tblCap = crearTablaLineas(balanceActual.getCuentasCapital(), "Cuentas de Patrimonio");

        // Fila destacada de la Utilidad del Ejercicio
        HBox utilBox = new HBox(10);
        utilBox.setAlignment(Pos.CENTER_LEFT);
        utilBox.setPadding(new Insets(6, 10, 6, 10));
        utilBox.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        Label lblUtilTit = new Label("330102 Utilidad Neta del Presente Ejercicio (5 - 4):");
        lblUtilTit.setStyle("-fx-font-weight: bold; -fx-text-fill: #15803d;");
        Region sp3 = new Region();
        HBox.setHgrow(sp3, Priority.ALWAYS);
        Label lblUtilVal = new Label(MONEDA.format(balanceActual.getUtilidadDelEjercicio()));
        lblUtilVal.setStyle("-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace; -fx-text-fill: #15803d;");
        utilBox.getChildren().addAll(lblUtilTit, sp3, lblUtilVal);

        HBox totCapBox = new HBox(10);
        totCapBox.setAlignment(Pos.CENTER_LEFT);
        totCapBox.setPadding(new Insets(8, 12, 8, 12));
        totCapBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8px;");
        Label lblTotCapTitle = new Label("TOTAL CAPITAL CONTABLE (CÓDIGO 3):");
        lblTotCapTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");
        Region sp4 = new Region();
        HBox.setHgrow(sp4, Priority.ALWAYS);
        Label lblTotCapVal = new Label(MONEDA.format(balanceActual.getTotalCapitalContable()));
        lblTotCapVal.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-font-family: 'Consolas', monospace; -fx-text-fill: #047857;");
        totCapBox.getChildren().addAll(lblTotCapTitle, sp4, lblTotCapVal);

        // Gran Total: Pasivo + Capital Contable
        HBox totPasCapBox = new HBox(10);
        totPasCapBox.setAlignment(Pos.CENTER_LEFT);
        totPasCapBox.setPadding(new Insets(10, 14, 10, 14));
        totPasCapBox.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 8px; -fx-border-color: #86efac; -fx-border-radius: 8px;");
        Label lblTotPasCapTitle = new Label("TOTAL PASIVO + CAPITAL (2 + 3):");
        lblTotPasCapTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #15803d;");
        Region sp5 = new Region();
        HBox.setHgrow(sp5, Priority.ALWAYS);
        Label lblTotPasCapVal = new Label(MONEDA.format(balanceActual.getTotalPasivoMasCapital()));
        lblTotPasCapVal.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-font-family: 'Consolas', monospace; -fx-text-fill: #15803d;");
        totPasCapBox.getChildren().addAll(lblTotPasCapTitle, sp5, lblTotPasCapVal);

        colPasivoCapital.getChildren().addAll(lblPasHeader, new Label("Pasivo Corriente"), tblPasCte, lblTotPasCte, totPasBox,
            new Separator(), lblCapHeader, tblCap, utilBox, totCapBox, new Separator(), totPasCapBox);

        columnas.getChildren().addAll(colActivos, colPasivoCapital);

        mainContainer.getChildren().addAll(topBar, columnas);
    }

    private TableView<BalanceGeneralDTO.LineaBalance> crearTablaLineas(java.util.List<BalanceGeneralDTO.LineaBalance> lineas, String titulo) {
        TableView<BalanceGeneralDTO.LineaBalance> tbl = new TableView<>();
        tbl.setPrefHeight(200);

        TableColumn<BalanceGeneralDTO.LineaBalance, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCod.setPrefWidth(90);

        TableColumn<BalanceGeneralDTO.LineaBalance, String> colNom = new TableColumn<>("Cuenta");
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colNom.setPrefWidth(260);

        TableColumn<BalanceGeneralDTO.LineaBalance, String> colMon = new TableColumn<>("Saldo ($)");
        colMon.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getMonto())));
        colMon.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colMon.setPrefWidth(120);

        tbl.getColumns().addAll(colCod, colNom, colMon);
        tbl.setItems(FXCollections.observableArrayList(lineas));
        return tbl;
    }

    private void exportarHTML() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Reporte de Balance General");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Página Web Imprimible (*.html)", "*.html"));
        fc.setInitialFileName("Balance_General_" + LocalDate.now() + ".html");
        File dest = fc.showSaveDialog(getScene().getWindow());
        if (dest != null) {
            try {
                ExportacionService.exportarBalanceGeneralHTML(balanceActual, "Empresa Práctica S.A. de C.V.", dest);
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Reporte generado con éxito. ¿Desea abrirlo en su navegador para imprimir o guardar como PDF?", ButtonType.YES, ButtonType.NO);
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

    private void exportarExcel() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Reporte de Balance General");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("Balance_General_" + LocalDate.now() + ".xlsx");
        File dest = fc.showSaveDialog(getScene().getWindow());
        if (dest != null) {
            try {
                ExportacionService.exportarBalanceGeneralExcel(balanceActual, "Empresa Práctica S.A. de C.V.", dest);
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Libro de Excel generado con éxito. ¿Desea abrirlo ahora?", ButtonType.YES, ButtonType.NO);
                a.setTitle("Exportación a Excel Exitosa");
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
