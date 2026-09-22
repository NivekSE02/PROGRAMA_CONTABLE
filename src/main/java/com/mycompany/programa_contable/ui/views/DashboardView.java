package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.dao.LibroDiarioDAO;
import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.BalanceGeneralDTO;
import com.mycompany.programa_contable.model.EstadoResultadosDTO;
import com.mycompany.programa_contable.service.ReportesFinancierosService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Dashboard Financiero Ejecutivo — Diseño Premium v3.0
 * Paleta: Rojo vino #881337 | Superficie #ffffff | Fondo #f1f5f9
 */
public class DashboardView extends ScrollPane {

    private final ReportesFinancierosService reportesService = new ReportesFinancierosService();
    private final LibroDiarioDAO libroDiarioDAO = new LibroDiarioDAO();
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    private VBox mainContainer;

    public DashboardView() {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane");

        mainContainer = new VBox(24);
        mainContainer.setPadding(new Insets(32, 40, 40, 40));
        mainContainer.setStyle("-fx-background-color: #f8fafc;");
        setContent(mainContainer);
        cargarDatos();
    }

    public void cargarDatos() {
        mainContainer.getChildren().clear();

        BalanceGeneralDTO bg = reportesService.generarBalanceGeneral();
        EstadoResultadosDTO er = reportesService.generarEstadoResultados();
        List<Asiento> asientos = libroDiarioDAO.listarAsientos("", "");

        // Cabecera
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(4);
        Label lblTitle = new Label("Dashboard Financiero");
        lblTitle.getStyleClass().add("page-title");

        Label lblSub = new Label(
            "Ciclo contable completo  ·  Partida doble automática  ·  Datos en tiempo real"
        );
        lblSub.getStyleClass().add("page-subtitle");
        titleBlock.getChildren().addAll(lblTitle, lblSub);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        Button btnRefresh = new Button("Actualizar");
        btnRefresh.getStyleClass().add("btn-secondary");
        btnRefresh.setOnAction(e -> cargarDatos());
        header.getChildren().addAll(titleBlock, btnRefresh);


        // KPI cards (4 x 2)
        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(20);
        kpiGrid.setVgap(20);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(25);
            kpiGrid.getColumnConstraints().add(cc);
        }

        double ratioLiq = bg.getTotalPasivoCorriente() > 0
            ? (bg.getTotalActivoCorriente() / bg.getTotalPasivoCorriente()) : 0.0;
        double margen = er.getTotalIngresos() > 0
            ? (er.getUtilidadNeta() / er.getTotalIngresos()) * 100.0 : 0.0;

        kpiGrid.add(kpiCard(
            "ACTIVO TOTAL",
            MONEDA.format(bg.getTotalActivo()),
            "Corriente: " + MONEDA.format(bg.getTotalActivoCorriente()),
            "card-accent-indigo", null, null
        ), 0, 0);
        kpiGrid.add(kpiCard(
            "PASIVO TOTAL",
            MONEDA.format(bg.getTotalPasivo()),
            "Corriente: " + MONEDA.format(bg.getTotalPasivoCorriente()),
            "card-accent-amber", null, null
        ), 1, 0);
        kpiGrid.add(kpiCard(
            "CAPITAL CONTABLE",
            MONEDA.format(bg.getTotalCapitalContable()),
            "Capital social + Utilidad del ejercicio",
            "card-accent-emerald", null, null
        ), 2, 0);
        kpiGrid.add(kpiCard(
            "UTILIDAD NETA",
            MONEDA.format(er.getUtilidadNeta()),
            "Ingresos: " + MONEDA.format(er.getTotalIngresos()),
            er.getUtilidadNeta() >= 0 ? "card-accent-emerald" : "card-accent-rose",
            er.getUtilidadNeta() >= 0 ? "▲ Ganancia" : "▼ Pérdida",
            er.getUtilidadNeta() >= 0
        ), 3, 0);

