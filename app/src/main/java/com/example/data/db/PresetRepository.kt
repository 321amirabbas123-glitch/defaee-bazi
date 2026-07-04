package com.example.data.db

import com.example.domain.model.GamePreset
import kotlinx.coroutines.flow.Flow

class PresetRepository(private val dao: GamePresetDao) {
    val allPresets: Flow<List<GamePreset>> = dao.getAllPresets()

    suspend fun insert(preset: GamePreset) {
        dao.insertPreset(preset)
    }

    suspend fun update(preset: GamePreset) {
        dao.updatePreset(preset)
    }

    suspend fun delete(preset: GamePreset) {
        dao.deletePreset(preset)
    }
}
