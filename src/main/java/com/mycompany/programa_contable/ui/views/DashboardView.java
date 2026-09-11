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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.util.List;

public class DashboardView extends ScrollPane {

    private final ReportesFinancierosService reportesService = new ReportesFinancierosService();
    private final LibroDiarioDAO libroDiarioDAO = new LibroDiarioDAO();
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    private VBox mainContainer;

    public DashboardView() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");

        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(24));
        setContent(mainContainer);

        cargarDatos();
    }

    public void cargarDatos() {
        mainContainer.getChildren().clear();

        // 1. Obtener estados financieros en tiempo real
        BalanceGeneralDTO bg = reportesService.generarBalanceGeneral();
        EstadoResultadosDTO er = reportesService.generarEstadoResultados();
        List<Asiento> ultimosAsientos = libroDiarioDAO.listarAsientos("", "");

        // Cabecera del Dashboard
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblTitle = new Label("Dashboard Financiero Ejecutivo");
        lblTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label lblSub = new Label("Monitoreo en tiempo real del ciclo contable y estados financieros automáticos");
        lblSub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button btnRefresh = new Button("🔄 Actualizar Datos");
        btnRefresh.getStyleClass().add("btn-secondary");
        btnRefresh.setOnAction(e -> cargarDatos());

        header.getChildren().addAll(titleBox, btnRefresh);

        // 2. Tarjetas KPI Principales
        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(16);
        kpiGrid.setVgap(16);

        VBox cardActivo = crearCardKPI("ACTIVO TOTAL (1)", MONEDA.format(bg.getTotalActivo()),
            "Act. Corriente: " + MONEDA.format(bg.getTotalActivoCorriente()), "card-accent-indigo");

        VBox cardPasivo = crearCardKPI("PASIVO TOTAL (2)", MONEDA.format(bg.getTotalPasivo()),
            "Pas. Corriente: " + MONEDA.format(bg.getTotalPasivoCorriente()), "card-accent-amber");

        VBox cardCapital = crearCardKPI("CAPITAL CONTABLE (3)", MONEDA.format(bg.getTotalCapitalContable()),
            "Capital + Resultados", "card-accent-emerald");

        String estiloUtilidad = er.getUtilidadNeta() >= 0 ? "card-accent-emerald" : "card-accent-rose";
        VBox cardUtilidad = crearCardKPI("UTILIDAD NETA (5 - 4)", MONEDA.format(er.getUtilidadNeta()),
            "Ingresos: " + MONEDA.format(er.getTotalIngresos()) + " | Costos: " + MONEDA.format(er.getTotalCostosYGastos()), estiloUtilidad);

        // Ratios Clave
        double ratioLiquidez = bg.getTotalPasivoCorriente() > 0
            ? (bg.getTotalActivoCorriente() / bg.getTotalPasivoCorriente()) : 0.0;
        VBox cardLiquidez = crearCardKPI("RATIO DE LIQUIDEZ", String.format("%.2f", ratioLiquidez) + "x",
            "Activo Cte / Pasivo Cte (Solvencia)", "card-accent-indigo");

        double margenNeto = er.getTotalIngresos() > 0 ? (er.getUtilidadNeta() / er.getTotalIngresos()) * 100.0 : 0.0;
        VBox cardMargen = crearCardKPI("MARGEN NETO", String.format("%.1f", margenNeto) + "%",
            "Rentabilidad sobre ventas", "card-accent-emerald");

        kpiGrid.add(cardActivo, 0, 0);
        kpiGrid.add(cardPasivo, 1, 0);
        kpiGrid.add(cardCapital, 2, 0);
        kpiGrid.add(cardUtilidad, 3, 0);
        kpiGrid.add(cardLiquidez, 0, 1);
        kpiGrid.add(cardMargen, 1, 1);

        // Banner de verificación de la partida doble en el Balance General
        HBox bannerBalance = new HBox(12);
        bannerBalance.setAlignment(Pos.CENTER_LEFT);
        bannerBalance.setPadding(new Insets(12, 18, 12, 18));
        if (bg.isCuadrado()) {
            bannerBalance.setStyle("-fx-background-color: #dcfce7; -fx-background-radius: 10px; -fx-border-color: #86efac; -fx-border-radius: 10px;");
            Label lblCheck = new Label("✔ ECUACIÓN PATRIMONIAL BALANCEADA:");
            lblCheck.setStyle("-fx-font-weight: bold; -fx-text-fill: #15803d; -fx-font-size: 13px;");
            Label lblFormula = new Label("Activo (" + MONEDA.format(bg.getTotalActivo()) + ") = Pasivo (" + MONEDA.format(bg.getTotalPasivo()) + ") + Capital Contable (" + MONEDA.format(bg.getTotalCapitalContable()) + ")");
            lblFormula.setStyle("-fx-text-fill: #166534; -fx-font-weight: 600;");
            bannerBalance.getChildren().addAll(lblCheck, lblFormula);
        } else {
            bannerBalance.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 10px; -fx-border-color: #fca5a5; -fx-border-radius: 10px;");
            Label lblErr = new Label("⚠ ATENCIÓN: Descuadre en Balance General:");
            lblErr.setStyle("-fx-font-weight: bold; -fx-text-fill: #b91c1c; -fx-font-size: 13px;");
            Label lblDif = new Label("Diferencia detectada de " + MONEDA.format(bg.getDiferencia()));
            lblDif.setStyle("-fx-text-fill: #991b1b;");
            bannerBalance.getChildren().addAll(lblErr, lblDif);
        }

        // 3. Gráficos Visuales (Ingresos vs Gastos + Distribución de Activos)
        HBox chartsBox = new HBox(16);

        // Gráfico de Barras: Comparativo del Estado de Resultados
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Categoría Contable");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Monto en Dólares ($)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Rendimiento: Ingresos vs Costos y Gastos (USD)");
        barChart.setLegendVisible(false);
        barChart.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-padding: 10px;");
        HBox.setHgrow(barChart, Priority.ALWAYS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Ingresos (5)", er.getTotalIngresos()));
        series.getData().add(new XYChart.Data<>("Costos/Gastos (4)", er.getTotalCostosYGastos()));
        series.getData().add(new XYChart.Data<>("Utilidad Neta", er.getUtilidadNeta()));
        barChart.getData().add(series);

        // Gráfico de Pastel: Distribución de Activos
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
            new PieChart.Data("Activo Corriente", bg.getTotalActivoCorriente()),
            new PieChart.Data("Activo No Corriente", bg.getTotalActivoNoCorriente())
        );
        PieChart pieChart = new PieChart(pieData);
        pieChart.setTitle("Composición de Activos");
        pieChart.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-padding: 10px;");
        pieChart.setPrefWidth(350);

        chartsBox.getChildren().addAll(barChart, pieChart);

        // 4. Tabla de Últimos Asientos del Libro Diario
        VBox recentBox = new VBox(10);
        recentBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-padding: 16px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");
        Label lblRecent = new Label("Últimos Asientos Registrados en el Libro Diario");
        lblRecent.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        TableView<Asiento> tblAsientos = new TableView<>();
        tblAsientos.setPrefHeight(200);

        TableColumn<Asiento, String> colNum = new TableColumn<>("N°");
        colNum.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getNumero())));
        colNum.setPrefWidth(60);

        TableColumn<Asiento, String> colFec = new TableColumn<>("Fecha");
        colFec.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha()));
        colFec.setPrefWidth(100);

        TableColumn<Asiento, String> colCon = new TableColumn<>("Concepto General");
        colCon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConcepto()));
        colCon.setPrefWidth(380);

        TableColumn<Asiento, String> colDebe = new TableColumn<>("Total Debe");
        colDebe.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getTotalDebe())));
        colDebe.setPrefWidth(120);

        TableColumn<Asiento, String> colHaber = new TableColumn<>("Total Haber");
        colHaber.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getTotalHaber())));
        colHaber.setPrefWidth(120);

        TableColumn<Asiento, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isPartidaDobleValida() ? "✔ Cuadrado" : "⚠ Descuadrado"));
        colEstado.setPrefWidth(110);

        tblAsientos.getColumns().addAll(colNum, colFec, colCon, colDebe, colHaber, colEstado);
        tblAsientos.setItems(FXCollections.observableArrayList(ultimosAsientos));

        recentBox.getChildren().addAll(lblRecent, tblAsientos);

        mainContainer.getChildren().addAll(header, kpiGrid, bannerBalance, chartsBox, recentBox);
    }

    private VBox crearCardKPI(String titulo, String valor, String subtitulo, String estiloAcento) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("card", estiloAcento);
        card.setPrefWidth(240);

        Label lblT = new Label(titulo);
        lblT.getStyleClass().add("card-title");

        Label lblV = new Label(valor);
        lblV.getStyleClass().add("card-value");

        Label lblS = new Label(subtitulo);
        lblS.getStyleClass().add("card-subtitle");

        card.getChildren().addAll(lblT, lblV, lblS);
        return card;
    }
}
