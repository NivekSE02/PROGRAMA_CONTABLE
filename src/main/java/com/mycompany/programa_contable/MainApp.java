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
        this.primaryStage.setTitle("UNICAES - Sistema Contable Automatizado (Módulo de Contabilidad)");

        // 1. Inicializar persistencia SQLite y scripts DDL/DML
        DatabaseManager.getInstance().initDatabase();

        // 2. Iniciar con la pantalla de inicio de sesión
        mostrarLogin();

        stage.setMinWidth(1150);
        stage.setMinHeight(750);
        stage.show();
    }

    public void mostrarLogin() {
        LoginView loginView = new LoginView(this::onLoginSuccess);
        Scene scene = new Scene(loginView, 1200, 800);
        aplicarEstilos(scene);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    private void onLoginSuccess(Usuario usuario) {
        MainLayoutView mainLayout = new MainLayoutView(this::mostrarLogin);
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
