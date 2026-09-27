package com.mycompany.programa_contable.ui.views;

import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.awt.Desktop;
import java.io.File;

/** Menú de exportación uniforme para reportes. */
final class ExportMenuFactory {
    private ExportMenuFactory() { }

    static MenuButton crear(Runnable pdf, Runnable excel) {
        MenuButton menu = new MenuButton("Exportar");
        menu.getStyleClass().addAll("btn-primary", "export-button");
        MenuItem pdfItem = new MenuItem("Exportar a PDF (.pdf)");
        pdfItem.getStyleClass().add("export-menu-item");
        pdfItem.setOnAction(e -> pdf.run());
        MenuItem excelItem = new MenuItem("Exportar a Excel (.xlsx)");
        excelItem.getStyleClass().add("export-menu-item");
        excelItem.setOnAction(e -> excel.run());
        menu.getItems().setAll(pdfItem, excelItem);
        return menu;
    }

    static void ofrecerAbrir(File archivo, String formato) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "Exportación completada. ¿Desea abrir el archivo ahora?", ButtonType.YES, ButtonType.NO);
        alerta.setTitle("Exportación a " + formato);
        alerta.setHeaderText(null);
        alerta.showAndWait().filter(ButtonType.YES::equals).ifPresent(respuesta -> {
            try {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(archivo);
                else new Alert(Alert.AlertType.WARNING, "El sistema no permite abrir archivos desde la aplicación.").showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "No se pudo abrir el archivo: " + ex.getMessage()).showAndWait();
            }
        });
    }
}
