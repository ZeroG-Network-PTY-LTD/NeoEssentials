package com.zerog.neoessentials.economy.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Credit score tracking for players
 */
public class CreditScore {
    private final UUID playerId;
    private int score;
    private LocalDateTime lastUpdate;
    private int positiveActions;
    private int negativeActions;
    
    public CreditScore(UUID playerId) {
        this.playerId = playerId;
        this.score = 650; // Starting credit score
        this.lastUpdate = LocalDateTime.now();
        this.positiveActions = 0;
        this.negativeActions = 0;
    }
    
    public void updateScore(int change) {
        score = Math.max(300, Math.min(850, score + change));
        lastUpdate = LocalDateTime.now();
        
        if (change > 0) {
            positiveActions++;
        } else if (change < 0) {
            negativeActions++;
        }
    }
    
    // Getters
    public UUID getPlayerId() { return playerId; }
    public int getScore() { return score; }
    public LocalDateTime getLastUpdate() { return lastUpdate; }
    public int getPositiveActions() { return positiveActions; }
    public int getNegativeActions() { return negativeActions; }
}
