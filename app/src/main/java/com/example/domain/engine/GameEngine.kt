package com.example.domain.engine

import com.example.domain.model.*
import java.util.UUID

object GameEngine {

    // --- COORD MATHS ---

    fun offsetToCube(row: Int, col: Int): Triple<Int, Int, Int> {
        val x = col - (row + (row and 1)) / 2
        val z = row
        val y = -x - z
        return Triple(x, y, z)
    }

    fun cubeToOffset(x: Int, y: Int, z: Int): Pair<Int, Int> {
        val row = z
        val col = x + (row + (row and 1)) / 2
        return Pair(row, col)
    }

    fun cubeRound(x: Float, y: Float, z: Float): Triple<Int, Int, Int> {
        var rx = kotlin.math.round(x).toInt()
        var ry = kotlin.math.round(y).toInt()
        var rz = kotlin.math.round(z).toInt()

        val xDiff = kotlin.math.abs(rx - x)
        val yDiff = kotlin.math.abs(ry - y)
        val zDiff = kotlin.math.abs(rz - z)

        if (xDiff > yDiff && xDiff > zDiff) {
            rx = -ry - rz
        } else if (yDiff > zDiff) {
            ry = -rx - rz
        } else {
            rz = -rx - ry
        }
        return Triple(rx, ry, rz)
    }

    fun hexDistance(r1: Int, c1: Int, r2: Int, c2: Int): Int {
        val (x1, y1, z1) = offsetToCube(r1, c1)
        val (x2, y2, z2) = offsetToCube(r2, c2)
        return (kotlin.math.abs(x1 - x2) + kotlin.math.abs(y1 - y2) + kotlin.math.abs(z1 - z2)) / 2
    }

    fun getNeighbors(row: Int, col: Int, width: Int, height: Int): List<Pair<Int, Int>> {
        val neighbors = mutableListOf<Pair<Int, Int>>()
        val isRowEven = row % 2 == 0
        val dirs = if (isRowEven) {
            arrayOf(
                Pair(-1, 0), Pair(-1, 1),
                Pair(0, -1), Pair(0, 1),
                Pair(1, 0), Pair(1, 1)
            )
        } else {
            arrayOf(
                Pair(-1, -1), Pair(-1, 0),
                Pair(0, -1), Pair(0, 1),
                Pair(1, -1), Pair(1, 0)
            )
        }

        for (d in dirs) {
            val nr = row + d.first
            val nc = col + d.second
            if (nr in 0 until height && nc in 0 until width) {
                neighbors.add(Pair(nr, nc))
            }
        }
        return neighbors
    }

    // Line of Sight: List of all intermediate coordinates (excluding start and end)
    fun getLineOfSightPath(r1: Int, c1: Int, r2: Int, c2: Int): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        val dist = hexDistance(r1, c1, r2, c2)
        if (dist <= 1) return path // adjacent, nothing blocks

        val p1 = offsetToCube(r1, c1)
        val p2 = offsetToCube(r2, c2)

