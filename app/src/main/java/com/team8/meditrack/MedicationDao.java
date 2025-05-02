package com.team8.meditrack;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for medications
 */
@Dao
public interface MedicationDao {
    @Insert
    void insert(Medication medication);

    @Delete
    void delete(Medication medication);

    @Query("SELECT * FROM medications ORDER BY time ASC")
    List<Medication> getAllMedications();

    @Query("SELECT * FROM medications WHERE id = :id")
    Medication getMedicationById(int id);
}