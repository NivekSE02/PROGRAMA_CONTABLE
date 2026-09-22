package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.dao.CuentaDAO;
import com.mycompany.programa_contable.dao.LibroDiarioDAO;
import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.Cuenta;
import com.mycompany.programa_contable.model.DetalleAsiento;
import com.mycompany.programa_contable.service.ExportacionService;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.util.converter.DoubleStringConverter;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

public class LibroDiarioView extends VBox {

    private final LibroDiarioDAO libroDiarioDAO = new LibroDiarioDAO();
    private final CuentaDAO cuentaDAO = new CuentaDAO();
    private final com.mycompany.programa_contable.model.ConfiguracionDAO configuracionDAO = new com.mycompany.programa_contable.model.ConfiguracionDAO();
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    // Formulario de Registro
    private DatePicker dpFecha;
    private TextField txtNumero;
    private TextField txtComentarioAsiento;
    private TableView<DetalleAsiento> tblDetalle;
    private ObservableList<DetalleAsiento> lineasAsiento;

    // Indicadores de Cuadre
    private Label lblTotalDebe;
    private Label lblTotalHaber;
    private Label lblDiferencia;
    private Label lblBadgeCuadre;
    private Button btnGuardar;

    // Componentes de la segunda pestaña (Historial)
    private TableView<Asiento> tblHistorial;
    private TableView<DetalleAsiento> tblDetalleHistorial;
    private ObservableList<Asiento> listaHistorial;

    public LibroDiarioView() {
        setPadding(new Insets(24, 32, 32, 32));
        setSpacing(0);
        setStyle("-fx-background-color: #f8fafc;");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        Tab tabNuevo = new Tab("Registrar Asiento", crearTabNuevoAsiento());
        Tab tabHistorial = new Tab("Historial del Libro Diario", crearTabHistorial());

        tabPane.getTabs().addAll(tabNuevo, tabHistorial);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        getChildren().add(tabPane);
        recargarHistorial();
    }

