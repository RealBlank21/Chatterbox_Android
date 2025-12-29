package com.example.testing;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ScenarioDao {
    @Insert
    long insert(Scenario scenario);

    @Update
    void update(Scenario scenario);

    @Delete
    void delete(Scenario scenario);

    @Query("SELECT * FROM scenario WHERE character_id = :characterId ORDER BY id DESC")
    LiveData<List<Scenario>> getScenariosForCharacter(int characterId);

    @Query("SELECT * FROM scenario WHERE character_id = :characterId ORDER BY id DESC")
    List<Scenario> getScenariosForCharacterSync(int characterId);

    @Query("SELECT * FROM scenario WHERE character_id = :characterId AND is_default = 1 LIMIT 1")
    Scenario getDefaultScenario(int characterId);

    @Query("SELECT * FROM scenario WHERE character_id = :characterId AND is_default = 1 LIMIT 1")
    LiveData<Scenario> getDefaultScenarioLive(int characterId);

    @Query("SELECT * FROM scenario WHERE id = :id LIMIT 1")
    Scenario getScenarioById(int id);

    @Query("SELECT * FROM scenario WHERE id = :id LIMIT 1")
    LiveData<Scenario> getScenarioByIdLive(int id);

    @Query("UPDATE scenario SET is_default = 0 WHERE character_id = :characterId")
    void clearDefaultsForCharacter(int characterId);
}