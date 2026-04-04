// Bet.java
package roulette.model;

public class Bet {
    private String playerName;
    private String type;      // "number"
    private int value;        // номер от 0 до 36
    private int amount;

    public Bet(String playerName, String type, int value, int amount) {
        this.playerName = playerName;
        this.type = type;
        this.value = value;
        this.amount = amount;
    }

    // Getters and setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}