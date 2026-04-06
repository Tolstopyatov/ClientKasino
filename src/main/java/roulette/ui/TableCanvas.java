package roulette.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import roulette.model.Bet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableCanvas extends Canvas {
    // Параметры стола
    private final int ROWS = 3;
    private final int COLS = 12;
    private double tableStartX; // Начальная X-координата для стола (после колеса)
    private double zeroWidth;
    private double cellWidth, cellHeight;

    // Параметры колеса
    private double wheelCenterX, wheelCenterY, wheelRadius;
    private double wheelRotation = 0;
    private boolean isSpinning = false;

    // Данные
    private List<Bet> allBets;
    private String currentPlayerName;
    private Bet pendingBet;           // временная фишка
    private Object highlightedArea; // подсвеченная ячейка/область при pendingBet

    private static final Map<Integer, Color> ROULETTE_COLORS = new HashMap<>();
    static {
        // Красные
        int[] redNumbers = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};
        for (int num : redNumbers) {
            ROULETTE_COLORS.put(num, Color.web("#DC3545")); // Bootstrap red
        }
        // Черные
        int[] blackNumbers = {2, 4, 6, 8, 10, 11, 13, 15, 17, 20, 22, 24, 26, 28, 29, 31, 33, 35};
        for (int num : blackNumbers) {
            ROULETTE_COLORS.put(num, Color.web("#343A40")); // Bootstrap dark
        }
        // Зеро
        ROULETTE_COLORS.put(0, Color.web("#28A745")); // Bootstrap green
    }

    public TableCanvas() {
        widthProperty().addListener(obs -> redraw());
        heightProperty().addListener(obs -> redraw());
    }

    private void updateGeometry() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Колесо рулетки занимает примерно 25% ширины слева
        wheelRadius = Math.min(w * 0.15, h * 0.5);
        wheelCenterX = wheelRadius + 10; // Отступ от левого края
        wheelCenterY = h / 2;

        tableStartX = wheelCenterX + wheelRadius + 30; // Отступ между колесом и столом

        double availableTableWidth = w - tableStartX - 100; // 100px для боковых ставок
        zeroWidth = availableTableWidth * 0.05; // 5% от ширины стола для 0
        cellWidth = (availableTableWidth - zeroWidth) / COLS;
        cellHeight = h / ROWS/2;
    }

    public void redraw() {
        updateGeometry();
        if (getWidth() <= 0 || getHeight() <= 0) return;
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        drawWheel(gc);
        drawTable(gc);
    }

    private void drawTable(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 14));

        // Ячейка 0
        gc.setFill(ROULETTE_COLORS.get(0));
        gc.fillRect(tableStartX, 0, zeroWidth, getHeight());
        gc.setStroke(Color.web("#ADB5BD"));
        gc.setLineWidth(1);
        gc.strokeRect(tableStartX, 0, zeroWidth, getHeight());
        gc.setFill(Color.WHITE);
        gc.fillText("0", tableStartX + zeroWidth / 2, getHeight() / 2 + 5);

        // Ячейки 1-36
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                double x = tableStartX + zeroWidth + col * cellWidth;
                double y = row * cellHeight;
                int number = numberAt(row, col);
                Color color = ROULETTE_COLORS.getOrDefault(number, Color.LIGHTGRAY);

                gc.setFill(color);
                gc.fillRect(x, y, cellWidth, cellHeight);
                gc.setStroke(Color.web("#ADB5BD"));
                gc.strokeRect(x, y, cellWidth, cellHeight);

                gc.setFill(color.equals(Color.web("#343A40")) ? Color.WHITE : Color.BLACK);
                gc.fillText(String.valueOf(number), x + cellWidth, y + cellHeight + 5);
            }
        }

        drawSideBets(gc);

        if (allBets != null) {
            for (Bet bet : allBets) {
                drawBet(gc, bet);
            }
        }
        if (highlightedArea != null) {
            highlightCell(gc, highlightedArea);
        }
    }

    private void drawSideBets(GraphicsContext gc) {
        double sidePanelWidth = 80;
        double sideX = getWidth() - sidePanelWidth - 10; // Справа
        double sideY = 10;
        double sideHeight = (getHeight() - 40) / 4; // Распределяем по высоте

        String[] labels = {"Красное", "Чёрное", "Чёт", "Нечёт"};
        Color[] colors = {Color.web("#DC3545"), Color.web("#343A40"), Color.web("#28A745"), Color.web("#FFC107")};

        for (int i = 0; i < labels.length; i++) {
            double y = sideY + i * (sideHeight + 10);
            gc.setFill(colors[i]);
            gc.fillRect(sideX, y, sidePanelWidth, sideHeight);
            gc.setStroke(Color.web("#ADB5BD"));
            gc.strokeRect(sideX, y, sidePanelWidth, sideHeight);
            gc.setFill(Color.WHITE);
            gc.fillText(labels[i], sideX + sidePanelWidth / 2, y + sideHeight / 2 + 5);
        }
    }

    private int numberAt(int row, int col) {
        int base = col * 3 + (row == 0 ? 3 : row == 1 ? 2 : 1);
        return base;
    }

    private void drawBet(GraphicsContext gc, Bet bet) {
        Color chipColor = bet.getPlayerName().equals(currentPlayerName) ? Color.web("#FFD700") : Color.web("#A9A9A9"); // Gold for player, DarkGray for others
        Color textColor = Color.BLACK;
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));

        double chipSize = 20;
        double chipOffsetX = 5;
        double chipOffsetY = 5;

        if ("number".equals(bet.getType())) {
            int number = bet.getValue();
            double x, y;
            if (number == 0) {
                x = tableStartX + zeroWidth / 2 - chipSize / 2;
                y = getHeight() / 2 - chipSize / 2;
            } else {
                int row = (number % 3 == 0) ? 0 : (number % 3 == 2) ? 1 : 2;
                int col = (number - 1) / 3;
                x = tableStartX + zeroWidth + col * cellWidth + chipOffsetX;
                y = row * cellHeight + chipOffsetY;
            }
            gc.setFill(chipColor);
            gc.fillOval(x, y, chipSize, chipSize);
            gc.setFill(textColor);
            gc.fillText(String.valueOf(bet.getAmount()), x + chipSize / 2, y + chipSize / 2 + 4);
        } else {
            double sidePanelWidth = 80;
            double sideX = getWidth() - sidePanelWidth - 10;
            double sideY = 10;
            double sideHeight = (getHeight() - 40) / 4;
            int idx;
            switch (bet.getType()) {
                case "red": idx = 0; break;
                case "black": idx = 1; break;
                case "even": idx = 2; break;
                case "odd": idx = 3; break;
                default: return;
            }
            double x = sideX + sidePanelWidth / 2 - chipSize / 2;
            double y = sideY + idx * (sideHeight + 10) + sideHeight / 2 - chipSize / 2;

            gc.setFill(chipColor);
            gc.fillOval(x, y, chipSize, chipSize);
            gc.setFill(textColor);
            gc.fillText(String.valueOf(bet.getAmount()), x + chipSize / 2, y + chipSize / 2 + 4);
        }
    }

    private void highlightCell(GraphicsContext gc, Object area) {
        gc.setStroke(Color.web("#007BFF")); // Bootstrap blue for highlight
        gc.setLineWidth(3);

        if (area instanceof Integer) {
            int number = (Integer) area;
            if (number == 0) {
                gc.strokeRect(tableStartX, 0, zeroWidth, getHeight());
            } else {
                int row = (number % 3 == 0) ? 0 : (number % 3 == 2) ? 1 : 2;
                int col = (number - 1) / 3;
                double x = tableStartX + zeroWidth + col * cellWidth;
                double y = row * cellHeight;
                gc.strokeRect(x, y, cellWidth, cellHeight);
            }
        } else if (area instanceof String) {
            String type = (String) area;
            double sidePanelWidth = 80;
            double sideX = getWidth() - sidePanelWidth - 10;
            double sideY = 10;
            double sideHeight = (getHeight() - 40) / 4;
            int idx;
            switch (type) {
                case "red": idx = 0; break;
                case "black": idx = 1; break;
                case "even": idx = 2; break;
                case "odd": idx = 3; break;
                default: return;
            }
            double y = sideY + idx * (sideHeight + 10);
            gc.strokeRect(sideX, y, sidePanelWidth, sideHeight);
        }
    }

    public Object hitTest(double x, double y) {
        // Проверяем side-ставки
        double sidePanelWidth = 80;
        double sideX = getWidth() - sidePanelWidth - 10;
        double sideY = 10;
        double sideHeight = (getHeight() - 40) / 4;
        for (int i = 0; i < 4; i++) {
            double y0 = sideY + i * (sideHeight + 10);
            if (x >= sideX && x <= sideX + sidePanelWidth && y >= y0 && y <= y0 + sideHeight) {
                switch (i) {
                    case 0: return "red";
                    case 1: return "black";
                    case 2: return "even";
                    case 3: return "odd";
                }
            }
        }

        // 0
        if (x >= tableStartX && x <= tableStartX + zeroWidth && y >= 0 && y <= getHeight()) return 0;

        // 1-36
        if (x >= tableStartX + zeroWidth && x <= tableStartX + zeroWidth + COLS * cellWidth && y >= 0 && y <= getHeight()) {
            int col = (int) ((x - (tableStartX + zeroWidth)) / cellWidth);
            int row = (int) (y / cellHeight);
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                return numberAt(row, col);
            }
        }
        return null;
    }

    public void setPendingBetFromDrag(String betTypeStr, Object value, int chipAmount, String playerName) {
        if (value instanceof Integer) {
            int number = (Integer) value;
            pendingBet = new Bet(playerName, "number", number, chipAmount);
            highlightedArea = number;
        } else if (value instanceof String) {
            String type = (String) value;
            pendingBet = new Bet(playerName, type, 0, chipAmount);
            highlightedArea = type;
        }
        redraw();
    }

    public Bet getPendingBet() { return pendingBet; }
    public void clearPendingBet() { pendingBet = null; highlightedArea = null; redraw(); }

    public void updateBets(List<Bet> bets, String playerName) {
        this.allBets = bets;
        this.currentPlayerName = playerName;
        redraw();
    }

    public void spinWheel(int winningNumber, Runnable onComplete) {
        if (isSpinning) return;
        isSpinning = true;
        final long durationNanos = 5_000_000_000L; // 5 сек
        final long startTime = System.nanoTime();
        final double startRotation = wheelRotation;

        double targetAngleForNumber = (360.0 / 37) * winningNumber;
        double targetRotation = Math.abs(startRotation) + 360 * 10 + (targetAngleForNumber - Math.abs(startRotation % 360));

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = now - startTime;
                double t = Math.min(1.0, elapsed / (double) durationNanos);
                double ease = 1 - Math.pow(1 - t, 3);
                wheelRotation = startRotation + (targetRotation - startRotation) * ease;
                redraw();

                if (t >= 1.0) {
                    stop();
                    isSpinning = false;
                    wheelRotation = targetRotation % 360;
                    redraw();
                    if (onComplete != null) onComplete.run();
                }
            }
        };
        timer.start();
    }

    private void drawWheel(GraphicsContext gc) {
        if (wheelRadius <= 0) return;

        gc.save();
        gc.translate(wheelCenterX, wheelCenterY);
        gc.rotate(wheelRotation); // Вращаем весь контекст для колеса
        gc.translate(-wheelCenterX, -wheelCenterY);

        // Фон колеса
        gc.setFill(Color.web("#343A40")); // Dark background for wheel
        gc.fillOval(wheelCenterX - wheelRadius - 5, wheelCenterY - wheelRadius - 5, wheelRadius * 2 + 10, wheelRadius * 2 + 10);

        // Рисуем сектора
        double sectorAngle = 360.0 / 37;
        for (int i = 0; i < 37; i++) {
            Color color = ROULETTE_COLORS.getOrDefault(i, Color.LIGHTGRAY);
            gc.setFill(color);
            gc.fillArc(wheelCenterX - wheelRadius, wheelCenterY - wheelRadius, wheelRadius * 2, wheelRadius * 2, i * sectorAngle, sectorAngle, javafx.scene.shape.ArcType.ROUND);
        }

        gc.setStroke(Color.web("#F0F2F5"));
        gc.setLineWidth(2);
        for (int i = 0; i < 37; i++) {
            double angle = Math.toRadians(i * sectorAngle);
            gc.strokeLine(wheelCenterX, wheelCenterY, wheelCenterX + wheelRadius * Math.cos(angle), wheelCenterY + wheelRadius * Math.sin(angle));
        }

        // Рисуем числа на секторах
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        for (int i = 0; i < 37; i++) {
            double angle = Math.toRadians(i * sectorAngle + sectorAngle / 2); // Центр сектора
            double textRadius = wheelRadius * 0.8; // Радиус для текста
            double textX = wheelCenterX + textRadius * Math.cos(angle);
            double textY = wheelCenterY + textRadius * Math.sin(angle);
            gc.fillText(String.valueOf(i), textX, textY + 4); // +4 для центрирования текста
        }

        // Центральный круг
        gc.setFill(Color.web("#6C757D"));
        gc.fillOval(wheelCenterX - wheelRadius * 0.2, wheelCenterY - wheelRadius * 0.2, wheelRadius * 0.4, wheelRadius * 0.4);
        gc.setStroke(Color.web("#ADB5BD"));
        gc.strokeOval(wheelCenterX - wheelRadius * 0.2, wheelCenterY - wheelRadius * 0.2, wheelRadius * 0.4, wheelRadius * 0.4);

        gc.restore();

        gc.setFill(Color.web("#ecc800"));
        gc.fillPolygon(
            new double[]{wheelCenterX, wheelCenterX - 10, wheelCenterX + 10},
            new double[]{wheelCenterY - wheelRadius + 10, wheelCenterY - wheelRadius-10, wheelCenterY - wheelRadius-10},
            3
        );
    }
}