        for (i in 1 until dist) {
            val t = i.toFloat() / dist
            val x = p1.first * (1 - t) + p2.first * t
            val y = p1.second * (1 - t) + p2.second * t
            val z = p1.third * (1 - t) + p2.third * t
            val rounded = cubeRound(x, y, z)
            path.add(cubeToOffset(rounded.first, rounded.second, rounded.third))
        }
        return path
    }

    // Check if line of fire is blocked by Mountain
    fun isLineOfFireBlocked(r1: Int, c1: Int, r2: Int, c2: Int, tiles: List<BoardTile>, width: Int): Boolean {
        val path = getLineOfSightPath(r1, c1, r2, c2)
        val tileMap = tiles.associateBy { Pair(it.row, it.col) }
        for (coord in path) {
            val t = tileMap[coord]
            if (t?.terrain == TerrainType.MOUNTAINS) {
                return true
            }
        }
        return false
    }

    // --- MAP GENERATION ---

    fun generateMap(preset: GamePreset): List<BoardTile> {
        val width = preset.mapWidth
        val height = preset.mapHeight
        val totalTiles = width * height

        // Calculate distribution
        val gCount = (totalTiles * preset.grassPercent / 100).coerceAtLeast(1)
        val hCount = (totalTiles * preset.hillsPercent / 100).coerceAtLeast(0)
        val mCount = (totalTiles * preset.mountainsPercent / 100).coerceAtLeast(0)
        val wCount = (totalTiles * preset.waterPercent / 100).coerceAtLeast(0)

        val list = mutableListOf<TerrainType>()
        repeat(gCount) { list.add(TerrainType.GRASSLAND) }
        repeat(hCount) { list.add(TerrainType.HILLS) }
        repeat(mCount) { list.add(TerrainType.MOUNTAINS) }
        repeat(wCount) { list.add(TerrainType.WATER) }

        // Fill remaining with Grassland if any rounding errors
        while (list.size < totalTiles) {
            list.add(TerrainType.GRASSLAND)
        }

        // Shuffle with seed if provided
        val random = if (preset.seed != null) java.util.Random(preset.seed) else java.util.Random()
        list.shuffle(random)

        val tiles = mutableListOf<BoardTile>()
        var index = 0
        for (r in 0 until height) {
            for (c in 0 until width) {
                tiles.add(BoardTile(r, c, list[index++]))
            }
        }
        return tiles
    }

    // --- GAMEPLAY CONFIG GETTERS ---

    fun getUnitMaxHp(type: UnitType, preset: GamePreset): Int {
        return when (type) {
            UnitType.TANK -> preset.tankMaxHp
            UnitType.INFANTRY -> preset.infantryMaxHp
            UnitType.ARTILLERY -> preset.artilleryMaxHp
            UnitType.ENGINEER -> preset.engineerMaxHp
        }
    }

    fun getUnitMaxDamage(type: UnitType, preset: GamePreset): Int {
        return when (type) {
            UnitType.TANK -> preset.tankMaxDamage
            UnitType.INFANTRY -> preset.infantryMaxDamage
            UnitType.ARTILLERY -> preset.artilleryMaxDamage
            UnitType.ENGINEER -> 0
        }
    }

    fun getUnitMaxEnergy(type: UnitType, preset: GamePreset): Int {
        return when (type) {
            UnitType.TANK -> preset.tankMaxEnergy
            UnitType.INFANTRY -> preset.infantryMaxEnergy
            UnitType.ARTILLERY -> preset.artilleryMaxEnergy
            UnitType.ENGINEER -> preset.engineerMaxEnergy
        }
    }

    fun getUnitAttackRange(type: UnitType, preset: GamePreset): Int {
        return when (type) {
            UnitType.TANK -> preset.tankAttackRange
            UnitType.INFANTRY -> preset.infantryAttackRange
            UnitType.ARTILLERY -> preset.artilleryAttackRange
            UnitType.ENGINEER -> 0
        }
    }

    fun getUnitVisionRange(type: UnitType, preset: GamePreset): Int {
        return when (type) {
            UnitType.TANK -> preset.tankVisionRange
            UnitType.INFANTRY -> preset.infantryVisionRange
            UnitType.ARTILLERY -> preset.artilleryVisionRange
            UnitType.ENGINEER -> preset.engineerVisionRange
        }
    }

    // --- PATHFINDING & REACHABILITY ---

    fun getTileMoveCost(tile: BoardTile, preset: GamePreset): Int? {
        if (tile.terrain == TerrainType.MOUNTAINS) return null
        if (tile.terrain == TerrainType.WATER && !tile.hasBridge) return null
        return when (tile.terrain) {
            TerrainType.GRASSLAND, TerrainType.WATER -> preset.costMoveGrass
            TerrainType.HILLS -> preset.costMoveHills
            TerrainType.MOUNTAINS -> null
        }
    }

    // Dijkstra algorithm for all reachable tiles from a starting position
    fun getReachableTiles(
        unit: GameUnit,
        tiles: List<BoardTile>,
        units: List<GameUnit>,
        preset: GamePreset
    ): Map<Pair<Int, Int>, List<Pair<Int, Int>>> {
        val width = preset.mapWidth
        val height = preset.mapHeight
        val tileMap = tiles.associateBy { Pair(it.row, it.col) }
        
        // Maps tile coord to standard occupied unit
        val unitMap = units.filter { it.isAlive() }.associateBy { Pair(it.row, it.col) }

        val start = Pair(unit.row, unit.col)
        val dists = mutableMapOf<Pair<Int, Int>, Int>()
        val paths = mutableMapOf<Pair<Int, Int>, List<Pair<Int, Int>>>()

        dists[start] = 0
        paths[start] = listOf(start)

        val queue = java.util.PriorityQueue<Pair<Pair<Int, Int>, Int>>(compareBy { it.second })
        queue.add(Pair(start, 0))

        while (queue.isNotEmpty()) {
            val (current, d) = queue.poll()!!
            val currentDist = dists[current] ?: continue
            if (d > currentDist) continue

            val neighbors = getNeighbors(current.first, current.second, width, height)
            for (nbr in neighbors) {
                val tile = tileMap[nbr] ?: continue
                val moveCost = getTileMoveCost(tile, preset) ?: continue

                // Check occupancy blocking:
                // "Friendly Units block movement. Enemy Units block movement. Units cannot move through other Units."
                // Wait! Engineer exception:
                // "Engineer Units are the ONLY Units allowed to occupy the same Tile as another friendly Unit.
                // Engineer + CombatUnit is legal. Engineer + Engineer is illegal. Engineer + Enemy is illegal."
                // Since friendly units block movement, can we move THROUGH an occupied tile?
                // "Units cannot move through other Units. Only occupied Tiles block movement."
                // So ANY tile occupied by an active unit blocks passing THROUGH it.
                // But we CAN stop on it if it's the destination AND matches the engineer sharing rules:
                // To support this:
                // Let's check: Is there a unit on nbr?
                val existingUnit = unitMap[nbr]
                val isNbrDestination = true // For Dijkstra, we check if neighbor can be a step. 
                // Wait! If neighbor is blocked by a unit, we CANNOT pass through it. So we cannot extend path *beyond* it.
                // But we can enter it as a final tile if we can stop on it.
                // So if we just treat occupied tiles as blocked from passing through:
                val canPass = existingUnit == null
                val canStopOnDestination = existingUnit == null || (
                    // Sharing rules:
                    existingUnit.owner == unit.owner && (
                        (unit.type == UnitType.ENGINEER && existingUnit.type != UnitType.ENGINEER) ||
                        (unit.type != UnitType.ENGINEER && existingUnit.type == UnitType.ENGINEER)
                    )
                )

                if (!canPass && !canStopOnDestination) {
                    continue // completely blocked
                }

                val nextDist = d + moveCost
                if (nextDist <= unit.energy) {
                    val existingDist = dists[nbr] ?: Int.MAX_VALUE
                    if (nextDist < existingDist) {
                        dists[nbr] = nextDist
                        paths[nbr] = paths[current]!! + nbr
                        
                        // We can only search BEYOND neighbor if it is empty (no unit blocks passing through)
                        if (canPass) {
                            queue.add(Pair(nbr, nextDist))
                        }
                    }
                }
            }
        }

        // Return all valid destinations (excluding start tile unless desired, but usually start is reachable for 0 energy)
        // Also ensure final destination matches sharing rules:
        return paths.filter { (coord, path) ->
            if (coord == start) return@filter true
            val existingUnit = unitMap[coord]
            existingUnit == null || (
                existingUnit.owner == unit.owner && (
                    (unit.type == UnitType.ENGINEER && existingUnit.type != UnitType.ENGINEER) ||
                    (unit.type != UnitType.ENGINEER && existingUnit.type == UnitType.ENGINEER)
                )
            )
        }
    }

    // --- ATTACK LOGIC ---

    fun getLegalAttackTargets(
        attacker: GameUnit,
        tiles: List<BoardTile>,
        units: List<GameUnit>,
        castles: List<Castle>,
        preset: GamePreset
    ): List<Any> { // Can return GameUnit or Castle
        if (attacker.type == UnitType.ENGINEER) return emptyList()
        val range = getUnitAttackRange(attacker.type, preset)
        if (range <= 0) return emptyList()

        val targets = mutableListOf<Any>()

        // Enemy units in range
        val enemies = units.filter { it.isAlive() && it.owner != attacker.owner }
        for (enemy in enemies) {
            val dist = hexDistance(attacker.row, attacker.col, enemy.row, enemy.col)
            if (dist <= range) {
                // Line of fire blocked by mountain?
                if (!isLineOfFireBlocked(attacker.row, attacker.col, enemy.row, enemy.col, tiles, preset.mapWidth)) {
                    targets.add(enemy)
                }
            }
        }

        // Enemy castle in range
        val enemyCastle = castles.firstOrNull { it.owner != attacker.owner && it.isAlive() }
        if (enemyCastle != null) {
            val dist = hexDistance(attacker.row, attacker.col, enemyCastle.row, enemyCastle.col)
            if (dist <= range) {
                if (!isLineOfFireBlocked(attacker.row, attacker.col, enemyCastle.row, enemyCastle.col, tiles, preset.mapWidth)) {
                    targets.add(enemyCastle)
                }
            }
        }

        return targets
    }

    // Attack Range for Castle is calculated from Castle's fixed position
    fun getCastleLegalAttackTargets(
        castle: Castle,
        tiles: List<BoardTile>,
        units: List<GameUnit>,
        preset: GamePreset
    ): List<GameUnit> {
        if (!castle.isAlive()) return emptyList()
        val range = preset.castleAttackRange
        val targets = mutableListOf<GameUnit>()

        val enemies = units.filter { it.isAlive() && it.owner != castle.owner }
        for (enemy in enemies) {
            val dist = hexDistance(castle.row, castle.col, enemy.row, enemy.col)
            if (dist <= range) {
                if (!isLineOfFireBlocked(castle.row, castle.col, enemy.row, enemy.col, tiles, preset.mapWidth)) {
                    targets.add(enemy)
                }
            }
        }
        return targets
    }

    // --- DAMAGE FORMULAS ---

    fun calculateCombatDamage(
        attacker: GameUnit,
        targetCount: Int,
        preset: GamePreset
    ): Int {
        val maxDamage = getUnitMaxDamage(attacker.type, preset).toFloat()
        val maxHp = getUnitMaxHp(attacker.type, preset).toFloat()
        val maxEnergy = getUnitMaxEnergy(attacker.type, preset).toFloat()

        if (maxHp <= 0 || maxEnergy <= 0 || targetCount <= 0) return 0

        // Damage = MaxDamage * (Current HP / Max HP) * (Current Energy / Max Energy) / Target Count
        val rawDamage = maxDamage * (attacker.hp.toFloat() / maxHp) * (attacker.energy.toFloat() / maxEnergy) / targetCount
        return kotlin.math.round(rawDamage).toInt().coerceAtLeast(1)
    }

    fun calculateCastleDamage(
        castle: Castle,
        targetCount: Int,
        preset: GamePreset
    ): Int {
        val maxDamage = preset.castleMaxDamage.toFloat()
        if (targetCount <= 0) return 0

        // Castle Damage = MaxDamage / Target Count (does NOT depend on Castle HP!)
        val rawDamage = maxDamage / targetCount
        return kotlin.math.round(rawDamage).toInt().coerceAtLeast(1)
    }

    // --- DEPLOYMENT ZONES ---

    // Deployment Zone: first two rows nearest to their Castle
    // Wait! Let's understand:
    // Host (Player 1) has Castle at some position. Usually Player 1 is at the top (row 0, 1), Client (Player 2) is at the bottom (row height-1, height-2).
    // Let's make it general:
    // P1 Castle is near top. Deployment zone is rows 0 and 1.
    // P2 Castle is near bottom. Deployment zone is rows height-1 and height-2.
    // This perfectly fits: "Deployment Zone consists of the first two Hex rows nearest to their Castle."
    fun isCoordinateInDeploymentZone(player: Int, row: Int, height: Int): Boolean {
        return if (player == 1) {
            row == 0 || row == 1
        } else {
            row == height - 1 || row == height - 2
        }
    }

    // --- VISION CALCULATION ---

    // Returns a set of currently visible coordinates for a player
    fun getVisibleCoordinates(
        player: Int,
        tiles: List<BoardTile>,
        units: List<GameUnit>,
        castles: List<Castle>,
        preset: GamePreset
    ): Set<Pair<Int, Int>> {
        if (preset.visibilityMode == VisibilityMode.FULL_VISIBILITY) {
            return tiles.map { Pair(it.row, it.col) }.toSet()
        }

        val visible = mutableSetOf<Pair<Int, Int>>()

        // Friendly units vision
        val friendlyUnits = units.filter { it.isAlive() && it.owner == player }
        for (unit in friendlyUnits) {
            val range = getUnitVisionRange(unit.type, preset)
            // Add everything in hex distance
            for (tile in tiles) {
                if (hexDistance(unit.row, unit.col, tile.row, tile.col) <= range) {
                    visible.add(Pair(tile.row, tile.col))
                }
            }
        }

        // Friendly Castle vision
        val friendlyCastle = castles.firstOrNull { it.owner == player && it.isAlive() }
        if (friendlyCastle != null) {
            val range = preset.castleVisionRange
            for (tile in tiles) {
                if (hexDistance(friendlyCastle.row, friendlyCastle.col, tile.row, tile.col) <= range) {
                    visible.add(Pair(tile.row, tile.col))
                }
            }
        }

        return visible
    }

    // --- STATE MODIFIERS (VALIDATED BY HOST) ---

    fun createDeploymentUnits(owner: Int, preset: GamePreset): List<GameUnit> {
        val units = mutableListOf<GameUnit>()
        
        // Helper to add units of a type
        fun addUnits(type: UnitType, count: Int) {
            repeat(count) { i ->
                val id = "P${owner}_${type.name}_$i"
                units.add(
                    GameUnit(
                        id = id,
                        owner = owner,
                        type = type,
                        row = -1, // undeployed
                        col = -1,
                        hp = getUnitMaxHp(type, preset),
                        energy = getUnitMaxEnergy(type, preset)
                    )
                )
            }
        }

        addUnits(UnitType.TANK, preset.tankCount)
        addUnits(UnitType.INFANTRY, preset.infantryCount)
        addUnits(UnitType.ARTILLERY, preset.artilleryCount)
        addUnits(UnitType.ENGINEER, preset.engineerCount)

        return units
    }

    // Returns updated state and lists of visual events
    fun validateAndPerformAction(
        state: GameState,
        player: Int,
        action: ClientAction
    ): Pair<GameState, List<GameVisualEvent>> {
        // Only allow active player
        if (state.phase != GamePhase.PLAYING || state.activePlayer != player) {
            return Pair(state, emptyList())
        }

        val events = mutableListOf<GameVisualEvent>()
        var nextState = state

        when (action.actionType) {
            "MOVE" -> {
                val unitId = action.unitId ?: return Pair(state, emptyList())
                val path = action.targetPath ?: return Pair(state, emptyList())
                if (path.isEmpty()) return Pair(state, emptyList())

                val unit = nextState.units.firstOrNull { it.id == unitId && it.owner == player && it.isAlive() } ?: return Pair(state, emptyList())

                // Validate path step-by-step
                // First coord is where unit currently is, last coord is target
                val firstCoord = path.first()
                if (firstCoord.r != unit.row || firstCoord.c != unit.col) {
                    return Pair(state, emptyList())
                }

                // Verify reachable
                val reachable = getReachableTiles(unit, nextState.tiles, nextState.units, nextState.preset)
                val destination = Pair(path.last().r, path.last().c)
                if (!reachable.containsKey(destination)) {
                    return Pair(state, emptyList())
                }

                // Deduct cost and move
                // The cost of path is the total movement cost of all entered tiles
                val tileMap = nextState.tiles.associateBy { Pair(it.row, it.col) }
                var totalCost = 0
                for (i in 1 until path.size) {
                    val tile = tileMap[Pair(path[i].r, path[i].c)] ?: return Pair(state, emptyList())
                    val stepCost = getTileMoveCost(tile, nextState.preset) ?: return Pair(state, emptyList())
                    totalCost += stepCost
                }

                if (unit.energy < totalCost) return Pair(state, emptyList())

                // Update Unit position and Energy
                val updatedUnits = nextState.units.map { u ->
                    if (u.id == unit.id) {
                        u.copy(
                            row = destination.first,
                            col = destination.second,
                            energy = u.energy - totalCost
                        )
                    } else u
                }

                events.add(
                    GameVisualEvent(
                        eventId = UUID.randomUUID().toString(),
                        type = "MOVE",
                        unitId = unit.id,
                        owner = player,
                        fromRow = unit.row,
                        fromCol = unit.col,
                        toRow = destination.first,
                        toCol = destination.second,
                        amount = totalCost
                    )
                )

                nextState = nextState.copy(units = updatedUnits)
            }

            "ATTACK" -> {
                val unitId = action.unitId ?: return Pair(state, emptyList())
                val unit = nextState.units.firstOrNull { it.id == unitId && it.owner == player && it.isAlive() } ?: return Pair(state, emptyList())
                if (unit.type == UnitType.ENGINEER || unit.energy <= 0) return Pair(state, emptyList())

                val targetIds = action.targetIds ?: emptyList()
                val targetCastleOwner = action.targetCastleOwner

                if (targetIds.isEmpty() && targetCastleOwner == null) return Pair(state, emptyList())

                val targets = mutableListOf<Any>()
                
                // Get list of targets in state
                for (tid in targetIds) {
                    val tUnit = nextState.units.firstOrNull { it.id == tid && it.isAlive() && it.owner != player }
                    if (tUnit != null) targets.add(tUnit)
                }

                if (targetCastleOwner != null && targetCastleOwner != player) {
                    val castle = nextState.castles.firstOrNull { it.owner == targetCastleOwner && it.isAlive() }
                    if (castle != null) targets.add(castle)
                }

                if (targets.isEmpty()) return Pair(state, emptyList())

                // Verify all targets are within Attack Range and line of fire
                val legalTargets = getLegalAttackTargets(unit, nextState.tiles, nextState.units, nextState.castles, nextState.preset)
                val legalTargetIds = legalTargets.filterIsInstance<GameUnit>().map { it.id }.toSet()
                val hasLegalCastle = legalTargets.filterIsInstance<Castle>().any { it.owner == targetCastleOwner }

                for (tid in targetIds) {
                    if (!legalTargetIds.contains(tid)) return Pair(state, emptyList())
                }
                if (targetCastleOwner != null && !hasLegalCastle) return Pair(state, emptyList())

                // Compute Damage
                val targetCount = targets.size
                val damagePerTarget = calculateCombatDamage(unit, targetCount, nextState.preset)

                val updatedUnits = nextState.units.toMutableList()
                val updatedCastles = nextState.castles.toMutableList()

                // Apply Damage to targets and set attacker energy to 0
                for (target in targets) {
                    if (target is GameUnit) {
                        val index = updatedUnits.indexOfFirst { it.id == target.id }
                        if (index != -1) {
                            val targetUnit = updatedUnits[index]
                            
                            // Check Trench defense
                            val defendingTile = nextState.tiles.firstOrNull { it.row == targetUnit.row && it.col == targetUnit.col }
                            val isDefendingInTrench = defendingTile?.hasTrench == true
                            val finalDamage = if (isDefendingInTrench) {
                                val reduction = nextState.preset.trenchDamageReduction
                                val rawVal = damagePerTarget.toFloat() * (1f - reduction)
                                kotlin.math.round(rawVal).toInt().coerceAtLeast(1)
                            } else {
                                damagePerTarget
                            }

                            val nextHp = (targetUnit.hp - finalDamage).coerceAtLeast(0)
                            updatedUnits[index] = targetUnit.copy(hp = nextHp)

                            events.add(
                                GameVisualEvent(
                                    eventId = UUID.randomUUID().toString(),
                                    type = "DAMAGE",
                                    unitId = targetUnit.id,
                                    owner = targetUnit.owner,
                                    fromRow = unit.row,
                                    fromCol = unit.col,
                                    toRow = targetUnit.row,
                                    toCol = targetUnit.col,
                                    amount = finalDamage
                                )
                            )

                            if (nextHp == 0) {
                                events.add(
                                    GameVisualEvent(
                                        eventId = UUID.randomUUID().toString(),
                                        type = "DESTROYED",
                                        unitId = targetUnit.id,
                                        owner = targetUnit.owner,
                                        toRow = targetUnit.row,
                                        toCol = targetUnit.col
                                    )
                                )
                            }
                        }
                    } else if (target is Castle) {
                        val index = updatedCastles.indexOfFirst { it.owner == target.owner }
                        if (index != -1) {
                            val targetCastle = updatedCastles[index]
                            // Castle does not receive trench reduction
                            val nextHp = (targetCastle.hp - damagePerTarget).coerceAtLeast(0)
                            updatedCastles[index] = targetCastle.copy(hp = nextHp)

                            events.add(
                                GameVisualEvent(
                                    eventId = UUID.randomUUID().toString(),
                                    type = "DAMAGE",
                                    owner = targetCastle.owner,
                                    fromRow = unit.row,
                                    fromCol = unit.col,
                                    toRow = targetCastle.row,
                                    toCol = targetCastle.col,
                                    amount = damagePerTarget
                                )
                            )
                        }
                    }
                }

                // Attack consumes ALL energy
                val attackerIndex = updatedUnits.indexOfFirst { it.id == unit.id }
                if (attackerIndex != -1) {
                    updatedUnits[attackerIndex] = updatedUnits[attackerIndex].copy(energy = 0)
                }

                events.add(
                    GameVisualEvent(
                        eventId = UUID.randomUUID().toString(),
                        type = "ATTACK",
                        unitId = unit.id,
                        owner = player,
                        fromRow = unit.row,
                        fromCol = unit.col
                    )
                )

                // Check Victory Conditions
                var winner: Int? = null
                val castle1 = updatedCastles.first { it.owner == 1 }
                val castle2 = updatedCastles.first { it.owner == 2 }

                if (castle1.hp == 0) {
                    winner = 2
                } else if (castle2.hp == 0) {
                    winner = 1
                }

                nextState = nextState.copy(
                    units = updatedUnits,
                    castles = updatedCastles,
                    winnerPlayer = winner,
                    phase = if (winner != null) GamePhase.VICTORY else GamePhase.PLAYING
                )
            }

            "TRENCH" -> {
                val unitId = action.unitId ?: return Pair(state, emptyList())
                val unit = nextState.units.firstOrNull { it.id == unitId && it.owner == player && it.isAlive() } ?: return Pair(state, emptyList())
                if (unit.type != UnitType.ENGINEER) return Pair(state, emptyList())

                val cost = nextState.preset.costBuildTrench
                if (unit.energy < cost) return Pair(state, emptyList())

                // Verify no trench exists
                val tileIndex = nextState.tiles.indexOfFirst { it.row == unit.row && it.col == unit.col }
                if (tileIndex == -1) return Pair(state, emptyList())
                val tile = nextState.tiles[tileIndex]
                if (tile.hasTrench) return Pair(state, emptyList()) // Already exists

                val updatedTiles = nextState.tiles.toMutableList()
                updatedTiles[tileIndex] = tile.copy(hasTrench = true)

                val updatedUnits = nextState.units.map { u ->
                    if (u.id == unit.id) {
                        u.copy(energy = u.energy - cost)
                    } else u
                }

                events.add(
                    GameVisualEvent(
                        eventId = UUID.randomUUID().toString(),
                        type = "TRENCH",
                        unitId = unit.id,
                        owner = player,
                        toRow = unit.row,
                        toCol = unit.col,
                        amount = cost
                    )
                )

                nextState = nextState.copy(tiles = updatedTiles, units = updatedUnits)
            }

            "BRIDGE" -> {
                val unitId = action.unitId ?: return Pair(state, emptyList())
                val unit = nextState.units.firstOrNull { it.id == unitId && it.owner == player && it.isAlive() } ?: return Pair(state, emptyList())
                if (unit.type != UnitType.ENGINEER) return Pair(state, emptyList())

                val cost = nextState.preset.costBuildBridge
                if (unit.energy < cost) return Pair(state, emptyList())

                // Must build bridge on a neighbor Water tile that has no bridge yet
                // Or wait: "The Trench is ALWAYS built on the Tile currently occupied by the Engineer...
                // Engineers may construct Bridges. Bridges are permanent structures built on Water Tiles...
                // Water Tiles containing a Bridge becomes traversable. Without a Bridge, Water Tiles are completely impassable."
                // Wait, if an Engineer is standing on water without a bridge, how did it get there? It couldn't, since water without a bridge is impassable!
                // Ah! The Engineer must build a bridge on an ADJACENT water tile!
                // Let's check: can they construct it on any neighboring Water tile? Yes! This is extremely logical and necessary since they cannot stand on water without a bridge!
                // Let's look at the action targets or we can find an adjacent Water tile that does not have a bridge.
                // Let's make it so the action specifies a neighbor coordinate, or if they tap "Build Bridge", we look for any adjacent unbridged Water tile, or let them pick!
                // Wait, if we let them pick or if the game automatically selects the adjacent water tile they are facing, let's look at the targetPath or targetIds.
                // Let's pass the target coordinate in `targetPath` (containing exactly one element: the neighbor coordinate to build the bridge on).
                val targetPath = action.targetPath ?: return Pair(state, emptyList())
                if (targetPath.isEmpty()) return Pair(state, emptyList())
                val targetCoord = targetPath.first()

                // Verify targetCoord is a neighbor of the Engineer
                if (hexDistance(unit.row, unit.col, targetCoord.r, targetCoord.c) != 1) return Pair(state, emptyList())

                val tileIndex = nextState.tiles.indexOfFirst { it.row == targetCoord.r && it.col == targetCoord.c }
                if (tileIndex == -1) return Pair(state, emptyList())
                val tile = nextState.tiles[tileIndex]
                if (tile.terrain != TerrainType.WATER || tile.hasBridge) return Pair(state, emptyList()) // Not Water or already bridged

                val updatedTiles = nextState.tiles.toMutableList()
                updatedTiles[tileIndex] = tile.copy(hasBridge = true)

                val updatedUnits = nextState.units.map { u ->
                    if (u.id == unit.id) {
                        u.copy(energy = u.energy - cost)
                    } else u
                }

                events.add(
                    GameVisualEvent(
                        eventId = UUID.randomUUID().toString(),
                        type = "BRIDGE",
                        unitId = unit.id,
                        owner = player,
                        toRow = targetCoord.r,
                        toCol = targetCoord.c,
                        amount = cost
                    )
                )

                nextState = nextState.copy(tiles = updatedTiles, units = updatedUnits)
            }

            "END_TURN" -> {
                // Toggle active player: 1 -> 2, 2 -> 1
                val nextActive = if (state.activePlayer == 1) 2 else 1

                // At the beginning of its owner's turn, every surviving Unit restores its Energy to Maximum Energy
                val updatedUnits = nextState.units.map { u ->
                    if (u.owner == nextActive && u.isAlive()) {
                        u.copy(energy = getUnitMaxEnergy(u.type, nextState.preset))
                    } else u
                }

                // Castle attacks also execute at the end/start of a turn?
                // Wait, "Destroying the enemy Castle immediately wins the game. Castle Damage NEVER depends on Castle HP.
                // Castle Damage Formula: Damage = Maximum Damage / Selected Target Count. If only one target exists, the Castle deals full Maximum Damage."
                // Wait, when does a Castle attack?
                // The spec says:
                // "Each Castle has configurable: Maximum HP, Maximum Damage, Attack Range, Vision Range...
                // Castle Damage Formula: Damage = Maximum Damage / Selected Target Count"
                // How does Castle attack? Is it manual, or automatic?
                // "If a Unit supports Multi-Target attacks, the player manually selects which enemies to attack...
                // Attack UI: When Attack is selected: ... Selected enemies receive a visual highlight ... Player presses Confirm... Host validates the attack..."
                // Since Castles can attack, does a player manually activate their friendly Castle to attack enemy units?
                // Yes! That is extremely cool! A player can select their Castle (just like any unit) and choose "Attack" from the Action Panel, then select enemy units inside the Castle's range, split-damage among them, and fire! This gives a wonderful defensive mechanic to castles and makes them active players on the field!
                // Let's support this! If player selects Castle, they can tap "Attack" and perform attack just like a unit. Castles have infinite "energy" or can attack once per turn. Let's make it once per turn!
                // Wait! To keep it simple, let's track Castle attack usage in state or let it attack once per turn. Since a Castle has no "Energy" pool, we can reset Castle attack status at the start of the turn. This is extremely intuitive and clean!
                
                nextState = nextState.copy(
                    activePlayer = nextActive,
                    units = updatedUnits
                )
            }
        }

        // Clean old events, keep only recent ones to display animations briefly
        return Pair(nextState.copy(events = events), events)
    }
}
