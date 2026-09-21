package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.db.DatabaseManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.*;

public class ConfiguracionView extends ScrollPane {

    private VBox mainContainer;
    private TextField txtNombreProducto;
    private TextField txtCostoCompra;
    private TextField txtPrecioVenta;
    private ComboBox<String> cmbRegimenIva;

    public ConfiguracionView() {
        setFitToWidth(true);
        setStyle("-fx-background-color: transparent;");

        mainContainer = new VBox(20);
        mainContainer.setPadding(new Insets(24));
        setContent(mainContainer);

        cargarUI();
    }

    private void cargarUI() {
        mainContainer.getChildren().clear();

        // Inicializamos vacíos (sin valores quemados)
        String nombre = "";
        String costo = "";
        String precio = "";

        // Consulta directa a la base de datos
        String sql = "SELECT nombre, costo_compra, precio_venta FROM productos WHERE id = 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                nombre = rs.getString("nombre") != null ? rs.getString("nombre") : "";
                double cVal = rs.getDouble("costo_compra");
                double pVal = rs.getDouble("precio_venta");
                costo = cVal > 0 ? String.valueOf(cVal) : "";
                precio = pVal > 0 ? String.valueOf(pVal) : "";
            }
        } catch (Exception e) {
            System.err.println("[ConfiguracionView] Error al consultar la BD: " + e.getMessage());
        }

        VBox titleBox = new VBox(4);
        Label lblInst = new Label("UNIVERSIDAD CATÓLICA DE EL SALVADOR - EMPRESA PRÁCTICA S.A. DE C.V.");
        lblInst.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");
        Label lblTitulo = new Label("CONFIGURACIÓN Y GENERALIDADES DEL SISTEMA");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label lblSub = new Label("Parámetros globales del producto y política de IVA (13% El Salvador).");
        titleBox.getChildren().addAll(lblInst, lblTitulo, lblSub);

        VBox cardForm = new VBox(16);
        cardForm.getStyleClass().add("card");
        cardForm.setMaxWidth(600);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        // Campos obligatorios con textos de ayuda (prompt text) si están vacíos
        txtNombreProducto = new TextField(nombre);
        txtNombreProducto.setPromptText("Ej. Lote de Queso");

        txtCostoCompra = new TextField(costo);
        txtCostoCompra.setPromptText("0.00");

        txtPrecioVenta = new TextField(precio);
        txtPrecioVenta.setPromptText("0.00");

        cmbRegimenIva = new ComboBox<>();
        cmbRegimenIva.getItems().addAll("IVA Incluido en el Monto Total", "Más IVA (Se calcula adicional)");
        cmbRegimenIva.setValue("IVA Incluido en el Monto Total");
        cmbRegimenIva.setPrefWidth(280);

        grid.addRow(0, new Label("Nombre del Producto:"), txtNombreProducto);
        grid.addRow(1, new Label("Costo Unitario de Compra ($):"), txtCostoCompra);
        grid.addRow(2, new Label("Precio Unitario de Venta ($):"), txtPrecioVenta);
        grid.addRow(3, new Label("Modalidad de Cálculo de IVA:"), cmbRegimenIva);

        Button btnGuardar = new Button("💾 Guardar Cambios");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setOnAction(e -> {
            try {
                String nuevoNombre = txtNombreProducto.getText().trim();
                if (nuevoNombre.isEmpty()) {
                    throw new IllegalArgumentException("El nombre del producto no puede estar vacío.");
                }

                double nuevoCosto = Double.parseDouble(txtCostoCompra.getText().trim());
                double nuevoPrecio = Double.parseDouble(txtPrecioVenta.getText().trim());

                if (nuevoCosto <= 0 || nuevoPrecio <= 0) {
                    throw new IllegalArgumentException("El costo y el precio de venta deben ser mayores a cero.");
                }

                String updateSql = "UPDATE productos SET nombre = ?, costo_compra = ?, precio_venta = ? WHERE id = 1";
                try (Connection conn = DatabaseManager.getInstance().getConnection();
                     PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, nuevoNombre);
                    ps.setDouble(2, nuevoCosto);
                    ps.setDouble(3, nuevoPrecio);
                    ps.executeUpdate();
                }

                Alert a = new Alert(Alert.AlertType.INFORMATION, "¡Configuración actualizada en la base de datos con éxito!", ButtonType.OK);
                a.showAndWait();
            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error de formato: Ingrese valores numéricos válidos para el costo y precio.");
                a.showAndWait();
            } catch (IllegalArgumentException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Datos requeridos: " + ex.getMessage());
                a.showAndWait();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error al guardar en la base de datos: " + ex.getMessage());
                a.showAndWait();
            }
        });

        cardForm.getChildren().addAll(new Label("📦 Parámetros del Producto Único"), grid, new Separator(), btnGuardar);
        mainContainer.getChildren().addAll(titleBox, cardForm);
    }
}