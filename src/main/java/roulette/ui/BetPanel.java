package roulette.ui;

import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import roulette.model.Bet;

public class BetPanel extends VBox {
    private final int chipValue;
    private final String betType;
    private final Object betValue; // для "number" не используется, для side – строка

    public BetPanel(int chipValue, String betType, Object betValue, String displayText, Color color) {
        this.chipValue = chipValue;
        this.betType = betType;
        this.betValue = betValue;

        setStyle("-fx-border-color: #7d7d7d; -fx-border-width: 1; -fx-padding: 8; -fx-background-radius: 5;");
        setSpacing(8);
        setPrefWidth(90);

        Circle chip = new Circle(28);
        chip.setFill(color);
        chip.setStroke(Color.BLACK);
        chip.setStrokeWidth(2);

        Label valueLabel = new Label(chipValue == 0 ? displayText : String.valueOf(chipValue));
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        valueLabel.setFont(Font.font(14));

        Label typeLabel = new Label(displayText);
        typeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
        setOnDragDetected(event -> {
            Dragboard db = startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            // Формат: тип:значение:номинал
            content.putString(betType + ":" + betValue + ":" + chipValue);
            db.setContent(content);
            event.consume();
        });

        getChildren().addAll(chip, valueLabel, typeLabel);
    }

    public int getChipValue() { return chipValue; }
    public String getBetType() { return betType; }
    public Object getBetValue() { return betValue; }
}