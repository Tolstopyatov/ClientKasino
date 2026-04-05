package roulette;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import roulette.model.Bet;
import roulette.model.GameState;
import roulette.net.NetworkClient;
import roulette.ui.BetPanel;
import roulette.ui.TableCanvas;

import java.io.IOException;

public class RouletteController {
    @FXML private TableCanvas tableCanvas;
    @FXML private Label balanceLabel;
    @FXML private Label lastNumberLabel;
    @FXML private TextField betAmountField;
    @FXML private Button placeBetButton;
    @FXML private ScrollPane messageScrollPane;
    @FXML private VBox messageContentBox;
    @FXML private HBox chipPanelBox;

    private NetworkClient networkClient;
    private String playerName;
    private GameState currentState;
    private Bet pendingBet;          // ставка, собранная из drag&drop
    private int selectedChipValue;   // номинал перетаскиваемой фишки

    @FXML
    public void initialize() {
        balanceLabel.setText("Баланс: 0");
        lastNumberLabel.setText("Последнее: -");
        placeBetButton.setOnAction(e -> sendBet());

        createChipPanels();
        setupDragAndDrop();

        // Кнопка выхода (можно перенести в меню)
        Button logoutButton = new Button("Выйти");
        logoutButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        logoutButton.setOnAction(e -> logout());
        if (messageContentBox != null) {
            messageContentBox.getChildren().add(logoutButton);
        }
    }

    public void initAfterLogin(NetworkClient client, String name) {
        this.networkClient = client;
        this.playerName = name;

        networkClient.setOnGameStateUpdate(this::updateGameState);
        networkClient.setOnSpinResult(this::showSpinResult);
        networkClient.setOnRoundStart(this::onRoundStart);
        networkClient.setOnError(this::onError);
    }

    private void createChipPanels() {
        if (chipPanelBox == null) return;
        chipPanelBox.getChildren().clear();
        chipPanelBox.setSpacing(10);
        chipPanelBox.setStyle("-fx-background-color: #327c0d; -fx-padding: 10;");

        // Фишки для числовых ставок (номиналы)
        int[] values = {10, 50, 100, 500};
        Color[] colors = {Color.web("#ffea76"), Color.web("#ff7676"), Color.web("#7688ff"), Color.web("#525252")};
        for (int i = 0; i < values.length; i++) {
            BetPanel chip = new BetPanel(values[i], "number", 0, "" + values[i], colors[i]);
            chipPanelBox.getChildren().add(chip);
        }

        // Фишки для side-ставок (тоже с номиналом)
        BetPanel redChip = new BetPanel(10, "red", "red", "RED", Color.web("#E63946"));
        BetPanel blackChip = new BetPanel(10, "black", "black", "BLACK", Color.web("#666565"));
        BetPanel evenChip = new BetPanel(10, "even", "even", "EVEN", Color.web("#6ea270"));
        BetPanel oddChip = new BetPanel(10, "odd", "odd", "ODD", Color.web("#eab25f"));
        chipPanelBox.getChildren().addAll(redChip, blackChip, evenChip, oddChip);
    }

    private void setupDragAndDrop() {
        tableCanvas.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        tableCanvas.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                String[] parts = db.getString().split(":");
                if (parts.length >= 3) {
                    String betType = parts[0];
                    String valueStr = parts[1];
                    int chipValue = Integer.parseInt(parts[2]);
                    double x = event.getX();
                    double y = event.getY();

                    Object hit = tableCanvas.hitTest(x, y);
                    if (hit != null) {
                        if (betType.equals("number") && hit instanceof Integer) {
                            int number = (Integer) hit;
                            pendingBet = new Bet(playerName, "number", number, 0);
                            selectedChipValue = chipValue;
                            tableCanvas.setPendingBetFromDrag(betType, number, chipValue, playerName);
                            addMessage("Выбрана ставка на число " + number + " номиналом " + chipValue);
                        } else if ((betType.equals("red") || betType.equals("black") ||
                                    betType.equals("even") || betType.equals("odd")) && hit instanceof String) {
                            String sideType = (String) hit;
                            if (betType.equals(sideType)) {
                                pendingBet = new Bet(playerName, betType, 0, 0);
                                selectedChipValue = chipValue;
                                tableCanvas.setPendingBetFromDrag(betType, sideType, chipValue, playerName);
                                addMessage("Выбрана ставка на " + sideType.toUpperCase() + " номиналом " + chipValue);
                            } else {
                                addMessage("Перетащите фишку на соответствующее поле (" + betType + " -> " + sideType + ")");
                            }
                        } else {
                            addMessage("Некорректное место для ставки");
                        }
                    }
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void sendBet() {
        if (pendingBet == null) {
            addMessage("Сначала перетащите фишку на поле!");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(betAmountField.getText());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            addMessage("Введите корректную сумму ставки");
            return;
        }

        if (currentState != null && amount > currentState.getBalance()) {
            addMessage("Недостаточно средств! Ваш баланс: " + currentState.getBalance());
            return;
        }

        // Устанавливаем сумму в pendingBet
        pendingBet.setAmount(amount);
        // Отправляем на сервер
        networkClient.placeBet(pendingBet);
        addMessage("Ставка " + amount + " на " + betDescription(pendingBet) + " отправлена!");
        pendingBet = null;
        tableCanvas.clearPendingBet();
        betAmountField.clear();
    }

    private String betDescription(Bet bet) {
        if ("number".equals(bet.getType())) return "число " + bet.getValue();
        else return bet.getType().toUpperCase();
    }

    private void updateGameState(GameState state) {
        Platform.runLater(() -> {
            this.currentState = state;
            balanceLabel.setText("Баланс: " + state.getBalance());
            tableCanvas.updateBets(state.getAllBets(), playerName);
            placeBetButton.setDisable(!state.isBettingOpen());
            if (!state.isBettingOpen()) {
                addMessage("Ставки больше не принимаются – рулетка вращается!");
            }
        });
    }

    private void showSpinResult(int number) {
        Platform.runLater(() -> {
            lastNumberLabel.setText("Последнее: " + number);
            addMessage("Выпало число " + number);
            tableCanvas.spinWheel(number, () -> {
                addMessage("Раунд завершён. Результаты выплачены.");
                tableCanvas.updateBets(currentState.getAllBets(), playerName);
            });
        });
    }

    private void onRoundStart(String msg) {
        Platform.runLater(() -> addMessage("Новый раунд! Делайте ставки."));
    }

    private void onError(String error) {
        Platform.runLater(() -> addMessage("Ошибка: " + error));
    }

    private void addMessage(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: white; -fx-background-color: #5d5b5b; -fx-padding: 4 8; -fx-background-radius: 3;");
            messageContentBox.getChildren().add(label);
            messageScrollPane.setVvalue(1.0);
        });
    }

    private void logout() {
        networkClient.disconnect();
        Platform.exit();
    }

    @FXML
    private void openMainMenu() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_menu.fxml"));
        Parent root = loader.load();
        MainMenuController controller = loader.getController();
        controller.initData(networkClient, playerName);
        Stage stage = (Stage) balanceLabel.getScene().getWindow();
        stage.setScene(new Scene(root, 600, 400));
        stage.setTitle("Казино - " + playerName);
    }
}