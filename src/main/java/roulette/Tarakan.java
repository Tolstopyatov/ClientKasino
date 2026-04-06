package roulette;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import roulette.model.GameState;
import roulette.net.NetworkClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Tarakan {
    @FXML private Pane raceTrack;
    @FXML private Label balanceLabel;
    @FXML private Label raceStatusLabel;
    @FXML private TextField betAmountField;
    @FXML private Button placeBetButton;
    @FXML private VBox messageBox;
    @FXML private ScrollPane messageScrollPane;
    @FXML private Button roach1Btn, roach2Btn, roach3Btn, roach4Btn, roach5Btn, roach6Btn;

    private NetworkClient networkClient;
    private String playerName;
    private GameState currentState;

    private int selectedRoach = -1;          // id выбранного таракана (1..6)
    private boolean raceInProgress = false;
    private Map<Integer, Circle> roachCircles;       // id -> круг на дорожке
    private Map<Integer, Double> startX;              // начальная X позиция
    private Map<Integer, Double> finishX;             // финишная X
    private Map<Integer, TranslateTransition> activeTransitions;
    private int raceWinner = -1;

    // Цвета тараканов
    private final Color[] roachColors = {
        Color.web("#D32F2F"), Color.web("#FBC02D"), Color.web("#388E3C"),
        Color.web("#1976D2"), Color.web("#8E24AA"), Color.web("#FF6D00")
    };

    public void initAfterLogin(NetworkClient client, String name) {
        this.networkClient = client;
        this.playerName = name;

        networkClient.setOnGameStateUpdate(this::updateGameState);
        networkClient.setOnError(this::onError);
        // Регистрируем обработчики для тараканьих бегов
        networkClient.setOnRaceStart(this::onRaceStart);
        networkClient.setOnRaceResult(this::onRaceResult);
    }

    @FXML
    public void initialize() {
        buildRaceTrack();
        clearSelection();
        placeBetButton.setDisable(true);
        raceStatusLabel.setText("Выберите таракана и сделайте ставку");
    }

    private void buildRaceTrack() {
        roachCircles = new HashMap<>();
        startX = new HashMap<>();
        finishX = new HashMap<>();
        activeTransitions = new HashMap<>();

        double trackWidth = raceTrack.getWidth();
        double trackHeight = raceTrack.getHeight();
        double laneHeight = trackHeight / 6.0;
        double startPos = 30;
        double finishPos = trackWidth - 50;

        for (int i = 1; i <= 6; i++) {
            double y = (i - 0.5) * laneHeight;
            Circle roach = new Circle(15, roachColors[i-1]);
            roach.setStroke(Color.BLACK);
            roach.setStrokeWidth(2);
            roach.setTranslateX(startPos);
            roach.setTranslateY(y);
            raceTrack.getChildren().add(roach);
            roachCircles.put(i, roach);
            startX.put(i, startPos);
            finishX.put(i, finishPos);

            // Подпись номера
            Text label = new Text(String.valueOf(i));
            label.setFill(Color.WHITE);
            label.setTranslateX(startPos - 5);
            label.setTranslateY(y + 5);
            raceTrack.getChildren().add(label);
        }
        // Финишная линия
        javafx.scene.shape.Line finishLine = new javafx.scene.shape.Line(finishPos, 0, finishPos, trackHeight);
        finishLine.setStroke(Color.RED);
        finishLine.setStrokeWidth(3);
        finishLine.getStrokeDashArray().addAll(10.0, 10.0);
        raceTrack.getChildren().add(finishLine);
    }

    private void clearSelection() {
        selectedRoach = -1;
        for (int i = 1; i <= 6; i++) {
            Button btn = getButtonById(i);
            if (btn != null) btn.setStyle("-fx-background-color: #ffaa00; -fx-font-weight: bold;");
        }
    }

    private Button getButtonById(int id) {
        switch (id) {
            case 1: return roach1Btn;
            case 2: return roach2Btn;
            case 3: return roach3Btn;
            case 4: return roach4Btn;
            case 5: return roach5Btn;
            case 6: return roach6Btn;
            default: return null;
        }
    }

    @FXML private void selectRoach1() { selectRoach(1); }
    @FXML private void selectRoach2() { selectRoach(2); }
    @FXML private void selectRoach3() { selectRoach(3); }
    @FXML private void selectRoach4() { selectRoach(4); }
    @FXML private void selectRoach5() { selectRoach(5); }
    @FXML private void selectRoach6() { selectRoach(6); }

    private void selectRoach(int id) {
        if (raceInProgress) {
            addMessage("Сейчас идёт забег, подождите!");
            return;
        }
        selectedRoach = id;
        for (int i = 1; i <= 6; i++) {
            Button btn = getButtonById(i);
            if (btn != null) {
                btn.setStyle(i == id ? "-fx-background-color: #ff5500; -fx-font-weight: bold;" : "-fx-background-color: #ffaa00; -fx-font-weight: bold;");
            }
        }
        placeBetButton.setDisable(false);
        raceStatusLabel.setText("Выбран таракан " + id + ". Введите сумму ставки.");
    }

    @FXML
    private void placeBet() {
        if (selectedRoach == -1) {
            addMessage("Сначала выберите таракана!");
            return;
        }
        if (raceInProgress) {
            addMessage("Забег уже идёт, нельзя делать ставку.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(betAmountField.getText());
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            addMessage("Введите корректную сумму (целое число >0)");
            return;
        }
        if (currentState != null && amount > currentState.getBalance()) {
            addMessage("Недостаточно средств! Ваш баланс: " + currentState.getBalance());
            return;
        }
        // Отправляем ставку на сервер
        networkClient.placeCockroachBet(selectedRoach, amount);
        addMessage("Ставка " + amount + " на таракана №" + selectedRoach + " отправлена!");
        betAmountField.clear();
        placeBetButton.setDisable(true);
        clearSelection();
        raceStatusLabel.setText("Ставка принята. Ожидаем начала забега...");
    }

    // Обработчик начала забега от сервера
    private void onRaceStart(Map<String, Object> data) {
        Platform.runLater(() -> {
            raceInProgress = true;
            raceStatusLabel.setText("ЗАБЕГ НАЧАЛСЯ!");
            addMessage("Старт! Тараканы побежали!");
            // Сброс позиций
            for (int i = 1; i <= 6; i++) {
                Circle c = roachCircles.get(i);
                if (c != null) c.setTranslateX(startX.get(i));
                if (activeTransitions.containsKey(i)) {
                    activeTransitions.get(i).stop();
                }
            }
            // Запускаем анимацию для всех тараканов со случайной скоростью (имитация)
            // В реальном проекте сервер может прислать продолжительность или позиции, но для демо сделаем рандом
            Random rand = new Random();
            for (int i = 1; i <= 6; i++) {
                double distance = finishX.get(i) - startX.get(i);
                double durationSec = 2.0 + rand.nextDouble() * 2.0; // от 2 до 4 секунд
                TranslateTransition tt = new TranslateTransition(Duration.seconds(durationSec), roachCircles.get(i));
                tt.setByX(distance);
                tt.setOnFinished(e -> {
                    // Не делаем ничего, победителя определит сервер
                });
                tt.play();
                activeTransitions.put(i, tt);
            }
            // Если через некоторое время не пришёл результат, можно остановить (но сервер пришлёт)
        });
    }

    // Обработчик результата забега
    private void onRaceResult(Map<String, Object> data) {
        Platform.runLater(() -> {
            if (!raceInProgress) return;
            int winnerId = ((Number) data.get("winner")).intValue();
            // Останавливаем все анимации и показываем победителя
            for (int i = 1; i <= 6; i++) {
                if (activeTransitions.containsKey(i)) {
                    activeTransitions.get(i).stop();
                }
                // Принудительно ставим всех на финишную позицию? Лучше переместить победителя на финиш, остальных оставить где были
                if (i == winnerId) {
                    roachCircles.get(i).setTranslateX(finishX.get(i));
                }
            }
            raceWinner = winnerId;
            raceInProgress = false;
            raceStatusLabel.setText("Победил таракан №" + winnerId + "!");
            addMessage("РЕЗУЛЬТАТ: Победитель - таракан №" + winnerId);
            // Подсветим победителя
            Circle winnerCircle = roachCircles.get(winnerId);
            winnerCircle.setStroke(Color.GOLD);
            winnerCircle.setStrokeWidth(4);
            // Через 3 секунды убрать подсветку
            new Thread(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException e) {}
                Platform.runLater(() -> winnerCircle.setStroke(Color.BLACK));
            }).start();
            placeBetButton.setDisable(false);
        });
    }

    private void updateGameState(GameState state) {
        Platform.runLater(() -> {
            this.currentState = state;
            balanceLabel.setText("Баланс: " + state.getBalance());
        });
    }

    private void onError(String error) {
        Platform.runLater(() -> addMessage("Ошибка: " + error));
    }

    private void addMessage(String msg) {
        Platform.runLater(() -> {
            Label label = new Label(msg);
            label.setStyle("-fx-text-fill: white; -fx-background-color: #5d5b5b; -fx-padding: 4 8; -fx-background-radius: 3;");
            messageBox.getChildren().add(label);
            messageScrollPane.setVvalue(1.0);
        });
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