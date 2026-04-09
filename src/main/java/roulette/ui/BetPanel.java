package roulette.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class BetPanel extends StackPane {
    private final int chipValue;
    private final String betType;

    public BetPanel(int chipValue, String betType, Color color) {
        this.chipValue = chipValue;
        this.betType = betType;

        setPrefSize(60, 60);
        setStyle("-fx-background-color: transparent;");

        Circle chip = new Circle(28);
        chip.setFill(color);
        chip.setStroke(Color.web("#343A40"));
        chip.setStrokeWidth(2);

        Label valueLabel = new Label(String.valueOf(chipValue));
        valueLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        valueLabel.setTextFill(Color.WHITE);

        setAlignment(Pos.CENTER);
        getChildren().addAll(chip, valueLabel);

        setOnDragDetected(event -> {
            Dragboard db = startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            // Передаём тип ставки и номинал (значение определится при drop)
            content.putString(betType + ":" + chipValue);
            db.setContent(content);
            event.consume();
        });
    }

    public int getChipValue() { return chipValue; }
    public String getBetType() { return betType; }
}