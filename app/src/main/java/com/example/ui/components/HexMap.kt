package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.domain.engine.GameEngine
import com.example.domain.model.*

@Composable
fun HexMap(
    tiles: List<BoardTile>,
    castles: List<Castle>,
    units: List<GameUnit>,
    visibleTiles: Set<Pair<Int, Int>>,
    selectedCoord: Pair<Int, Int>?,
    reachableTiles: Set<Pair<Int, Int>>,
    attackRangeTiles: Set<Pair<Int, Int>>,
    selectedAttackTargets: Set<String>,
    selectedCastleAttackTarget: Int?,
    pendingPath: List<Pair<Int, Int>>?,
    isAttackMode: Boolean,
    onTileClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    isMapEditing: Boolean = false,
    player1Castle: Pair<Int, Int>? = null,
    player2Castle: Pair<Int, Int>? = null
) {
    // Zoom & Pan state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val baseHexRadius = 40f // Base radius of pointy-topped hexagon
    val W = kotlin.math.sqrt(3f) * baseHexRadius

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF141218))
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    // Convert screen tap coordinates to model space
                    val modelX = (tapOffset.x - offset.x) / scale
                    val modelY = (tapOffset.y - offset.y) / scale

                    // Find closest tile
                    var closestTile: BoardTile? = null
                    var minDist = Float.MAX_VALUE
                    for (tile in tiles) {
                        val row = tile.row
                        val col = tile.col
                        val cy = row * baseHexRadius * 1.5f + baseHexRadius + 20f
                        val cx = col * W + (if (row % 2 == 0) W / 2f else 0f) + W / 2f + 20f

                        val dx = modelX - cx
                        val dy = modelY - cy
                        val dist = dx * dx + dy * dy
                        if (dist < minDist) {
                            minDist = dist
                            closestTile = tile
                        }
                    }

                    if (closestTile != null && minDist < W * W * 0.75f) {
                        onTileClick(closestTile.row, closestTile.col)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 3.0f)
                    offset += pan
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Apply scale and offset transforms
            drawContext.transform.translate(offset.x, offset.y)
            drawContext.transform.scale(scale, scale)

            val tileMap = tiles.associateBy { Pair(it.row, it.col) }

            // 1. Draw Terrains
            for (tile in tiles) {
                val row = tile.row
                val col = tile.col
                val cy = row * baseHexRadius * 1.5f + baseHexRadius + 20f
                val cx = col * W + (if (row % 2 == 0) W / 2f else 0f) + W / 2f + 20f

                val isVisible = visibleTiles.contains(Pair(row, col))
                
                // Color choices based on terrain spec:
                // Grassland: Green, Hills: Yellow, Mountains: Brown, Water: Blue
                val baseColor = when (tile.terrain) {
                    TerrainType.GRASSLAND -> Color(0xFF2E7D32) // Soft Green
                    TerrainType.HILLS -> Color(0xFFFBC02D)     // Amber/Yellow
                    TerrainType.MOUNTAINS -> Color(0xFF5D4037) // Brown
                    TerrainType.WATER -> Color(0xFF1565C0)     // Blue
                }

                val finalColor = if (!isVisible && !isMapEditing) {
                    // Outside vision fog overlay
                    baseColor.copy(alpha = 0.4f)
                } else {
                    baseColor
                }

                val hexPath = getHexPath(cx, cy, baseHexRadius)
                drawPath(path = hexPath, color = finalColor)

                // Hex outline
                val outlineColor = if (isVisible || isMapEditing) Color(0x33FFFFFF) else Color(0x11FFFFFF)
                drawPath(
                    path = hexPath,
                    color = outlineColor,
                    style = Stroke(width = 1f)
                )

                // Mountain peaks or hills contours
                if (tile.terrain == TerrainType.MOUNTAINS) {
                    drawMountainPeak(cx, cy, baseHexRadius * 0.6f)
                } else if (tile.terrain == TerrainType.HILLS) {
                    drawHillsCurve(cx, cy, baseHexRadius * 0.5f)
                }

                // 2. Draw Trenches & Bridges
                if (tile.hasTrench) {
                    drawTrenchIcon(cx, cy, baseHexRadius * 0.4f)
                }
                if (tile.hasBridge && tile.terrain == TerrainType.WATER) {
                    drawBridgeIcon(cx, cy, baseHexRadius * 0.7f, W)
                }

                // 3. Draw Highlights
                val coord = Pair(row, col)
                if (selectedCoord == coord) {
                    // selected highlight: thick lavender border
                    drawPath(
                        path = hexPath,
                        color = Color(0xFFD0BCFF),
                        style = Stroke(width = 4f)
                    )
                } else if (isAttackMode && attackRangeTiles.contains(coord)) {
                    // legal target highlight in attack mode: thick red border
                    drawPath(
                        path = hexPath,
                        color = Color(0xFFE53935),
                        style = Stroke(width = 3f)
                    )
                } else if (reachableTiles.contains(coord)) {
                    // standard movement range highlight: teal border
                    drawPath(
                        path = hexPath,
                        color = Color(0xFF00E676),
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // 4. Draw Castles
            for (castle in castles) {
                // If castle is dead, we don't draw it or draw as collapsed ruins
                val cy = castle.row * baseHexRadius * 1.5f + baseHexRadius + 20f
                val cx = castle.col * W + (if (castle.row % 2 == 0) W / 2f else 0f) + W / 2f + 20f

                val isVisible = visibleTiles.contains(Pair(castle.row, castle.col)) || isMapEditing
                if (isVisible) {
                    val ownerColor = if (castle.owner == 1) Color(0xFF3F51B5) else Color(0xFFF44336)
                    drawCastleFortress(cx, cy, baseHexRadius * 0.65f, ownerColor, castle.hp, castle.maxHp)
                }
            }

            // 5. Draw Units
            val activeUnits = units.filter { it.isAlive() }
            for (unit in activeUnits) {
                val isVisible = visibleTiles.contains(Pair(unit.row, unit.col)) || isMapEditing
                if (isVisible) {
                    val cy = unit.row * baseHexRadius * 1.5f + baseHexRadius + 20f
                    val cx = unit.col * W + (if (unit.row % 2 == 0) W / 2f else 0f) + W / 2f + 20f

                    val teamColor = if (unit.owner == 1) Color(0xFF2196F3) else Color(0xFFFF5722)
                    
                    // Determine if this specific unit is selected as multi-target or castle target
                    val isTargeted = selectedAttackTargets.contains(unit.id)

                    drawUnitEmblem(
                        cx = cx,
                        cy = cy,
                        radius = baseHexRadius * 0.5f,
                        unit = unit,
                        teamColor = teamColor,
                        isTargeted = isTargeted
                    )
                }
            }

            // 6. Draw Pending Path
            if (pendingPath != null && pendingPath.size > 1) {
                val pathPoints = pendingPath.map { p ->
                    val cy = p.first * baseHexRadius * 1.5f + baseHexRadius + 20f
                    val cx = p.second * W + (if (p.first % 2 == 0) W / 2f else 0f) + W / 2f + 20f
                    Offset(cx, cy)
                }
                
                // Draw path line
                for (i in 0 until pathPoints.size - 1) {
                    drawLine(
                        color = Color(0xFF00E676),
                        start = pathPoints[i],
                        end = pathPoints[i+1],
                        strokeWidth = 5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                }
                
                // Draw path endpoint dot
                drawCircle(
                    color = Color(0xFF00E676),
                    radius = 8f,
                    center = pathPoints.last()
                )
            }
        }
    }
}

// Generate pointing hex vertices
private fun getHexPath(cx: Float, cy: Float, radius: Float): Path {
    val path = Path()
    for (i in 0 until 6) {
        val angleRad = (60 * i + 30) * Math.PI / 180f
        val px = cx + radius * kotlin.math.cos(angleRad).toFloat()
        val py = cy + radius * kotlin.math.sin(angleRad).toFloat()
        if (i == 0) {
            path.moveTo(px, py)
        } else {
            path.lineTo(px, py)
        }
    }
    path.close()
    return path
}

// Embellishments inside Mountain tiles
private fun DrawScope.drawMountainPeak(cx: Float, cy: Float, size: Float) {
    val path = Path().apply {
        moveTo(cx - size, cy + size * 0.6f)
        lineTo(cx, cy - size * 0.8f)
        lineTo(cx + size, cy + size * 0.6f)
        close()
        // Smaller secondary peak
        moveTo(cx - size * 0.5f, cy + size * 0.6f)
        lineTo(cx - size * 0.1f, cy - size * 0.2f)
        lineTo(cx + size * 0.3f, cy + size * 0.6f)
        close()
    }
    drawPath(path = path, color = Color(0x55FFFFFF))
}

// Curve for Hills
private fun DrawScope.drawHillsCurve(cx: Float, cy: Float, size: Float) {
    val path = Path().apply {
        moveTo(cx - size, cy + size * 0.2f)
        quadraticTo(cx - size * 0.3f, cy - size * 0.4f, cx, cy + size * 0.2f)
        moveTo(cx, cy)
        quadraticTo(cx + size * 0.4f, cy - size * 0.5f, cx + size, cy)
    }
    drawPath(path = path, color = Color(0x33000000), style = Stroke(width = 3f, cap = StrokeCap.Round))
}

// Trench drawing: a defensive bracket around center
private fun DrawScope.drawTrenchIcon(cx: Float, cy: Float, size: Float) {
    // Draw fortification outline
    val trenchColor = Color(0xFF8D6E63) // Sand/Clay brown
    drawRect(
        color = trenchColor,
        topLeft = Offset(cx - size, cy - size * 0.2f),
        size = androidx.compose.ui.geometry.Size(size * 2, size * 0.4f)
    )
    drawRect(
        color = Color(0xFF1E1E1E),
        topLeft = Offset(cx - size + 4, cy - size * 0.1f),
        size = androidx.compose.ui.geometry.Size(size * 2 - 8, size * 0.2f)
    )
}

// Bridge drawing: a plank across the tile
private fun DrawScope.drawBridgeIcon(cx: Float, cy: Float, size: Float, hexW: Float) {
    val bridgeColor = Color(0xFF8D6E63)
    // Horizontal bridge plank
    drawRect(
        color = bridgeColor,
        topLeft = Offset(cx - hexW / 2.2f, cy - 8f),
        size = androidx.compose.ui.geometry.Size(hexW / 1.1f, 16f)
    )
    // Planks details
    for (i in -2..2) {
        val px = cx + i * (hexW / 6)
        drawLine(
            color = Color(0xFF3E2723),
            start = Offset(px, cy - 8f),
            end = Offset(px, cy + 8f),
            strokeWidth = 2f
        )
    }
}

// Castle Fortress rendering
private fun DrawScope.drawCastleFortress(
    cx: Float,
    cy: Float,
    size: Float,
    color: Color,
    hp: Int,
    maxHp: Int
) {
    // Backdrop shadow/base
    drawCircle(color = Color(0x44000000), radius = size * 1.1f, center = Offset(cx, cy))

    // Castle tower structure
    val path = Path().apply {
        // Left tower
        moveTo(cx - size * 0.8f, cy + size * 0.8f)
        lineTo(cx - size * 0.8f, cy - size * 0.2f)
        lineTo(cx - size * 0.5f, cy - size * 0.2f)
        lineTo(cx - size * 0.5f, cy + size * 0.8f)
        
        // Right tower
        moveTo(cx + size * 0.5f, cy + size * 0.8f)
        lineTo(cx + size * 0.5f, cy - size * 0.2f)
        lineTo(cx + size * 0.8f, cy - size * 0.2f)
        lineTo(cx + size * 0.8f, cy + size * 0.8f)

        // Middle wall
        moveTo(cx - size * 0.5f, cy + size * 0.8f)
        lineTo(cx - size * 0.5f, cy)
        lineTo(cx + size * 0.5f, cy)
        lineTo(cx + size * 0.5f, cy + size * 0.8f)
    }
    
    // Draw fortress filling
    drawPath(path = path, color = color)
    drawPath(path = path, color = Color.White, style = Stroke(width = 2f))

    // Draw crenellations (tops)
    val crenPoints = Path().apply {
        // Left tower tops
        moveTo(cx - size * 0.85f, cy - size * 0.2f)
        lineTo(cx - size * 0.85f, cy - size * 0.5f)
        lineTo(cx - size * 0.7f, cy - size * 0.5f)
        lineTo(cx - size * 0.7f, cy - size * 0.35f)
        lineTo(cx - size * 0.6f, cy - size * 0.35f)
        lineTo(cx - size * 0.6f, cy - size * 0.5f)
        lineTo(cx - size * 0.45f, cy - size * 0.5f)
        lineTo(cx - size * 0.45f, cy - size * 0.2f)

        // Right tower tops
        moveTo(cx + size * 0.45f, cy - size * 0.2f)
        lineTo(cx + size * 0.45f, cy - size * 0.5f)
        lineTo(cx + size * 0.6f, cy - size * 0.5f)
        lineTo(cx + size * 0.6f, cy - size * 0.35f)
        lineTo(cx + size * 0.7f, cy - size * 0.35f)
        lineTo(cx + size * 0.7f, cy - size * 0.5f)
        lineTo(cx + size * 0.85f, cy - size * 0.5f)
        lineTo(cx + size * 0.85f, cy - size * 0.2f)
    }
    drawPath(path = crenPoints, color = color)
    drawPath(path = crenPoints, color = Color.White, style = Stroke(width = 2f))

    // HP Bar
    val barW = size * 1.6f
    val barH = 6f
    val hpPercent = (hp.toFloat() / maxHp).coerceIn(0f, 1f)
    val barY = cy + size * 1.2f

    // Draw background
    drawRect(
        color = Color.Black,
        topLeft = Offset(cx - barW / 2, barY),
        size = androidx.compose.ui.geometry.Size(barW, barH)
    )
    // Draw current hp
    drawRect(
        color = Color(0xFF4CAF50),
        topLeft = Offset(cx - barW / 2, barY),
        size = androidx.compose.ui.geometry.Size(barW * hpPercent, barH)
    )
}

// Unit Emblem rendering
private fun DrawScope.drawUnitEmblem(
    cx: Float,
    cy: Float,
    radius: Float,
    unit: GameUnit,
    teamColor: Color,
    isTargeted: Boolean
) {
    // Backdrop shadow/glowing circle
    val glowColor = if (isTargeted) Color(0xFFFF1744) else Color(0x88000000)
    val glowRadius = if (isTargeted) radius * 1.2f else radius
    drawCircle(color = glowColor, radius = glowRadius, center = Offset(cx, cy))

    // Team circular base
    drawCircle(color = teamColor, radius = radius * 0.9f, center = Offset(cx, cy))
    // Outer white/black ring
    val ringColor = if (isTargeted) Color.White else Color(0xCCFFFFFF)
    drawCircle(
        color = ringColor,
        radius = radius * 0.9f,
        center = Offset(cx, cy),
        style = Stroke(width = if (isTargeted) 3.5f else 1.5f)
    )

    // Draw unit specific glyph symbol
    when (unit.type) {
        UnitType.TANK -> {
            // Circle body with a turret barrel pointing right/up
            drawCircle(color = Color.White, radius = radius * 0.4f, center = Offset(cx, cy))
            drawLine(
                color = Color.White,
                start = Offset(cx, cy),
                end = Offset(cx + radius * 0.6f, cy - radius * 0.3f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
        UnitType.INFANTRY -> {
            // Cross symbol or classic helmet chevron representation
            val path = Path().apply {
                moveTo(cx - radius * 0.3f, cy - radius * 0.1f)
                lineTo(cx, cy - radius * 0.4f)
                lineTo(cx + radius * 0.3f, cy - radius * 0.1f)
                lineTo(cx, cy + radius * 0.4f)
                close()
            }
            drawPath(path = path, color = Color.White)
        }
        UnitType.ARTILLERY -> {
            // Cannon wheel circle + crossed logs
            drawLine(
                color = Color.White,
                start = Offset(cx - radius * 0.4f, cy + radius * 0.4f),
                end = Offset(cx + radius * 0.4f, cy - radius * 0.4f),
                strokeWidth = 4f
            )
            drawLine(
                color = Color.White,
                start = Offset(cx - radius * 0.4f, cy - radius * 0.4f),
                end = Offset(cx + radius * 0.4f, cy + radius * 0.4f),
                strokeWidth = 4f
            )
            drawCircle(color = Color.White, radius = radius * 0.25f, center = Offset(cx, cy))
        }
        UnitType.ENGINEER -> {
            // Gear outline or support wrench
            drawLine(
                color = Color.White,
                start = Offset(cx - radius * 0.35f, cy),
                end = Offset(cx + radius * 0.35f, cy),
                strokeWidth = 5f
            )
            drawLine(
                color = Color.White,
                start = Offset(cx, cy - radius * 0.35f),
                end = Offset(cx, cy + radius * 0.35f),
                strokeWidth = 5f
            )
            drawCircle(color = Color.White, radius = radius * 0.2f, center = Offset(cx, cy))
        }
    }

    // Mini status stats (HP number or bars overlay)
    // Draw tiny green dot/bar for energy, red for hp on sides of emblem
    val offsetHp = radius * 1.1f
    
    // HP bar (Vertical, left)
    val hpHeight = radius * 1.2f
    val hpPct = (unit.hp.toFloat() / GameEngine.getUnitMaxHp(unit.type, GamePreset(name="D"))).coerceIn(0f,1f)
    drawLine(
        color = Color(0x66000000),
        start = Offset(cx - offsetHp, cy + hpHeight/2),
        end = Offset(cx - offsetHp, cy - hpHeight/2),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF4CAF50),
        start = Offset(cx - offsetHp, cy + hpHeight/2),
        end = Offset(cx - offsetHp, cy + hpHeight/2 - hpHeight * hpPct),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )

    // Energy bar (Vertical, right)
    val enPct = (unit.energy.toFloat() / GameEngine.getUnitMaxEnergy(unit.type, GamePreset(name="D"))).coerceIn(0f,1f)
    drawLine(
        color = Color(0x66000000),
        start = Offset(cx + offsetHp, cy + hpHeight/2),
        end = Offset(cx + offsetHp, cy - hpHeight/2),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFFFEB3B),
        start = Offset(cx + offsetHp, cy + hpHeight/2),
        end = Offset(cx + offsetHp, cy + hpHeight/2 - hpHeight * enPct),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )
}
