package roulette;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import roulette.model.Bet;
import roulette.model.GameState;
import roulette.net.NetworkClient;
import roulette.ui.TableCanvas;

public class RouletteController {
    @FXML private TableCanvas tableCanvas;
    @FXML private Label balanceLabel;
    @FXML private Label lastNumberLabel;
    @FXML private TextField betAmountField;
    @FXML private Button placeBetButton;
    @FXML private VBox messageBox;

    private NetworkClient networkClient;
    private String playerName;
    private GameState currentState;

    // Этот метод вызывается из LoginController после успешного входа
    public void initAfterLogin(NetworkClient client, String name) {
        this.networkClient = client;
        this.playerName = name;

        // Устанавливаем колбэки
        networkClient.setOnGameStateUpdate(this::updateGameState);
        networkClient.setOnSpinResult(this::showSpinResult);
        networkClient.setOnRoundStart(this::onRoundStart);
        networkClient.setOnError(this::onError);

        // Запрашиваем начальное состояние (сервер сам пришлёт после логина)
        // Можно также добавить явный запрос, но сервер присылает update_state автоматически
    }

    @FXML
    public void initialize() {

        balanceLabel.setText("Balance: 0");
        lastNumberLabel.setText("Last: -");
        placeBetButton.setOnAction(e -> sendBet());

        // Добавим кнопку для выхода
        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> logout());
        // Можно добавить в верхнюю панель, но для простоты добавим в messageBox
        // В реальном приложении лучше разместить отдельно.
    }

    private void updateGameState(GameState state) {
        Platform.runLater(() -> {
            this.currentState = state;
            balanceLabel.setText("Balance: " + state.getBalance());
            tableCanvas.updateBets(state.getAllBets(), playerName);
            placeBetButton.setDisable(!state.isBettingOpen());
            if (!state.isBettingOpen()) {
                addMessage("Betting closed! Waiting for spin...");
            }
        });
    }

    private void showSpinResult(int number) {
        Platform.runLater(() -> {
            lastNumberLabel.setText("Last: " + number);
            addMessage("Wheel stopped at " + number);
            // Здесь можно добавить анимацию вращения колеса
        });
    }

    private void onRoundStart(String msg) {
        Platform.runLater(() -> addMessage("New round started! Place your bets."));
    }

    private void onError(String error) {
        Platform.runLater(() -> {
            addMessage("ERROR: " + error);
            // При критической ошибке можно закрыть приложение
        });
    }

    private void sendBet() {
        Bet pending = tableCanvas.getPendingBet();
        if (pending == null) {
            addMessage("Select a number on the table first.");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(betAmountField.getText());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            addMessage("Enter valid positive amount.");
            return;
        }

        // Проверка баланса (локальная)
        if (currentState != null && amount > currentState.getBalance()) {
            addMessage("Insufficient balance!");
            return;
        }

        pending.setAmount(amount);
        pending.setPlayerName(playerName);
        networkClient.placeBet(pending);
        tableCanvas.clearPendingBet();
        betAmountField.clear();
        addMessage("Bet placed on " + pending.getValue() + " - " + amount + " chips");
    }

    private void addMessage(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: white; -fx-background-color: #333; -fx-padding: 2 5;");
            messageBox.getChildren().add(label);
            // Автопрокрутка вниз
            messageBox.layout();
            messageBox.setScrollTop(Double.MAX_VALUE);
        });
    }

    private void logout() {
        networkClient.disconnect();
        Platform.exit();
    }
}