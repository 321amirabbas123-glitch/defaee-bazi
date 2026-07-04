package com.example.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.PresetRepository
import com.example.data.net.SocketManager
import com.example.domain.engine.GameEngine
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MainViewModel"

    private val database = AppDatabase.getDatabase(application)
    private val presetRepository = PresetRepository(database.gamePresetDao())
    private val socketManager = SocketManager()

    // Database Presets
    val allPresets: StateFlow<List<GamePreset>> = presetRepository.allPresets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current screen layout
    enum class AppScreen {
        MAIN_MENU,
        PRESET_LIST,
        PRESET_EDITOR,
        MAP_EDITOR,
        LAN_SETUP,
        DEPLOYMENT,
        BATTLE,
        VICTORY
    }

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Host or Client role
    enum class NetworkRole {
        NONE, HOST, CLIENT, SANDBOX
    }

    private val _networkRole = MutableStateFlow(NetworkRole.NONE)
    val networkRole: StateFlow<NetworkRole> = _networkRole.asStateFlow()

    // This client's active perspective in Sandbox mode (1 or 2)
    private val _sandboxActivePerspective = MutableStateFlow(1)
    val sandboxActivePerspective: StateFlow<Int> = _sandboxActivePerspective.asStateFlow()

    // The single source of truth GameState (for Host) or synchronized GameState (for Client)
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // Live configuration setup on Host
    private val _activeConfig = MutableStateFlow(GamePreset(name = "New Preset"))
    val activeConfig: StateFlow<GamePreset> = _activeConfig.asStateFlow()

    // IP Address Info
    val localIp: String = SocketManager.getLocalIpAddress()
    private val _connectedClientIp = MutableStateFlow<String?>(null)
    val connectedClientIp: StateFlow<String?> = _connectedClientIp.asStateFlow()

    private val _isNetworkConnecting = MutableStateFlow(false)
    val isNetworkConnecting: StateFlow<Boolean> = _isNetworkConnecting.asStateFlow()

    // Selection & Highlight state
    private val _selectedCoord = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedCoord: StateFlow<Pair<Int, Int>?> = _selectedCoord.asStateFlow()

    private val _selectedUnit = MutableStateFlow<GameUnit?>(null)
    val selectedUnit: StateFlow<GameUnit?> = _selectedUnit.asStateFlow()

    private val _reachableTiles = MutableStateFlow<Map<Pair<Int, Int>, List<Pair<Int, Int>>>>(emptyMap())
    val reachableTiles: StateFlow<Map<Pair<Int, Int>, List<Pair<Int, Int>>>> = _reachableTiles.asStateFlow()

    private val _attackTargets = MutableStateFlow<List<Any>>(emptyList())
    val attackTargets: StateFlow<List<Any>> = _attackTargets.asStateFlow()

    private val _selectedAttackTargets = MutableStateFlow<Set<String>>(emptySet())
    val selectedAttackTargets: StateFlow<Set<String>> = _selectedAttackTargets.asStateFlow()

    private val _selectedCastleAttackTarget = MutableStateFlow<Int?>(null)
    val selectedCastleAttackTarget: StateFlow<Int?> = _selectedCastleAttackTarget.asStateFlow()

    private val _isAttackMode = MutableStateFlow(false)
    val isAttackMode: StateFlow<Boolean> = _isAttackMode.asStateFlow()

    // For Client UI path confirmation
    private val _pendingMovePath = MutableStateFlow<List<Pair<Int, Int>>?>(null)
    val pendingMovePath: StateFlow<List<Pair<Int, Int>>?> = _pendingMovePath.asStateFlow()

    private val _pendingMoveCost = MutableStateFlow(0)
    val pendingMoveCost: StateFlow<Int> = _pendingMoveCost.asStateFlow()

    // System Log Toasts
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Player assignment (Host is 1, Client is 2)
    var myPlayerNumber: Int = 1
        private set

    init {
        // Configure Socket Server callbacks
        socketManager.onClientConnected = { ip ->
            _connectedClientIp.value = ip
            showToast("Player 2 connected!")
            if (_networkRole.value == NetworkRole.HOST) {
                // Progress Host to DEPLOYMENT state
                startDeploymentPhase()
            }
        }

        socketManager.onServerMessageReceived = { msg ->
            handleIncomingClientMessage(msg)
        }

        socketManager.onServerDisconnected = {
            _connectedClientIp.value = null
            showToast("Player 2 disconnected!")
            if (_gameState.value.phase != GamePhase.CONFIG && _gameState.value.phase != GamePhase.VICTORY) {
                // Game interrupted
                exitGame()
            }
        }

        // Configure Socket Client callbacks
        socketManager.onClientConnectionEstablished = {
            _isNetworkConnecting.value = false
            showToast("Connected to Host!")
            // Wait for Server to send the initialized board & configurations
        }

        socketManager.onClientMessageReceived = { msg ->
            handleIncomingHostMessage(msg)
        }

        socketManager.onClientDisconnected = {
            _isNetworkConnecting.value = false
            showToast("Disconnected from host.")
            exitGame()
        }

        // Insert a default preset if none exists
        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            if (allPresets.value.isEmpty()) {
                presetRepository.insert(GamePreset(name = "Classic Battle (11x11)"))
                presetRepository.insert(GamePreset(name = "Canyon Valley (9x15)", mapWidth = 9, mapHeight = 15, hillsPercent = 30, mountainsPercent = 25, waterPercent = 20))
            }
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
        viewModelScope.launch {
            delay(3000)
            if (_toastMessage.value == msg) {
                _toastMessage.value = null
            }
        }
    }

    // --- SCREEN NAVIGATION ---

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun exitGame() {
        socketManager.clear()
        _networkRole.value = NetworkRole.NONE
        _connectedClientIp.value = null
        _gameState.value = GameState()
        _currentScreen.value = AppScreen.MAIN_MENU
        clearSelection()
    }

    // --- PRESETS CRUD ---

    fun createNewPreset() {
        _activeConfig.value = GamePreset(name = "My Custom Game")
        _currentScreen.value = AppScreen.PRESET_EDITOR
    }

    fun editPreset(preset: GamePreset) {
        _activeConfig.value = preset
        _currentScreen.value = AppScreen.PRESET_EDITOR
    }

    fun updateActiveConfig(config: GamePreset) {
        _activeConfig.value = config
    }

    fun savePreset(preset: GamePreset) {
        viewModelScope.launch(Dispatchers.IO) {
            if (preset.id == 0L) {
                presetRepository.insert(preset)
            } else {
                presetRepository.update(preset)
            }
            withContext(Dispatchers.Main) {
                showToast("Preset saved successfully!")
                _currentScreen.value = AppScreen.PRESET_LIST
            }
        }
    }

    fun deletePreset(preset: GamePreset) {
        viewModelScope.launch(Dispatchers.IO) {
            presetRepository.delete(preset)
            withContext(Dispatchers.Main) {
                showToast("Preset deleted.")
            }
        }
    }

    fun duplicatePreset(preset: GamePreset) {
        viewModelScope.launch(Dispatchers.IO) {
            val copy = preset.copy(id = 0L, name = "${preset.name} (Copy)")
            presetRepository.insert(copy)
            withContext(Dispatchers.Main) {
                showToast("Preset duplicated.")
            }
        }
    }

    // --- GAME LOBBY & HOST SEQUENCING ---

    fun hostLANMatch(preset: GamePreset) {
        myPlayerNumber = 1
        _networkRole.value = NetworkRole.HOST
        _activeConfig.value = preset
        
        // Step 1: Generate initial map
        val tiles = GameEngine.generateMap(preset)
        
        // Place initial castles automatically at suggested position
        val c1Row = preset.castle1Row.coerceIn(0, preset.mapHeight - 1)
        val c1Col = preset.castle1Col.coerceIn(0, preset.mapWidth - 1)
        val c2Row = preset.castle2Row.coerceIn(0, preset.mapHeight - 1)
        val c2Col = preset.castle2Col.coerceIn(0, preset.mapWidth - 1)

        val castles = listOf(
            Castle(1, c1Row, c1Col, preset.castleMaxHp, preset.castleMaxHp),
            Castle(2, c2Row, c2Col, preset.castleMaxHp, preset.castleMaxHp)
        )

        _gameState.value = GameState(
            phase = GamePhase.MAP_EDIT,
            tiles = tiles,
            castles = castles,
            mapWidth = preset.mapWidth,
            mapHeight = preset.mapHeight,
            preset = preset
        )
        
        _currentScreen.value = AppScreen.MAP_EDITOR
    }

    fun startLANLobby() {
        // Start TCP Server
        socketManager.startServer()
        _gameState.value = _gameState.value.copy(phase = GamePhase.WAITING_FOR_CLIENT)
        _currentScreen.value = AppScreen.LAN_SETUP
    }

    fun startSandboxMatch(preset: GamePreset) {
        myPlayerNumber = 1 // Controls Player 1 Host perspective
        _networkRole.value = NetworkRole.SANDBOX
        _sandboxActivePerspective.value = 1
        _activeConfig.value = preset

        // 1. Generate map
        val tiles = GameEngine.generateMap(preset)
        val castles = listOf(
            Castle(1, preset.castle1Row, preset.castle1Col, preset.castleMaxHp, preset.castleMaxHp),
            Castle(2, preset.castle2Row, preset.castle2Col, preset.castleMaxHp, preset.castleMaxHp)
        )

        _gameState.value = GameState(
            phase = GamePhase.MAP_EDIT,
            tiles = tiles,
            castles = castles,
            mapWidth = preset.mapWidth,
            mapHeight = preset.mapHeight,
            preset = preset
        )

        _currentScreen.value = AppScreen.MAP_EDITOR
    }

    fun toggleSandboxPerspective() {
        val current = _sandboxActivePerspective.value
        val next = if (current == 1) 2 else 1
        _sandboxActivePerspective.value = next
        myPlayerNumber = next
        clearSelection()
        showToast("Switched viewpoint to Player $next")
    }

    fun joinLANMatch(ip: String) {
        myPlayerNumber = 2
        _networkRole.value = NetworkRole.CLIENT
        _isNetworkConnecting.value = true
        _currentScreen.value = AppScreen.LAN_SETUP
        socketManager.connectToHost(ip)
    }

    // --- MAP EDITOR METHODS (HOST ONLY) ---

    fun paintTileTerrain(row: Int, col: Int, terrain: TerrainType) {
        if (_networkRole.value != NetworkRole.HOST && _networkRole.value != NetworkRole.SANDBOX) return
        val current = _gameState.value
        val updatedTiles = current.tiles.map { t ->
            if (t.row == row && t.col == col) t.copy(terrain = terrain) else t
        }
        _gameState.value = current.copy(tiles = updatedTiles)
    }

    fun placeCastle(player: Int, row: Int, col: Int) {
        if (_networkRole.value != NetworkRole.HOST && _networkRole.value != NetworkRole.SANDBOX) return
        val current = _gameState.value
        val updatedCastles = current.castles.map { c ->
            if (c.owner == player) c.copy(row = row, col = col) else c
        }
        _gameState.value = current.copy(castles = updatedCastles)
    }

    fun generateNewRandomMap(preset: GamePreset) {
        if (_networkRole.value != NetworkRole.HOST && _networkRole.value != NetworkRole.SANDBOX) return
        val current = _gameState.value
        val nextTiles = GameEngine.generateMap(preset)
        _gameState.value = current.copy(
            tiles = nextTiles,
            preset = preset,
            mapWidth = preset.mapWidth,
            mapHeight = preset.mapHeight
        )
    }

    fun finalizeMapAndContinue() {
        // Verify both castles placed, not overlapping, and on valid tiles (not mountain or water ideally, but at least distinct coords)
        val current = _gameState.value
        val c1 = current.castles.first { it.owner == 1 }
        val c2 = current.castles.first { it.owner == 2 }

        if (c1.row == c2.row && c1.col == c2.col) {
            showToast("Castles cannot be on the exact same tile!")
            return
        }

        // Lock map layout into preset serialization
        val updatedPreset = current.preset.copy(
            castle1Row = c1.row, castle1Col = c1.col,
            castle2Row = c2.row, castle2Col = c2.col,
            customTerrainData = current.tiles.map { it.terrain.ordinal }.joinToString(",")
        )

        _gameState.value = current.copy(preset = updatedPreset)

        if (_networkRole.value == NetworkRole.SANDBOX) {
            startDeploymentPhase()
        } else {
            startLANLobby()
        }
    }

    // --- DEPLOYMENT PHASE ---

    private fun startDeploymentPhase() {
        val current = _gameState.value
        val p1Units = GameEngine.createDeploymentUnits(1, current.preset)
        val p2Units = GameEngine.createDeploymentUnits(2, current.preset)

        val nextState = current.copy(
            phase = GamePhase.DEPLOYMENT,
            units = p1Units + p2Units,
            readyPlayers = emptyList()
        )
        _gameState.value = nextState

        if (_networkRole.value == NetworkRole.HOST) {
            // Push initialization payload to Client
            socketManager.sendToClient(
                NetworkMessage(
                    type = "INIT_GAME",
                    playerNumber = 2,
                    initPreset = nextState.preset,
                    tiles = nextState.tiles,
                    castles = nextState.castles,
                    deployedUnits = nextState.units
                )
            )
            _currentScreen.value = AppScreen.DEPLOYMENT
        } else if (_networkRole.value == NetworkRole.SANDBOX) {
            _currentScreen.value = AppScreen.DEPLOYMENT
        }
    }

    // Place a unit from reserves during Deployment
    fun deployUnit(unitId: String, row: Int, col: Int) {
        val current = _gameState.value
        if (current.phase != GamePhase.DEPLOYMENT) return

        // Verify coordinate inside deployment zone
        if (!GameEngine.isCoordinateInDeploymentZone(myPlayerNumber, row, current.mapHeight)) {
            showToast("You can only deploy inside your deployment zone (nearest 2 rows)!")
            return
        }

        // Verify coordinate not already occupied by another deployed unit of same/different player
        // Exception: Engineers can share with a combat unit! But Engineer + Engineer is illegal.
        val otherUnits = current.units.filter { it.row == row && it.col == col && it.id != unitId }
        val placementUnit = current.units.firstOrNull { it.id == unitId } ?: return

        val isEngineer = placementUnit.type == UnitType.ENGINEER
        val hasEngineer = otherUnits.any { it.type == UnitType.ENGINEER }
        val hasCombatUnit = otherUnits.any { it.type != UnitType.ENGINEER }

        if (otherUnits.isNotEmpty()) {
            val blocked = when {
                isEngineer && hasEngineer -> true
                !isEngineer && hasCombatUnit -> true
                !isEngineer && hasEngineer -> false // legal
                isEngineer && hasCombatUnit -> false // legal
                else -> true
            }
            if (blocked) {
                showToast("This tile is fully occupied!")
                return
            }
        }

        // Also check if Castle is on this coordinate
        val hasCastle = current.castles.any { it.row == row && it.col == col }
        if (hasCastle) {
            showToast("Cannot place unit on a Castle!")
            return
        }

        // Update coordinate
        val nextUnits = current.units.map { u ->
            if (u.id == unitId) u.copy(row = row, col = col) else u
        }

        _gameState.value = current.copy(units = nextUnits)

        // If client, we synchronize deployment updates to server
        if (_networkRole.value == NetworkRole.CLIENT) {
            socketManager.sendToHost(
                NetworkMessage(
                    type = "SUBMIT_DEPLOYMENT",
                    playerNumber = 2,
                    deployedUnits = nextUnits.filter { it.owner == 2 }
                )
            )
        }
    }

    fun undeployUnit(unitId: String) {
        val current = _gameState.value
        if (current.phase != GamePhase.DEPLOYMENT) return

        val nextUnits = current.units.map { u ->
            if (u.id == unitId) u.copy(row = -1, col = -1) else u
        }
        _gameState.value = current.copy(units = nextUnits)

        if (_networkRole.value == NetworkRole.CLIENT) {
            socketManager.sendToHost(
                NetworkMessage(
                    type = "SUBMIT_DEPLOYMENT",
                    playerNumber = 2,
                    deployedUnits = nextUnits.filter { it.owner == 2 }
                )
            )
        }
    }

    fun setReady() {
        val current = _gameState.value
        val incompleteDeploy = current.units.any { it.owner == myPlayerNumber && it.row == -1 }
        if (incompleteDeploy) {
            showToast("You must deploy all units before setting Ready!")
            return
        }

        if (_networkRole.value == NetworkRole.CLIENT) {
            // Tell host we are ready
            socketManager.sendToHost(
                NetworkMessage(
                    type = "SUBMIT_DEPLOYMENT",
                    playerNumber = 2,
                    deployedUnits = current.units.filter { it.owner == 2 },
                    initPreset = current.preset // signal ready
                )
            )
            // Local state waits for Host sync to transition screen
            showToast("Waiting for Host...")
        } else {
            // Host or Sandbox
            val currentReady = current.readyPlayers.toMutableList()
            if (!currentReady.contains(myPlayerNumber)) {
                currentReady.add(myPlayerNumber)
            }

            val nextState = current.copy(readyPlayers = currentReady)
            _gameState.value = nextState

            checkReadyAndStartMatch(nextState)
        }
    }

    private fun checkReadyAndStartMatch(state: GameState) {
        if (_networkRole.value == NetworkRole.SANDBOX) {
            if (state.readyPlayers.contains(1) && state.readyPlayers.contains(2)) {
                _gameState.value = state.copy(
                    phase = GamePhase.PLAYING,
                    activePlayer = 1,
                    readyPlayers = emptyList()
                )
                _currentScreen.value = AppScreen.BATTLE
                showToast("Deployment complete! Battle starts! Player 1's Turn.")
            } else {
                showToast("Player $myPlayerNumber is Ready! Toggle view to configure and ready Player ${if (myPlayerNumber == 1) 2 else 1}.")
            }
        } else if (_networkRole.value == NetworkRole.HOST) {
            // Host waits for Client ready signal (Client sets ready by submitting deployment with initPreset flag)
            if (state.readyPlayers.contains(1) && state.readyPlayers.contains(2)) {
                _gameState.value = state.copy(
                    phase = GamePhase.PLAYING,
                    activePlayer = 1,
                    readyPlayers = emptyList()
                )
                _currentScreen.value = AppScreen.BATTLE
                showToast("Battle starts! Player 1's Turn.")
                syncStateToClient()
            }
        }
    }

    // --- GAMEPLAY SCREEN INTERACTION ---

    fun onTileSelected(row: Int, col: Int) {
        val state = _gameState.value
        if (state.phase != GamePhase.PLAYING) return

        // Verify whose turn it is
        if (_networkRole.value != NetworkRole.SANDBOX && state.activePlayer != myPlayerNumber) {
            showToast("It is not your turn!")
            return
        }

        val clickedUnit = state.units.firstOrNull { it.row == row && it.col == col && it.isAlive() }
        val activeSelectedUnit = _selectedUnit.value

        // Handle target selection in Attack Mode
        if (_isAttackMode.value && activeSelectedUnit != null) {
            val targetedCastle = state.castles.firstOrNull { it.row == row && it.col == col && it.isAlive() }
            if (clickedUnit != null && clickedUnit.owner != myPlayerNumber) {
                // Tapping an enemy unit in Attack Mode
                val id = clickedUnit.id
                val updatedTargets = _selectedAttackTargets.value.toMutableSet()
                if (updatedTargets.contains(id)) {
                    updatedTargets.remove(id)
                } else {
                    updatedTargets.add(id)
                }
                _selectedAttackTargets.value = updatedTargets
                _selectedCastleAttackTarget.value = null
                return
            } else if (targetedCastle != null && targetedCastle.owner != myPlayerNumber) {
                // Tapping enemy castle in Attack Mode
                _selectedCastleAttackTarget.value = targetedCastle.owner
                _selectedAttackTargets.value = emptySet()
                return
            }
        }

        // Regular selection
        if (clickedUnit != null && clickedUnit.owner == myPlayerNumber) {
            // Tapping friendly unit select
            _selectedCoord.value = Pair(row, col)
            _selectedUnit.value = clickedUnit
            _isAttackMode.value = false
            _selectedAttackTargets.value = emptySet()
            _selectedCastleAttackTarget.value = null
            _pendingMovePath.value = null

            // Compute reachable
            _reachableTiles.value = GameEngine.getReachableTiles(clickedUnit, state.tiles, state.units, state.preset)
            // Compute attack targets
            _attackTargets.value = GameEngine.getLegalAttackTargets(clickedUnit, state.tiles, state.units, state.castles, state.preset)
        } else {
            // Tapping somewhere else
            if (activeSelectedUnit != null) {
                val path = _reachableTiles.value[Pair(row, col)]
                if (path != null) {
                    // Clicked a reachable tile! Show pending move for confirmation
                    _pendingMovePath.value = path
                    var totalCost = 0
                    val tileMap = state.tiles.associateBy { Pair(it.row, it.col) }
                    for (i in 1 until path.size) {
                        val tile = tileMap[path[i]] ?: break
                        val stepCost = GameEngine.getTileMoveCost(tile, state.preset) ?: break
                        totalCost += stepCost
                    }
                    _pendingMoveCost.value = totalCost
                } else {
                    // Tapping empty tile with active unit - clears selection or handles bridge building
                    clearSelection()
                }
            } else {
                clearSelection()
            }
        }
    }

    fun confirmPendingMove() {
        val path = _pendingMovePath.value ?: return
        val unit = _selectedUnit.value ?: return
        
        requestGameplayAction(
            ClientAction(
                actionType = "MOVE",
                unitId = unit.id,
                targetPath = path.map { HexCoord(it.first, it.second) }
            )
        )
        clearSelection()
    }

    fun startAttackMode() {
        val unit = _selectedUnit.value ?: return
        if (unit.type == UnitType.ENGINEER) return
        _isAttackMode.value = true
        _selectedAttackTargets.value = emptySet()
        _selectedCastleAttackTarget.value = null
        showToast("Tap enemy target(s) inside red hexes, then click Confirm!")
    }

    fun confirmAttack() {
        val unit = _selectedUnit.value ?: return
        val targetIds = _selectedAttackTargets.value.toList()
        val castleOwner = _selectedCastleAttackTarget.value

        if (targetIds.isEmpty() && castleOwner == null) {
            showToast("Select at least one enemy target first!")
            return
        }

        requestGameplayAction(
            ClientAction(
                actionType = "ATTACK",
                unitId = unit.id,
                targetIds = targetIds,
                targetCastleOwner = castleOwner
            )
        )
        clearSelection()
    }

    fun buildTrench() {
        val unit = _selectedUnit.value ?: return
        if (unit.type != UnitType.ENGINEER) return

        requestGameplayAction(
            ClientAction(
                actionType = "TRENCH",
                unitId = unit.id
            )
        )
        clearSelection()
    }

    fun buildBridge(neighborRow: Int, neighborCol: Int) {
        val unit = _selectedUnit.value ?: return
        if (unit.type != UnitType.ENGINEER) return

        requestGameplayAction(
            ClientAction(
                actionType = "BRIDGE",
                unitId = unit.id,
                targetPath = listOf(HexCoord(neighborRow, neighborCol))
            )
        )
        clearSelection()
    }

    fun endTurn() {
        requestGameplayAction(
            ClientAction(
                actionType = "END_TURN"
            )
        )
        clearSelection()
    }

    fun clearSelection() {
        _selectedCoord.value = null
        _selectedUnit.value = null
        _reachableTiles.value = emptyMap()
        _attackTargets.value = emptyList()
        _selectedAttackTargets.value = emptySet()
        _selectedCastleAttackTarget.value = null
        _isAttackMode.value = false
        _pendingMovePath.value = null
        _pendingMoveCost.value = 0
    }

    // --- GAME ACTIONS: LOCAL & SOCKET TRANSMISSION ---

    private fun requestGameplayAction(action: ClientAction) {
        if (_networkRole.value == NetworkRole.CLIENT) {
            // Send request to Host for validation
            socketManager.sendToHost(
                NetworkMessage(
                    type = "PERFORM_ACTION",
                    playerNumber = 2,
                    requestedAction = action
                )
            )
        } else {
            // Host or Sandbox: Execute directly
            executeActionOnHost(myPlayerNumber, action)
        }
    }

    private fun executeActionOnHost(player: Int, action: ClientAction) {
        val current = _gameState.value
        val (nextState, events) = GameEngine.validateAndPerformAction(current, player, action)
        
        _gameState.value = nextState

        // Process visual descriptions to display toast log briefly
        for (event in events) {
            when (event.type) {
                "DAMAGE" -> {
                    val targetName = if (event.unitId != null) event.unitId.split("_").getOrNull(1) ?: "Unit" else "Castle"
                    showToast("Player ${event.owner} $targetName took ${event.amount} Damage!")
                }
                "DESTROYED" -> {
                    showToast("${event.unitId ?: "Unit"} was DESTROYED!")
                }
                "TRENCH" -> {
                    showToast("Trench built on tile (${event.toRow}, ${event.toCol})!")
                }
                "BRIDGE" -> {
                    showToast("Bridge built on Water tile (${event.toRow}, ${event.toCol})!")
                }
                "END_TURN" -> {
                    showToast("Turn ended. Player ${nextState.activePlayer}'s Turn!")
                }
            }
        }

        if (_networkRole.value == NetworkRole.HOST) {
            syncStateToClient()
        }
    }

    private fun syncStateToClient() {
        socketManager.sendToClient(
            NetworkMessage(
                type = "SYNC_STATE",
                syncState = _gameState.value
            )
        )
    }

    // --- PACKET/SOCKET MESSAGE DISPATCHERS ---

    private fun handleIncomingClientMessage(msg: NetworkMessage) {
        when (msg.type) {
            "CONNECT" -> {
                // Done automatically in socketManager
            }
            "SUBMIT_DEPLOYMENT" -> {
                val current = _gameState.value
                val clientUnits = msg.deployedUnits ?: return
                
                // Overlay client units onto host units
                val nextUnits = current.units.map { u ->
                    val replacement = clientUnits.firstOrNull { it.id == u.id }
                    replacement ?: u
                }

                var nextReady = current.readyPlayers.toMutableList()
                if (msg.initPreset != null) { // client signaled ready
                    if (!nextReady.contains(2)) {
                        nextReady.add(2)
                    }
                }

                val nextState = current.copy(units = nextUnits, readyPlayers = nextReady)
                _gameState.value = nextState

                checkReadyAndStartMatch(nextState)
            }
            "PERFORM_ACTION" -> {
                val action = msg.requestedAction ?: return
                executeActionOnHost(2, action)
            }
        }
    }

    private fun handleIncomingHostMessage(msg: NetworkMessage) {
        when (msg.type) {
            "INIT_GAME" -> {
                val preset = msg.initPreset ?: return
                _gameState.value = GameState(
                    phase = GamePhase.DEPLOYMENT,
                    tiles = msg.tiles ?: emptyList(),
                    castles = msg.castles ?: emptyList(),
                    units = msg.deployedUnits ?: emptyList(),
                    preset = preset,
                    mapWidth = preset.mapWidth,
                    mapHeight = preset.mapHeight
                )
                _currentScreen.value = AppScreen.DEPLOYMENT
            }
            "SYNC_STATE" -> {
                val sync = msg.syncState ?: return
                _gameState.value = sync
                
                // Show floating message
                val event = sync.events.firstOrNull()
                if (event != null) {
                    when (event.type) {
                        "DAMAGE" -> {
                            val targetName = if (event.unitId != null) event.unitId.split("_").getOrNull(1) ?: "Unit" else "Castle"
                            showToast("Player ${event.owner} $targetName took ${event.amount} Damage!")
                        }
                        "DESTROYED" -> {
                            showToast("${event.unitId ?: "Unit"} was DESTROYED!")
                        }
                        "TRENCH" -> {
                            showToast("Trench built on tile (${event.toRow}, ${event.toCol})!")
                        }
                        "BRIDGE" -> {
                            showToast("Bridge built on Water tile (${event.toRow}, ${event.toCol})!")
                        }
                        "END_TURN" -> {
                            showToast("Turn ended. Player ${sync.activePlayer}'s Turn!")
                        }
                    }
                }

                if (sync.phase == GamePhase.VICTORY) {
                    _currentScreen.value = AppScreen.VICTORY
                } else if (sync.phase == GamePhase.PLAYING) {
                    _currentScreen.value = AppScreen.BATTLE
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.clear()
    }
}
