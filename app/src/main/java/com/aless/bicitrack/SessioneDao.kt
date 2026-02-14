package com.aless.bicitrack.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessioneDao {

    @Query("SELECT * FROM sessioni LIMIT 1")
    suspend fun getSessione(): SessioneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSessione(sessione: SessioneEntity)
}
