package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.ConfiguracionDAO;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.*;

public class ConfiguracionView extends ScrollPane {

    private VBox mainContainer;
    private TextField txtNombreProducto;
    private TextField txtCostoCompra;
    private TextField txtPrecioVenta;
    private TextField txtTasaIva;
    private ComboBox<String> cmbRegimenIva;

    public ConfiguracionView() {
        setFitToWidth(true);
        getStyleClass().add("scroll-pane");
        setStyle("-fx-background-color: #f8fafc; -fx-background: #f8fafc;");

        mainContainer = new VBox(24);
        mainContainer.setPadding(new Insets(28, 32, 32, 32));
        mainContainer.setStyle("-fx-background-color: #f8fafc;");
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

        VBox titleBox = new VBox(3);
        Label lblTitulo = new Label("Configuración del Sistema");
        lblTitulo.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");
        Label lblSub = new Label("Parámetros del producto y tasa de IVA para los próximos asientos");
        lblSub.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(lblTitulo, lblSub);

        VBox cardForm = new VBox(20);
        cardForm.getStyleClass().add("card");
        cardForm.setMaxWidth(640);
        cardForm.setPadding(new Insets(28));

        // Campos de configuracion organizados en VBox con etiquetas
        Label lblNomProd = new Label("Nombre del Producto");
        lblNomProd.getStyleClass().add("form-label");
        txtNombreProducto = new TextField(nombre);
        txtNombreProducto.setPromptText("Ej. Lote de Queso");
        VBox rowNombre = new VBox(6, lblNomProd, txtNombreProducto);

        Label lblCosto = new Label("Costo Unitario de Compra ($)");
        lblCosto.getStyleClass().add("form-label");
        txtCostoCompra = new TextField(costo);
        txtCostoCompra.setPromptText("0.00");
        VBox rowCosto = new VBox(6, lblCosto, txtCostoCompra);

        Label lblPrecio = new Label("Precio Unitario de Venta ($)");
        lblPrecio.getStyleClass().add("form-label");
        txtPrecioVenta = new TextField(precio);
        txtPrecioVenta.setPromptText("0.00");
        VBox rowPrecio = new VBox(6, lblPrecio, txtPrecioVenta);

        Label lblIva = new Label("Modalidad de Cálculo de IVA");
        lblIva.getStyleClass().add("form-label");
        cmbRegimenIva = new ComboBox<>();
        cmbRegimenIva.getItems().addAll("IVA Incluido en el Monto Total", "Más IVA (Se calcula adicional)");
        String modalidadActual = new ConfiguracionDAO().obtenerModalidadIva();
        cmbRegimenIva.setValue(ConfiguracionDAO.IVA_MAS_IVA.equals(modalidadActual)
                ? "Más IVA (Se calcula adicional)"
                : "IVA Incluido en el Monto Total");
        cmbRegimenIva.setMaxWidth(Double.MAX_VALUE);
        VBox rowIva = new VBox(6, lblIva, cmbRegimenIva);

        double tasaIvaActual = new ConfiguracionDAO().obtenerTasaIva();
        Label lblTasaIva = new Label("Tasa de IVA (%)");
        lblTasaIva.getStyleClass().add("form-label");
        txtTasaIva = new TextField(String.valueOf(tasaIvaActual * 100));
        txtTasaIva.setPromptText("13");
        Label notaIva = new Label("Se aplica únicamente al agregar renglones de asientos nuevos. Los asientos históricos no se modifican.");
        notaIva.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        VBox rowTasaIva = new VBox(6, lblTasaIva, txtTasaIva, notaIva);

        Button btnGuardar = new Button("Guardar Cambios");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setMinWidth(200);
        btnGuardar.setOnAction(e -> {
            try {
                String nuevoNombre = txtNombreProducto.getText().trim();
                if (nuevoNombre.isEmpty()) {
                    throw new IllegalArgumentException("El nombre del producto no puede estar vacío.");
                }

                double nuevoCosto = Double.parseDouble(txtCostoCompra.getText().trim());
                double nuevoPrecio = Double.parseDouble(txtPrecioVenta.getText().trim());
                double nuevaTasaIva = Double.parseDouble(txtTasaIva.getText().trim().replace(',', '.')) / 100.0;

                if (nuevoCosto <= 0 || nuevoPrecio <= 0) {
                    throw new IllegalArgumentException("El costo y el precio de venta deben ser mayores a cero.");
                }
                if (nuevaTasaIva < 0 || nuevaTasaIva > 1) {
                    throw new IllegalArgumentException("La tasa de IVA debe estar entre 0 % y 100 %.");
                }

                String updateSql = "UPDATE productos SET nombre = ?, costo_compra = ?, precio_venta = ? WHERE id = 1";
                try (Connection conn = DatabaseManager.getInstance().getConnection();
                     PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, nuevoNombre);
                    ps.setDouble(2, nuevoCosto);
                    ps.setDouble(3, nuevoPrecio);
                    ps.executeUpdate();
                }
                new ConfiguracionDAO().guardarTasaIva(nuevaTasaIva);
                String modalidad = "Más IVA (Se calcula adicional)".equals(cmbRegimenIva.getValue())
                        ? ConfiguracionDAO.IVA_MAS_IVA
                        : ConfiguracionDAO.IVA_INCLUIDO;
                new ConfiguracionDAO().guardarModalidadIva(modalidad);

                Alert a = new Alert(Alert.AlertType.INFORMATION,
                        "Configuración actualizada. La tasa y modalidad se aplicarán solo a los próximos asientos.", ButtonType.OK);
                a.showAndWait();
            } catch (NumberFormatException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error de formato: Ingrese valores numéricos válidos para el costo y precio.");
                a.showAndWait();
            } catch (IllegalArgumentException ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Datos requeridos: " + ex.getMessage());
                a.showAndWait();
            } catch (Exception ex) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error al guardar: " + ex.getMessage());
                a.showAndWait();
            }
        });

        // Titulo de seccion de la tarjeta
        Label lblCardTitle = new Label("Parámetros del Producto");
        lblCardTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");

        cardForm.getChildren().addAll(lblCardTitle, new Separator(), rowNombre, rowCosto, rowPrecio, rowIva, rowTasaIva, new Separator(), btnGuardar);
        mainContainer.getChildren().addAll(titleBox, cardForm);
    }
}
