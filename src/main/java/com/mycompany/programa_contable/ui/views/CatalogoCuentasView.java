package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.dao.CuentaDAO;
import com.mycompany.programa_contable.model.Cuenta;
import com.mycompany.programa_contable.model.NaturalezaCuenta;
import com.mycompany.programa_contable.model.TipoCuenta;
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
    private List<Cuenta> todasLasCuentas = List.of();
    private TextField txtBuscar;

    // Formulario de Nueva Cuenta
    private TextField txtCodigo;
    private TextField txtNombre;
    private ComboBox<Cuenta> cbCuentaPadre;
    private ComboBox<String> cbSubtipo;
    private ComboBox<NaturalezaCuenta> cbNaturaleza;
    private Label lblVistaPrevia;
    private CheckBox chkMovimiento;
    private Button btnGuardar;

    public CatalogoCuentasView() {
        setPadding(new Insets(24, 32, 32, 32));
        setSpacing(16);
        setStyle("-fx-background-color: #f8fafc;");

        HBox mainBox = new HBox(20);
        VBox.setVgrow(mainBox, Priority.ALWAYS);

        // Panel Izquierdo: Tabla y Buscador
        VBox leftPane = new VBox(16);
        leftPane.getStyleClass().add("card");
        leftPane.setPadding(new Insets(24));
        HBox.setHgrow(leftPane, Priority.ALWAYS);

        HBox barBusqueda = new HBox(12);
        barBusqueda.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Catálogo de Cuentas");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label lblTitleSub = new Label("NIIF para PYMES — Clasificación por dígitos");
        lblTitleSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblTitle, lblTitleSub);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por código o nombre...");
        txtBuscar.setPrefWidth(260);
        txtBuscar.textProperty().addListener((obs, oldV, newV) -> filtrarCuentas(newV));

        Button btnRefrescar = new Button("Actualizar");
        btnRefrescar.getStyleClass().add("btn-secondary");
        btnRefrescar.setOnAction(e -> recargarCuentas());

        barBusqueda.getChildren().addAll(titleBox, txtBuscar, btnRefrescar);

        tblCuentas = new TableView<>();
        listaCuentas = FXCollections.observableArrayList();
        tblCuentas.setItems(listaCuentas);
        VBox.setVgrow(tblCuentas, Priority.ALWAYS);

        TableColumn<Cuenta, String> colCod = new TableColumn<>("Código");
        colCod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCodigo()));
        colCod.setPrefWidth(115);

        TableColumn<Cuenta, String> colNom = new TableColumn<>("Nombre de la Cuenta");
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        colNom.setPrefWidth(260);

        TableColumn<Cuenta, String> colTip = new TableColumn<>("Clase / Dígito");
        colTip.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTipo().getDigito() + " - " + c.getValue().getTipo().getNombre()));
        colTip.setPrefWidth(160);

        TableColumn<Cuenta, String> colNat = new TableColumn<>("Naturaleza");
        colNat.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNaturaleza().getEtiqueta()));
        colNat.setPrefWidth(100);

        TableColumn<Cuenta, String> colMov = new TableColumn<>("Uso");
        colMov.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().isPermiteMovimiento() ? "Detalle" : "Mayor / Título"));
        colMov.setPrefWidth(110);

        tblCuentas.getColumns().addAll(colCod, colNom, colTip, colNat, colMov);

        leftPane.getChildren().addAll(barBusqueda, tblCuentas);

        // Panel Derecho: Registro de Nueva Cuenta
        VBox rightPane = new VBox(16);
        rightPane.getStyleClass().add("card");
        rightPane.setPadding(new Insets(24));
        rightPane.setPrefWidth(360);
        rightPane.setMinWidth(320);

        VBox formTitleBox = new VBox(2);
        Label lblFormTitle = new Label("Nueva Cuenta");
        lblFormTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label lblFormSub = new Label("Crea una subcuenta dentro del grupo seleccionado");
        lblFormSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        formTitleBox.getChildren().addAll(lblFormTitle, lblFormSub);

        Label lblPadre = new Label("Cuenta superior:");
        lblPadre.getStyleClass().add("form-label");
        cbCuentaPadre = new ComboBox<>();
        cbCuentaPadre.setPromptText("Selecciona el grupo contable");
        cbCuentaPadre.setMaxWidth(Double.MAX_VALUE);
        cbCuentaPadre.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Cuenta cuenta, boolean empty) {
                super.updateItem(cuenta, empty);
                setText(empty || cuenta == null ? null
                        : "  ".repeat(Math.max(0, cuenta.getNivel() - 1))
                                + cuenta.getCodigo() + " - " + cuenta.getNombre());
            }
        });
        cbCuentaPadre.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Cuenta cuenta, boolean empty) {
                super.updateItem(cuenta, empty);
                setText(empty || cuenta == null ? null : cuenta.getCodigo() + " - " + cuenta.getNombre());
            }
        });
        cbCuentaPadre.setOnAction(e -> configurarFormularioPorPadre());

        Label lblCod = new Label("Código generado:");
        lblCod.getStyleClass().add("form-label");
        txtCodigo = new TextField();
        txtCodigo.setEditable(false);
        txtCodigo.setPromptText("Se genera al elegir la cuenta superior");

        Label lblNom = new Label("Nombre de la Cuenta:");
        lblNom.getStyleClass().add("form-label");
        txtNombre = new TextField();
        txtNombre.setPromptText("Ej. Banco Agrícola Cta. Ahorros");

        Label lblCategoria = new Label("Clasificación para reportes:");
        lblCategoria.getStyleClass().add("form-label");
        cbSubtipo = new ComboBox<>();
        cbSubtipo.setMaxWidth(Double.MAX_VALUE);
        cbSubtipo.setOnAction(e -> actualizarNaturalezaYVistaPrevia());

        Label lblNat = new Label("Naturaleza del Saldo:");
        lblNat.getStyleClass().add("form-label");
        cbNaturaleza = new ComboBox<>(FXCollections.observableArrayList(NaturalezaCuenta.values()));
        cbNaturaleza.setMaxWidth(Double.MAX_VALUE);
        cbNaturaleza.setOnAction(e -> actualizarVistaPreviaSeleccionada());

        lblVistaPrevia = new Label("Selecciona la cuenta superior para ver cómo se clasificará esta cuenta.");
        lblVistaPrevia.setWrapText(true);
        lblVistaPrevia.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        chkMovimiento = new CheckBox("Permite Movimiento (Cuenta de Detalle)");
        chkMovimiento.setSelected(true);

        btnGuardar = new Button("Guardar Cuenta");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setMaxWidth(Double.MAX_VALUE);
        btnGuardar.setOnAction(e -> guardarCuenta());

        Button btnEliminar = new Button("Eliminar Seleccionada");
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setMaxWidth(Double.MAX_VALUE);
        btnEliminar.setOnAction(e -> eliminarCuenta());

        rightPane.getChildren().addAll(
                formTitleBox,
                new Separator(),
                new VBox(6, lblPadre, cbCuentaPadre),
                new VBox(6, lblCod, txtCodigo),
                new VBox(6, lblNom, txtNombre),
                new VBox(6, lblCategoria, cbSubtipo),
                new VBox(6, lblNat, cbNaturaleza),
                lblVistaPrevia,
                chkMovimiento,
                btnGuardar,
                new Separator(),
                btnEliminar);

        ScrollPane formScroll = new ScrollPane(rightPane);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setPrefWidth(360);
        formScroll.setMinWidth(320);
        formScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        mainBox.getChildren().addAll(leftPane, formScroll);
        getChildren().add(mainBox);

        recargarCuentas();
    }

    public void recargarCuentas() {
        List<Cuenta> cuentas = cuentaDAO.listarTodas();
        todasLasCuentas = List.copyOf(cuentas);
        listaCuentas.setAll(cuentas);
        String codigoPadre = cbCuentaPadre.getValue() == null ? "1" : cbCuentaPadre.getValue().getCodigo();
        List<Cuenta> padresValidos = cuentas.stream()
                .filter(c -> !c.isPermiteMovimiento())
                .toList();
        cbCuentaPadre.setItems(FXCollections.observableArrayList(padresValidos));
        Cuenta padre = padresValidos.stream()
                .filter(c -> c.getCodigo().equals(codigoPadre))
                .findFirst()
                .orElseGet(() -> padresValidos.stream().findFirst().orElse(null));
        cbCuentaPadre.setValue(padre);
        configurarFormularioPorPadre();
    }

    private void configurarFormularioPorPadre() {
        Cuenta padre = cbCuentaPadre.getValue();
        if (padre == null) {
            txtCodigo.clear();
            cbSubtipo.getItems().clear();
            lblVistaPrevia.setText("No hay cuentas disponibles para usar como cuenta superior.");
            btnGuardar.setDisable(true);
            return;
        }

        btnGuardar.setDisable(false);
        txtCodigo.setText(generarCodigoHijo(padre));
        cbNaturaleza.setDisable(esSubcuentaDe(txtCodigo.getText(), "4.2")
                || esSubcuentaDe(txtCodigo.getText(), "5.1"));
        TipoCuenta tipo = padre.getTipo() != null ? padre.getTipo() : TipoCuenta.desdeCodigo(padre.getCodigo());
        cbSubtipo.setItems(FXCollections.observableArrayList(categoriasPara(padre, tipo)));
        String subtipo = categoriaInicial(padre, tipo);
        cbSubtipo.setValue(subtipo);
        cbNaturaleza.setValue(naturalezaSugerida(padre, subtipo, tipo));
        actualizarVistaPrevia(padre, tipo, subtipo);
    }

    private List<String> categoriasPara(Cuenta padre, TipoCuenta tipo) {
        return switch (tipo) {
            case ACTIVO -> categoriaDelPadre(padre, List.of("ACTIVO CORRIENTE", "ACTIVO NO CORRIENTE"));
            case PASIVO -> categoriaDelPadre(padre, List.of("PASIVO CORRIENTE", "PASIVO NO CORRIENTE"));
            case PATRIMONIO -> List.of("PATRIMONIO");
            case INGRESO -> esSubcuentaDe(padre.getCodigo(), "4.2")
                    ? List.of("RESTA A INGRESOS")
                    : esSubcuentaDe(padre.getCodigo(), "4.1")
                            ? List.of("INGRESOS DE OPERACIÓN")
                            : categoriaDelPadre(padre, List.of("INGRESOS DE OPERACIÓN", "OTROS INGRESOS"));
            case COSTO -> esSubcuentaDe(padre.getCodigo(), "5.1")
                    ? List.of("RESTA A COSTOS")
                    : esSubcuentaDe(padre.getCodigo(), "5.3")
                            ? List.of("CUENTA TRANSITORIA")
                            : categoriaDelPadre(padre, List.of("COSTOS", "CUENTA TRANSITORIA"));
            case GASTO -> esSubcuentaDe(padre.getCodigo(), "6.1")
                    ? List.of("GASTOS FINANCIEROS")
                    : esSubcuentaDe(padre.getCodigo(), "6.2")
                            ? List.of("GASTOS DE ADMINISTRACIÓN")
                            : esSubcuentaDe(padre.getCodigo(), "6.3")
                                    ? List.of("GASTOS DE VENTA")
                                    : categoriaDelPadre(padre, List.of("GASTOS FINANCIEROS", "GASTOS DE ADMINISTRACIÓN", "GASTOS DE VENTA"));
            case ORDEN -> List.of("CUENTAS DE ORDEN");
        };
    }

    private List<String> categoriaDelPadre(Cuenta padre, List<String> opciones) {
        String subtipoPadre = padre.getSubtipo();
        return subtipoPadre != null && opciones.contains(subtipoPadre)
                ? List.of(subtipoPadre)
                : opciones;
    }

    private String categoriaInicial(Cuenta padre, TipoCuenta tipo) {
        String categoriaPadre = padre.getSubtipo();
        if (categoriaPadre != null && categoriasPara(padre, tipo).contains(categoriaPadre)) return categoriaPadre;
        String codigo = padre.getCodigo();
        return switch (tipo) {
            case ACTIVO -> "ACTIVO CORRIENTE";
            case PASIVO -> "PASIVO CORRIENTE";
            case PATRIMONIO -> "PATRIMONIO";
            case INGRESO -> esSubcuentaDe(codigo, "4.2") ? "RESTA A INGRESOS" : "INGRESOS DE OPERACIÓN";
            case COSTO -> esSubcuentaDe(codigo, "5.1") ? "RESTA A COSTOS"
                    : esSubcuentaDe(codigo, "5.3") ? "CUENTA TRANSITORIA" : "COSTOS";
            case GASTO -> esSubcuentaDe(codigo, "6.1") ? "GASTOS FINANCIEROS"
                    : esSubcuentaDe(codigo, "6.2") ? "GASTOS DE ADMINISTRACIÓN" : "GASTOS DE VENTA";
            case ORDEN -> "CUENTAS DE ORDEN";
        };
    }

    private NaturalezaCuenta naturalezaSugerida(Cuenta padre, String subtipo, TipoCuenta tipo) {
        if ("RESTA A INGRESOS".equals(subtipo)) return NaturalezaCuenta.DEUDORA;
        if ("RESTA A COSTOS".equals(subtipo)) return NaturalezaCuenta.ACREEDORA;
        if (subtipo.equals(padre.getSubtipo()) && padre.getNaturaleza() != null) return padre.getNaturaleza();
        return tipo.getNaturalezaPorDefecto();
    }

    private String generarCodigoHijo(Cuenta padre) {
        String prefijo = padre.getCodigo() + ".";
        int siguiente = 1;
        for (Cuenta cuenta : todasLasCuentas) {
            if (!padre.getCodigo().equals(cuenta.getCuentaPadre()) || !cuenta.getCodigo().startsWith(prefijo)) continue;
            String segmento = cuenta.getCodigo().substring(prefijo.length());
            if (segmento.indexOf('.') >= 0) continue;
            try {
                siguiente = Math.max(siguiente, Integer.parseInt(segmento) + 1);
            } catch (NumberFormatException ignored) {
                // Ignorar códigos existentes que no sigan el formato jerárquico.
            }
        }
        return prefijo + siguiente;
    }

    private boolean esSubcuentaDe(String codigo, String codigoGrupo) {
        return codigo.equals(codigoGrupo) || codigo.startsWith(codigoGrupo + ".");
    }

    private void actualizarNaturalezaYVistaPrevia() {
        Cuenta padre = cbCuentaPadre.getValue();
        if (padre == null || cbSubtipo.getValue() == null) return;
        TipoCuenta tipo = padre.getTipo() != null ? padre.getTipo() : TipoCuenta.desdeCodigo(padre.getCodigo());
        cbNaturaleza.setValue(naturalezaSugerida(padre, cbSubtipo.getValue(), tipo));
        actualizarVistaPrevia(padre, tipo, cbSubtipo.getValue());
    }

    private void actualizarVistaPrevia(Cuenta padre, TipoCuenta tipo, String subtipo) {
        String destino = switch (tipo) {
            case ACTIVO -> subtipo.contains("NO CORRIENTE") ? "Balance general · Activos no corrientes" : "Balance general · Activos corrientes";
            case PASIVO -> subtipo.contains("NO CORRIENTE") ? "Balance general · Pasivos no corrientes" : "Balance general · Pasivos corrientes";
            case PATRIMONIO -> "Balance general · Patrimonio";
            case INGRESO -> subtipo.equals("RESTA A INGRESOS") ? "Estado de resultados · Devoluciones de ventas"
                    : subtipo.equals("OTROS INGRESOS") ? "Estado de resultados · Otros ingresos" : "Estado de resultados · Ingresos de operación";
            case COSTO -> (padre.getCodigo().equals("5.4") || padre.getCodigo().startsWith("5.4."))
                    ? "Balanza de comprobación · Compras reflejadas por inventario/Kárdex"
                    : subtipo.equals("CUENTA TRANSITORIA") ? "Balanza de comprobación · Cuenta transitoria"
                    : subtipo.equals("RESTA A COSTOS") ? "Balanza de comprobación · Ajuste de compras gestionado por Kárdex"
                    : "Estado de resultados · Costos (con reglas de inventario y Kárdex)";
            case GASTO -> "Estado de resultados · " + subtipo;
            case ORDEN -> "Balanza de comprobación · Cuentas de orden";
        };
        NaturalezaCuenta naturaleza = cbNaturaleza.getValue();
        String detalleNaturaleza = naturaleza == null ? "" : " · Saldo " + naturaleza.getEtiqueta().toLowerCase();
        lblVistaPrevia.setText("Clase " + tipo.getNombre() + " · Se guardará debajo de "
                + padre.getCodigo() + " · " + destino + detalleNaturaleza + ".");
    }

    private void actualizarVistaPreviaSeleccionada() {
        Cuenta padre = cbCuentaPadre.getValue();
        String subtipo = cbSubtipo.getValue();
        if (padre == null || subtipo == null) return;
        TipoCuenta tipo = padre.getTipo() != null ? padre.getTipo() : TipoCuenta.desdeCodigo(padre.getCodigo());
        actualizarVistaPrevia(padre, tipo, subtipo);
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
        Cuenta padre = cbCuentaPadre.getValue();
        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();
        String subtipo = cbSubtipo.getValue();
        NaturalezaCuenta naturaleza = cbNaturaleza.getValue();

        if (padre == null || codigo.isEmpty() || nombre.isEmpty() || subtipo == null || naturaleza == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Selecciona una cuenta superior, una clasificación para reportes y completa el nombre.",
                    ButtonType.OK).showAndWait();
            return;
        }

        TipoCuenta tipo = padre.getTipo() != null ? padre.getTipo() : TipoCuenta.desdeCodigo(padre.getCodigo());
        Cuenta nueva = new Cuenta(codigo, nombre, tipo, subtipo, padre.getNivel() + 1,
                naturaleza, padre.getCodigo(), chkMovimiento.isSelected());

        if (cuentaDAO.insertar(nueva)) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Cuenta agregada exitosamente al catálogo contable.", ButtonType.OK).showAndWait();
            txtNombre.clear();
            recargarCuentas();
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo guardar la cuenta. Verifica que el código y la cuenta superior sean válidos.",
                    ButtonType.OK).showAndWait();
        }
    }
    private void eliminarCuenta() {
        Cuenta sel = tblCuentas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Alert a = new Alert(Alert.AlertType.WARNING, "Seleccione una cuenta para eliminar.", ButtonType.OK);
            a.showAndWait();
            return;
        }

        if (cuentaDAO.tieneMovimientos(sel.getCodigo())) {
            Alert a = new Alert(Alert.AlertType.ERROR, "No se puede eliminar la cuenta " + sel.getCodigo()
                    + " porque ya tiene movimientos registrados en el Libro Diario.", ButtonType.OK);
            a.showAndWait();
            return;
        }

        if (cuentaDAO.tieneSubcuentas(sel.getCodigo())) {
            new Alert(Alert.AlertType.ERROR,
                    "No se puede eliminar la cuenta " + sel.getCodigo()
                            + " porque todavía tiene subcuentas. Elimina o reasigna primero esas cuentas.",
                    ButtonType.OK).showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Está seguro de eliminar la cuenta " + sel.getCodigo() + " - " + sel.getNombre() + "?", ButtonType.YES,
                ButtonType.NO);
        confirm.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.YES) {
                cuentaDAO.eliminar(sel.getCodigo());
                recargarCuentas();
            }
        });
    }
}
