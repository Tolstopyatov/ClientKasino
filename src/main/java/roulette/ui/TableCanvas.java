package roulette.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import roulette.model.Bet;

public class TableCanvas extends Canvas {
    private final int ROWS = 3;
    private final int COLS = 12;
    private double tableStartX;
    private double zeroWidth;
    private double cellWidth, cellHeight;
    private double tableHeight;

    private double wheelCenterX, wheelCenterY, wheelRadius;
    private double wheelRotation = 0;
    private boolean isSpinning = false;

    private List<Bet> allBets;
    private String currentPlayerName;
    private Bet pendingBet;
    private Object highlightedArea;

    private double sideBetsStartX, sideBetsStartY;
    private double sideBetWidth, sideBetHeight;
    private static final int SIDE_BETS_COUNT = 4;
    private final String[] sideBetLabels = {"Красное", "Чёрное", "Чёт", "Нечёт"};
    private final Color[] sideBetColors = {Color.web("#DC3545"), Color.web("#343A40"), Color.web("#28A745"), Color.web("#FFC107")};
    private final String[] sideBetTypes = {"red", "black", "even", "odd"};

    private static final int[] WHEEL_ORDER = {
        0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5,
        24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26
    };

    private static final Map<Integer, Color> ROULETTE_COLORS = new HashMap<>();
    static {
        int[] redNumbers = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};
        for (int num : redNumbers) ROULETTE_COLORS.put(num, Color.web("#DC3545"));
        int[] blackNumbers = {2, 4, 6, 8, 10, 11, 13, 15, 17, 20, 22, 24, 26, 28, 29, 31, 33, 35};
        for (int num : blackNumbers) ROULETTE_COLORS.put(num, Color.web("#343A40"));
        ROULETTE_COLORS.put(0, Color.web("#28A745"));
    }

    public TableCanvas() {
        widthProperty().addListener(obs -> redraw());
        heightProperty().addListener(obs -> redraw());
    }

    private void updateGeometry() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        wheelRadius = Math.min(w * 0.15, h * 0.5);
        wheelCenterX = wheelRadius + 10;
        wheelCenterY = h / 2;

        tableStartX = wheelCenterX + wheelRadius + 30;

        double availableTableWidth = w - tableStartX - 10;
        zeroWidth = availableTableWidth * 0.05;
        cellWidth = (availableTableWidth - zeroWidth) / COLS;
        cellHeight = (h * 0.6) / ROWS;
        tableHeight = cellHeight * ROWS;

        sideBetsStartY = tableHeight + 10;
        sideBetHeight = 50;
        double totalSideWidth = w - tableStartX - 10;
        sideBetWidth = totalSideWidth / SIDE_BETS_COUNT;
        sideBetsStartX = tableStartX;
    }

    public void redraw() {
        updateGeometry();
        if (getWidth() <= 0 || getHeight() <= 0) return;
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        drawWheel(gc);
        drawTable(gc);
        drawSideBets(gc);
        drawAllBets(gc);
        if (highlightedArea != null) highlightCell(gc, highlightedArea);
    }

    private void drawTable(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 14));

        gc.setFill(ROULETTE_COLORS.get(0));
        gc.fillRect(tableStartX, 0, zeroWidth, tableHeight);
        gc.setStroke(Color.web("#ADB5BD"));
        gc.strokeRect(tableStartX, 0, zeroWidth, tableHeight);
        gc.setFill(Color.WHITE);
        gc.fillText("0", tableStartX + zeroWidth / 2, tableHeight / 2 + 5);

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
                gc.fillText(String.valueOf(number), x + cellWidth / 2, y + cellHeight / 2 + 5);
            }
        }
    }

    private void drawSideBets(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 14));
        for (int i = 0; i < SIDE_BETS_COUNT; i++) {
            double x = sideBetsStartX + i * sideBetWidth;
            double y = sideBetsStartY;
            gc.setFill(sideBetColors[i]);
            gc.fillRect(x, y, sideBetWidth, sideBetHeight);
            gc.setStroke(Color.web("#ADB5BD"));
            gc.strokeRect(x, y, sideBetWidth, sideBetHeight);
            gc.setFill(sideBetColors[i].equals(Color.web("#343A40")) ? Color.WHITE : Color.BLACK);
            gc.fillText(sideBetLabels[i], x + sideBetWidth / 2, y + sideBetHeight / 2 + 5);
        }
    }

    private void drawAllBets(GraphicsContext gc) {
        if (allBets != null) {
            for (Bet bet : allBets) drawBet(gc, bet);
        }
        if (pendingBet != null) drawBet(gc, pendingBet);
    }

    private void drawBet(GraphicsContext gc, Bet bet) {
        Color chipColor = bet.getPlayerName().equals(currentPlayerName) ? Color.web("#FFD700") : Color.web("#A9A9A9");
        Color textColor = Color.BLACK;
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        double chipSize = 20;
        double chipOffsetX = 5, chipOffsetY = 5;

        if ("number".equals(bet.getType())) {
            int number = bet.getValue();
            double x, y;
            if (number == 0) {
                x = tableStartX + zeroWidth / 2 - chipSize / 2;
                y = tableHeight / 2 - chipSize / 2;
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
            int idx = switch (bet.getType()) {
                case "red" -> 0;
                case "black" -> 1;
                case "even" -> 2;
                case "odd" -> 3;
                default -> -1;
            };
            if (idx == -1) return;
            double x = sideBetsStartX + idx * sideBetWidth + sideBetWidth / 2 - chipSize / 2;
            double y = sideBetsStartY + sideBetHeight / 2 - chipSize / 2;
            gc.setFill(chipColor);
            gc.fillOval(x, y, chipSize, chipSize);
            gc.setFill(textColor);
            gc.fillText(String.valueOf(bet.getAmount()), x + chipSize / 2, y + chipSize / 2 + 4);
        }
    }

    private void highlightCell(GraphicsContext gc, Object area) {
        gc.setStroke(Color.web("#007BFF"));
        gc.setLineWidth(3);
        if (area instanceof Integer number) {
            if (number == 0) {
                gc.strokeRect(tableStartX, 0, zeroWidth, tableHeight);
            } else {
                int row = (number % 3 == 0) ? 0 : (number % 3 == 2) ? 1 : 2;
                int col = (number - 1) / 3;
                double x = tableStartX + zeroWidth + col * cellWidth;
                double y = row * cellHeight;
                gc.strokeRect(x, y, cellWidth, cellHeight);
            }
        } else if (area instanceof String type) {
            int idx = switch (type) {
                case "red" -> 0;
                case "black" -> 1;
                case "even" -> 2;
                case "odd" -> 3;
                default -> -1;
            };
            if (idx != -1) {
                double x = sideBetsStartX + idx * sideBetWidth;
                double y = sideBetsStartY;
                gc.strokeRect(x, y, sideBetWidth, sideBetHeight);
            }
        }
    }

    public Object hitTest(double x, double y) {
        if (y >= sideBetsStartY && y <= sideBetsStartY + sideBetHeight) {
            int col = (int) ((x - sideBetsStartX) / sideBetWidth);
            if (col >= 0 && col < SIDE_BETS_COUNT) {
                return sideBetTypes[col];
            }
        }
        if (x >= tableStartX && x <= tableStartX + zeroWidth && y >= 0 && y <= tableHeight) return 0;
        if (x >= tableStartX + zeroWidth && x <= tableStartX + zeroWidth + COLS * cellWidth && y >= 0 && y <= tableHeight) {
            int col = (int) ((x - (tableStartX + zeroWidth)) / cellWidth);
            int row = (int) (y / cellHeight);
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                return numberAt(row, col);
            }
        }
        return null;
    }

    private int numberAt(int row, int col) {
        return col * 3 + (row == 0 ? 3 : row == 1 ? 2 : 1);
    }

    public Bet getPendingBet() { return pendingBet; }
    public void clearPendingBet() { pendingBet = null; highlightedArea = null; redraw(); }
    public void updateBets(List<Bet> bets, String playerName) { this.allBets = bets; this.currentPlayerName = playerName; redraw(); }

    public void spinWheel(int winningNumber, Runnable onComplete) {
        if (isSpinning) return;

        int winningIndex = -1;
        for (int i = 0; i < WHEEL_ORDER.length; i++) {
            if (WHEEL_ORDER[i] == winningNumber) {
                winningIndex = i;
                break;
            }
        }
        if (winningIndex == -1) {
            System.err.println("Нипраильное число: " + winningNumber);
            if (onComplete != null) onComplete.run();
            return;
        }

        isSpinning = true;
        final long durationNanos = 5_000_000_000L;
        final long startTime = System.nanoTime();
        final double startRotation = wheelRotation;

        double sectorAngle = 360.0 / 37;
        double targetSectorMiddle = winningIndex * sectorAngle + sectorAngle / 2.0;
        double rawRotation = 270 - targetSectorMiddle;
        double normalized = rawRotation % 360;
        if (normalized < 0) normalized += 360;
        double targetRotation = startRotation + 360 * 10 + (normalized - (startRotation % 360));

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
        gc.rotate(wheelRotation);
        gc.translate(-wheelCenterX, -wheelCenterY);

        gc.setFill(Color.web("#343A40"));
        gc.fillOval(wheelCenterX - wheelRadius - 5, wheelCenterY - wheelRadius - 5, wheelRadius * 2 + 10, wheelRadius * 2 + 10);

        double sectorAngle = 360.0 / 37;
        for (int i = 0; i < 37; i++) {
            int number = WHEEL_ORDER[i];
            Color color = ROULETTE_COLORS.getOrDefault(number, Color.LIGHTGRAY);
            gc.setFill(color);
            gc.fillArc(wheelCenterX - wheelRadius, wheelCenterY - wheelRadius, wheelRadius * 2, wheelRadius * 2,
                       i * sectorAngle, sectorAngle, javafx.scene.shape.ArcType.ROUND);
        }

        gc.setStroke(Color.web("#F0F2F5"));
        gc.setLineWidth(2);
        for (int i = 0; i < 37; i++) {
            double angle = Math.toRadians(i * sectorAngle);
            gc.strokeLine(wheelCenterX, wheelCenterY,
                          wheelCenterX + wheelRadius * Math.cos(angle),
                          wheelCenterY + wheelRadius * Math.sin(angle));
        }

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        for (int i = 0; i < 37; i++) {
            int number = WHEEL_ORDER[i];
            double angle = Math.toRadians(i * sectorAngle + sectorAngle / 2);
            double textRadius = wheelRadius * 0.8;
            double textX = wheelCenterX + textRadius * Math.cos(angle);
            double textY = wheelCenterY + textRadius * Math.sin(angle);
            gc.fillText(String.valueOf(number), textX, textY + 4);
        }

        gc.setFill(Color.web("#6C757D"));
        gc.fillOval(wheelCenterX - wheelRadius * 0.2, wheelCenterY - wheelRadius * 0.2, wheelRadius * 0.4, wheelRadius * 0.4);
        gc.setStroke(Color.web("#ADB5BD"));
        gc.strokeOval(wheelCenterX - wheelRadius * 0.2, wheelCenterY - wheelRadius * 0.2, wheelRadius * 0.4, wheelRadius * 0.4);

        gc.restore();

        gc.setFill(Color.web("#ecc800"));
        gc.fillPolygon(
            new double[]{wheelCenterX, wheelCenterX - 10, wheelCenterX + 10},
            new double[]{wheelCenterY - wheelRadius + 10, wheelCenterY - wheelRadius - 10, wheelCenterY - wheelRadius - 10},
            3
        );
    }
}