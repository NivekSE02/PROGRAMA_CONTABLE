package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.dao.CuentaDAO;
import com.mycompany.programa_contable.dao.LibroDiarioDAO;
import com.mycompany.programa_contable.model.Asiento;
import com.mycompany.programa_contable.model.Cuenta;
import com.mycompany.programa_contable.model.DetalleAsiento;
import com.mycompany.programa_contable.service.ExportacionService;
import com.mycompany.programa_contable.service.SessionManager;
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
    private static final DecimalFormat MONEDA = new DecimalFormat("$#,##0.00");

    // Formulario de Registro
    private DatePicker dpFecha;
    private TextField txtNumero;
    private TextField txtConcepto;
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
        setPadding(new Insets(20));
        setSpacing(16);
        setStyle("-fx-background-color: transparent;");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabNuevo = new Tab("📝 Registrar Nuevo Asiento", crearTabNuevoAsiento());
        Tab tabHistorial = new Tab("📚 Historial del Libro Diario", crearTabHistorial());

        tabPane.getTabs().addAll(tabNuevo, tabHistorial);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        getChildren().add(tabPane);
        recargarHistorial();
    }

    private VBox crearTabNuevoAsiento() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");

        // Cabecera del formulario
        GridPane gridHeader = new GridPane();
        gridHeader.setHgap(16);
        gridHeader.setVgap(8);

        Label lblNumTitle = new Label("N° de Asiento:");
        lblNumTitle.getStyleClass().add("form-label");
        txtNumero = new TextField();
        txtNumero.setEditable(false);
        txtNumero.setPrefWidth(90);
        txtNumero.setStyle("-fx-font-weight: bold; -fx-background-color: #f1f5f9;");
        actualizarNumeroAsiento();

        Label lblFecTitle = new Label("Fecha del Asiento:");
        lblFecTitle.getStyleClass().add("form-label");
        dpFecha = new DatePicker(LocalDate.now());
        dpFecha.setPrefWidth(160);

        Label lblConTitle = new Label("Concepto General / Glosa de la Transacción:");
        lblConTitle.getStyleClass().add("form-label");
        txtConcepto = new TextField();
        txtConcepto.setPromptText("Ej. Pago de factura a proveedores con transferencia bancaria...");
        GridPane.setHgrow(txtConcepto, Priority.ALWAYS);

        gridHeader.add(lblNumTitle, 0, 0);
        gridHeader.add(txtNumero, 0, 1);
        gridHeader.add(lblFecTitle, 1, 0);
        gridHeader.add(dpFecha, 1, 1);
        gridHeader.add(lblConTitle, 2, 0);
        gridHeader.add(txtConcepto, 2, 1);

        // Barra de acciones para renglones
        HBox barAcciones = new HBox(10);
        barAcciones.setAlignment(Pos.CENTER_LEFT);

        List<Cuenta> cuentasPermitidas = cuentaDAO.listarPermitenMovimiento();
        ComboBox<Cuenta> cbCuenta = new ComboBox<>(FXCollections.observableArrayList(cuentasPermitidas));
        cbCuenta.setPromptText("Seleccione una cuenta del catálogo...");
        cbCuenta.setPrefWidth(380);

        TextField txtConceptoLinea = new TextField();
        txtConceptoLinea.setPromptText("Detalle opcional del renglón");
        txtConceptoLinea.setPrefWidth(220);

        TextField txtMontoDebe = new TextField("0.00");
        txtMontoDebe.setPromptText("Debe");
        txtMontoDebe.setPrefWidth(90);

        TextField txtMontoHaber = new TextField("0.00");
        txtMontoHaber.setPromptText("Haber");
        txtMontoHaber.setPrefWidth(90);

        Button btnAgregarLinea = new Button("➕ Agregar Renglón");
        btnAgregarLinea.getStyleClass().add("btn-primary");
        btnAgregarLinea.setOnAction(e -> {
            Cuenta sel = cbCuenta.getValue();
            if (sel == null) {
                mostrarAlerta(Alert.AlertType.WARNING, "Cuenta Requerida", "Debe seleccionar una cuenta contable.");
                return;
            }

            double debeVal = parseMonto(txtMontoDebe.getText());
            double haberVal = parseMonto(txtMontoHaber.getText());

            if (debeVal == 0 && haberVal == 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Monto Inválido", "Debe ingresar un valor en el Debe o en el Haber.");
                return;
            }
            if (debeVal > 0 && haberVal > 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Monto Inválido", "Un renglón no puede tener valor al Debe y al Haber simultáneamente.");
                return;
            }

            String conc = txtConceptoLinea.getText().trim();
            if (conc.isEmpty()) {
                conc = txtConcepto.getText().trim();
            }

            DetalleAsiento nuevo = new DetalleAsiento(
                lineasAsiento.size() + 1,
                sel.getCodigo(),
                sel.getNombre(),
                conc,
                debeVal,
                haberVal
            );
            lineasAsiento.add(nuevo);
            actualizarCuadre();

            // Limpiar campos de renglón
            cbCuenta.setValue(null);
            txtConceptoLinea.clear();
            txtMontoDebe.setText("0.00");
            txtMontoHaber.setText("0.00");
            cbCuenta.requestFocus();
        });

        Button btnEliminarLinea = new Button("🗑️ Quitar Renglón");
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
            new Label("Detalle:"), txtConceptoLinea,
            new Label("Debe ($):"), txtMontoDebe,
            new Label("Haber ($):"), txtMontoHaber,
            btnAgregarLinea,
            btnEliminarLinea
        );

        // Tabla de Detalle del Asiento
        tblDetalle = new TableView<>();
        lineasAsiento = FXCollections.observableArrayList();
        tblDetalle.setItems(lineasAsiento);
        VBox.setVgrow(tblDetalle, Priority.ALWAYS);

        TableColumn<DetalleAsiento, Number> colRenglon = new TableColumn<>("#");
        colRenglon.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getRenglon()));
        colRenglon.setPrefWidth(45);

        TableColumn<DetalleAsiento, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCuentaCodigo()));
        colCod.setPrefWidth(90);

        TableColumn<DetalleAsiento, String> colNom = new TableColumn<>("Nombre de la Cuenta");
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCuentaNombre()));
        colNom.setPrefWidth(260);

        TableColumn<DetalleAsiento, String> colLinCon = new TableColumn<>("Concepto / Referencia");
        colLinCon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConceptoLinea()));
        colLinCon.setPrefWidth(300);

        TableColumn<DetalleAsiento, String> colDebe = new TableColumn<>("Debe ($)");
        colDebe.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getDebe())));
        colDebe.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colDebe.setPrefWidth(120);

        TableColumn<DetalleAsiento, String> colHaber = new TableColumn<>("Haber ($)");
        colHaber.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getHaber())));
        colHaber.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colHaber.setPrefWidth(120);

        tblDetalle.getColumns().addAll(colRenglon, colCod, colNom, colLinCon, colDebe, colHaber);

        // Panel de Cuadre y Validación Obligatoria de Partida Doble
        HBox panelCuadre = new HBox(20);
        panelCuadre.setAlignment(Pos.CENTER_LEFT);
        panelCuadre.setPadding(new Insets(12, 16, 12, 16));
        panelCuadre.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        lblTotalDebe = new Label("Total Debe: $0.00");
        lblTotalDebe.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

        lblTotalHaber = new Label("Total Haber: $0.00");
        lblTotalHaber.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");

        lblDiferencia = new Label("Diferencia: $0.00");
        lblDiferencia.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #dc2626;");

        lblBadgeCuadre = new Label("⚠ ASIENTO VACÍO");
        lblBadgeCuadre.getStyleClass().add("badge-descuadrado");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnGuardar = new Button("💾 Guardar Asiento en Libro Diario");
        btnGuardar.getStyleClass().add("btn-success");
        btnGuardar.setDisable(true); // Bloqueado por defecto hasta cumplir Partida Doble
        btnGuardar.setOnAction(e -> guardarAsiento());

        Button btnLimpiar = new Button("🧹 Limpiar Formulario");
        btnLimpiar.getStyleClass().add("btn-secondary");
        btnLimpiar.setOnAction(e -> limpiarFormulario());

        panelCuadre.getChildren().addAll(lblTotalDebe, lblTotalHaber, lblDiferencia, lblBadgeCuadre, spacer, btnLimpiar, btnGuardar);

        root.getChildren().addAll(gridHeader, new Separator(), barAcciones, tblDetalle, panelCuadre);
        actualizarCuadre();
        return root;
    }

    private VBox crearTabHistorial() {
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");

        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblHist = new Label("Registro Cronológico de Transacciones (Libro Diario)");
        lblHist.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        HBox.setHgrow(lblHist, Priority.ALWAYS);

        Button btnExportarCSV = new Button("📊 Exportar Libro Diario a CSV");
        btnExportarCSV.getStyleClass().add("btn-secondary");
        btnExportarCSV.setOnAction(e -> exportarHistorialCSV());

        Button btnRefrescar = new Button("🔄 Refrescar");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> recargarHistorial());

        Button btnEliminar = new Button("🗑️ Eliminar Asiento Seleccionado");
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

        topBar.getChildren().addAll(lblHist, btnExportarCSV, btnRefrescar, btnEliminar);

        // Tabla de Asientos
        tblHistorial = new TableView<>();
        listaHistorial = FXCollections.observableArrayList();
        tblHistorial.setItems(listaHistorial);
        tblHistorial.setPrefHeight(260);

        TableColumn<Asiento, Number> colNum = new TableColumn<>("N° Asiento");
        colNum.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getNumero()));
        colNum.setPrefWidth(90);

        TableColumn<Asiento, String> colFec = new TableColumn<>("Fecha");
        colFec.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFecha()));
        colFec.setPrefWidth(100);

        TableColumn<Asiento, String> colCon = new TableColumn<>("Concepto General");
        colCon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConcepto()));
        colCon.setPrefWidth(420);

        TableColumn<Asiento, String> colDeb = new TableColumn<>("Total Debe");
        colDeb.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getTotalDebe())));
        colDeb.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colDeb.setPrefWidth(120);

        TableColumn<Asiento, String> colHab = new TableColumn<>("Total Haber");
        colHab.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getTotalHaber())));
        colHab.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        colHab.setPrefWidth(120);

        TableColumn<Asiento, String> colUser = new TableColumn<>("Registrado Por");
        colUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsuarioNombre() != null ? c.getValue().getUsuarioNombre() : "Sistema"));
        colUser.setPrefWidth(160);

        tblHistorial.getColumns().addAll(colNum, colFec, colFec, colCon, colDeb, colHab, colUser);

        // Tabla de detalle del asiento seleccionado en el historial
        Label lblDet = new Label("Detalle de Partidas del Asiento Seleccionado:");
        lblDet.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #475569;");

        tblDetalleHistorial = new TableView<>();
        VBox.setVgrow(tblDetalleHistorial, Priority.ALWAYS);

        TableColumn<DetalleAsiento, Number> dColReng = new TableColumn<>("Renglón");
        dColReng.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getRenglon()));
        dColReng.setPrefWidth(70);

        TableColumn<DetalleAsiento, String> dColCod = new TableColumn<>("Código");
        dColCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCuentaCodigo()));
        dColCod.setPrefWidth(100);

        TableColumn<DetalleAsiento, String> dColNom = new TableColumn<>("Cuenta");
        dColNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCuentaNombre()));
        dColNom.setPrefWidth(260);

        TableColumn<DetalleAsiento, String> dColCon = new TableColumn<>("Concepto");
        dColCon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getConceptoLinea()));
        dColCon.setPrefWidth(350);

        TableColumn<DetalleAsiento, String> dColDeb = new TableColumn<>("Debe ($)");
        dColDeb.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getDebe())));
        dColDeb.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        dColDeb.setPrefWidth(120);

        TableColumn<DetalleAsiento, String> dColHab = new TableColumn<>("Haber ($)");
        dColHab.setCellValueFactory(c -> new SimpleStringProperty(MONEDA.format(c.getValue().getHaber())));
        dColHab.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-family: 'Consolas', monospace;");
        dColHab.setPrefWidth(120);

        tblDetalleHistorial.getColumns().addAll(dColReng, dColCod, dColNom, dColCon, dColDeb, dColHab);

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

    /**
     * Validación obligatoria de Partida Doble en tiempo real:
     * Si no cumple la partida doble, el botón de Guardar se BLOQUEA estrictamente.
     */
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
            lblBadgeCuadre.setText("⚠ INGRESE PARTIDAS");
            lblBadgeCuadre.getStyleClass().add("badge-descuadrado");
            btnGuardar.setDisable(true);
            btnGuardar.setTooltip(new Tooltip("Agregue al menos dos renglones contables."));
        } else if (esValido) {
            lblBadgeCuadre.setText("✔ PARTIDA DOBLE CUADRADA - LISTO PARA GUARDAR");
            lblBadgeCuadre.getStyleClass().add("badge-cuadrado");
            btnGuardar.setDisable(false); // Desbloquear guardado
            btnGuardar.setTooltip(new Tooltip("El asiento cumple la Partida Doble y puede guardarse."));
        } else {
            lblBadgeCuadre.setText("⚠ DESCUADRADO (Diferencia: " + MONEDA.format(diff) + ") - GUARDADO BLOQUEADO");
            lblBadgeCuadre.getStyleClass().add("badge-descuadrado");
            btnGuardar.setDisable(true); // BLOQUEO OBLIGATORIO SEGÚN REQUERIMIENTO
            btnGuardar.setTooltip(new Tooltip("Bloqueado: La suma del Debe debe ser exactamente igual a la suma del Haber."));
        }
    }

    private void guardarAsiento() {
        String concepto = txtConcepto.getText().trim();
        if (concepto.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Campo Requerido", "Debe ingresar el concepto general o glosa del asiento.");
            txtConcepto.requestFocus();
            return;
        }

        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha Requerida", "Debe seleccionar una fecha para el asiento.");
            return;
        }

        int numero = Integer.parseInt(txtNumero.getText().trim());
        int usuarioId = SessionManager.getInstance().getUsuarioActual() != null
            ? SessionManager.getInstance().getUsuarioActual().getId() : 1;

        Asiento asiento = new Asiento(numero, fecha.toString(), concepto, usuarioId);
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
                    "¡Asiento N° " + numero + " registrado exitosamente en el Libro Diario!\nLa mayorización ha sido actualizada en tiempo real.");
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
        txtConcepto.clear();
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
}
