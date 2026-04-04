package roulette;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import roulette.net.NetworkClient;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private NetworkClient networkClient;

    @FXML
    public void initialize() {
        networkClient = new NetworkClient();
        networkClient.setOnLoginResult(this::onLoginResult);
        networkClient.setOnRegisterResult(this::onRegisterResult);
        // Подключаемся к серверу при старте окна
        new Thread(() -> {
            try {
                networkClient.connect("localhost", 12345);
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> messageLabel.setText("Connection error"));
            }
        }).start();
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Enter username and password");
            return;
        }
        networkClient.login(username, password);
    }

    @FXML
    private void onRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Enter username and password");
            return;
        }
        networkClient.register(username, password);
    }

    private void onLoginResult(boolean success, String message) {
        javafx.application.Platform.runLater(() -> {
            if (success) {
                // Открыть игровой стол
                openGameWindow();
            } else {
                messageLabel.setText("Login failed: " + message);
            }
        });
    }

    private void onRegisterResult(boolean success, String message) {
        javafx.application.Platform.runLater(() -> {
            if (success) {
                messageLabel.setText("Registration successful! Please login.");
            } else {
                messageLabel.setText("Registration failed: " + message);
            }
        });
    }

    private void openGameWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/roulette.fxml"));
            Parent root = loader.load();
            RouletteController controller = loader.getController();
            controller.initAfterLogin(networkClient, usernameField.getText());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 500));
            stage.setTitle("Roulette - " + usernameField.getText());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}