package com.mycompany.programa_contable;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.model.Usuario;
import com.mycompany.programa_contable.ui.views.LoginView;
import com.mycompany.programa_contable.ui.views.MainLayoutView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("FINANCE PRO - Sistema Contable");

        // 1. Inicializar persistencia SQLite y scripts DDL/DML
        DatabaseManager.getInstance().initDatabase();

        // 2. Iniciar directamente en el MainLayout
        // SessionManager ya tiene un usuario mock (administrador) por defecto.
        mostrarMain();

        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        stage.show();
    }

    private void mostrarMain() {
        MainLayoutView mainLayout = new MainLayoutView(() -> {
            // El logout simplemente cerrará la aplicación ahora que no hay login
            System.exit(0);
        });
        Scene scene = new Scene(mainLayout, 1280, 840);
        aplicarEstilos(scene);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    private void aplicarEstilos(Scene scene) {
        URL cssUrl = getClass().getResource("/com/mycompany/programa_contable/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("[MainApp] No se encontró la hoja de estilos styles.css");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