    private ScrollPane crearTabNuevoAsiento() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(28, 28, 24, 28));
        root.setStyle("-fx-background-color: #ffffff;");

        // Cabecera del formulario de registro
        HBox headerForm = new HBox(32);
        headerForm.setAlignment(Pos.CENTER_LEFT);

        VBox numBox = new VBox(6);
        Label lblNumTitle = new Label("N° de Asiento");
        lblNumTitle.getStyleClass().add("form-label");
        txtNumero = new TextField();
        txtNumero.setEditable(false);
        txtNumero.setPrefWidth(120);
        txtNumero.setStyle("-fx-font-weight: 700; -fx-background-color: #f8fafc; -fx-font-size: 15px;");
        actualizarNumeroAsiento();
        numBox.getChildren().addAll(lblNumTitle, txtNumero);

        VBox fechaBox = new VBox(6);
        Label lblFecTitle = new Label("Fecha del Asiento");
        lblFecTitle.getStyleClass().add("form-label");
        dpFecha = new DatePicker(LocalDate.now());
        dpFecha.setPrefWidth(200);
        fechaBox.getChildren().addAll(lblFecTitle, dpFecha);

        headerForm.getChildren().addAll(numBox, fechaBox);

        // Barra de acciones para renglones
        HBox barAcciones = new HBox(12);
        barAcciones.setAlignment(Pos.CENTER_LEFT);
        barAcciones.setPadding(new Insets(14, 16, 14, 16));
        barAcciones.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        List<Cuenta> cuentasPermitidas = cuentaDAO.listarPermitenMovimiento();
        ObservableList<Cuenta> itemsOriginales = FXCollections.observableArrayList(cuentasPermitidas);
        ComboBox<Cuenta> cbCuenta = new ComboBox<>(itemsOriginales);
        cbCuenta.setPromptText("Cuenta contable...");
        cbCuenta.setPrefWidth(420);
        cbCuenta.setMinHeight(38);
        
        // Habilitar búsqueda por texto
        cbCuenta.setEditable(true);
        cbCuenta.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            Cuenta selected = cbCuenta.getSelectionModel().getSelectedItem();
            if (selected != null && selected.toString().equals(cbCuenta.getEditor().getText())) {
                return;
            }
            if (newValue == null || newValue.isEmpty()) {
                cbCuenta.setItems(itemsOriginales);
            } else {
                String filter = newValue.toLowerCase();
                List<Cuenta> filtered = itemsOriginales.stream()
                        .filter(c -> c.toString().toLowerCase().contains(filter))
                        .toList();
                cbCuenta.setItems(FXCollections.observableArrayList(filtered));
                if (!filtered.isEmpty()) {
                    cbCuenta.show();
                }
            }
        });
        cbCuenta.setConverter(new javafx.util.StringConverter<Cuenta>() {
            @Override
            public String toString(Cuenta object) {
                return object == null ? "" : object.toString();
            }
            @Override
            public Cuenta fromString(String string) {
                return itemsOriginales.stream().filter(c -> c.toString().equals(string)).findFirst().orElse(null);
            }
        });

        TextField txtMontoDebe = new TextField("0.00");
        txtMontoDebe.setPromptText("Debe");
        txtMontoDebe.setPrefWidth(130);
        txtMontoDebe.setMinHeight(38);

        TextField txtMontoHaber = new TextField("0.00");
        txtMontoHaber.setPromptText("Haber");
        txtMontoHaber.setPrefWidth(130);
        txtMontoHaber.setMinHeight(38);

        Button btnAgregarLinea = new Button("Agregar Renglón");
        btnAgregarLinea.setMinSize(150, 38);
        btnAgregarLinea.getStyleClass().add("btn-primary");
        
        btnAgregarLinea.setOnAction(e -> {
            Cuenta sel = cbCuenta.getValue();
            if (sel == null) {
                mostrarAlerta(Alert.AlertType.WARNING, "Cuenta Requerida", "Debe seleccionar una cuenta contable.");
                return;
            }

            double debeVal = parseMonto(txtMontoDebe.getText());
            double haberVal = parseMonto(txtMontoHaber.getText());
            String codigoCuenta = sel.getCodigo();
            // Se consulta al crear el renglón: los asientos ya almacenados nunca se recalculan.
            double tasaIva = configuracionDAO.obtenerTasaIva();
            boolean ivaIncluido = com.mycompany.programa_contable.model.ConfiguracionDAO.IVA_INCLUIDO
                    .equals(configuracionDAO.obtenerModalidadIva());

            if (debeVal == 0 && haberVal == 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Monto Inválido", "Debe ingresar un valor en el Debe o en el Haber.");
                return;
            }
            if (debeVal > 0 && haberVal > 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Monto Inválido", "Un renglón no puede tener valor al Debe y al Haber simultáneamente.");
                return;
            }

            // 1. AUTOMATIZACIÓN INTELIGENTE DE IVA EN COMPRAS Y ACTIVOS FIJOS (Al Debe)
            boolean esCompraOActivoConIva = ("5.4".equals(codigoCuenta) || codigoCuenta.startsWith("5.4") || 
                                             codigoCuenta.startsWith("1.5") || codigoCuenta.startsWith("1.6") || 
                                             codigoCuenta.startsWith("1.7") );

            if (esCompraOActivoConIva && debeVal > 0) {
                double valorNeto = ivaIncluido ? redondear(debeVal / (1.0 + tasaIva)) : debeVal;
                double ivaCredito = ivaIncluido
                        ? redondear(debeVal - valorNeto)
                        : redondear(valorNeto * tasaIva);

                agregarLineaContable(sel, valorNeto, 0.0);
                agregarLineaContable(cuentaDAO.buscarPorCodigo("1.4"), ivaCredito, 0.0);

                finalizarAgregarLinea(cbCuenta, txtMontoDebe, txtMontoHaber);
                return;
            }

            // 2. AUTOMATIZACIÓN INTELIGENTE DE IVA EN VENTAS (Al Haber)
            if ("4.1".equals(codigoCuenta) && haberVal > 0) {
                double valorNetoVenta = ivaIncluido ? redondear(haberVal / (1.0 + tasaIva)) : haberVal;
                double ivaDebito = ivaIncluido
                        ? redondear(haberVal - valorNetoVenta)
                        : redondear(valorNetoVenta * tasaIva);

                agregarLineaContable(sel, 0.0, valorNetoVenta);
                agregarLineaContable(cuentaDAO.buscarPorCodigo("2.3"), 0.0, ivaDebito);

                finalizarAgregarLinea(cbCuenta, txtMontoDebe, txtMontoHaber);
                return; 
            }

            // 3. AUTOMATIZACIÓN INTELIGENTE DE IVA EN GASTOS FINANCIEROS / COMISIONES (6.1)
            boolean esGastoFinancieroConIva = ("6.1".equals(codigoCuenta) || codigoCuenta.startsWith("6.1"));
            if (esGastoFinancieroConIva && debeVal > 0) {
                double valorComisionNeto = ivaIncluido ? redondear(debeVal / (1.0 + tasaIva)) : debeVal;
                double ivaComision = ivaIncluido
                        ? redondear(debeVal - valorComisionNeto)
                        : redondear(valorComisionNeto * tasaIva);

                agregarLineaContable(sel, valorComisionNeto, 0.0);
                agregarLineaContable(cuentaDAO.buscarPorCodigo("1.4"), ivaComision, 0.0);

                finalizarAgregarLinea(cbCuenta, txtMontoDebe, txtMontoHaber);
                return; 
            }

            // Inserción Manual Normal
            agregarLineaContable(sel, debeVal, haberVal);
            finalizarAgregarLinea(cbCuenta, txtMontoDebe, txtMontoHaber);
        });

        Button btnEliminarLinea = new Button("Quitar Renglón");
        btnEliminarLinea.setMinSize(140, 38);
        btnEliminarLinea.getStyleClass().add("btn-secondary");
        btnEliminarLinea.setOnAction(e -> {
            DetalleAsiento sel = tblDetalle.getSelectionModel().getSelectedItem();
            if (sel != null) {
                lineasAsiento.remove(sel);
                // Reindexar renglones
                for (int i = 0; i < lineasAsiento.size(); i++) {
                    lineasAsiento.get(i).setRenglon(i + 1);
                }
                tblDetalle.refresh();
                actualizarCuadre();
            }
        });

        barAcciones.getChildren().addAll(
            new Label("Cuenta:"), cbCuenta,
            new Label("  Debe ($):"), txtMontoDebe,
            new Label("  Haber ($):"), txtMontoHaber,
            btnAgregarLinea, btnEliminarLinea
        );

        // Tabla de Detalle del Asiento
        tblDetalle = new TableView<>();
        lineasAsiento = FXCollections.observableArrayList();
        tblDetalle.setItems(lineasAsiento);
        tblDetalle.setPrefHeight(250);
        tblDetalle.setMinHeight(200);

        TableColumn<DetalleAsiento, Number> colRenglon = new TableColumn<>("#");
        colRenglon.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getRenglon()));
        colRenglon.setPrefWidth(50);

        TableColumn<DetalleAsiento, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCuentaCodigo()));
        colCod.setPrefWidth(120);

        TableColumn<DetalleAsiento, String> colNom = new TableColumn<>("Nombre de la Cuenta");
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCuentaNombre()));
        colNom.setPrefWidth(350);

        TableColumn<DetalleAsiento, String> colParcial = new TableColumn<>("Parcial ($)");
        colParcial.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConceptoLinea()));
        colParcial.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colParcial.setPrefWidth(150);

        TableColumn<DetalleAsiento, String> colDebe = new TableColumn<>("Debe ($)");
        colDebe.setCellValueFactory(c -> {
            double debe = c.getValue().getDebe();
            return new SimpleStringProperty(debe > 0 ? MONEDA.format(debe) : "");
        });
        colDebe.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colDebe.setPrefWidth(150);

        TableColumn<DetalleAsiento, String> colHaber = new TableColumn<>("Haber ($)");
        colHaber.setCellValueFactory(c -> {
            double haber = c.getValue().getHaber();
            return new SimpleStringProperty(haber > 0 ? MONEDA.format(haber) : "");
        });
        colHaber.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colHaber.setPrefWidth(150);

        tblDetalle.getColumns().addAll(colRenglon, colCod, colNom, colParcial, colDebe, colHaber);

        // Area de Comentario
        VBox boxComentario = new VBox(6);
        boxComentario.setPadding(new Insets(4, 0, 0, 0));
        Label lblComentario = new Label("Comentario del Asiento (Opcional)");
        lblComentario.getStyleClass().add("form-label");
        txtComentarioAsiento = new TextField();
        txtComentarioAsiento.setPromptText("Ej. Pago de factura a proveedores con transferencia bancaria...");
        txtComentarioAsiento.setMinHeight(40);
        boxComentario.getChildren().addAll(lblComentario, txtComentarioAsiento);

        // Panel de Cuadre de Partida Doble
        HBox panelCuadre = new HBox(20);
        panelCuadre.setAlignment(Pos.CENTER_LEFT);
        panelCuadre.setPadding(new Insets(16, 20, 16, 20));
        panelCuadre.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        lblTotalDebe = new Label("Debe: $0.00");
        lblTotalDebe.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: #0f172a;");

        lblTotalHaber = new Label("Haber: $0.00");
        lblTotalHaber.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: #0f172a;");

        lblDiferencia = new Label("Diferencia: $0.00");
        lblDiferencia.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: #881337;");

        lblBadgeCuadre = new Label("INGRESE PARTIDAS");
        lblBadgeCuadre.setStyle("-fx-font-size: 13px; -fx-padding: 6 14; -fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnGuardar = new Button("Guardar Asiento");
        btnGuardar.setMinSize(200, 40);
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setDisable(true);
        btnGuardar.setOnAction(e -> guardarAsiento());

        Button btnLimpiar = new Button("Limpiar Formulario");
        btnLimpiar.setMinSize(160, 40);
        btnLimpiar.getStyleClass().add("btn-secondary");
        btnLimpiar.setOnAction(e -> limpiarFormulario());

        panelCuadre.getChildren().addAll(lblTotalDebe, lblTotalHaber, lblDiferencia, lblBadgeCuadre, spacer, btnLimpiar, btnGuardar);

        root.getChildren().addAll(headerForm, new Separator(), barAcciones, tblDetalle, boxComentario, panelCuadre);
        actualizarCuadre();
        
        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        return sp;
    }

    private VBox crearTabHistorial() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(24, 28, 24, 28));
        root.setStyle("-fx-background-color: #ffffff;");
        VBox.setVgrow(root, Priority.ALWAYS);

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label lblHist = new Label("Historial del Libro Diario");
        lblHist.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label lblHistSub = new Label("Registro cronológico de transacciones del período");
        lblHistSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblHist, lblHistSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        MenuButton btnExportar = new MenuButton("Exportar");
        btnExportar.setMinSize(160, 36);
        btnExportar.getStyleClass().add("btn-secondary");
        btnExportar.getStyleClass().add("export-button");
        btnExportar.setStyle("-fx-text-fill: white;");
        MenuItem mnuCsv = new MenuItem("Exportar a CSV");
        mnuCsv.getStyleClass().add("export-menu-item");
        mnuCsv.setOnAction(e -> exportarHistorialCSV());
        MenuItem mnuExcel = new MenuItem("Exportar a Excel (.xlsx)");
        mnuExcel.getStyleClass().add("export-menu-item");
        mnuExcel.setOnAction(e -> exportarHistorialExcel());
        btnExportar.getItems().addAll(mnuCsv, mnuExcel);

        Button btnRefrescar = new Button("Refrescar");
        btnRefrescar.setMinSize(110, 36);
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> recargarHistorial());

        Button btnEliminar = new Button("Eliminar Seleccionado");
        btnEliminar.setMinSize(180, 36);
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setOnAction(e -> {
            Asiento sel = tblHistorial.getSelectionModel().getSelectedItem();
            if (sel == null) {
                mostrarAlerta(Alert.AlertType.WARNING, "Selección Requerida", "Seleccione un asiento del historial para eliminar.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea eliminar el Asiento N° " + sel.getNumero() + "? Esta acción actualizará la mayorización en tiempo real.", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirmar Eliminación");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.YES) {
                    libroDiarioDAO.eliminarAsiento(sel.getId());
                    recargarHistorial();
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Asiento Eliminado", "El asiento fue eliminado y los saldos se recalcularon.");
                }
            });
        });

        topBar.getChildren().addAll(titleBox, btnExportar, btnRefrescar, btnEliminar);

        // Tabla de Asientos
        tblHistorial = new TableView<>();
        listaHistorial = FXCollections.observableArrayList();
        tblHistorial.setItems(listaHistorial);
        tblHistorial.setPrefHeight(240);
        VBox.setVgrow(tblHistorial, Priority.ALWAYS);

        TableColumn<Asiento, Number> colNum = new TableColumn<>("N° Asiento");
        colNum.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getNumero()));
        colNum.setPrefWidth(100);

        TableColumn<Asiento, String> colFec = new TableColumn<>("Fecha");
        colFec.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha()));
        colFec.setPrefWidth(120);

        TableColumn<Asiento, String> colCon = new TableColumn<>("Comentario");
        colCon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConcepto()));
        colCon.setPrefWidth(450);

        TableColumn<Asiento, String> colDeb = new TableColumn<>("Total Debe");
        colDeb.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getTotalDebe())));
        colDeb.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colDeb.setPrefWidth(140);

        TableColumn<Asiento, String> colHab = new TableColumn<>("Total Haber");
        colHab.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getTotalHaber())));
        colHab.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colHab.setPrefWidth(140);

        tblHistorial.getColumns().addAll(colNum, colFec, colCon, colDeb, colHab);

        Label lblDet = new Label("Detalle del Asiento Seleccionado");
        lblDet.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #475569; -fx-padding: 8 0 4 0;");

        tblDetalleHistorial = new TableView<>();
        VBox.setVgrow(tblDetalleHistorial, Priority.ALWAYS);

        TableColumn<DetalleAsiento, Number> dColReng = new TableColumn<>("Renglón");
        dColReng.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getRenglon()));
        dColReng.setPrefWidth(70);

        TableColumn<DetalleAsiento, String> dColCod = new TableColumn<>("Código");
        dColCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCuentaCodigo()));
        dColCod.setPrefWidth(120);

        TableColumn<DetalleAsiento, String> dColNom = new TableColumn<>("Cuenta");
        dColNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCuentaNombre()));
        dColNom.setPrefWidth(320);

        TableColumn<DetalleAsiento, String> dColParcial = new TableColumn<>("Parcial ($)");
        dColParcial.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConceptoLinea()));
        dColParcial.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        dColParcial.setPrefWidth(140);

        TableColumn<DetalleAsiento, String> dColDeb = new TableColumn<>("Debe ($)");
        dColDeb.setCellValueFactory(c -> {
            double debe = c.getValue().getDebe();
            return new SimpleStringProperty(debe > 0 ? MONEDA.format(debe) : "");
        });
        dColDeb.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        dColDeb.setPrefWidth(140);

        TableColumn<DetalleAsiento, String> dColHab = new TableColumn<>("Haber ($)");
        dColHab.setCellValueFactory(c -> {
            double haber = c.getValue().getHaber();
            return new SimpleStringProperty(haber > 0 ? MONEDA.format(haber) : "");
        });
        dColHab.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        dColHab.setPrefWidth(140);

        tblDetalleHistorial.getColumns().addAll(dColReng, dColCod, dColNom, dColParcial, dColDeb, dColHab);

        tblHistorial.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                tblDetalleHistorial.setItems(FXCollections.observableArrayList(newVal.getDetalles()));
            } else {
                tblDetalleHistorial.getItems().clear();
            }
        });

        root.getChildren().addAll(topBar, tblHistorial, lblDet, tblDetalleHistorial);
        return root;
    }

    private void actualizarCuadre() {
        double d = 0.0;
        double h = 0.0;
        for (DetalleAsiento det : lineasAsiento) {
            d += det.getDebe();
            h += det.getHaber();
        }
        d = redondear(d);
        h = redondear(h);
        double diff = redondear(Math.abs(d - h));

        lblTotalDebe.setText("Total Debe: " + MONEDA.format(d));
        lblTotalHaber.setText("Total Haber: " + MONEDA.format(h));
        lblDiferencia.setText("Diferencia: " + MONEDA.format(diff));

        lblBadgeCuadre.getStyleClass().removeAll("badge-cuadrado", "badge-descuadrado");

        boolean esValido = (diff < 0.005) && (d > 0) && (lineasAsiento.size() >= 2);

        if (lineasAsiento.isEmpty()) {
            lblBadgeCuadre.setText("INGRESE PARTIDAS");
            lblBadgeCuadre.setStyle("-fx-font-size: 13px; -fx-padding: 6 14; -fx-background-color: #f1f5f9; -fx-border-color: #e2e8f0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #64748b;");
            btnGuardar.setDisable(true);
        } else if (esValido) {
            lblBadgeCuadre.setText("PARTIDA DOBLE CUADRADA");
            lblBadgeCuadre.setStyle("-fx-font-size: 13px; -fx-padding: 6 14; -fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #15803d; -fx-font-weight: 700;");
            btnGuardar.setDisable(false);
            btnGuardar.setTooltip(new Tooltip("El asiento cumple la Partida Doble y puede guardarse."));
        } else {
            lblBadgeCuadre.setText("DESCUADRADO: " + MONEDA.format(diff));
            lblBadgeCuadre.setStyle("-fx-font-size: 13px; -fx-padding: 6 14; -fx-background-color: #fff1f2; -fx-border-color: #fecaca; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-text-fill: #881337; -fx-font-weight: 700;");
            btnGuardar.setDisable(true);
            btnGuardar.setTooltip(new Tooltip("Bloqueado: La suma del Debe debe ser exactamente igual a la suma del Haber."));
        }
    }

    private void guardarAsiento() {
        String concepto = txtComentarioAsiento.getText().trim();
        if (concepto.isEmpty()) {
            concepto = "Sin comentario";
        }

        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha Requerida", "Debe seleccionar una fecha para el asiento.");
            return;
        }

        int numero = Integer.parseInt(txtNumero.getText().trim());
        Asiento asiento = new Asiento(numero, fecha.toString(), concepto);
        for (DetalleAsiento det : lineasAsiento) {
            asiento.agregarDetalle(det);
        }

        // Validación estricta antes de invocar persistencia
        if (!asiento.isPartidaDobleValida()) {
            mostrarAlerta(Alert.AlertType.ERROR, "Validación Obligatoria",
                "El sistema bloquea el guardado debido a que no cumple la Partida Doble.");
            return;
        }

        try {
            boolean exito = libroDiarioDAO.registrarAsiento(asiento);
            if (exito) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Asiento Registrado",
                    "Asiento N° " + numero + " registrado exitosamente en el Libro Diario.\nLa mayorización ha sido actualizada en tiempo real.");
                limpiarFormulario();
                recargarHistorial();
            } else {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo guardar el asiento contable.");
            }
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Guardado", ex.getMessage());
        }
    }

    private void limpiarFormulario() {
        txtComentarioAsiento.clear();
        lineasAsiento.clear();
        dpFecha.setValue(LocalDate.now());
        actualizarNumeroAsiento();
        actualizarCuadre();
    }

    private void actualizarNumeroAsiento() {
        txtNumero.setText(String.valueOf(libroDiarioDAO.obtenerSiguienteNumeroAsiento()));
    }

    public void recargarHistorial() {
        List<Asiento> lista = libroDiarioDAO.listarAsientos("", "");
        listaHistorial.setAll(lista);
        actualizarNumeroAsiento();
    }

    private void exportarHistorialCSV() {
        if (listaHistorial.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin Datos", "No hay asientos registrados para exportar.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar Libro Diario a CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV (*.csv)", "*.csv"));
        fc.setInitialFileName("Libro_Diario_" + LocalDate.now() + ".csv");
        File file = fc.showSaveDialog(getScene().getWindow());
        if (file != null) {
            try {
                ExportacionService.exportarLibroDiarioCSV(listaHistorial, file);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación Exitosa", "Libro Diario exportado a: " + file.getAbsolutePath());
            } catch (Exception ex) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error al Exportar", ex.getMessage());
            }
        }
    }
    
    private void exportarHistorialExcel() {
        if (listaHistorial.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin Datos", "No hay asientos registrados para exportar.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar Reporte en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("Libro_Diario_" + LocalDate.now() + ".xlsx");
        File dest = fc.showSaveDialog(getScene().getWindow());
        if (dest != null) {
            try {
                ExportacionService.exportarLibroDiarioExcel(listaHistorial, dest);
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Libro Diario exportado a Excel correctamente.");
                a.showAndWait();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error al exportar a Excel: " + ex.getMessage());
                a.showAndWait();
            }
        }
    }

    private double parseMonto(String val) {
        if (val == null || val.trim().isEmpty()) return 0.0;
        try {
            return Math.max(0.0, Double.parseDouble(val.trim().replace("$", "").replace(",", "")));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double redondear(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert a = new Alert(tipo);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.setContentText(mensaje);
        a.showAndWait();
    }

    private void agregarLineaContable(Cuenta cuentaDada, double debeVal, double haberVal) {
        if (cuentaDada == null) return;
        
        Cuenta cuentaPrincipal = cuentaDada;
        Cuenta subcuenta = null;

        // Buscar ancestro de Nivel 2 (Cuenta de Mayor) si es de detalle
        if (cuentaDada.getNivel() > 2) {
            subcuenta = cuentaDada;
            String padreCod = cuentaDada.getCuentaPadre();
            while (padreCod != null) {
                Cuenta p = cuentaDAO.buscarPorCodigo(padreCod);
                if (p != null) {
                    if (p.getNivel() == 2) {
                        cuentaPrincipal = p;
                        break;
                    }
                    padreCod = p.getCuentaPadre();
                } else {
                    break;
                }
            }
        }

        // Agregar o actualizar Cuenta Principal (Nivel 2) con DEBE / HABER
        DetalleAsiento rowPrincipal = null;
        for (DetalleAsiento d : lineasAsiento) {
            if (d.getCuentaCodigo().equals(cuentaPrincipal.getCodigo())) {
                rowPrincipal = d;
                break;
            }
        }

        if (rowPrincipal == null) {
            rowPrincipal = new DetalleAsiento(
                lineasAsiento.size() + 1,
                cuentaPrincipal.getCodigo(),
                cuentaPrincipal.getNombre(),
                "",
                debeVal,
                haberVal
            );
            lineasAsiento.add(rowPrincipal);
        } else {
            rowPrincipal.setDebe(redondear(rowPrincipal.getDebe() + debeVal));
            rowPrincipal.setHaber(redondear(rowPrincipal.getHaber() + haberVal));
        }

        // Agregar Subcuenta (Nivel 3+) con PARCIAL
        if (subcuenta != null) {
            double montoParcial = Math.max(debeVal, haberVal);
            DetalleAsiento rowSub = new DetalleAsiento(
                lineasAsiento.size() + 1,
                subcuenta.getCodigo(),
                subcuenta.getNombre(),
                MONEDA.format(montoParcial),
                0.0,
                0.0
            );
            lineasAsiento.add(rowSub);
        }
    }

    private void finalizarAgregarLinea(ComboBox<Cuenta> cbCuenta, TextField txtMontoDebe, TextField txtMontoHaber) {
        actualizarCuadre();
        cbCuenta.setValue(null);
        txtMontoDebe.setText("0.00");
        txtMontoHaber.setText("0.00");
        cbCuenta.requestFocus();
    }
}