        kpiGrid.add(kpiCard(
            "RATIO DE LIQUIDEZ",
            String.format("%.2f", ratioLiq) + "×",
            "Activo Corriente / Pasivo Corriente",
            ratioLiq >= 1.0 ? "card-accent-indigo" : "card-accent-rose",
            ratioLiq >= 1.0 ? "▲ Solvente" : "▼ Riesgo",
            ratioLiq >= 1.0
        ), 0, 1);
        kpiGrid.add(kpiCard(
            "MARGEN NETO",
            String.format("%.1f", margen) + "%",
            "Rentabilidad sobre ventas brutas",
            margen >= 0 ? "card-accent-emerald" : "card-accent-rose",
            margen >= 0 ? "▲ Positivo" : "▼ Negativo",
            margen >= 0
        ), 1, 1);
        kpiGrid.add(kpiCard(
            "TOTAL INGRESOS",
            MONEDA.format(er.getTotalIngresos()),
            "Ventas del período contable",
            "card-accent-indigo", null, null
        ), 2, 1);
        kpiGrid.add(kpiCard(
            "COSTOS Y GASTOS",
            MONEDA.format(er.getTotalCostosYGastos()),
            "Costo de ventas + Gastos operativos",
            "card-accent-amber", null, null
        ), 3, 1);

        // Gráficos
        HBox chartsRow = new HBox(20);

        // — BarChart card
        VBox barCard = new VBox(12);
        barCard.getStyleClass().add("card");
        barCard.setPadding(new Insets(26, 26, 18, 26));
        HBox.setHgrow(barCard, Priority.ALWAYS);

        Label barTitle = new Label("Rendimiento del Período");
        barTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label barSub = new Label("Ingresos · Costos · Utilidad  (USD)");
        barSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #94a3b8;");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelGap(6);

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setBarGap(8);
        barChart.setCategoryGap(32);
        barChart.getStyleClass().add("chart");
        barChart.setPrefHeight(210);
        barChart.setMinHeight(190);
        VBox.setVgrow(barChart, Priority.ALWAYS);

        XYChart.Series<String, Number> barSeries = new XYChart.Series<>();
        barSeries.getData().add(new XYChart.Data<>("Ingresos", er.getTotalIngresos()));
        barSeries.getData().add(new XYChart.Data<>("Costos", er.getTotalCostosYGastos()));
        barSeries.getData().add(new XYChart.Data<>("Utilidad", Math.max(er.getUtilidadNeta(), 0)));
        barChart.getData().add(barSeries);

        barCard.getChildren().addAll(barTitle, barSub, barChart);

        // — PieChart card
        VBox pieCard = new VBox(12);
        pieCard.getStyleClass().add("card");
        pieCard.setPadding(new Insets(26, 26, 18, 26));
        pieCard.setPrefWidth(310);
        pieCard.setMinWidth(280);

