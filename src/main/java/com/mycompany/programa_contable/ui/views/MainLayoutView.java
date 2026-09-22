package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.db.DatabaseManager;
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
        // — Inicializar vistas
        dashboardView       = new DashboardView();
        libroDiarioView     = new LibroDiarioView();
        libroMayorView      = new LibroMayorView();
        balanzaView         = new BalanzaComprobacionView();
        balanceGeneralView  = new BalanceGeneralView();
        estadoResultadosView = new EstadoResultadosView();
        catalogoView        = new CatalogoCuentasView();
        kardexView          = new KardexView();
        configuracionView   = new ConfiguracionView();

        // ── TOPBAR — Marca y utilidades ────────────────────────────────────────
        VBox topHeader = new VBox(0);
        topHeader.getStyleClass().add("top-header");

        HBox topbar = new HBox(16);
        topbar.getStyleClass().add("topbar");
        topbar.setAlignment(Pos.CENTER_LEFT);
        topbar.setPadding(new Insets(16, 36, 10, 36));

        Label lblBrand = new Label("FinancePro");
        lblBrand.getStyleClass().add("brand-title");

        Label lblTopTitle = new Label("Sistema Contable Automatizado");
        lblTopTitle.getStyleClass().add("topbar-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Botón de demostración
        Button btnResetDemo = new Button("Datos Demo");
        btnResetDemo.getStyleClass().add("btn-secondary");
        btnResetDemo.setOnAction(e -> {
            Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Desea restablecer la base de datos a los valores de demostración originales?",
                ButtonType.YES, ButtonType.NO
            );
            confirm.setTitle("Restablecer Datos de Demostración");
            confirm.showAndWait().ifPresent(resp -> {
                if (resp == ButtonType.YES) {
                    DatabaseManager.getInstance().resetDatabase();
                    actualizarTodasLasVistas();
                    Alert a = new Alert(Alert.AlertType.INFORMATION,
                        "Base de datos restablecida exitosamente.", ButtonType.OK);
                    a.showAndWait();
                }
            });
        });

        Button btnSalir = new Button("Salir");
        btnSalir.getStyleClass().add("btn-logout");
        btnSalir.setOnAction(e -> { if (onLogout != null) onLogout.run(); });

        VBox brandBox = new VBox(2);
        brandBox.getChildren().addAll(lblBrand, lblTopTitle);

        topbar.getChildren().addAll(brandBox, spacer, btnResetDemo, btnSalir);


        // ── RIBBON — Barra de navegación ───────────────────────────────────────
        HBox ribbonBar = new HBox(0);
        ribbonBar.getStyleClass().add("ribbon-bar");
        ribbonBar.setAlignment(Pos.CENTER_LEFT);
        ribbonBar.setSpacing(24);

        Button btnNavDashboard = crearBotonNav("Dashboard", () -> {
            dashboardView.cargarDatos();
            mostrarVista(dashboardView);
        });

        Button btnNavDiario = crearBotonNav("Libro Diario", () -> {
            libroDiarioView.recargarHistorial();
            mostrarVista(libroDiarioView);
        });

        Button btnNavMayor = crearBotonNav("Libro Mayor", () -> {
            libroMayorView.recargarMayorizacion();
            mostrarVista(libroMayorView);
        });

        Button btnNavBalanza = crearBotonNav("Balanza", () -> {
            balanzaView.cargarDatos();
            mostrarVista(balanzaView);
        });

        Button btnNavKardex = crearBotonNav("Kárdex", () -> {
            kardexView.cargarDatos();
            mostrarVista(kardexView);
        });

        Button btnNavResultados = crearBotonNav("Est. Resultados", () -> {
            estadoResultadosView.cargarDatos();
            mostrarVista(estadoResultadosView);
        });

        Button btnNavBalance = crearBotonNav("Balance General", () -> {
            balanceGeneralView.cargarDatos();
            mostrarVista(balanceGeneralView);
        });

        Button btnNavCatalogo = crearBotonNav("Catálogo", () -> {
            catalogoView.recargarCuentas();
            mostrarVista(catalogoView);
        });

        Button btnNavConfig = crearBotonNav("Configuración", () -> mostrarVista(configuracionView));

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

        // — Área de contenido
        contentPane = new StackPane();
        contentPane.getStyleClass().add("main-content-pane");
        setCenter(contentPane);

        // Iniciar en Dashboard
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