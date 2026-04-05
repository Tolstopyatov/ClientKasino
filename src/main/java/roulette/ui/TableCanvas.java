package roulette.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import roulette.model.Bet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableCanvas extends Canvas {
    // Параметры стола
    private final double ZERO_WIDTH = 70;      // ширина ячейки 0
    private final int ROWS = 3;
    private final int COLS = 12;
    private double cellWidth, cellHeight;      // вычисляются динамически

    // Параметры колеса
    private double wheelCenterX, wheelCenterY, wheelRadius;
    private double wheelRotation = 0;
    private boolean isSpinning = false;

    // Данные
    private List<Bet> allBets;
    private String currentPlayerName;
    private Bet pendingBet;           // временная фишка (перетаскивается, но ещё не отправлена)
    private int highlightedNumber = -1; // подсвеченная ячейка при pendingBet

    // Цвета чисел рулетки
    private static final Map<Integer, Color> ROULETTE_COLORS = new HashMap<>();
    static {
        int[] redNumbers = {1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36};
        for (int num : redNumbers) ROULETTE_COLORS.put(num, Color.web("#E63946"));
        int[] blackNumbers = {2,4,6,8,10,11,13,15,17,20,22,24,26,28,29,31,33,35};
        for (int num : blackNumbers) ROULETTE_COLORS.put(num, Color.web("#1A1A1A"));
        ROULETTE_COLORS.put(0, Color.web("#2ECC71"));
    }

    public TableCanvas() {
        // Подписываемся на изменение размеров – перерисовываем всё
        widthProperty().addListener(obs -> redraw());
        heightProperty().addListener(obs -> redraw());
        setOnMouseClicked(e -> handleMouseClick(e.getX(), e.getY()));
    }

    // Обновить геометрию (вызывается перед каждой отрисовкой)
    private void updateGeometry() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;
        cellWidth = (w - ZERO_WIDTH) / COLS;
        cellHeight = h / ROWS;

        // Колесо располагается справа от стола, занимая ~25% ширины
        wheelRadius = Math.min(w * 0.18, h * 0.4);
        wheelCenterX = wheelRadius;
        wheelCenterY = h / 2;
    }

    public void redraw() {
        updateGeometry();
        if (getWidth() <= 0 || getHeight() <= 0) return;
        drawTable();
        drawWheel();
    }

    // Отрисовка игрового стола (ячейки, ставки, подсветка)
    private void drawTable() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        // Ячейка 0
        gc.setFill(ROULETTE_COLORS.get(0));
        gc.fillRect(0, 0, ZERO_WIDTH, getHeight());
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(0, 0, ZERO_WIDTH, getHeight());
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("0", ZERO_WIDTH / 2, getHeight() / 2);

        // Ячейки 1-36
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                double x = ZERO_WIDTH + col * cellWidth;
                double y = row * cellHeight/4;
                int number = numberAt(row, col);
                Color color = ROULETTE_COLORS.getOrDefault(number, Color.LIGHTGRAY);
                gc.setFill(color);
                gc.fillRect(x, y, cellWidth, cellHeight);
                gc.setStroke(Color.BLACK);
                gc.strokeRect(x, y, cellWidth, cellHeight);
                gc.setFill(color.equals(Color.web("#1A1A1A")) ? Color.WHITE : Color.BLACK);
                gc.setFont(Font.font(14));
                gc.fillText(String.valueOf(number), x + cellWidth/2, y + cellHeight/2 + 5);
            }
        }

        drawSideBets(gc);

        if (allBets != null) {
            for (Bet bet : allBets) {
                drawBet(bet, false);
            }
        }

        // Временная ставка (перетаскиваемая фишка)
        if (pendingBet != null && pendingBet.getAmount() == 0) {
            drawBet(pendingBet, true);
        }

        if (highlightedNumber != -1 && pendingBet != null) {
            highlightCell(highlightedNumber);
        }
    }

    private void drawSideBets(GraphicsContext gc) {
        double sideX = ZERO_WIDTH + COLS * cellWidth + 10;
        double sideY = 10;
        double sideWidth = 80;
        double sideHeight = 30;
        String[] labels = {"RED", "BLACK", "EVEN", "ODD"};
        Color[] colors = {Color.web("#E63946"), Color.web("#474747"), Color.web("#4CAF50"), Color.web("#FF9800")};
        for (int i = 0; i < labels.length; i++) {
            double y = sideY + i * (sideHeight + 5);
            gc.setFill(colors[i]);
            gc.fillRect(sideX, y, sideWidth, sideHeight);
            gc.setStroke(Color.BLACK);
            gc.strokeRect(sideX, y, sideWidth, sideHeight);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font(12));
            gc.fillText(labels[i], sideX + sideWidth/2, y + sideHeight/2 + 4);
        }
    }

    // Преобразование (row, col) -> номер числа (1-36)
    private int numberAt(int row, int col) {
        int base = col * 3 + (row == 0 ? 3 : row == 1 ? 2 : 1);
        return base;
    }

    // Отрисовка одной фишки (ставки)
    private void drawBet(Bet bet, boolean temporary) {
        GraphicsContext gc = getGraphicsContext2D();
        Color color = temporary ? Color.rgb(255, 255, 0, 0.7) : 
                        (bet.getPlayerName().equals(currentPlayerName) ? Color.YELLOW : Color.ORANGE);
        if ("number".equals(bet.getType())) {
            int number = bet.getValue();
            if (number == 0) {
                double x = 10, y = getHeight()/2 - 15;
                gc.setFill(color);
                gc.fillOval(x, y, 24, 24);
                gc.setFill(Color.BLACK);
                gc.fillText(String.valueOf(bet.getAmount()), x+12, y+16);
            } else {
                int row = (number % 3 == 0) ? 0 : (number % 3 == 2) ? 1 : 2;
                int col = (number - 1) / 3;
                double x = ZERO_WIDTH + col * cellWidth + 5;
                double y = row * cellHeight + 5;
                gc.setFill(color);
                gc.fillOval(x, y, 24, 24);
                gc.setFill(Color.BLACK);
                gc.fillText(String.valueOf(bet.getAmount()), x+12, y+16);
            }
        } else {
            // side bet: рисуем справа от боковой панели
            double sideX = ZERO_WIDTH + COLS * cellWidth + 10;
            double sideY = 10;
            double sideHeight = 30;
            int idx;
            switch (bet.getType()) {
                case "red": idx = 0; break;
                case "black": idx = 1; break;
                case "even": idx = 2; break;
                case "odd": idx = 3; break;
                default: idx = 0;
            }
            double y = sideY + idx * (sideHeight + 5);
            double x = sideX + 85; // правее боковой панели
            gc.setFill(color);
            gc.fillOval(x, y + 3, 24, 24);
            gc.setFill(Color.BLACK);
            gc.fillText(String.valueOf(bet.getAmount()), x+12, y+18);
        }
    }

    private void highlightCell(int number) {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(4);
        if (number == 0) {
            gc.strokeRect(0, 0, ZERO_WIDTH, getHeight());
        } else {
            int row = (number % 3 == 0) ? 0 : (number % 3 == 2) ? 1 : 2;
            int col = (number - 1) / 3;
            double x = ZERO_WIDTH + col * cellWidth;
            double y = row * cellHeight;
            gc.strokeRect(x, y, cellWidth, cellHeight);
        }
    }

    // Hit test по координатам мыши (определяет номер ячейки или тип side-ставки)
    public Object hitTest(double x, double y) {
        // Проверяем side-ставки
        double sideX = ZERO_WIDTH + COLS * cellWidth + 10;
        double sideY = 10;
        double sideWidth = 80;
        double sideHeight = 30;
        for (int i = 0; i < 4; i++) {
            double y0 = sideY + i * (sideHeight + 5);
            if (x >= sideX && x <= sideX + sideWidth && y >= y0 && y <= y0 + sideHeight) {
                switch (i) {
                    case 0: return "red";
                    case 1: return "black";
                    case 2: return "even";
                    case 3: return "odd";
                }
            }
        }
        // Ячейка 0
        if (x >= 0 && x <= ZERO_WIDTH) return 0;
        // Ячейки 1-36
        int col = (int) ((x - ZERO_WIDTH) / cellWidth);
        int row = (int) (y / cellHeight);
        if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
            return numberAt(row, col);
        }
        return null;
    }

    // Создать временную ставку на основе перетаскивания
    public void setPendingBetFromDrag(String betTypeStr, Object value, int chipAmount, String playerName) {
        if (value instanceof Integer) {
            int number = (Integer) value;
            pendingBet = new Bet(playerName, "number", number, 0);
            highlightedNumber = number;
        } else if (betTypeStr.equals("red") || betTypeStr.equals("black") || betTypeStr.equals("even") || betTypeStr.equals("odd")) {
            pendingBet = new Bet(playerName, betTypeStr, 0, 0);
            highlightedNumber = -1;
        }
        if (pendingBet != null) {
            // Сохраняем номинал фишки, чтобы потом установить сумму
            pendingBet.setAmount(chipAmount); // временно сохраняем сумму, потом заменим
        }
        redraw();
    }

    public Bet getPendingBet() { return pendingBet; }
    public void clearPendingBet() { pendingBet = null; highlightedNumber = -1; redraw(); }

    // Обновить список ставок от сервера
    public void updateBets(List<Bet> bets, String playerName) {
        this.allBets = bets;
        this.currentPlayerName = playerName;
        redraw();
    }

    // Анимация вращения колеса
    public void spinWheel(int winningNumber, Runnable onComplete) {
        if (isSpinning) return;
        isSpinning = true;
        final long durationNanos = 3_000_000_000L;
        final long startTime = System.nanoTime();
        final double startRotation = wheelRotation;
        // Количество полных оборотов + доворот до winningNumber
        double targetRotation = startRotation + 360 * 8 + (winningNumber * 360.0 / 37);

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = now - startTime;
                double t = Math.min(1.0, elapsed / (double) durationNanos);
                double ease = 1 - Math.pow(1 - t, 3);
                wheelRotation = 360 -startRotation + (targetRotation - startRotation) * ease;
                redraw(); // перерисовываем стол и колесо
                if (t >= 1.0) {
                    stop();
                    isSpinning = false;
                    if (onComplete != null) onComplete.run();
                }
            }
        };
        timer.start();
    }

    // Отрисовка колеса рулетки
    private void drawWheel() {
        GraphicsContext gc = getGraphicsContext2D();
        if (wheelRadius <= 0) return;
        // Рисуем сектора
        for (int i = 0; i < 37; i++) {
            double angle = Math.toRadians(i * 360.0 / 37 + wheelRotation);
            double x = wheelCenterX + wheelRadius * Math.cos(angle);
            double y = wheelCenterY + wheelRadius * Math.sin(angle);
            Color color = ROULETTE_COLORS.getOrDefault(i, Color.LIGHTGRAY);
            gc.setFill(color);
            gc.fillOval(x - 12, y - 12, 24, 24);
            gc.setStroke(Color.BLACK);
            gc.strokeOval(x - 12, y - 12, 24, 24);
            gc.setFill(color.equals(Color.web("#1A1A1A")) ? Color.WHITE : Color.BLACK);
            gc.setFont(Font.font(11));
            gc.fillText(String.valueOf(i), x, y + 4);
        }
        // Центр колеса
        gc.setFill(Color.web("#2ECC71"));
        gc.fillOval(wheelCenterX - 15, wheelCenterY - 15, 30, 30);
        gc.setStroke(Color.BLACK);
        gc.strokeOval(wheelCenterX - 15, wheelCenterY - 15, 30, 30);
        // Стрелка
        gc.setFill(Color.RED);
        gc.fillPolygon(
            new double[]{wheelCenterX, wheelCenterX - 8, wheelCenterX + 8},
            new double[]{wheelCenterY - wheelRadius - 15, wheelCenterY - wheelRadius, wheelCenterY - wheelRadius},
            3
        );
    }

    // Обработка клика мышью (опционально, можно использовать для быстрой ставки)
    private void handleMouseClick(double x, double y) {
        // Не используется для drag-and-drop, оставлено для возможного расширения
    }
}