        Label pieTitle = new Label("Composición de Activos");
        pieTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label pieSub = new Label("Corriente vs. No Corriente");
        pieSub.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #94a3b8;");

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
            new PieChart.Data("Corriente", bg.getTotalActivoCorriente()),
            new PieChart.Data("No Corriente", bg.getTotalActivoNoCorriente())
        );
        PieChart pieChart = new PieChart(pieData);
        pieChart.setLegendVisible(true);
        pieChart.setAnimated(false);
        pieChart.getStyleClass().add("chart");
        pieChart.setPrefHeight(230);
        VBox.setVgrow(pieChart, Priority.ALWAYS);

        pieCard.getChildren().addAll(pieTitle, pieSub, pieChart);

        chartsRow.getChildren().addAll(barCard, pieCard);

        // ── Tabla de últimos asientos (ocupa todo el espacio disponible en 1920×1080)
        VBox tableCard = new VBox(16);
        tableCard.getStyleClass().add("card");
        tableCard.setPadding(new Insets(24, 28, 24, 28));
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        // Header de la sección tabla
        HBox tableHeader = new HBox();
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        VBox tableHeaderText = new VBox(3);
        Label tableTitle = new Label("Últimos Asientos del Libro Diario");
        tableTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label tableSub = new Label("Registro cronológico de operaciones del período");
        tableSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        tableHeaderText.getChildren().addAll(tableTitle, tableSub);
        HBox.setHgrow(tableHeaderText, Priority.ALWAYS);

        Label lblCount = new Label(asientos.size() + " registros");
        lblCount.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: 600; -fx-padding: 4 12; -fx-background-color: #f1f5f9; -fx-background-radius: 20px;");
        tableHeader.getChildren().addAll(tableHeaderText, lblCount);

        Separator sep = new Separator();

        // Tabla de asientos
        TableView<Asiento> table = new TableView<>();
        table.setPrefHeight(300);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("table-view");
        table.setStyle("-fx-effect: none; -fx-border-color: transparent;");

        TableColumn<Asiento, String> colN = col("N°",
            a -> String.valueOf(a.getNumero()), 50, 55);
        TableColumn<Asiento, String> colF = col("Fecha",
            a -> a.getFecha(), 110, 115);
        TableColumn<Asiento, String> colC = col("Concepto",
            a -> a.getConcepto(), -1, -1);
        TableColumn<Asiento, String> colD = col("Debe",
            a -> MONEDA.format(a.getTotalDebe()), 130, 135);
        colD.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<Asiento, String> colH = col("Haber",
            a -> MONEDA.format(a.getTotalHaber()), 130, 135);
        colH.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<Asiento, String> colE = col("Estado",
            a -> a.isPartidaDobleValida() ? "Cuadrado" : "Descuadrado", 120, 130);
        colE.setStyle("-fx-alignment: CENTER;");

        table.getColumns().addAll(colN, colF, colC, colD, colH, colE);
        table.setItems(FXCollections.observableArrayList(asientos));

        tableCard.getChildren().addAll(tableHeader, sep, table);

        // ── Ensamblar todo
        mainContainer.getChildren().addAll(header, kpiGrid, chartsRow, tableCard);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────


    /**
     * Tarjeta KPI premium con badge de variación.
     */
    private VBox kpiCard(String titulo, String valor, String subtitulo,
                          String acento, String badge, Boolean positivo) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("card", acento);
        card.setPadding(new Insets(24, 24, 20, 24));

        Label lblT = new Label(titulo);
        lblT.getStyleClass().add("card-title");

        Label lblV = new Label(valor);
        lblV.getStyleClass().add("card-value");
        lblV.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-padding: 6px 0 2px 0;");

        HBox bottom = new HBox(8);
        bottom.setAlignment(Pos.CENTER_LEFT);
        Label lblS = new Label(subtitulo);
        lblS.getStyleClass().add("card-subtitle");
        lblS.setWrapText(true);
        HBox.setHgrow(lblS, Priority.ALWAYS);
        bottom.getChildren().add(lblS);

        if (badge != null) {
            Label bdg = new Label(badge);
            String badgeStyle = positivo
                ? "-fx-background-color: #f0fdf4; -fx-text-fill: #15803d; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 20px;"
                : "-fx-background-color: #fff1f2; -fx-text-fill: #881337; -fx-font-weight: 700; -fx-font-size: 11px; -fx-padding: 3 10; -fx-background-radius: 20px;";
            bdg.setStyle(badgeStyle);
            bottom.getChildren().add(bdg);
        }

        card.getChildren().addAll(lblT, lblV, bottom);
        return card;
    }

    /**
     * Crea una columna de tabla de forma concisa.
     */
    private TableColumn<Asiento, String> col(
            String header,
            java.util.function.Function<Asiento, String> extractor,
            double minW, double maxW) {
        TableColumn<Asiento, String> c = new TableColumn<>(header);
        c.setCellValueFactory(cell -> new SimpleStringProperty(extractor.apply(cell.getValue())));
        if (minW > 0) c.setMinWidth(minW);
        if (maxW > 0) c.setMaxWidth(maxW);
        return c;
    }
}
