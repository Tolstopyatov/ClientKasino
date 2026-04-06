package roulette;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import roulette.model.Bet;
import roulette.model.GameState;
import roulette.net.NetworkClient;
import roulette.ui.BetPanel;
import roulette.ui.TableCanvas;

import java.io.IOException;
import java.util.List;

public class RouletteController {
    @FXML private TableCanvas tableCanvas;
    @FXML private Label balanceLabel;
    @FXML private Label lastNumberLabel;
    @FXML private ScrollPane messageScrollPane;
    @FXML private VBox messageContentBox;
    @FXML private HBox chipPanelBox;
    @FXML private TextField chatInputField;

    private NetworkClient networkClient;
    private String playerName;
    private GameState currentState;
    private Bet pendingBet;          // ставка, собранная из drag&drop
    private int selectedChipValue;   // номинал перетаскиваемой фишки

    public void initAfterLogin(NetworkClient client, String name) {
        this.networkClient = client;
        this.playerName = name;

        networkClient.setOnGameStateUpdate(this::updateGameState);
        networkClient.setOnSpinResult(this::showSpinResult);
        networkClient.setOnRoundStart(this::onRoundStart);
        networkClient.setOnError(this::onError);
        networkClient.setOnChatMessage(this::addChatMessage);

        // Запрашиваем начальное состояние игры
        // networkClient.requestGameState(); // Если есть такой метод на сервере
    }

    @FXML
    public void initialize() {
        balanceLabel.setText("Баланс: 0");
        lastNumberLabel.setText("Последнее: -");

        createChipPanels();
        setupDragAndDrop();

        // Убедимся, что TableCanvas перерисовывается при изменении размеров
        tableCanvas.widthProperty().addListener((obs, oldVal, newVal) -> tableCanvas.redraw());
        tableCanvas.heightProperty().addListener((obs, oldVal, newVal) -> tableCanvas.redraw());
    }

    private void createChipPanels() {
        if (chipPanelBox == null) return;
        chipPanelBox.getChildren().clear();
        chipPanelBox.setSpacing(10);
        chipPanelBox.setStyle("-fx-background-color: #E9ECEF; -fx-padding: 10; -fx-background-radius: 5;");

        // Фишки для числовых ставок (номиналы)
        int[] values = {10, 50, 100, 500};
        Color[] colors = {Color.web("#FFD700"), Color.web("#FF6347"), Color.web("#6A5ACD"), Color.web("#4682B4")};
        for (int i = 0; i < values.length; i++) {
            BetPanel chip = new BetPanel(values[i], "number", colors[i]);
            chipPanelBox.getChildren().add(chip);
        }

        // Фишки для side-ставок
        BetPanel redChip = new BetPanel(10, "red", Color.web("#E63946"));
        BetPanel blackChip = new BetPanel(10, "black", Color.web("#343A40"));
        BetPanel evenChip = new BetPanel(10, "even", Color.web("#28A745"));
        BetPanel oddChip = new BetPanel(10, "odd", Color.web("#FFC107"));

        chipPanelBox.getChildren().addAll(redChip, blackChip, evenChip, oddChip);
    }

    private void setupDragAndDrop() {
        // Обработка начала перетаскивания с фишки
        chipPanelBox.getChildren().forEach(node -> {
            if (node instanceof BetPanel) {
                BetPanel betPanel = (BetPanel) node;
                betPanel.setOnDragDetected(event -> {
                    Dragboard db = betPanel.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    // Передаем тип ставки, значение (если есть) и номинал фишки
                    content.putString(betPanel.getBetType() + ":" + betPanel.getChipValue());
                    db.setContent(content);
                    event.consume();
                });
            }
        });

        // Обработка перетаскивания над TableCanvas
        tableCanvas.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // Обработка отпускания фишки на TableCanvas
        tableCanvas.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                String[] parts = db.getString().split(":");
                if (parts.length >= 2) {
                    String betTypeStr = parts[0];
                    int chipValue = Integer.parseInt(parts[1]);

                    double x = event.getX();
                    double y = event.getY();

                    Object hit = tableCanvas.hitTest(x, y);
                    if (hit != null) {
                        if (betTypeStr.equals("number") && hit instanceof Integer) {
                            int number = (Integer) hit;
                            pendingBet = new Bet(playerName, "number", number, chipValue);
                            addMessage("Выбрана ставка на число " + number + " номиналом " + chipValue);
                        } else if ((betTypeStr.equals("red") || betTypeStr.equals("black") ||
                                    betTypeStr.equals("even") || betTypeStr.equals("odd")) && hit instanceof String) {
                            String sideType = (String) hit;
                            if (betTypeStr.equals(sideType)) {
                                pendingBet = new Bet(playerName, betTypeStr, 0, chipValue);
                                addMessage("Выбрана ставка на " + sideType.toUpperCase() + " номиналом " + chipValue);
                            } else {
                                addMessage("Перетащите фишку на соответствующее поле (" + betTypeStr + " -> " + sideType + ")");
                            }
                        } else {
                            addMessage("Некорректное место для ставки");
                        }

                        if (pendingBet != null) {
                            networkClient.placeBet(pendingBet);
                            addMessage("Ставка " + chipValue + " на " + betDescription(pendingBet) + " отправлена!");
                            pendingBet = null;
                        }
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
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
            // Кнопка подтверждения ставки больше не нужна, ставки отправляются сразу
            // placeBetButton.setDisable(!state.isBettingOpen());
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
        Platform.runLater(() -> {
            addMessage("Ошибка: " + error);
        });
    }

    private void addMessage(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: #343A40; -fx-background-color: #E9ECEF; -fx-padding: 4 8; -fx-background-radius: 3;");
            messageContentBox.getChildren().add(label);
            messageScrollPane.setVvalue(1.0);
        });
    }

    private void addChatMessage(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: #495057; -fx-background-color: #F8F9FA; -fx-padding: 4 8; -fx-background-radius: 3;");
            messageContentBox.getChildren().add(label);
            messageScrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void sendChatMessage() {
        String message = chatInputField.getText();
        if (message != null && !message.trim().isEmpty()) {
            networkClient.sendChatMessage(message);
            chatInputField.clear();
        }
    }

    @FXML
    private void openMainMenu() throws IOException {
        networkClient.disconnect(); // Отключаемся от сервера рулетки
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_menu.fxml"));
        Parent root = loader.load();
        MainMenuController controller = loader.getController();
        // Передаем playerName, но не networkClient, так как для меню он не нужен
        // Если нужно переподключаться к серверу при возвращении в меню, то логика сложнее
        controller.initData(null, playerName); // networkClient = null, так как мы отключились
        Stage stage = (Stage) balanceLabel.getScene().getWindow();
        stage.setScene(new Scene(root, 600, 400));
        stage.setTitle("Казино - " + playerName);
    }
}
