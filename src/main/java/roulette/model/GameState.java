// GameState.java
package roulette.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private int balance;
    private List<Bet> allBets = new ArrayList<>();
    private int lastSpinNumber = -1;
    private boolean bettingOpen = true;

    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
    public List<Bet> getAllBets() { return allBets; }
    public void setAllBets(List<Bet> allBets) { this.allBets = allBets; }
    public int getLastSpinNumber() { return lastSpinNumber; }
    public void setLastSpinNumber(int lastSpinNumber) { this.lastSpinNumber = lastSpinNumber; }
    public boolean isBettingOpen() { return bettingOpen; }
    public void setBettingOpen(boolean bettingOpen) { this.bettingOpen = bettingOpen; }
}