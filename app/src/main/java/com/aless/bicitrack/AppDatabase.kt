package com.aless.bicitrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.TypeConverter
import com.aless.bicitrack.FaseAllenamento
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// --- Converters per gestire il JSON delle fasi ---
class Converters {
    @TypeConverter
    fun fromString(value: String): List<FaseAllenamento> {
        val listType = object : TypeToken<List<FaseAllenamento>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<FaseAllenamento>): String {
        return Gson().toJson(list)
    }
}

// --- Definizione del Database ---
@Database(entities = [SessioneEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // Colleghiamo i convertitori qui
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessioneDao(): SessioneDao
}