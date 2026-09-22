package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.model.MayorCuenta;
import com.mycompany.programa_contable.model.MovimientoMayor;
import com.mycompany.programa_contable.service.MayorizacionService;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.DecimalFormat;
import java.util.List;

public class LibroMayorView extends VBox {

    private final MayorizacionService mayorizacionService = new MayorizacionService();
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    private TableView<MayorCuenta> tblConsolidado;
    private TableView<MovimientoMayor> tblKardex;
    private ObservableList<MayorCuenta> listaCuentas;
    private FlowPane flowCuentasT;
    private Label lblDetalleCuenta;

    public LibroMayorView() {
        setPadding(new Insets(24, 32, 32, 32));
        setSpacing(0);
        setStyle("-fx-background-color: #f8fafc;");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        Tab tabConsolidado = new Tab("Consolidación en Tiempo Real", crearTabConsolidado());
        Tab tabCuentasT = new Tab("Esquemas de Mayor (Cuentas T)", crearTabCuentasT());

        tabPane.getTabs().addAll(tabConsolidado, tabCuentasT);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        getChildren().add(tabPane);
        recargarMayorizacion();
    }

    private VBox crearTabConsolidado() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24, 28, 24, 28));
        root.setStyle("-fx-background-color: #ffffff;");
        VBox.setVgrow(root, Priority.ALWAYS);

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(2);
        Label lblTitle = new Label("Consolidación de Saldos");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label lblTitleSub = new Label("Débitos, créditos y saldos calculados automáticamente en tiempo real");
        lblTitleSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBlock.getChildren().addAll(lblTitle, lblTitleSub);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        Button btnRefrescar = new Button("Actualizar");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> recargarMayorizacion());

        topBar.getChildren().addAll(titleBlock, btnRefrescar);

        // Tabla Consolidada de Cuentas Mayorizadas
        tblConsolidado = new TableView<>();
        listaCuentas = FXCollections.observableArrayList();
        tblConsolidado.setItems(listaCuentas);
        tblConsolidado.setPrefHeight(280);

        TableColumn<MayorCuenta, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCod.setPrefWidth(90);

        TableColumn<MayorCuenta, String> colNom = new TableColumn<>("Cuenta");
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colNom.setPrefWidth(260);

        TableColumn<MayorCuenta, String> colTipo = new TableColumn<>("Clasificación");
        colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo().getNombre()));
        colTipo.setPrefWidth(140);

        TableColumn<MayorCuenta, String> colNat = new TableColumn<>("Naturaleza");
        colNat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNaturaleza().getEtiqueta()));
        colNat.setPrefWidth(110);

        TableColumn<MayorCuenta, String> colDeb = new TableColumn<>("Movimiento Debe");
        colDeb.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getTotalDebe())));
        colDeb.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colDeb.setPrefWidth(130);

        TableColumn<MayorCuenta, String> colHab = new TableColumn<>("Movimiento Haber");
        colHab.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getTotalHaber())));
        colHab.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colHab.setPrefWidth(130);

        TableColumn<MayorCuenta, String> colSalDeb = new TableColumn<>("Saldo Deudor");
        colSalDeb.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSaldoDeudor() > 0 ? MONEDA.format(c.getValue().getSaldoDeudor()) : "$0.00"));
        colSalDeb.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #15803d;");
        colSalDeb.setPrefWidth(130);

        TableColumn<MayorCuenta, String> colSalHab = new TableColumn<>("Saldo Acreedor");
        colSalHab.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSaldoAcreedor() > 0 ? MONEDA.format(c.getValue().getSaldoAcreedor()) : "$0.00"));
        colSalHab.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace; -fx-font-weight: bold; -fx-text-fill: #1e40af;");
        colSalHab.setPrefWidth(130);

        tblConsolidado.getColumns().addAll(colCod, colNom, colTipo, colNat, colDeb, colHab, colSalDeb, colSalHab);

        // Detalle de movimientos de la cuenta seleccionada
        lblDetalleCuenta = new Label("Seleccione una cuenta arriba para ver su historial de movimientos cronológicos:");
        lblDetalleCuenta.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #475569;");

        tblKardex = new TableView<>();
        VBox.setVgrow(tblKardex, Priority.ALWAYS);

        TableColumn<MovimientoMayor, Number> kColAsi = new TableColumn<>("N° Asiento");
        kColAsi.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getAsientoNumero()));
        kColAsi.setPrefWidth(90);

        TableColumn<MovimientoMayor, String> kColFec = new TableColumn<>("Fecha");
        kColFec.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha()));
        kColFec.setPrefWidth(100);

        TableColumn<MovimientoMayor, String> kColCon = new TableColumn<>("Concepto");
        kColCon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConcepto()));
        kColCon.setPrefWidth(420);

        TableColumn<MovimientoMayor, String> kColDeb = new TableColumn<>("Debe ($)");
        kColDeb.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDebe() > 0 ? MONEDA.format(c.getValue().getDebe()) : "-"));
        kColDeb.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        kColDeb.setPrefWidth(120);

        TableColumn<MovimientoMayor, String> kColHab = new TableColumn<>("Haber ($)");
        kColHab.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHaber() > 0 ? MONEDA.format(c.getValue().getHaber()) : "-"));
        kColHab.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        kColHab.setPrefWidth(120);

        TableColumn<MovimientoMayor, String> kColSal = new TableColumn<>("Saldo Parcial ($)");
        kColSal.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getSaldoAcumulado())));
        kColSal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace; -fx-font-weight: bold;");
        kColSal.setPrefWidth(130);

        tblKardex.getColumns().addAll(kColAsi, kColFec, kColCon, kColDeb, kColHab, kColSal);

        tblConsolidado.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                lblDetalleCuenta.setText("Movimientos de la cuenta: " + newVal.getCodigo() + " - " + newVal.getNombre() + " (" + newVal.getNaturaleza().getEtiqueta() + ")");
                tblKardex.setItems(FXCollections.observableArrayList(newVal.getMovimientos()));
            } else {
                lblDetalleCuenta.setText("Seleccione una cuenta arriba para ver su historial de movimientos:");
                tblKardex.getItems().clear();
            }
        });

        root.getChildren().addAll(topBar, tblConsolidado, lblDetalleCuenta, tblKardex);
        return root;
    }

    private ScrollPane crearTabCuentasT() {
        flowCuentasT = new FlowPane();
        flowCuentasT.setHgap(20);
        flowCuentasT.setVgap(20);
        flowCuentasT.setPadding(new Insets(16));
        flowCuentasT.setStyle("-fx-background-color: #f8fafc;");

        ScrollPane sp = new ScrollPane(flowCuentasT);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent;");
        return sp;
    }

    public void recargarMayorizacion() {
        List<MayorCuenta> mayores = mayorizacionService.obtenerMayorizacionCompleta();
        listaCuentas.setAll(mayores);
        if (!mayores.isEmpty()) {
            tblConsolidado.getSelectionModel().select(0);
        }

        // Generar las Cuentas T visuales en el FlowPane (usando el calculo consolidado exclusivo para T)
        flowCuentasT.getChildren().clear();
        List<MayorCuenta> cuentasT = mayorizacionService.obtenerMayorizacionParaCuentasT();
        for (MayorCuenta m : cuentasT) {
            flowCuentasT.getChildren().add(crearWidgetCuentaT(m));
        }
    }

    /**
     * Construye gráficamente la representación visual de la "Cuenta T"
     */
    private VBox crearWidgetCuentaT(MayorCuenta m) {
        VBox card = new VBox(0);
        card.getStyleClass().add("t-account-card");

        // Cabecera de la Cuenta T
        HBox header = new HBox();
        header.getStyleClass().add("t-account-header");
        Label lblHeader = new Label(m.getCodigo() + " " + m.getNombre());
        lblHeader.getStyleClass().add("t-account-title");
        header.getChildren().add(lblHeader);

        // Columnas DEBE y HABER con línea central divisoria
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(8));

        Label lblDebe = new Label("DEBE (Débitos)");
        lblDebe.getStyleClass().add("t-column-header");
        lblDebe.setMaxWidth(Double.MAX_VALUE);
        lblDebe.setAlignment(Pos.CENTER);

        Label lblHaber = new Label("HABER (Créditos)");
        lblHaber.getStyleClass().add("t-column-header");
        lblHaber.setMaxWidth(Double.MAX_VALUE);
        lblHaber.setAlignment(Pos.CENTER);

        grid.add(lblDebe, 0, 0);
        grid.add(lblHaber, 2, 0);

        // Separador vertical central de la T
        Separator vSep = new Separator(javafx.geometry.Orientation.VERTICAL);
        grid.add(vSep, 1, 0, 1, 20);

        // Filas de movimientos
        int rowDebe = 1;
        int rowHaber = 1;

        for (MovimientoMayor mov : m.getMovimientos()) {
            if (mov.getDebe() > 0) {
                Label lblVal = new Label("As." + mov.getAsientoNumero() + ":  " + MONEDA.format(mov.getDebe()));
                lblVal.getStyleClass().add("t-cell");
                grid.add(lblVal, 0, rowDebe++);
            }
            if (mov.getHaber() > 0) {
                Label lblVal = new Label("As." + mov.getAsientoNumero() + ":  " + MONEDA.format(mov.getHaber()));
                lblVal.getStyleClass().add("t-cell");
                grid.add(lblVal, 2, rowHaber++);
            }
        }

        int maxRow = Math.max(rowDebe, rowHaber);

        // Línea horizontal de sumas de la T
        Separator hSep1 = new Separator(javafx.geometry.Orientation.HORIZONTAL);
        Separator hSep2 = new Separator(javafx.geometry.Orientation.HORIZONTAL);
        grid.add(hSep1, 0, maxRow);
        grid.add(hSep2, 2, maxRow);

        // Totales de movimientos
        Label lblTotDebe = new Label(MONEDA.format(m.getTotalDebe()));
        lblTotDebe.setStyle("-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace; -fx-alignment: CENTER-RIGHT; -fx-padding: 4px;");
        lblTotDebe.setMaxWidth(Double.MAX_VALUE);

        Label lblTotHaber = new Label(MONEDA.format(m.getTotalHaber()));
        lblTotHaber.setStyle("-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace; -fx-alignment: CENTER-RIGHT; -fx-padding: 4px;");
        lblTotHaber.setMaxWidth(Double.MAX_VALUE);

        grid.add(lblTotDebe, 0, maxRow + 1);
        grid.add(lblTotHaber, 2, maxRow + 1);

        // Saldo final doble subrayado
        Separator hSepFinal1 = new Separator(javafx.geometry.Orientation.HORIZONTAL);
        Separator hSepFinal2 = new Separator(javafx.geometry.Orientation.HORIZONTAL);
        grid.add(hSepFinal1, 0, maxRow + 2);
        grid.add(hSepFinal2, 2, maxRow + 2);

        if (m.getSaldoDeudor() > 0) {
            Label lblSaldo = new Label("SD: " + MONEDA.format(m.getSaldoDeudor()));
            lblSaldo.setStyle("-fx-font-weight: bold; -fx-text-fill: #15803d; -fx-font-family: 'Consolas', monospace; -fx-padding: 4px;");
            grid.add(lblSaldo, 0, maxRow + 3);
        } else if (m.getSaldoAcreedor() > 0) {
            Label lblSaldo = new Label("SA: " + MONEDA.format(m.getSaldoAcreedor()));
            lblSaldo.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e40af; -fx-font-family: 'Consolas', monospace; -fx-padding: 4px; -fx-alignment: CENTER-RIGHT;");
            lblSaldo.setMaxWidth(Double.MAX_VALUE);
            grid.add(lblSaldo, 2, maxRow + 3);
        }

        ColumnConstraints col1 = new ColumnConstraints(140);
        ColumnConstraints colSep = new ColumnConstraints(10);
        ColumnConstraints col2 = new ColumnConstraints(140);
        grid.getColumnConstraints().addAll(col1, colSep, col2);

        card.getChildren().addAll(header, grid);
        return card;
    }
}
