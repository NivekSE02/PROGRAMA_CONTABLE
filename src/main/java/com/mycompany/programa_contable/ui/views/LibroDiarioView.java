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
        setPadding(new Insets(20));
        setSpacing(16);
        setStyle("-fx-background-color: transparent;");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab tabNuevo = new Tab("Registrar Nuevo Asiento", crearTabNuevoAsiento());
        Tab tabHistorial = new Tab("Historial del Libro Diario", crearTabHistorial());

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
        gridHeader.setHgap(20);
        gridHeader.setVgap(12);

        Label lblNumTitle = new Label("N° de Asiento:");
        lblNumTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        txtNumero = new TextField();
        txtNumero.setEditable(false);
        txtNumero.setPrefWidth(120);
        txtNumero.setMinHeight(35);
        txtNumero.setStyle("-fx-font-weight: bold; -fx-background-color: #f1f5f9; -fx-font-size: 14px;");
        actualizarNumeroAsiento();

        Label lblFecTitle = new Label("Fecha del Asiento:");
        lblFecTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        dpFecha = new DatePicker(LocalDate.now());
        dpFecha.setPrefWidth(180);
        dpFecha.setMinHeight(35);
        dpFecha.setStyle("-fx-font-size: 14px;");

        gridHeader.add(lblNumTitle, 0, 0);
        gridHeader.add(txtNumero, 0, 1);
        gridHeader.add(lblFecTitle, 1, 0);
        gridHeader.add(dpFecha, 1, 1);

        // Barra de acciones para renglones
        HBox barAcciones = new HBox(15);
        barAcciones.setAlignment(Pos.CENTER_LEFT);
        barAcciones.setPadding(new Insets(10, 0, 10, 0));

        List<Cuenta> cuentasPermitidas = cuentaDAO.listarPermitenMovimiento();
        ComboBox<Cuenta> cbCuenta = new ComboBox<>(FXCollections.observableArrayList(cuentasPermitidas));
        cbCuenta.setPromptText("Seleccione una cuenta del catálogo...");
        cbCuenta.setPrefWidth(450);
        cbCuenta.setMinHeight(40);
        cbCuenta.setStyle("-fx-font-size: 14px;");

        TextField txtMontoDebe = new TextField("0.00");
        txtMontoDebe.setPromptText("Debe");
        txtMontoDebe.setPrefWidth(130);
        txtMontoDebe.setMinHeight(40);
        txtMontoDebe.setStyle("-fx-font-size: 14px;");

        TextField txtMontoHaber = new TextField("0.00");
        txtMontoHaber.setPromptText("Haber");
        txtMontoHaber.setPrefWidth(130);
        txtMontoHaber.setMinHeight(40);
        txtMontoHaber.setStyle("-fx-font-size: 14px;");

        Button btnAgregarLinea = new Button("Agregar Renglón");
        btnAgregarLinea.setMinSize(150, 40);
        btnAgregarLinea.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;");
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

            // Lógica para subcuentas y cuentas principales
            if (sel.getCuentaPadre() != null && !sel.getCuentaPadre().isEmpty()) {
                // Es una subcuenta, buscar la cuenta padre
                Cuenta padre = cuentaDAO.buscarPorCodigo(sel.getCuentaPadre());
                if (padre != null) {
                    // Ver si el padre ya está en el detalle
                    DetalleAsiento rowPadre = null;
                    for (DetalleAsiento d : lineasAsiento) {
                        if (d.getCuentaCodigo().equals(padre.getCodigo())) {
                            rowPadre = d;
                            break;
                        }
                    }
                    if (rowPadre == null) {
                        rowPadre = new DetalleAsiento(
                            lineasAsiento.size() + 1,
                            padre.getCodigo(),
                            padre.getNombre(),
                            "", // parcial vacío
                            debeVal,
                            haberVal
                        );
                        lineasAsiento.add(rowPadre);
                    } else {
                        rowPadre.setDebe(rowPadre.getDebe() + debeVal);
                        rowPadre.setHaber(rowPadre.getHaber() + haberVal);
                    }
                    
                    // Ahora agregamos la subcuenta con el valor en parcial (conceptoLinea se usa para guardar parcial en UI)
                    String parcialStr = MONEDA.format(Math.max(debeVal, haberVal));
                    DetalleAsiento rowSub = new DetalleAsiento(
                        lineasAsiento.size() + 1,
                        sel.getCodigo(),
                        sel.getNombre(),
                        parcialStr,
                        0,
                        0
                    );
                    lineasAsiento.add(rowSub);
                } else {
                    DetalleAsiento nuevo = new DetalleAsiento(lineasAsiento.size() + 1, sel.getCodigo(), sel.getNombre(), "", debeVal, haberVal);
                    lineasAsiento.add(nuevo);
                }
            } else {
                DetalleAsiento nuevo = new DetalleAsiento(lineasAsiento.size() + 1, sel.getCodigo(), sel.getNombre(), "", debeVal, haberVal);
                lineasAsiento.add(nuevo);
            }

            actualizarCuadre();
            cbCuenta.setValue(null);
            txtMontoDebe.setText("0.00");
            txtMontoHaber.setText("0.00");
            cbCuenta.requestFocus();
        });

        Button btnEliminarLinea = new Button("Quitar Renglón");
        btnEliminarLinea.setMinSize(150, 40);
        btnEliminarLinea.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");
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
            new Label("Debe ($):"), txtMontoDebe,
            new Label("Haber ($):"), txtMontoHaber,
            btnAgregarLinea,
            btnEliminarLinea
        );

        // Tabla de Detalle del Asiento
        tblDetalle = new TableView<>();
        lineasAsiento = FXCollections.observableArrayList();
        tblDetalle.setItems(lineasAsiento);
        tblDetalle.setStyle("-fx-font-size: 14px;");
        VBox.setVgrow(tblDetalle, Priority.ALWAYS);

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
        HBox boxComentario = new HBox(15);
        boxComentario.setAlignment(Pos.CENTER_LEFT);
        boxComentario.setPadding(new Insets(10, 0, 10, 0));
        Label lblComentario = new Label("Comentario del Asiento (Opcional):");
        lblComentario.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        txtComentarioAsiento = new TextField();
        txtComentarioAsiento.setPromptText("Ej. Pago de factura a proveedores con transferencia bancaria...");
        txtComentarioAsiento.setMinHeight(40);
        txtComentarioAsiento.setStyle("-fx-font-size: 14px;");
        HBox.setHgrow(txtComentarioAsiento, Priority.ALWAYS);
        boxComentario.getChildren().addAll(lblComentario, txtComentarioAsiento);

        // Panel de Cuadre y Validación Obligatoria de Partida Doble
        HBox panelCuadre = new HBox(20);
        panelCuadre.setAlignment(Pos.CENTER_LEFT);
        panelCuadre.setPadding(new Insets(15, 20, 15, 20));
        panelCuadre.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        lblTotalDebe = new Label("Total Debe: $0.00");
        lblTotalDebe.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1e293b;");

        lblTotalHaber = new Label("Total Haber: $0.00");
        lblTotalHaber.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #1e293b;");

        lblDiferencia = new Label("Diferencia: $0.00");
        lblDiferencia.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #dc2626;");

        lblBadgeCuadre = new Label("ASIENTO VACÍO");
        lblBadgeCuadre.getStyleClass().add("badge-descuadrado");
        lblBadgeCuadre.setStyle("-fx-font-size: 14px; -fx-padding: 8 12;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnGuardar = new Button("Guardar Asiento en Libro Diario");
        btnGuardar.setMinSize(250, 45);
        btnGuardar.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnGuardar.getStyleClass().add("btn-success");
        btnGuardar.setDisable(true); // Bloqueado por defecto hasta cumplir Partida Doble
        btnGuardar.setOnAction(e -> guardarAsiento());

        Button btnLimpiar = new Button("Limpiar Formulario");
        btnLimpiar.setMinSize(180, 45);
        btnLimpiar.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnLimpiar.getStyleClass().add("btn-secondary");
        btnLimpiar.setOnAction(e -> limpiarFormulario());

        panelCuadre.getChildren().addAll(lblTotalDebe, lblTotalHaber, lblDiferencia, lblBadgeCuadre, spacer, btnLimpiar, btnGuardar);

        root.getChildren().addAll(gridHeader, new Separator(), barAcciones, tblDetalle, boxComentario, panelCuadre);
        actualizarCuadre();
        return root;
    }

    private VBox crearTabHistorial() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");

        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label lblHist = new Label("Registro Cronológico de Transacciones (Libro Diario)");
        lblHist.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        HBox.setHgrow(lblHist, Priority.ALWAYS);

        Button btnExportarCSV = new Button("Exportar Libro Diario a CSV");
        btnExportarCSV.setMinSize(200, 40);
        btnExportarCSV.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");
        btnExportarCSV.getStyleClass().add("btn-secondary");
        btnExportarCSV.setOnAction(e -> exportarHistorialCSV());

        Button btnRefrescar = new Button("Refrescar");
        btnRefrescar.setMinSize(120, 40);
        btnRefrescar.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> recargarHistorial());

        Button btnEliminar = new Button("Eliminar Asiento Seleccionado");
        btnEliminar.setMinSize(220, 40);
        btnEliminar.setStyle("-fx-font-size: 14px; -fx-cursor: hand;");
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
        tblHistorial.setPrefHeight(280);
        tblHistorial.setStyle("-fx-font-size: 14px;");

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

        TableColumn<Asiento, String> colUser = new TableColumn<>("Registrado Por");
        colUser.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsuarioNombre() != null ? c.getValue().getUsuarioNombre() : "Sistema"));
        colUser.setPrefWidth(180);

        tblHistorial.getColumns().addAll(colNum, colFec, colCon, colDeb, colHab, colUser);

        // Tabla de detalle del asiento seleccionado en el historial
        Label lblDet = new Label("Detalle de Partidas del Asiento Seleccionado:");
        lblDet.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #475569; -fx-padding: 10 0 5 0;");

        tblDetalleHistorial = new TableView<>();
        VBox.setVgrow(tblDetalleHistorial, Priority.ALWAYS);
        tblDetalleHistorial.setStyle("-fx-font-size: 14px;");

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
            lblBadgeCuadre.getStyleClass().add("badge-descuadrado");
            btnGuardar.setDisable(true);
            btnGuardar.setTooltip(new Tooltip("Agregue al menos dos renglones contables."));
        } else if (esValido) {
            lblBadgeCuadre.setText("PARTIDA DOBLE CUADRADA - LISTO PARA GUARDAR");
            lblBadgeCuadre.getStyleClass().add("badge-cuadrado");
            btnGuardar.setDisable(false); // Desbloquear guardado
            btnGuardar.setTooltip(new Tooltip("El asiento cumple la Partida Doble y puede guardarse."));
        } else {
            lblBadgeCuadre.setText("DESCUADRADO (Diferencia: " + MONEDA.format(diff) + ") - GUARDADO BLOQUEADO");
            lblBadgeCuadre.getStyleClass().add("badge-descuadrado");
            btnGuardar.setDisable(true); // BLOQUEO OBLIGATORIO SEGÚN REQUERIMIENTO
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
