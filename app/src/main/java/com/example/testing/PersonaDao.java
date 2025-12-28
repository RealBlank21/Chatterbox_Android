package com.example.testing;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PersonaDao {
    @Query("SELECT * FROM persona")
    LiveData<List<Persona>> getAllPersonas();

    @Query("SELECT * FROM persona")
    List<Persona> getAllPersonasSync();

    @Insert
    long insert(Persona persona);

    @Update
    void update(Persona persona);

    @Delete
    void delete(Persona persona);

    @Query("DELETE FROM persona")
    void deleteAll();

    @Query("SELECT * FROM persona WHERE id = :id")
    Persona getPersonaById(int id);

    @Query("SELECT * FROM persona WHERE id = :id")
    LiveData<Persona> getPersonaByIdLive(int id);
}