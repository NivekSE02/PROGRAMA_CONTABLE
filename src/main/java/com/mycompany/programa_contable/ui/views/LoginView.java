package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.model.Rol;
import com.mycompany.programa_contable.model.Usuario;
import com.mycompany.programa_contable.service.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class LoginView extends VBox {

    private final Consumer<Usuario> onLoginSuccess;

    public LoginView(Consumer<Usuario> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        setupUI();
    }

    private void setupUI() {
        setAlignment(Pos.CENTER);
        setSpacing(0);
        setStyle("-fx-background-color: #f8fafc;");

        // Card central
        VBox card = new VBox(24);
        card.setMaxWidth(420);
        card.setPadding(new Insets(40, 40, 36, 40));
        card.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 16px;" +
            "-fx-border-color: #e2e8f0;" +
            "-fx-border-width: 1px;" +
            "-fx-border-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 20, 0, 0, 8);"
        );
        card.setAlignment(Pos.CENTER_LEFT);

        // — Header institucional
        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);

        Label lblInst = new Label("UNIVERSIDAD CATÓLICA DE EL SALVADOR");
        lblInst.setStyle(
            "-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8; -fx-letter-spacing: 1.5px;"
        );

        // Logo / marca grande
        Label lblLogo = new Label("FinancePro");
        lblLogo.setStyle(
            "-fx-font-size: 30px; -fx-font-weight: 800; -fx-text-fill: #881337; -fx-letter-spacing: -1px;"
        );

        Label lblTitulo = new Label("Sistema Contable Automatizado");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #0f172a;");

        Label lblSub = new Label("Ciclo Contable Completo · Partida Doble Automática");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        headerBox.getChildren().addAll(lblInst, lblLogo, lblTitulo, lblSub);

        // — Separador visual
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #e2e8f0;");
        sep.setMaxWidth(Double.MAX_VALUE);

        // — Info demo
        VBox infoBox = new VBox(4);
        Label lblInfo = new Label("Acceso de demostración habilitado");
        lblInfo.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #475569;");
        Label lblInfoSub = new Label("No se requieren credenciales en este entorno de práctica.");
        lblInfoSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        infoBox.getChildren().addAll(lblInfo, lblInfoSub);

        // — Botón de acceso
        Button btnIngresar = new Button("Ingresar al Sistema");
        btnIngresar.getStyleClass().add("btn-primary");
        btnIngresar.setMaxWidth(Double.MAX_VALUE);
        btnIngresar.setPrefHeight(42);
        btnIngresar.setStyle("-fx-font-size: 14px;");
        btnIngresar.setOnAction(e -> {
            Usuario admin = new Usuario(1, "admin", "admin123", "Administrador", Rol.ADMINISTRADOR, "ACTIVO");
            SessionManager.getInstance().setUsuarioActual(admin);
            onLoginSuccess.accept(admin);
        });

        // — Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        Label lblFooter = new Label("Empresa Práctica S.A. de C.V. · 2026");
        lblFooter.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");
        footer.getChildren().add(lblFooter);

        card.getChildren().addAll(headerBox, sep, infoBox, btnIngresar, footer);

        getChildren().add(card);
    }
}
