package com.mycompany.programa_contable.ui.views;

import com.mycompany.programa_contable.dao.UsuarioDAO;
import com.mycompany.programa_contable.model.Rol;
import com.mycompany.programa_contable.model.Usuario;
import com.mycompany.programa_contable.service.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class LoginView extends VBox {

    // Authentication disabled – no UsuarioDAO
    private final Consumer<Usuario> onLoginSuccess;

    public LoginView(Consumer<Usuario> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        setupUI();
    }

    private void setupUI() {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40));
        setStyle("-fx-background-color: #0f172a;");

        VBox card = new VBox(18);
        card.setMaxWidth(420);
        card.setPadding(new Insets(32));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.25), 20, 0, 0, 8);");

        // Cabecera institucional
        Label lblInst = new Label("UNIVERSIDAD CATÓLICA DE EL SALVADOR");
        lblInst.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #6366f1; -fx-letter-spacing: 1px;");

        Label lblTitulo = new Label("Sistema Contable Automatizado");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label lblSub = new Label("Ciclo Contable Completo con Partida Doble");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        VBox headerBox = new VBox(4, lblInst, lblTitulo, lblSub);
        headerBox.setAlignment(Pos.CENTER);

        // No credential fields – auto login

        Button btnIngresar = new Button("Iniciar Sesión");
        btnIngresar.getStyleClass().add("btn-primary");
        btnIngresar.setMaxWidth(Double.MAX_VALUE);
        btnIngresar.setOnAction(e -> {
            // Auto‑login as admin
            Usuario admin = new Usuario(1, "admin", "admin123", "Administrador", com.mycompany.programa_contable.model.Rol.ADMINISTRADOR, "ACTIVO");
            SessionManager.getInstance().setUsuarioActual(admin);
            onLoginSuccess.accept(admin);
        });

        // Only login button is needed
        card.getChildren().addAll(
            headerBox,
            btnIngresar
        );

        getChildren().add(card);
    }
}
