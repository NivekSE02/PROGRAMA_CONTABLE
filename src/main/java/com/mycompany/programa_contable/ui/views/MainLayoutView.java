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
    private KardexView kardexView;
    private ConfiguracionView configuracionView;

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
        kardexView = new KardexView();
        configuracionView = new ConfiguracionView();

        // 1. Barra Superior Completa (Top Ribbon)
        VBox topHeader = new VBox();
        topHeader.getStyleClass().add("top-header");

        // Fila 1: Logo, Título y Utilidades
        HBox topbar = new HBox(16);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);

        Label lblBrand = new Label("FINANCE PRO");
        lblBrand.getStyleClass().add("brand-title");

        Label lblTopTitle = new Label("- Enterprise Accounting Edition");
        lblTopTitle.getStyleClass().add("topbar-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

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

        Usuario u = SessionManager.getInstance().getUsuarioActual();
        HBox userChip = new HBox(8);
        userChip.getStyleClass().add("user-chip");
        Label lblUser = new Label((u != null ? u.getNombreCompleto() : "Administrador"));
        lblUser.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
        userChip.getChildren().add(lblUser);

        Button btnSalir = new Button("SALIR");
        btnSalir.getStyleClass().add("btn-logout");
        btnSalir.setOnAction(e -> {
            if (onLogout != null) onLogout.run();
        });

        topbar.getChildren().addAll(lblBrand, lblTopTitle, spacer, btnResetDemo, userChip, btnSalir);

        HBox ribbonBar = new HBox(8);
        ribbonBar.getStyleClass().add("ribbon-bar");
        ribbonBar.setAlignment(Pos.CENTER_LEFT);

        Button btnNavDashboard = crearBotonNav("DASHBOARD", () -> {
            dashboardView.cargarDatos();
            mostrarVista(dashboardView);
        });

        Button btnNavDiario = crearBotonNav("LIBRO DIARIO", () -> {
            libroDiarioView.recargarHistorial();
            mostrarVista(libroDiarioView);
        });

        Button btnNavMayor = crearBotonNav("LIBRO MAYOR", () -> {
            libroMayorView.recargarMayorizacion();
            mostrarVista(libroMayorView);
        });

        Button btnNavBalanza = crearBotonNav("BALANZA COMPROBACIÓN", () -> {
            balanzaView.cargarDatos();
            mostrarVista(balanzaView);
        });

        Button btnNavBalance = crearBotonNav("BALANCE GENERAL", () -> {
            balanceGeneralView.cargarDatos();
            mostrarVista(balanceGeneralView);
        });

        Button btnNavResultados = crearBotonNav("ESTADO RESULTADOS", () -> {
            estadoResultadosView.cargarDatos();
            mostrarVista(estadoResultadosView);
        });

        Button btnNavKardex = crearBotonNav("KÁRDEX", () -> {
            kardexView.cargarDatos();
            mostrarVista(kardexView);
        });

        Button btnNavCatalogo = crearBotonNav("CATÁLOGO", () -> {
            catalogoView.recargarCuentas();
            mostrarVista(catalogoView);
        });

        Button btnNavConfig = crearBotonNav("CONFIGURACIÓN", () -> {
            mostrarVista(configuracionView);
        });
        
        ribbonBar.getChildren().addAll(
            btnNavDashboard,
            btnNavDiario,
            btnNavMayor,
            btnNavBalanza,
            btnNavKardex, 
            btnNavResultados,
            btnNavBalance,
            btnNavCatalogo,
            btnNavConfig
        );

        topHeader.getChildren().addAll(topbar, ribbonBar);
        setTop(topHeader);

        // 3. Contenedor Central
        contentPane = new StackPane();
        contentPane.getStyleClass().add("main-content-pane");
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
        kardexView.cargarDatos();
    }
}