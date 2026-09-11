package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.model.BalanzaComprobacionDTO;
import com.mycompany.programa_contable.service.ExportacionService;
import com.mycompany.programa_contable.service.ReportesFinancierosService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;

public class BalanzaComprobacionView extends VBox {

    private final ReportesFinancierosService reportesService = new ReportesFinancierosService();
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    private TableView<BalanzaComprobacionDTO.Renglon> tblBalanza;
    private Label lblTotMovDebe;
    private Label lblTotMovHaber;
    private Label lblTotSalDeudor;
    private Label lblTotSalAcreedor;
    private Label lblStatusCuadre;
    private BalanzaComprobacionDTO balanzaActual;

    public BalanzaComprobacionView() {
        setPadding(new Insets(20));
        setSpacing(16);
        setStyle("-fx-background-color: transparent;");

        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Cabecera institucional del reporte
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label lblEmpresa = new Label("UNIVERSIDAD CATÓLICA DE EL SALVADOR - EMPRESA PRÁCTICA S.A. DE C.V.");
        lblEmpresa.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
        Label lblTitulo = new Label("BALANZA DE COMPROBACIÓN DE SUMAS Y SALDOS");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label lblPeriodo = new Label("Período Actual | Expresado en Dólares de los Estados Unidos de América (USD)");
        lblPeriodo.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblEmpresa, lblTitulo, lblPeriodo);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Button btnExportar = new Button("📥 Exportar a CSV");
        btnExportar.getStyleClass().add("btn-secondary");
        btnExportar.setOnAction(e -> exportarCSV());

