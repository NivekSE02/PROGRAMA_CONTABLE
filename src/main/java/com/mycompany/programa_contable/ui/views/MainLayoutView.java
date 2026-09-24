package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.db.DatabaseManager;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.stage.Screen;

public class MainLayoutView extends BorderPane {

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

    public MainLayoutView() {
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

        ImageView logo = new ImageView(new Image(
            getClass().getResourceAsStream("/logo-grande.png")
        ));
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setFitHeight(64);

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

        Button btnMinimizar = new Button("—");
        Button btnMaximizar = new Button("□");
        Button btnCerrar = new Button("×");
        btnMinimizar.getStyleClass().add("window-control");
        btnMaximizar.getStyleClass().add("window-control");
        btnCerrar.getStyleClass().addAll("window-control", "window-close");
        btnMinimizar.setOnAction(e -> obtenerStage().ifPresent(s -> s.setIconified(true)));
        final boolean[] ventanaExpandida = {false};
        final double[] tamanoOriginal = new double[4];
        btnMaximizar.setOnAction(e -> obtenerStage().ifPresent(s -> {
            if (!ventanaExpandida[0]) {
                tamanoOriginal[0] = s.getX();
                tamanoOriginal[1] = s.getY();
                tamanoOriginal[2] = s.getWidth();
                tamanoOriginal[3] = s.getHeight();
                javafx.geometry.Rectangle2D area = Screen.getScreensForRectangle(
                    s.getX(), s.getY(), s.getWidth(), s.getHeight()
                ).stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
                s.setX(area.getMinX());
                s.setY(area.getMinY());
                s.setWidth(area.getWidth());
                s.setHeight(area.getHeight());
                ventanaExpandida[0] = true;
            } else {
                s.setX(tamanoOriginal[0]);
                s.setY(tamanoOriginal[1]);
                s.setWidth(tamanoOriginal[2]);
                s.setHeight(tamanoOriginal[3]);
                ventanaExpandida[0] = false;
            }
        }));
        btnCerrar.setOnAction(e -> obtenerStage().ifPresent(Stage::close));
        HBox controlesVentana = new HBox(2, btnMinimizar, btnMaximizar, btnCerrar);
        controlesVentana.setAlignment(Pos.CENTER);

        topbar.getChildren().addAll(logo, spacer, btnResetDemo, controlesVentana);
        final double[] posicionVentana = new double[2];
        topbar.setOnMousePressed(e -> {
            if (e.getTarget() instanceof Button) return;
            obtenerStage().ifPresent(s -> {
                posicionVentana[0] = e.getScreenX() - s.getX();
                posicionVentana[1] = e.getScreenY() - s.getY();
            });
        });
        topbar.setOnMouseDragged(e -> {
            if (e.getTarget() instanceof Button) return;
            obtenerStage().ifPresent(s -> {
            if (!ventanaExpandida[0]) {
                s.setX(e.getScreenX() - posicionVentana[0]);
                s.setY(e.getScreenY() - posicionVentana[1]);
            }
            });
        });


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

    private java.util.Optional<Stage> obtenerStage() {
        if (getScene() != null && getScene().getWindow() instanceof Stage stage) {
            return java.util.Optional.of(stage);
        }
        return java.util.Optional.empty();
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
        view.setOpacity(0);
        FadeTransition entrada = new FadeTransition(Duration.millis(180), view);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.play();
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
