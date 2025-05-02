package com.team8.meditrack;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database for the application
 */
@Database(entities = {HealthParameter.class, Medication.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "meditrack_db";
    private static AppDatabase instance;

    public abstract HealthParameterDao healthParameterDao();
    public abstract MedicationDao medicationDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                    .allowMainThreadQueries() // For simplicity; in production use background threads
                    .build();
        }
        return instance;
    }
}