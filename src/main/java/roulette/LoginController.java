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

import java.io.IOException;

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
        networkClient.setOnError(errorMsg -> javafx.application.Platform.runLater(() -> messageLabel.setText("Error: " + errorMsg)));

        new Thread(() -> {
            try {
                networkClient.connect("localhost", 12345);
            } catch (IOException e) {
                javafx.application.Platform.runLater(() -> messageLabel.setText("Connection error: " + e.getMessage()));
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
                openGameWindow(usernameField.getText());
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

    private void openGameWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/game_table.fxml")); // Изменено на game_table.fxml
            Parent root = loader.load();
            RouletteController controller = loader.getController();
            controller.initAfterLogin(networkClient, username);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600)); // Увеличена высота для ScrollPane
            stage.setTitle("Roulette - " + username);
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Failed to load game window: " + e.getMessage());
        }
    }
}
