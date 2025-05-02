package com.team8.meditrack;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for health parameters
 */
@Dao
public interface HealthParameterDao {
    @Insert
    void insert(HealthParameter parameter);

    @Query("SELECT * FROM health_parameters WHERE type = :type ORDER BY timestamp DESC LIMIT 10")
    List<HealthParameter> getLatestByType(String type);

    @Query("SELECT * FROM health_parameters WHERE type = :type ORDER BY timestamp DESC LIMIT 1")
    HealthParameter getLatestValueByType(String type);

    @Query("DELETE FROM health_parameters WHERE type = :type AND id NOT IN " +
            "(SELECT id FROM health_parameters WHERE type = :type ORDER BY timestamp DESC LIMIT 10)")
    void keepLatest10ByType(String type);
}