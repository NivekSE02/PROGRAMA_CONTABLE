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

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
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

        // Campos de inicio de sesión
        Label lblUser = new Label("Usuario:");
        lblUser.getStyleClass().add("form-label");
        TextField txtUser = new TextField("admin");
        txtUser.setPromptText("Ej. admin, contador, auditor");

        Label lblPass = new Label("Contraseña:");
        lblPass.getStyleClass().add("form-label");
        PasswordField txtPass = new PasswordField();
        txtPass.setText("admin123");
        txtPass.setPromptText("Ingrese su contraseña");

        Button btnIngresar = new Button("Iniciar Sesión");
        btnIngresar.getStyleClass().add("btn-primary");
        btnIngresar.setMaxWidth(Double.MAX_VALUE);
        btnIngresar.setOnAction(e -> {
            String u = txtUser.getText().trim();
            String p = txtPass.getText().trim();
            Usuario user = usuarioDAO.autenticar(u, p);
            if (user != null) {
                SessionManager.getInstance().setUsuarioActual(user);
                onLoginSuccess.accept(user);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error de Acceso");
                alert.setHeaderText("Credenciales Incorrectas");
                alert.setContentText("El usuario o contraseña ingresados no son válidos.");
                alert.showAndWait();
            }
        });

        // Separador con texto
        Label lblRapido = new Label("Accesos Rápidos para Evaluación");
        lblRapido.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        // Botones de acceso rápido para la defensa de 5 minutos
        Button btnQuickAdmin = new Button("👤 Administrador (admin)");
        btnQuickAdmin.getStyleClass().add("btn-secondary");
        btnQuickAdmin.setMaxWidth(Double.MAX_VALUE);
        btnQuickAdmin.setOnAction(e -> {
            txtUser.setText("admin");
            txtPass.setText("admin123");
            btnIngresar.fire();
        });

        Button btnQuickConta = new Button("📘 Contador (contador)");
        btnQuickConta.getStyleClass().add("btn-secondary");
        btnQuickConta.setMaxWidth(Double.MAX_VALUE);
        btnQuickConta.setOnAction(e -> {
            txtUser.setText("contador");
            txtPass.setText("conta123");
            btnIngresar.fire();
        });

        Button btnQuickAudit = new Button("🔍 Auditor (auditor)");
        btnQuickAudit.getStyleClass().add("btn-secondary");
        btnQuickAudit.setMaxWidth(Double.MAX_VALUE);
        btnQuickAudit.setOnAction(e -> {
            txtUser.setText("auditor");
            txtPass.setText("audit123");
            btnIngresar.fire();
        });

        card.getChildren().addAll(
            headerBox,
            new Separator(),
            new VBox(4, lblUser, txtUser),
            new VBox(4, lblPass, txtPass),
            btnIngresar,
            new Separator(),
            lblRapido,
            btnQuickAdmin,
            btnQuickConta,
            btnQuickAudit
        );

        getChildren().add(card);
    }
}
