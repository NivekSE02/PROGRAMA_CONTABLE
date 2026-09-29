package com.mycompany.programa_contable;

import com.mycompany.programa_contable.db.DatabaseManager;
import com.mycompany.programa_contable.dao.LibroDiarioDAO;
import com.mycompany.programa_contable.ui.views.MainLayoutView;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;

public class MainApp extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);
        this.primaryStage.setTitle("ContaNoPortable - Sistema Contable");
        URL iconUrl = getClass().getResource("/icono.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        try {
            DatabaseManager.getInstance().initDatabase();
            new LibroDiarioDAO().completarAsientosKardexExistentes();
        } catch (IllegalStateException e) {
            Alert error = new Alert(Alert.AlertType.ERROR,
                    "No se pudo iniciar ContaNoPortable porque no fue posible preparar su base de datos local.\n\n"
                            + e.getMessage());
            error.setTitle("Error al iniciar");
            error.setHeaderText(null);
            error.showAndWait();
            javafx.application.Platform.exit();
            return;
        }
        mostrarMain();

        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        stage.show();
    }

    private void mostrarMain() {
        MainLayoutView mainLayout = new MainLayoutView();
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
            System.err.println("[MainApp] No se encontró styles.css");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
