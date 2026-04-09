package roulette;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import roulette.model.GameState;
import roulette.model.TarakanData;
import roulette.net.NetworkClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CockroachRaceController {
    @FXML private Pane raceTrack;
    @FXML private Label balanceLabel;
    @FXML private Label raceStatusLabel;
    @FXML private VBox chatContentBox;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextField chatInputField;
    @FXML private TextField betAmountField;
    @FXML private Button placeBetButton;
    @FXML private Button roach1Btn, roach2Btn, roach3Btn, roach4Btn, roach5Btn, roach6Btn;

    private NetworkClient networkClient;
    private String playerName;
    private int selectedRoach = -1;
    private boolean raceInProgress = false;
    private Map<String, Circle> roachCircles = new HashMap<>();
    private Map<String, Double> startX = new HashMap<>();
    private final Color[] roachColors = {
        Color.web("#D32F2F"), Color.web("#FBC02D"), Color.web("#388E3C"),
        Color.web("#1976D2"), Color.web("#8E24AA"), Color.web("#FF6D00")
    };

    public void initAfterLogin(NetworkClient client, String name) {
        this.networkClient = client;
        this.playerName = name;

        networkClient.setOnGameStateUpdate(this::updateGameState);
        networkClient.setOnError(this::onError);
        networkClient.setOnChatMessage(this::addChatMessage);
        networkClient.setOnCockroachRoutes(this::onCockroachRoutes);
        networkClient.setOnCockroachPositions(this::onCockroachPositions);
        networkClient.setOnRaceWinner(this::onRaceWinner);
    }

    @FXML
    public void initialize() {
        buildRaceTrack();
        clearSelection();
        raceStatusLabel.setText("Выберите таракана и сумму ставки");
        betAmountField.setText("10");
    }

    private void buildRaceTrack() {
        raceTrack.getChildren().clear();
        roachCircles.clear();
        startX.clear();

        double trackWidth = raceTrack.getWidth();
        double trackHeight = raceTrack.getHeight();
        if (trackWidth <= 0) trackWidth = 850;
        if (trackHeight <= 0) trackHeight = 400;

        double laneHeight = trackHeight / 6.0;
        double startPos = 30;
        for (int i = 1; i <= 6; i++) {
            String id = String.valueOf(i);
            double y = (i - 0.5) * laneHeight;
            Circle roach = new Circle(15, roachColors[i-1]);
            roach.setStroke(Color.BLACK);
            roach.setStrokeWidth(2);
            roach.setTranslateX(startPos);
            roach.setTranslateY(y);
            raceTrack.getChildren().add(roach);
            roachCircles.put(id, roach);
            startX.put(id, startPos);

            Text label = new Text(String.valueOf(i));
            label.setFill(Color.WHITE);
            label.setTranslateX(startPos - 5);
            label.setTranslateY(y + 5);
            raceTrack.getChildren().add(label);
        }
        // Финишная линия
        double finishX = trackWidth - 50;
        javafx.scene.shape.Line finishLine = new javafx.scene.shape.Line(finishX, 0, finishX, trackHeight);
        finishLine.setStroke(Color.RED);
        finishLine.setStrokeWidth(3);
        finishLine.getStrokeDashArray().addAll(10.0, 10.0);
        raceTrack.getChildren().add(finishLine);
    }

    private void clearSelection() {
        selectedRoach = -1;
        Button[] btns = {roach1Btn, roach2Btn, roach3Btn, roach4Btn, roach5Btn, roach6Btn};
        for (int i = 0; i < btns.length; i++) {
            btns[i].setStyle("-fx-background-color: " + toWebColor(roachColors[i]) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 5;");
        }
        placeBetButton.setDisable(true);
    }

    @FXML private void selectRoach1() { selectRoach(1); }
    @FXML private void selectRoach2() { selectRoach(2); }
    @FXML private void selectRoach3() { selectRoach(3); }
    @FXML private void selectRoach4() { selectRoach(4); }
    @FXML private void selectRoach5() { selectRoach(5); }
    @FXML private void selectRoach6() { selectRoach(6); }

    private void selectRoach(int id) {
        if (raceInProgress) {
            addChatMessage("Сейчас идёт забег, ставки нельзя изменить");
            return;
        }
        selectedRoach = id;
        Button[] btns = {roach1Btn, roach2Btn, roach3Btn, roach4Btn, roach5Btn, roach6Btn};
        for (int i = 0; i < btns.length; i++) {
            if (i+1 == id) {
                btns[i].setStyle("-fx-background-color: " + toWebColor(roachColors[i].darker()) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 5;");
            } else {
                btns[i].setStyle("-fx-background-color: " + toWebColor(roachColors[i]) + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 5;");
            }
        }
        placeBetButton.setDisable(false);
        raceStatusLabel.setText("Выбран таракан " + id + ". Введите сумму и нажмите «Сделать ставку»");
    }

    @FXML
    private void placeBet() {
        if (selectedRoach == -1) {
            addChatMessage("Сначала выберите таракана");
            return;
        }
        if (raceInProgress) {
            addChatMessage("Забег уже идёт, ставки не принимаются");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(betAmountField.getText().trim());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            addChatMessage("Введите корректную сумму ставки (целое число >0)");
            return;
        }
        networkClient.placeTarakanBet(selectedRoach, amount);
        addChatMessage("Ставка " + amount + " на таракана №" + selectedRoach + " отправлена");
        placeBetButton.setDisable(true);
        // Не сбрасываем выбор, чтобы игрок мог видеть, на кого поставил
    }

    @FXML
    private void sendChatMessage() {
        String msg = chatInputField.getText();
        if (msg != null && !msg.trim().isEmpty()) {
            networkClient.sendChatMessage(msg);
            chatInputField.clear();
        }
    }

    private void onCockroachRoutes(List<TarakanData> tarakanes) {
        Platform.runLater(() -> {
            raceInProgress = true;
            raceStatusLabel.setText("ЗАБЕГ НАЧАЛСЯ!");
            addChatMessage("Старт! Тараканы побежали!");
            // Сброс позиций на старт
            for (TarakanData c : tarakanes) {
                Circle circle = roachCircles.get(c.getId());
                if (circle != null) {
                    circle.setTranslateX(startX.getOrDefault(c.getId(), 30.0));
                }
            }
        });
    }

    private void onCockroachPositions(List<TarakanData> tarakanes) {
        Platform.runLater(() -> {
            if (!raceInProgress) {
                raceInProgress = true;
                raceStatusLabel.setText("ЗАБЕГ ИДЁТ...");
            }
            for (TarakanData c : tarakanes) {
                Circle circle = roachCircles.get(c.getId());
                if (circle != null) {
                    circle.setTranslateX(c.getX());
                    circle.setTranslateY(c.getY());
                }
            }
        });
    }

    private void onRaceWinner(int winnerId) {
        Platform.runLater(() -> {
            raceInProgress = false;
            raceStatusLabel.setText("Победил таракан №" + winnerId + "!");
            addChatMessage("РЕЗУЛЬТАТ: Победитель - таракан №" + winnerId);
            clearSelection();
        });
    }

    private void updateGameState(GameState state) {
        Platform.runLater(() -> balanceLabel.setText("Баланс: " + state.getBalance()));
    }

    private void onError(String error) {
        Platform.runLater(() -> addChatMessage("Ошибка: " + error));
    }

    private void addChatMessage(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: #333333; -fx-background-color: #F8F9FA; -fx-padding: 4 8; -fx-background-radius: 3;");
            chatContentBox.getChildren().add(label);
            chatScrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void openMainMenu() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_menu.fxml"));
        Parent root = loader.load();
        MainMenuController controller = loader.getController();
        controller.initData(networkClient, playerName);  // не отключаемся!
        Stage stage = (Stage) balanceLabel.getScene().getWindow();
        stage.setScene(new Scene(root, 600, 400));
        stage.setTitle("Казино - " + playerName);
    }

    private String toWebColor(Color color) {
        return String.format("#%02X%02X%02X",
                (int)(color.getRed()*255),
                (int)(color.getGreen()*255),
                (int)(color.getBlue()*255));
    }
}