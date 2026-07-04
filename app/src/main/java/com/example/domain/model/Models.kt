package com.example.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

enum class TerrainType {
    GRASSLAND, HILLS, MOUNTAINS, WATER
}

enum class VisibilityMode {
    FULL_VISIBILITY, FOG_OF_WAR
}

enum class UnitCategory {
    MILITARY, ENGINEER
}

enum class UnitType(val displayName: String, val category: UnitCategory) {
    TANK("Tank", UnitCategory.MILITARY),
    INFANTRY("Infantry", UnitCategory.MILITARY),
    ARTILLERY("Artillery", UnitCategory.MILITARY),
    ENGINEER("Engineer", UnitCategory.ENGINEER)
}

@JsonClass(generateAdapter = true)
data class BoardTile(
    val row: Int,
    val col: Int,
    val terrain: TerrainType,
    val hasTrench: Boolean = false,
    val hasBridge: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GameUnit(
    val id: String,
    val owner: Int, // 1 for Player 1 (Host), 2 for Player 2 (Client)
    val type: UnitType,
    val row: Int,
    val col: Int,
    val hp: Int,
    val energy: Int
) {
    fun isAlive(): Boolean = hp > 0
}

@JsonClass(generateAdapter = true)
data class Castle(
    val owner: Int, // 1 or 2
    val row: Int,
    val col: Int,
    val hp: Int,
    val maxHp: Int
) {
    fun isAlive(): Boolean = hp > 0
}

enum class GamePhase {
    CONFIG,           // Host setup parameters
    MAP_EDIT,         // Host editing map and placing castles
    WAITING_FOR_CLIENT, // Host waiting for connection
    DEPLOYMENT,       // Both players placing their units
    PLAYING,          // Actual gameplay
    VICTORY           // Someone won
}

@Entity(tableName = "game_presets")
@JsonClass(generateAdapter = true)
data class GamePreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    
    // Map Config
    val mapWidth: Int = 11,
    val mapHeight: Int = 11,
    val grassPercent: Int = 50,
    val hillsPercent: Int = 20,
    val mountainsPercent: Int = 15,
    val waterPercent: Int = 15,
    val seed: Long? = null,
    
    // Raw serialized board layout: list of terrain ids in order of tiles
    val customTerrainData: String? = null, // e.g. "0,0,1,2..."
    val castle1Row: Int = 0,
    val castle1Col: Int = 5,
    val castle2Row: Int = 10,
    val castle2Col: Int = 5,

    // Castle Config
    val castleMaxHp: Int = 100,
    val castleMaxDamage: Int = 30,
    val castleAttackRange: Int = 2,
    val castleVisionRange: Int = 3,

    // Unit Configs
    val tankMaxHp: Int = 100,
    val tankMaxDamage: Int = 45,
    val tankMaxEnergy: Int = 100,
    val tankAttackRange: Int = 2,
    val tankVisionRange: Int = 3,
    val tankCount: Int = 2,

    val infantryMaxHp: Int = 50,
    val infantryMaxDamage: Int = 20,
    val infantryMaxEnergy: Int = 80,
    val infantryAttackRange: Int = 1,
    val infantryVisionRange: Int = 2,
    val infantryCount: Int = 3,

    val artilleryMaxHp: Int = 60,
    val artilleryMaxDamage: Int = 60,
    val artilleryMaxEnergy: Int = 60,
    val artilleryAttackRange: Int = 4,
    val artilleryVisionRange: Int = 2,
    val artilleryCount: Int = 1,

    val engineerMaxHp: Int = 40,
    val engineerMaxEnergy: Int = 80,
    val engineerVisionRange: Int = 2,
    val engineerCount: Int = 2,

    // Gameplay Rules
    val visibilityMode: VisibilityMode = VisibilityMode.FOG_OF_WAR,
    val trenchDamageReduction: Float = 0.3f, // 30% reduction
    
    // Energy Costs
    val costMoveGrass: Int = 10,
    val costMoveHills: Int = 20,
    val costBuildTrench: Int = 30,
    val costBuildBridge: Int = 40
)

@JsonClass(generateAdapter = true)
data class GameState(
    val phase: GamePhase = GamePhase.CONFIG,
    val activePlayer: Int = 1, // 1 or 2
    val mapWidth: Int = 11,
    val mapHeight: Int = 11,
    val tiles: List<BoardTile> = emptyList(),
    val castles: List<Castle> = emptyList(),
    val units: List<GameUnit> = emptyList(),
    val readyPlayers: List<Int> = emptyList(), // Ready states in DEPLOYMENT phase: e.g. [1, 2]
    val winnerPlayer: Int? = null,
    
    // Cloned Config rules from Preset
    val preset: GamePreset = GamePreset(name = "Default"),
    
    // Visual indicators / Floating log items (animations)
    val events: List<GameVisualEvent> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GameVisualEvent(
    val eventId: String,
    val type: String, // "MOVE", "ATTACK", "TRENCH", "BRIDGE", "DESTROYED", "DAMAGE"
    val unitId: String? = null,
    val owner: Int? = null,
    val fromRow: Int? = null,
    val fromCol: Int? = null,
    val toRow: Int? = null,
    val toCol: Int? = null,
    val amount: Int? = null, // e.g. damage amount
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class NetworkMessage(
    val type: String, // "CONNECT", "INIT_GAME", "SUBMIT_DEPLOYMENT", "PERFORM_ACTION", "SYNC_STATE", "DISCONNECT"
    val playerNumber: Int? = null, // 1 or 2
    val ipAddress: String? = null, // for connection info
    val initPreset: GamePreset? = null,
    val tiles: List<BoardTile>? = null,
    val castles: List<Castle>? = null,
    val deployedUnits: List<GameUnit>? = null,
    val requestedAction: ClientAction? = null,
    val syncState: GameState? = null
)

@JsonClass(generateAdapter = true)
data class ClientAction(
    val actionType: String, // "MOVE", "ATTACK", "TRENCH", "BRIDGE", "END_TURN"
    val unitId: String? = null,
    val targetPath: List<HexCoord>? = null, // for movement
    val targetIds: List<String>? = null, // for attacks (Military Unit attacking enemy Units)
    val targetCastleOwner: Int? = null // for attacks on Castle
)

@JsonClass(generateAdapter = true)
data class HexCoord(
    val r: Int,
    val c: Int
)
