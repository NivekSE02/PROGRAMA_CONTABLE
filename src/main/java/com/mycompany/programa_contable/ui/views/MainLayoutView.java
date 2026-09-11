package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.Usuario;
import com.mycompany.programa_contable.service.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class MainLayoutView extends BorderPane {

    private final Runnable onLogout;

    private StackPane contentPane;
    private DashboardView dashboardView;
    private LibroDiarioView libroDiarioView;
    private LibroMayorView libroMayorView;
    private BalanzaComprobacionView balanzaView;
    private BalanceGeneralView balanceGeneralView;
    private EstadoResultadosView estadoResultadosView;
    private CatalogoCuentasView catalogoView;

    private Button btnActive;

    public MainLayoutView(Runnable onLogout) {
        this.onLogout = onLogout;
        setupUI();
    }

    private void setupUI() {
        // Inicializar Vistas
        dashboardView = new DashboardView();
        libroDiarioView = new LibroDiarioView();
        libroMayorView = new LibroMayorView();
        balanzaView = new BalanzaComprobacionView();
        balanceGeneralView = new BalanceGeneralView();
        estadoResultadosView = new EstadoResultadosView();
        catalogoView = new CatalogoCuentasView();

        // 1. Barra Superior (Topbar)
        HBox topbar = new HBox(16);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);

        Label lblUni = new Label("🏛️ UNICAES");
        lblUni.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #6366f1;");

        Label lblTopTitle = new Label("SISTEMA CONTABLE AUTOMATIZADO - CICLO COMPLETO");
        lblTopTitle.getStyleClass().add("topbar-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Chip de usuario logueado
        Usuario u = SessionManager.getInstance().getUsuarioActual();
        HBox userChip = new HBox(8);
        userChip.getStyleClass().add("user-chip");
        Label lblUser = new Label("👤 " + (u != null ? u.getNombreCompleto() : "Invitado"));
        lblUser.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label lblRol = new Label(u != null ? u.getRol().getEtiqueta() : "");
        lblRol.getStyleClass().add("badge-rol");
        userChip.getChildren().addAll(lblUser, lblRol);

        // Botón de reinicio de demostración
        Button btnResetDemo = new Button("🔄 Datos de Demostración");
        btnResetDemo.getStyleClass().add("btn-secondary");
        btnResetDemo.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "¿Desea restablecer la base de datos a los valores de demostración originales?", ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Restablecer Datos");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.YES) {
                    DatabaseManager.getInstance().resetDatabase();
                    actualizarTodasLasVistas();
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Base de datos restablecida exitosamente con el catálogo y asientos de prueba.", ButtonType.OK);
                    a.showAndWait();
                }
            });
        });

        Button btnSalir = new Button("🚪 Salir");
        btnSalir.getStyleClass().add("btn-danger");
        btnSalir.setOnAction(e -> {
            SessionManager.getInstance().cerrarSesion();
            if (onLogout != null) onLogout.run();
        });

        topbar.getChildren().addAll(lblUni, new Label("|"), lblTopTitle, spacer, userChip, btnResetDemo, btnSalir);
        setTop(topbar);

        // 2. Barra Lateral de Navegación (Sidebar)
        VBox sidebar = new VBox(6);
        sidebar.getStyleClass().add("sidebar");

        VBox sideHeader = new VBox(4);
        sideHeader.getStyleClass().add("sidebar-title-container");
        Label lblBrand = new Label("Módulo Contable");
        lblBrand.getStyleClass().add("sidebar-app-title");
        Label lblBrandSub = new Label("Universidad Católica de El Salvador");
        lblBrandSub.getStyleClass().add("sidebar-app-subtitle");
        sideHeader.getChildren().addAll(lblBrand, lblBrandSub);

        Button btnNavDashboard = crearBotonNav("📊 Dashboard General", () -> {
            dashboardView.cargarDatos();
            mostrarVista(dashboardView);
        });

        Button btnNavDiario = crearBotonNav("📝 Libro Diario (Asientos)", () -> {
            libroDiarioView.recargarHistorial();
            mostrarVista(libroDiarioView);
        });

        Button btnNavMayor = crearBotonNav("⚖️ Libro Mayor (Cuentas T)", () -> {
            libroMayorView.recargarMayorizacion();
            mostrarVista(libroMayorView);
        });

        Button btnNavBalanza = crearBotonNav("📑 Balanza de Comprobación", () -> {
            balanzaView.cargarDatos();
            mostrarVista(balanzaView);
        });

        Button btnNavBalance = crearBotonNav("🏛️ Balance General (1=2+3)", () -> {
            balanceGeneralView.cargarDatos();
            mostrarVista(balanceGeneralView);
        });

        Button btnNavResultados = crearBotonNav("📈 Estado de Resultados (5-4)", () -> {
            estadoResultadosView.cargarDatos();
            mostrarVista(estadoResultadosView);
        });

        Button btnNavCatalogo = crearBotonNav("📚 Catálogo de Cuentas", () -> {
            catalogoView.recargarCuentas();
            mostrarVista(catalogoView);
        });

        sidebar.getChildren().addAll(
            sideHeader,
            btnNavDashboard,
            btnNavDiario,
            btnNavMayor,
            btnNavBalanza,
            btnNavBalance,
            btnNavResultados,
            btnNavCatalogo
        );
        setLeft(sidebar);

        // 3. Contenedor Central
        contentPane = new StackPane();
        contentPane.setStyle("-fx-background-color: #f8fafc;");
        setCenter(contentPane);

        // Iniciar en el Dashboard
        btnNavDashboard.fire();
    }

    private Button crearBotonNav(String texto, Runnable accion) {
        Button btn = new Button(texto);
        btn.getStyleClass().add("nav-button");
        btn.setOnAction(e -> {
            if (btnActive != null) {
                btnActive.getStyleClass().remove("nav-button-active");
            }
            btn.getStyleClass().add("nav-button-active");
            btnActive = btn;
            accion.run();
        });
        return btn;
    }

    private void mostrarVista(Node view) {
        contentPane.getChildren().setAll(view);
    }

    public void actualizarTodasLasVistas() {
        dashboardView.cargarDatos();
        libroDiarioView.recargarHistorial();
        libroMayorView.recargarMayorizacion();
        balanzaView.cargarDatos();
        balanceGeneralView.cargarDatos();
        estadoResultadosView.cargarDatos();
        catalogoView.recargarCuentas();
    }
}
