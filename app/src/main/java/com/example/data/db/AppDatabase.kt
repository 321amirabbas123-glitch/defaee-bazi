package com.example.data.db

import android.content.Context
import androidx.room.*
import com.example.domain.model.GamePreset
import com.example.domain.model.VisibilityMode

class Converters {
    @TypeConverter
    fun fromVisibilityMode(mode: VisibilityMode): String = mode.name

    @TypeConverter
    fun toVisibilityMode(name: String): VisibilityMode = VisibilityMode.valueOf(name)
}

@Database(entities = [GamePreset::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gamePresetDao(): GamePresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tactical_hex_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
