package com.aless.bicitrack.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessioneDao {

    // Ordiniamo per ID decrescente per assicurarci di prendere l'ultimo allenamento salvato
    @Query("SELECT * FROM sessioni ORDER BY id DESC LIMIT 1")
    suspend fun getSessione(): SessioneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSessione(sessione: SessioneEntity)

    // Opzionale: utile per la futura schermata storico
    @Query("SELECT * FROM sessioni ORDER BY id DESC")
    suspend fun getAllSessions(): List<SessioneEntity>
}