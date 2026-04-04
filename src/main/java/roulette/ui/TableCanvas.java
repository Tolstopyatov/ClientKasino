// TableCanvas.java
package roulette.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import roulette.model.Bet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TableCanvas extends Canvas {
    private final int ROWS = 3;
    private final int COLS = 12; // 0..36: 0 отдельно, затем 1-36 в 3x12
    private final double CELL_WIDTH;
    private final double CELL_HEIGHT;
    private final double ZERO_WIDTH = 60;
    private List<Bet> allBets;
    private String currentPlayerName = "You";
    private Bet pendingBet = null; // выбранная клетка для ставки

    public TableCanvas(double width, double height) {
        super(width, height);
        CELL_WIDTH = (width - ZERO_WIDTH) / COLS;
        CELL_HEIGHT = height / ROWS;
        setOnMouseClicked(e -> handleMouseClick(e.getX(), e.getY()));
        drawTable();
    }

    private void drawTable() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());
        // Рисуем поле для 0
        gc.setFill(Color.DARKGREEN);
        gc.fillRect(0, 0, ZERO_WIDTH, getHeight());
        gc.setStroke(Color.BLACK);
        gc.strokeRect(0, 0, ZERO_WIDTH, getHeight());
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(18));
        gc.fillText("0", ZERO_WIDTH/2 - 8, getHeight()/2 + 6);

        // Рисуем ячейки 1-36
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                double x = ZERO_WIDTH + col * CELL_WIDTH;
                double y = row * CELL_HEIGHT;
                int number = numberAt(row, col);
                gc.setFill(Color.LIGHTGRAY);
                gc.fillRect(x, y, CELL_WIDTH, CELL_HEIGHT);
                gc.setStroke(Color.BLACK);
                gc.strokeRect(x, y, CELL_WIDTH, CELL_HEIGHT);
                gc.setFill(Color.BLACK);
                gc.fillText(String.valueOf(number), x + CELL_WIDTH/2 - 8, y + CELL_HEIGHT/2 + 6);
            }
        }
        // Отрисовка ставок
        if (allBets != null) {
            for (Bet bet : allBets) {
                drawBet(bet);
            }
        }
    }

    private int numberAt(int row, int col) {
        // Европейская рулетка: 1-36 расположены: 3,6,9... 2,5,8... 1,4,7...
        int base = col * 3 + (row == 0 ? 3 : row == 1 ? 2 : 1);
        return base;
    }

    private void drawBet(Bet bet) {
        if (!"number".equals(bet.getType())) return;
        int num = bet.getValue();
        if (num == 0) {
            drawChipOnZero(bet);
        } else {
            drawChipOnNumber(num, bet);
        }
    }

    private void drawChipOnNumber(int number, Bet bet) {
        int row = (number % 3 == 0) ? 0 : (number % 3 == 2) ? 1 : 2;
        int col = (number - 1) / 3;
        double x = ZERO_WIDTH + col * CELL_WIDTH;
        double y = row * CELL_HEIGHT;
        GraphicsContext gc = getGraphicsContext2D();
        Color playerColor = bet.getPlayerName().equals(currentPlayerName) ? Color.YELLOW : Color.ORANGE;
        gc.setFill(playerColor);
        gc.fillOval(x + 5, y + 5, 20, 20);
        gc.setFill(Color.BLACK);
        gc.fillText(String.valueOf(bet.getAmount()), x + 10, y + 20);
        gc.fillText(bet.getPlayerName().substring(0, 1), x + 12, y + 12);
    }

    private void drawChipOnZero(Bet bet) {
        GraphicsContext gc = getGraphicsContext2D();
        Color playerColor = bet.getPlayerName().equals(currentPlayerName) ? Color.YELLOW : Color.ORANGE;
        gc.setFill(playerColor);
        gc.fillOval(15, getHeight()/2 - 10, 20, 20);
        gc.setFill(Color.BLACK);
        gc.fillText(String.valueOf(bet.getAmount()), 20, getHeight()/2 + 5);
        gc.fillText(bet.getPlayerName().substring(0, 1), 22, getHeight()/2 - 3);
    }

    private void handleMouseClick(double mouseX, double mouseY) {
        if (pendingBet == null) {
            int number = hitTest(mouseX, mouseY);
            if (number != -1) {
                pendingBet = new Bet(currentPlayerName, "number", number, 0);
                highlightCell(number);
            }
        }
    }

    private int hitTest(double x, double y) {
        if (x < ZERO_WIDTH) return 0;
        int col = (int) ((x - ZERO_WIDTH) / CELL_WIDTH);
        int row = (int) (y / CELL_HEIGHT);
        if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
            return numberAt(row, col);
        }
        return -1;
    }

    private void highlightCell(int number) {
        drawTable(); // перерисовка
        if (number == 0) {
            GraphicsContext gc = getGraphicsContext2D();
            gc.setStroke(Color.RED);
            gc.setLineWidth(3);
            gc.strokeRect(0, 0, ZERO_WIDTH, getHeight());
        } else {
            int row = (number % 3 == 0) ? 0 : (number % 3 == 2) ? 1 : 2;
            int col = (number - 1) / 3;
            double x = ZERO_WIDTH + col * CELL_WIDTH;
            double y = row * CELL_HEIGHT;
            GraphicsContext gc = getGraphicsContext2D();
            gc.setStroke(Color.RED);
            gc.setLineWidth(3);
            gc.strokeRect(x, y, CELL_WIDTH, CELL_HEIGHT);
        }
    }

    public Bet getPendingBet() { return pendingBet; }
    public void clearPendingBet() { pendingBet = null; drawTable(); }

    public void updateBets(List<Bet> bets, String playerName) {
        this.allBets = bets;
        this.currentPlayerName = playerName;
        drawTable();
    }
}