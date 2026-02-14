package com.aless.bicitrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SessioneEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessioneDao(): SessioneDao
}
