package roulette;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
    @FXML private ScrollPane messageScrollPane; // Изменено на ScrollPane
    @FXML private VBox messageContentBox; // Добавлен VBox внутри ScrollPane

    private NetworkClient networkClient;
    private String playerName;
    private GameState currentState;

    public void initAfterLogin(NetworkClient client, String name) {
        this.networkClient = client;
        this.playerName = name;

        networkClient.setOnGameStateUpdate(this::updateGameState);
        networkClient.setOnSpinResult(this::showSpinResult);
        networkClient.setOnRoundStart(this::onRoundStart);
        networkClient.setOnError(this::onError);
    }

    @FXML
    public void initialize() {
        balanceLabel.setText("Balance: 0");
        lastNumberLabel.setText("Last: -");
        placeBetButton.setOnAction(e -> sendBet());

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(e -> logout());
        if (messageContentBox != null) { // Добавляем кнопку в messageContentBox
            messageContentBox.getChildren().add(logoutButton);
        }
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
        });
    }

    private void onRoundStart(String msg) {
        Platform.runLater(() -> addMessage("New round started! Place your bets."));
    }

    private void onError(String error) {
        Platform.runLater(() -> {
            addMessage("ERROR: " + error);
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
            messageContentBox.getChildren().add(label);
            messageScrollPane.setVvalue(1.0); // Автопрокрутка вниз
        });
    }

    private void logout() {
        networkClient.disconnect();
        Platform.exit();
    }
}
