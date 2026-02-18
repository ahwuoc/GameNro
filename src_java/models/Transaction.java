package models;

import java.time.LocalDateTime;

public class Transaction {

    public Long id;
    public Long playerId;

    public double amount;

    public String description;

    public boolean status;

    public boolean isReceive;

    public LocalDateTime lastTimeCheck;

    public LocalDateTime createdDate;

    // Constructors, getters, and setters

    public Transaction() {
        // Default constructor
    }

    // Add getters and setters for all fields

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    // Other fields and methods...

}

