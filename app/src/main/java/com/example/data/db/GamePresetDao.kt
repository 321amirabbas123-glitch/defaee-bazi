package com.example.data.db

import androidx.room.*
import com.example.domain.model.GamePreset
import kotlinx.coroutines.flow.Flow

@Dao
interface GamePresetDao {
    @Query("SELECT * FROM game_presets ORDER BY id DESC")
    fun getAllPresets(): Flow<List<GamePreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: GamePreset)

    @Update
    suspend fun updatePreset(preset: GamePreset)

    @Delete
    suspend fun deletePreset(preset: GamePreset)
}
