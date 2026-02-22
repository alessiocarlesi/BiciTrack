package com.aless.bicitrack.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessioni")
data class SessioneEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val jsonFasi: String,
    val soloFC: Boolean = false // Aggiunto per salvare la preferenza
)