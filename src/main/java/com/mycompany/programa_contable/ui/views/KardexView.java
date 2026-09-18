package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.model.KardexFilaDTO;
import com.mycompany.programa_contable.service.KardexService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.text.DecimalFormat;
import java.util.List;

public class KardexView extends ScrollPane {

    private final KardexService kardexService = new KardexService();
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    private VBox mainContainer;
    // Asumiremos el ID 1 (Queso) para este ejercicio. Luego puedes agregar un ComboBox si hay más productos.
    private final int PRODUCTO_ACTUAL_ID = 1; 

    public KardexView() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");

        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(24));
        setContent(mainContainer);

        cargarDatos();
    }

    public void cargarDatos() {
        mainContainer.getChildren().clear();
        
        // Obtenemos las filas automáticas desde el servicio
        List<KardexFilaDTO> reporteKardex = kardexService.generarReporteKardex(PRODUCTO_ACTUAL_ID);

        // 1. Cabecera institucional
        HBox topBar = new HBox(16);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblInst = new Label("UNIVERSIDAD CATÓLICA DE EL SALVADOR - EMPRESA PRÁCTICA S.A. DE C.V.");
        lblInst.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
        Label lblTitulo = new Label("KÁRDEX DE INVENTARIO AUTOMÁTICO");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label lblSub = new Label("Método de Valuación: PEPS (Primeras Entradas, Primeras Salidas) - Producto: Queso");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblInst, lblTitulo, lblSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button btnImprimir = new Button("📄 Exportar Kárdex");
        btnImprimir.getStyleClass().add("btn-primary");
        btnImprimir.setOnAction(e -> mostrarAlertaConstruccion());

        Button btnRefrescar = new Button("🔄 Actualizar");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> cargarDatos());

        topBar.getChildren().addAll(titleBox, btnImprimir, btnRefrescar);

        // Extraer totales para el banner final
        int existenciasFinales = 0;
        double saldoFinal = 0.0;
        if (!reporteKardex.isEmpty()) {
            KardexFilaDTO ultimaFila = reporteKardex.get(reporteKardex.size() - 1);
            existenciasFinales = ultimaFila.getExistencias();
            saldoFinal = ultimaFila.getSaldoMonetario();
        }

        // 2. Banner Resumen del Inventario Actual
        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(14, 20, 14, 20));
        banner.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 10px; -fx-border-color: #bbf7d0; -fx-border-radius: 10px;");
        
        Label lblCheck = new Label("📦 INVENTARIO ACTUAL EN BODEGA:");
        lblCheck.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #166534;");
        
        Label lblFormula = new Label(existenciasFinales + " Unidades Disponibles  |  Valor Total: " + MONEDA.format(saldoFinal));
        lblFormula.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #15803d; -fx-font-family: 'Consolas', monospace;");
        banner.getChildren().addAll(lblCheck, lblFormula);

        // 3. Tarjeta central con la Tabla del Kárdex
        VBox cardReporte = new VBox(14);
        cardReporte.getStyleClass().add("card");

        Label lblTablaTitle = new Label("MOVIMIENTOS DE ENTRADAS Y SALIDAS");
        lblTablaTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e3a8a;");
        
        TableView<KardexFilaDTO> tblKardex = crearTablaKardex(reporteKardex);

        cardReporte.getChildren().addAll(lblTablaTitle, tblKardex);
        
        // -----------------------------------------------------------------
        // 4. Barra de Totales Inferior (Estilo Balanza de Comprobación)
        // -----------------------------------------------------------------
        int totalUnidadesEntrada = 0;
        int totalUnidadesSalida = 0;
        double totalDineroEntrada = 0.0;
        double totalDineroSalida = 0.0;

        for (KardexFilaDTO fila : reporteKardex) {
            totalUnidadesEntrada += fila.getEntrada();
            totalUnidadesSalida += fila.getSalida();
            if (fila.getEntrada() > 0) totalDineroEntrada += fila.getCostoTotal();
            if (fila.getSalida() > 0) totalDineroSalida += fila.getCostoTotal();
        }

        HBox bottomBar = new HBox(20);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(15, 20, 15, 20));
        bottomBar.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label lblTotalesTitle = new Label("SUMAS TOTALES:");
        lblTotalesTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label lblSumaEntradas = new Label("Mov. Entradas: " + totalUnidadesEntrada + " U (" + MONEDA.format(totalDineroEntrada) + ")");
        lblSumaEntradas.setStyle("-fx-text-fill: #047857; -fx-font-family: 'Consolas', monospace;");

        Label lblSumaSalidas = new Label("Mov. Salidas: " + totalUnidadesSalida + " U (" + MONEDA.format(totalDineroSalida) + ")");
        lblSumaSalidas.setStyle("-fx-text-fill: #b91c1c; -fx-font-family: 'Consolas', monospace;");

        Region spacerBottom = new Region();
        HBox.setHgrow(spacerBottom, Priority.ALWAYS);

        // El Badge Dorado idéntico a tu imagen
        Label badgeInventario = new Label("✔ INVENTARIO VALUADO (PEPS)");
        badgeInventario.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 16 8 16; -fx-background-radius: 5px;");

        bottomBar.getChildren().addAll(lblTotalesTitle, lblSumaEntradas, lblSumaSalidas, spacerBottom, badgeInventario);
            
        // Agregamos todo al contenedor principal, incluyendo la nueva barra inferior
        mainContainer.getChildren().addAll(topBar, banner, cardReporte, bottomBar);

    }

    private TableView<KardexFilaDTO> crearTablaKardex(List<KardexFilaDTO> lineas) {
        TableView<KardexFilaDTO> tbl = new TableView<>();
        tbl.setPrefHeight(Math.max(200, (lineas.size() + 1) * 35));

        TableColumn<KardexFilaDTO, String> colFec = new TableColumn<>("Fecha");
        colFec.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha()));
        colFec.setPrefWidth(90);

        TableColumn<KardexFilaDTO, String> colCon = new TableColumn<>("Concepto");
        colCon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConcepto()));
        colCon.setPrefWidth(200);

        TableColumn<KardexFilaDTO, String> colEnt = new TableColumn<>("Entrada (U)");
        colEnt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEntrada() > 0 ? String.valueOf(c.getValue().getEntrada()) : "-"));
        colEnt.setStyle("-fx-alignment: CENTER; -fx-text-fill: #047857; -fx-font-weight: bold;");
        colEnt.setPrefWidth(90);

        TableColumn<KardexFilaDTO, String> colSal = new TableColumn<>("Salida (U)");
        colSal.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSalida() > 0 ? String.valueOf(c.getValue().getSalida()) : "-"));
        colSal.setStyle("-fx-alignment: CENTER; -fx-text-fill: #b91c1c; -fx-font-weight: bold;");
        colSal.setPrefWidth(90);

        TableColumn<KardexFilaDTO, String> colExi = new TableColumn<>("Existencias");
        colExi.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getExistencias())));
        colExi.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-background-color: #f8fafc;");
        colExi.setPrefWidth(90);

        TableColumn<KardexFilaDTO, String> colCosU = new TableColumn<>("Costo Unit.");
        colCosU.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getCostoUnitario())));
        colCosU.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colCosU.setPrefWidth(100);

        TableColumn<KardexFilaDTO, String> colCosT = new TableColumn<>("Total Mov.");
        colCosT.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getCostoTotal())));
        colCosT.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colCosT.setPrefWidth(120);

        TableColumn<KardexFilaDTO, String> colSaldo = new TableColumn<>("Saldo Bodega ($)");
        colSaldo.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getSaldoMonetario())));
        colSaldo.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-font-family: 'Consolas', monospace; -fx-text-fill: #0f172a;");
        colSaldo.setPrefWidth(140);

        tbl.getColumns().addAll(colFec, colCon, colEnt, colSal, colExi, colCosU, colCosT, colSaldo);
        tbl.setItems(FXCollections.observableArrayList(lineas));
        return tbl;
    }

    private void mostrarAlertaConstruccion() {
        Alert a = new Alert(Alert.AlertType.INFORMATION, "El módulo de exportación a HTML para el Kárdex está en desarrollo.", ButtonType.OK);
        a.setTitle("Función en Desarrollo");
        a.setHeaderText(null);
        a.showAndWait();
    }
}