package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.dao.CuentaDAO;
import com.mycompany.programa_contable.model.Cuenta;
import com.mycompany.programa_contable.model.NaturalezaCuenta;
import com.mycompany.programa_contable.model.TipoCuenta;
import com.mycompany.programa_contable.service.SessionManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class CatalogoCuentasView extends VBox {

    private final CuentaDAO cuentaDAO = new CuentaDAO();

    private TableView<Cuenta> tblCuentas;
    private ObservableList<Cuenta> listaCuentas;
    private TextField txtBuscar;

    // Formulario de Nueva Cuenta
    private TextField txtCodigo;
    private TextField txtNombre;
    private ComboBox<TipoCuenta> cbTipo;
    private ComboBox<NaturalezaCuenta> cbNaturaleza;
    private CheckBox chkMovimiento;
    private Button btnGuardar;

    public CatalogoCuentasView() {
        setPadding(new Insets(20));
        setSpacing(16);
        setStyle("-fx-background-color: transparent;");

        HBox mainBox = new HBox(16);
        VBox.setVgrow(mainBox, Priority.ALWAYS);

        // Panel Izquierdo: Tabla y Buscador
        VBox leftPane = new VBox(12);
        leftPane.getStyleClass().add("card");
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        HBox barBusqueda = new HBox(12);
        barBusqueda.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("Catálogo de Cuentas Contables (NIIF para PYMES)");
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        HBox.setHgrow(lblTitle, Priority.ALWAYS);

        txtBuscar = new TextField();
        txtBuscar.setPromptText("🔍 Buscar por código o nombre...");
        txtBuscar.setPrefWidth(260);
        txtBuscar.textProperty().addListener((obs, oldV, newV) -> filtrarCuentas(newV));

        Button btnRefrescar = new Button("🔄");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> recargarCuentas());

        barBusqueda.getChildren().addAll(lblTitle, txtBuscar, btnRefrescar);

        tblCuentas = new TableView<>();
        listaCuentas = FXCollections.observableArrayList();
        tblCuentas.setItems(listaCuentas);
        VBox.setVgrow(tblCuentas, Priority.ALWAYS);

        TableColumn<Cuenta, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCod.setPrefWidth(90);

        TableColumn<Cuenta, String> colNom = new TableColumn<>("Nombre de la Cuenta");
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colNom.setPrefWidth(260);

        TableColumn<Cuenta, String> colTip = new TableColumn<>("Clase / Dígito");
        colTip.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTipo().getDigito() + " - " + c.getValue().getTipo().getNombre()));
        colTip.setPrefWidth(160);

        TableColumn<Cuenta, String> colNat = new TableColumn<>("Naturaleza");
        colNat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNaturaleza().getEtiqueta()));
        colNat.setPrefWidth(100);

        TableColumn<Cuenta, String> colMov = new TableColumn<>("Uso");
        colMov.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isPermiteMovimiento() ? "Detalle" : "Mayor / Título"));
        colMov.setPrefWidth(110);

        tblCuentas.getColumns().addAll(colCod, colNom, colTip, colNat, colMov);

        leftPane.getChildren().addAll(barBusqueda, tblCuentas);

        // Panel Derecho: Registro de Nueva Cuenta
        VBox rightPane = new VBox(14);
        rightPane.getStyleClass().add("card");
        rightPane.setPrefWidth(320);

        Label lblFormTitle = new Label("Registrar Nueva Cuenta");
        lblFormTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label lblCod = new Label("Código Contable:");
        lblCod.getStyleClass().add("form-label");
        txtCodigo = new TextField();
        txtCodigo.setPromptText("Ej. 110104, 420105...");
        txtCodigo.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.trim().isEmpty()) {
                TipoCuenta tc = TipoCuenta.desdeCodigo(newV);
                cbTipo.setValue(tc);
                cbNaturaleza.setValue(tc.getNaturalezaPorDefecto());
            }
        });

        Label lblNom = new Label("Nombre de la Cuenta:");
        lblNom.getStyleClass().add("form-label");
        txtNombre = new TextField();
        txtNombre.setPromptText("Ej. Banco Agrícola Cta. Ahorros");

        Label lblTip = new Label("Clasificación (Automática por 1er dígito):");
        lblTip.getStyleClass().add("form-label");
        cbTipo = new ComboBox<>(FXCollections.observableArrayList(TipoCuenta.values()));
        cbTipo.setMaxWidth(Double.MAX_VALUE);

        Label lblNat = new Label("Naturaleza del Saldo:");
        lblNat.getStyleClass().add("form-label");
        cbNaturaleza = new ComboBox<>(FXCollections.observableArrayList(NaturalezaCuenta.values()));
        cbNaturaleza.setMaxWidth(Double.MAX_VALUE);

        chkMovimiento = new CheckBox("Permite Movimiento (Cuenta de Detalle)");
        chkMovimiento.setSelected(true);

        btnGuardar = new Button("💾 Guardar Cuenta");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        btnGuardar.setOnAction(e -> guardarCuenta());

        Button btnEliminar = new Button("🗑️ Eliminar Seleccionada");
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setMaxWidth(Double.MAX_VALUE);
        btnEliminar.setOnAction(e -> eliminarCuenta());

        rightPane.getChildren().addAll(
            lblFormTitle,
            new Separator(),
            new VBox(4, lblCod, txtCodigo),
            new VBox(4, lblNom, txtNombre),
            new VBox(4, lblTip, cbTipo),
            new VBox(4, lblNat, cbNaturaleza),
            chkMovimiento,
            btnGuardar,
            new Separator(),
            btnEliminar
        );

        mainBox.getChildren().addAll(leftPane, rightPane);
        getChildren().add(mainBox);

        recargarCuentas();
    }

    public void recargarCuentas() {
        List<Cuenta> cuentas = cuentaDAO.listarTodas();
        listaCuentas.setAll(cuentas);
    }

    private void filtrarCuentas(String filtro) {
        if (filtro == null || filtro.trim().isEmpty()) {
            recargarCuentas();
        } else {
            List<Cuenta> filtradas = cuentaDAO.buscar(filtro);
            listaCuentas.setAll(filtradas);
        }
    }

    private void guardarCuenta() {
        if (!SessionManager.getInstance().puedeModificarCatalogo()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Acceso Restringido", ButtonType.OK);
            a.setHeaderText("Permiso Insuficiente");
            a.setContentText("Solo los usuarios con rol de ADMINISTRADOR pueden agregar o modificar cuentas del catálogo.");
            a.showAndWait();
            return;
        }

        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();
        TipoCuenta tipo = cbTipo.getValue();
        NaturalezaCuenta nat = cbNaturaleza.getValue();

        if (codigo.isEmpty() || nombre.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Campos Incompletos", ButtonType.OK);
            a.setContentText("Debe ingresar el código y nombre de la cuenta.");
            a.showAndWait();
            return;
        }

        if (tipo == null) {
            tipo = TipoCuenta.desdeCodigo(codigo);
        }
        if (nat == null) {
            nat = tipo.getNaturalezaPorDefecto();
        }

        int nivel = codigo.length() <= 1 ? 1 : (codigo.length() <= 2 ? 2 : (codigo.length() <= 4 ? 3 : 4));
        Cuenta nueva = new Cuenta(codigo, nombre, tipo, null, nivel, nat, null, chkMovimiento.isSelected());

        boolean exito = cuentaDAO.insertar(nueva);
        if (exito) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Cuenta agregada exitosamente al catálogo contable.", ButtonType.OK);
            a.showAndWait();
            txtCodigo.clear();
            txtNombre.clear();
            recargarCuentas();
        } else {
            Alert a = new Alert(Alert.AlertType.ERROR, "No se pudo guardar la cuenta (es posible que el código ya exista).", ButtonType.OK);
            a.showAndWait();
        }
    }

    private void eliminarCuenta() {
        if (!SessionManager.getInstance().puedeModificarCatalogo()) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Acceso Restringido", ButtonType.OK);
            a.setContentText("Solo los administradores pueden eliminar cuentas.");
            a.showAndWait();
            return;
        }

        Cuenta sel = tblCuentas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Seleccione una cuenta para eliminar.", ButtonType.OK);
            a.showAndWait();
            return;
        }

        if (cuentaDAO.tieneMovimientos(sel.getCodigo())) {
            Alert a = new Alert(Alert.AlertType.ERROR, "No se puede eliminar la cuenta " + sel.getCodigo() + " porque ya tiene movimientos registrados en el Libro Diario.", ButtonType.OK);
            a.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Está seguro de eliminar la cuenta " + sel.getCodigo() + " - " + sel.getNombre() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                cuentaDAO.eliminar(sel.getCodigo());
                recargarCuentas();
            }
        });
    }
}
