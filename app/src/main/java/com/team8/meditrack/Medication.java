package com.team8.meditrack;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity class for storing medication schedules
 */
@Entity(tableName = "medications")
public class Medication {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String time;
    private long createdAt;

    public Medication(String name, String time, long createdAt) {
        this.name = name;
        this.time = time;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}