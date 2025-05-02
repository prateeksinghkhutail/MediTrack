package com.team8.meditrack;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity class for storing health parameter readings
 */
@Entity(tableName = "health_parameters")
public class HealthParameter {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String type; // "temperature", "heart_rate", "spo2"
    private double value;
    private long timestamp;

    public HealthParameter(String type, double value, long timestamp) {
        this.type = type;
        this.value = value;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}