        Button btnRefrescar = new Button("🔄 Actualizar");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> cargarDatos());

        topBar.getChildren().addAll(titleBox, btnExportar, btnRefrescar);

        // Tabla de la Balanza
        tblBalanza = new TableView<>();
        VBox.setVgrow(tblBalanza, Priority.ALWAYS);

        TableColumn<BalanzaComprobacionDTO.Renglon, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCod.setPrefWidth(90);

        TableColumn<BalanzaComprobacionDTO.Renglon, String> colNom = new TableColumn<>("Nombre de la Cuenta");
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colNom.setPrefWidth(280);

        TableColumn<BalanzaComprobacionDTO.Renglon, String> colTip = new TableColumn<>("Clase");
        colTip.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo().getNombre()));
        colTip.setPrefWidth(120);

        // Grupo de columnas: Movimientos
        TableColumn<BalanzaComprobacionDTO.Renglon, String> colMovGroup = new TableColumn<>("MOVIMIENTOS DEL PERÍODO");

        TableColumn<BalanzaComprobacionDTO.Renglon, String> colMovDebe = new TableColumn<>("Debe ($)");
        colMovDebe.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getMovimientoDebe())));
        colMovDebe.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colMovDebe.setPrefWidth(130);

        TableColumn<BalanzaComprobacionDTO.Renglon, String> colMovHaber = new TableColumn<>("Haber ($)");
        colMovHaber.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getMovimientoHaber())));
        colMovHaber.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colMovHaber.setPrefWidth(130);

        colMovGroup.getColumns().addAll(colMovDebe, colMovHaber);

        // Grupo de columnas: Saldos
        TableColumn<BalanzaComprobacionDTO.Renglon, String> colSalGroup = new TableColumn<>("SALDOS FINALES");

        TableColumn<BalanzaComprobacionDTO.Renglon, String> colSalDeudor = new TableColumn<>("Deudor ($)");
        colSalDeudor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSaldoDeudor() > 0 ? MONEDA.format(c.getValue().getSaldoDeudor()) : "$0.00"));
        colSalDeudor.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #15803d;");
        colSalDeudor.setPrefWidth(130);

        TableColumn<BalanzaComprobacionDTO.Renglon, String> colSalAcreedor = new TableColumn<>("Acreedor ($)");
        colSalAcreedor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSaldoAcreedor() > 0 ? MONEDA.format(c.getValue().getSaldoAcreedor()) : "$0.00"));
        colSalAcreedor.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #1e40af;");
        colSalAcreedor.setPrefWidth(130);

        colSalGroup.getColumns().addAll(colSalDeudor, colSalAcreedor);

        tblBalanza.getColumns().addAll(colCod, colNom, colTip, colMovGroup, colSalGroup);

        // Panel de Sumas Iguales
        GridPane gridTotales = new GridPane();
        gridTotales.setHgap(20);
        gridTotales.setVgap(6);
        gridTotales.setPadding(new Insets(12, 16, 12, 16));
        gridTotales.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label lblTotTitle = new Label("SUMAS TOTALES:");
        lblTotTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #0f172a;");

        lblTotMovDebe = new Label("Mov. Debe: $0.00");
        lblTotMovDebe.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold;");

        lblTotMovHaber = new Label("Mov. Haber: $0.00");
        lblTotMovHaber.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold;");

        lblTotSalDeudor = new Label("Saldo Deudor: $0.00");
        lblTotSalDeudor.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #15803d;");

        lblTotSalAcreedor = new Label("Saldo Acreedor: $0.00");
        lblTotSalAcreedor.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #1e40af;");

        lblStatusCuadre = new Label("✔ BALANZA CUADRADA");
        lblStatusCuadre.getStyleClass().add("badge-cuadrado");

        gridTotales.add(lblTotTitle, 0, 0);
        gridTotales.add(lblTotMovDebe, 1, 0);
        gridTotales.add(lblTotMovHaber, 2, 0);
        gridTotales.add(lblTotSalDeudor, 3, 0);
        gridTotales.add(lblTotSalAcreedor, 4, 0);
        gridTotales.add(lblStatusCuadre, 5, 0);

        card.getChildren().addAll(topBar, tblBalanza, gridTotales);
        getChildren().add(card);

        cargarDatos();
    }

    public void cargarDatos() {
        balanzaActual = reportesService.generarBalanzaComprobacion();
        tblBalanza.setItems(FXCollections.observableArrayList(balanzaActual.getRenglones()));

        lblTotMovDebe.setText("Mov. Debe: " + MONEDA.format(balanzaActual.getTotalMovimientoDebe()));
        lblTotMovHaber.setText("Mov. Haber: " + MONEDA.format(balanzaActual.getTotalMovimientoHaber()));
        lblTotSalDeudor.setText("Saldo Deudor: " + MONEDA.format(balanzaActual.getTotalSaldoDeudor()));
        lblTotSalAcreedor.setText("Saldo Acreedor: " + MONEDA.format(balanzaActual.getTotalSaldoAcreedor()));

        lblStatusCuadre.getStyleClass().removeAll("badge-cuadrado", "badge-descuadrado");
        if (balanzaActual.isCuadrada()) {
            lblStatusCuadre.setText("✔ BALANZA CUADRADA EXACTAMENTE");
            lblStatusCuadre.getStyleClass().add("badge-cuadrado");
        } else {
            lblStatusCuadre.setText("⚠ DESCUADRE EN BALANZA");
            lblStatusCuadre.getStyleClass().add("badge-descuadrado");
        }
    }

    private void exportarCSV() {
        if (balanzaActual == null || balanzaActual.getRenglones().isEmpty()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Balanza de Comprobación");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV (*.csv)", "*.csv"));
        fc.setInitialFileName("Balanza_Comprobacion_" + LocalDate.now() + ".csv");
        File dest = fc.showSaveDialog(getScene().getWindow());
        if (dest != null) {
            try {
                ExportacionService.exportarBalanzaComprobacionCSV(balanzaActual, dest);
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Balanza exportada correctamente a: " + dest.getAbsolutePath());
                a.showAndWait();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error: " + ex.getMessage());
                a.showAndWait();
            }
        }
    }
}
