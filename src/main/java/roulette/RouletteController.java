package roulette;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    @FXML private HBox chipPanelBox;
    @FXML private TextField chatInputField;
    @FXML private VBox messageContentBox;
    @FXML private ScrollPane messageScrollPane;

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
        networkClient.setOnChatMessage(this::addChatMessage);
    }

    @FXML
    public void initialize() {
        balanceLabel.setText("Баланс: Загрузка...");
        lastNumberLabel.setText("Последнее: -");
        createChipPanels();
        setupDragAndDrop();
        tableCanvas.widthProperty().addListener(obs -> tableCanvas.redraw());
        tableCanvas.heightProperty().addListener(obs -> tableCanvas.redraw());
    }

    private void createChipPanels() {
        if (chipPanelBox == null) return;
        chipPanelBox.getChildren().clear();
        chipPanelBox.setSpacing(10);
        int[] values = {10, 50, 100, 500};
        javafx.scene.paint.Color[] colors = {
            javafx.scene.paint.Color.web("#FFD700"),
            javafx.scene.paint.Color.web("#FF6347"),
            javafx.scene.paint.Color.web("#6A5ACD"),
            javafx.scene.paint.Color.web("#4682B4")
        };
        for (int i = 0; i < values.length; i++) {
            chipPanelBox.getChildren().add(new BetPanel(values[i], colors[i]));
        }
    }

    private void setupDragAndDrop() {
        chipPanelBox.getChildren().forEach(node -> {
            if (node instanceof BetPanel) {
                BetPanel bp = (BetPanel) node;
                bp.setOnDragDetected(event -> {
                    Dragboard db = bp.startDragAndDrop(TransferMode.COPY);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(String.valueOf(bp.getChipValue()));
                    db.setContent(content);
                    event.consume();
                });
            }
        });

        tableCanvas.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });

        tableCanvas.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                int chipValue;
                try {
                    chipValue = Integer.parseInt(db.getString());
                } catch (NumberFormatException e) {
                    return;
                }

                double x = event.getX();
                double y = event.getY();
                Object hit = tableCanvas.hitTest(x, y);
                if (hit != null) {
                    Bet bet = null;
                    if (hit instanceof Integer number) {
                        bet = new Bet(playerName, "number", number, chipValue);
                    } else if (hit instanceof String type) {
                        if ("red".equals(type) || "black".equals(type) || "even".equals(type) || "odd".equals(type)) {
                            bet = new Bet(playerName, type, 0, chipValue);
                        }
                    }
                    if (bet != null) {
                        networkClient.placeBet(bet);
                        addChatMessage("Ставка " + chipValue + " на " + betDescription(bet) + " отправлена");
                    } else {
                        addChatMessage("Некорректное место для ставки");
                    }
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private String betDescription(Bet bet) {
        if ("number".equals(bet.getType())) return "число " + bet.getValue();
        else return bet.getType().toUpperCase();
    }

    private void updateGameState(GameState state) {
        Platform.runLater(() -> {
            currentState = state;
            balanceLabel.setText("Баланс: " + state.getBalance());
            tableCanvas.updateBets(state.getAllBets(), playerName);
            if (!state.isBettingOpen()) {
                addChatMessage("Ставки больше не принимаются – рулетка вращается!");
            }
        });
    }

    private void showSpinResult(int number) {
        Platform.runLater(() -> {
            lastNumberLabel.setText("Последнее: " + number);
            addChatMessage("Выпало число " + number);
            tableCanvas.spinWheel(number, () -> {
                addChatMessage("Раунд завершён. Результаты выплачены.");
                if (currentState != null)
                    tableCanvas.updateBets(currentState.getAllBets(), playerName);
            });
        });
    }

    private void onRoundStart(String msg) {
        Platform.runLater(() -> addChatMessage("Новый раунд! Делайте ставки."));
    }

    private void onError(String error) {
        Platform.runLater(() -> addChatMessage("Ошибка: " + error));
    }

    private void addChatMessage(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: #333; -fx-background-color: #F8F9FA; -fx-padding: 4 8; -fx-background-radius: 3;");
            messageContentBox.getChildren().add(label);
            messageScrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void sendChatMessage() {
        String msg = chatInputField.getText();
        if (msg != null && !msg.trim().isEmpty()) {
            networkClient.sendChatMessage(msg);
            chatInputField.clear();
        }
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