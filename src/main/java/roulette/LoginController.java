package roulette;

import java.io.IOException;

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
        networkClient.setOnError(errorMsg -> javafx.application.Platform.runLater(() -> messageLabel.setText("Ошибка: " + errorMsg)));

        new Thread(() -> {
            try {
                networkClient.connect("127.0.0.1", 12345);//"192.168.1.151"
            } catch (IOException e) {
                javafx.application.Platform.runLater(() -> messageLabel.setText("Ошибка соединения: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Введите имя и пароль");
            return;
        }
        networkClient.login(username, password);
    }

    @FXML
    private void onRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Введите имя и пароль");
            return;
        }
        networkClient.register(username, password);
    }

    private void onLoginResult(boolean success, String message) {
        javafx.application.Platform.runLater(() -> {
            if (success) {
                openGameWindow(usernameField.getText());
            } else {
                messageLabel.setText("Вход не удался: " + message);
            }
        });
    }

    private void onRegisterResult(boolean success, String message) {
        javafx.application.Platform.runLater(() -> {
            if (success) {
                messageLabel.setText("Регистрация случилась! Войдите.");
            } else {
                messageLabel.setText("Регистрация обломалась: " + message);
            }
        });
    }

    private void openGameWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_menu.fxml"));
            Parent root = loader.load();
            MainMenuController controller = loader.getController();
            controller.initData(networkClient, username);
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Казино - " + username);
        } catch (IOException e) {
            e.printStackTrace();
            messageLabel.setText("Ошибка загрузки меню: " + e.getMessage());
        }
    }
}
