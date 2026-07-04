package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.engine.GameEngine
import com.example.domain.model.*
import com.example.ui.components.HexMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (currentScreen != MainViewModel.AppScreen.MAIN_MENU && currentScreen != MainViewModel.AppScreen.BATTLE) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                MainViewModel.AppScreen.PRESET_LIST -> "Presets"
                                MainViewModel.AppScreen.PRESET_EDITOR -> "Configure Match Rules"
                                MainViewModel.AppScreen.MAP_EDITOR -> "Design Battlefield Map"
                                MainViewModel.AppScreen.LAN_SETUP -> "Local Multiplayer LAN"
                                MainViewModel.AppScreen.DEPLOYMENT -> "Deployment Phase"
                                MainViewModel.AppScreen.VICTORY -> "Game Complete"
                                else -> "Tactical Hex Strategy"
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitGame() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit to Main Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1C1B1F),
                        titleContentColor = Color(0xFFE6E1E5),
                        navigationIconContentColor = Color(0xFFE6E1E5)
                    )
                )
            }
        },
        containerColor = Color(0xFF1C1B1F)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                MainViewModel.AppScreen.MAIN_MENU -> MainMenuScreen(viewModel)
                MainViewModel.AppScreen.PRESET_LIST -> PresetListScreen(viewModel)
                MainViewModel.AppScreen.PRESET_EDITOR -> PresetEditorScreen(viewModel)
                MainViewModel.AppScreen.MAP_EDITOR -> MapEditorScreen(viewModel)
                MainViewModel.AppScreen.LAN_SETUP -> LANSetupScreen(viewModel)
                MainViewModel.AppScreen.DEPLOYMENT -> DeploymentScreen(viewModel)
                MainViewModel.AppScreen.BATTLE -> BattleScreen(viewModel)
                MainViewModel.AppScreen.VICTORY -> VictoryScreen(viewModel)
            }

            // Toast system
            toastMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 32.dp, end = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

// --- SCREEN 1: MAIN MENU ---

@Composable
fun MainMenuScreen(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Hero Icon & Banner
        Icon(
            imageVector = Icons.Default.Grid4x4,
            contentDescription = "App Icon",
            modifier = Modifier.size(90.dp),
            tint = Color(0xFFD0BCFF)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "TACTICAL HEX\nSTRATEGY",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            ),
            color = Color(0xFFE6E1E5)
        )
        
        Text(
            text = "LAN Local Multiplayer Strategy Game Engine",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFCAC4D0),
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Actions
        Button(
            onClick = { viewModel.navigateTo(MainViewModel.AppScreen.PRESET_LIST) },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(54.dp)
                .testTag("host_game_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Create / Host Game", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        var showIpJoinDialog by remember { mutableStateOf(false) }
        var hostIpInput by remember { mutableStateOf("") }

        Button(
            onClick = { showIpJoinDialog = true },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(54.dp)
                .testTag("join_game_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF49454F),
                contentColor = Color(0xFFE6E1E5)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.ConnectWithoutContact, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Join LAN Game", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Single-device testing Sandbox
        Button(
            onClick = { viewModel.navigateTo(MainViewModel.AppScreen.PRESET_LIST) },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(54.dp)
                .testTag("sandbox_game_button")
                .border(1.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF381E72),
                contentColor = Color(0xFFD0BCFF)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.padding(end = 8.dp))
            Text("Local Sandbox Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD0BCFF))
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Connection IP status footer
        Text(
            text = "Your IP: ${viewModel.localIp}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCAC4D0),
            fontFamily = FontFamily.Monospace
        )

        if (showIpJoinDialog) {
            AlertDialog(
                onDismissRequest = { showIpJoinDialog = false },
                title = { Text("Connect to Host IP", color = Color(0xFFE6E1E5)) },
                text = {
                    Column {
                        Text("Enter the Host's local IP address displayed on their screen:", color = Color(0xFFCAC4D0), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = hostIpInput,
                            onValueChange = { hostIpInput = it },
                            placeholder = { Text("e.g. 192.168.1.5") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F)
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (hostIpInput.isNotBlank()) {
                                showIpJoinDialog = false
                                viewModel.joinLANMatch(hostIpInput.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72)
                        )
                    ) {
                        Text("Connect")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showIpJoinDialog = false }) {
                        Text("Cancel", color = Color(0xFFCAC4D0))
                    }
                },
                containerColor = Color(0xFF2B2930)
            )
        }
    }
}

// --- SCREEN 2: PRESET LIST ---

@Composable
fun PresetListScreen(viewModel: MainViewModel) {
    val presets by viewModel.allPresets.collectAsStateWithLifecycle()
    val networkRole by viewModel.networkRole.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Select a Game Preset", fontWeight = FontWeight.Bold, color = Color(0xFFE6E1E5))
                    Text(
                        text = "Presets store all gameplay configurations: map ratios, castle health, energy costs, and unit stats.",
                        color = Color(0xFFCAC4D0),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Create new button
        Button(
            onClick = { viewModel.createNewPreset() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Custom Preset", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (presets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFD0BCFF))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(presets) { preset ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(preset.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE6E1E5))
                            Text(
                                "Grid: ${preset.mapWidth}x${preset.mapHeight} | Fog: ${preset.visibilityMode.name} | Castle HP: ${preset.castleMaxHp}",
                                fontSize = 13.sp,
                                color = Color(0xFFCAC4D0),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Button(
                                    onClick = {
                                        if (networkRole == MainViewModel.NetworkRole.SANDBOX) {
                                            viewModel.startSandboxMatch(preset)
                                        } else {
                                            viewModel.hostLANMatch(preset)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF381E72),
                                        contentColor = Color(0xFFD0BCFF)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Load & Play", fontWeight = FontWeight.SemiBold)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = { viewModel.editPreset(preset) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFFCAC4D0))
                                    }
                                    IconButton(onClick = { viewModel.duplicatePreset(preset) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color(0xFFCAC4D0))
                                    }
                                    IconButton(onClick = { viewModel.deletePreset(preset) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF2B8B5))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 3: PRESET EDITOR ---

@Composable
fun PresetEditorScreen(viewModel: MainViewModel) {
    val activePreset by viewModel.activeConfig.collectAsStateWithLifecycle()
    var name by remember(activePreset.id) { mutableStateOf(activePreset.name) }
    
    // Map dimensions
    var width by remember(activePreset.id) { mutableStateOf(activePreset.mapWidth.toFloat()) }
    var height by remember(activePreset.id) { mutableStateOf(activePreset.mapHeight.toFloat()) }

    // Ratios
    var grassPct by remember(activePreset.id) { mutableStateOf(activePreset.grassPercent.toFloat()) }
    var hillsPct by remember(activePreset.id) { mutableStateOf(activePreset.hillsPercent.toFloat()) }
    var mountainsPct by remember(activePreset.id) { mutableStateOf(activePreset.mountainsPercent.toFloat()) }
    var waterPct by remember(activePreset.id) { mutableStateOf(activePreset.waterPercent.toFloat()) }

    // Castles
    var castleMaxHp by remember(activePreset.id) { mutableStateOf(activePreset.castleMaxHp.toString()) }
    var castleMaxDmg by remember(activePreset.id) { mutableStateOf(activePreset.castleMaxDamage.toString()) }
    var castleRange by remember(activePreset.id) { mutableStateOf(activePreset.castleAttackRange.toString()) }
    var castleVision by remember(activePreset.id) { mutableStateOf(activePreset.castleVisionRange.toString()) }

    // Rules
    var fogMode by remember(activePreset.id) { mutableStateOf(activePreset.visibilityMode) }
    var trenchReduction by remember(activePreset.id) { mutableStateOf(activePreset.trenchDamageReduction) }

    // Costs
    var costMoveGrass by remember(activePreset.id) { mutableStateOf(activePreset.costMoveGrass.toString()) }
    var costMoveHills by remember(activePreset.id) { mutableStateOf(activePreset.costMoveHills.toString()) }
    var costTrench by remember(activePreset.id) { mutableStateOf(activePreset.costBuildTrench.toString()) }
    var costBridge by remember(activePreset.id) { mutableStateOf(activePreset.costBuildBridge.toString()) }

    // Unit lists stats
    var tankCount by remember(activePreset.id) { mutableStateOf(activePreset.tankCount.toString()) }
    var tankHp by remember(activePreset.id) { mutableStateOf(activePreset.tankMaxHp.toString()) }
    var tankDmg by remember(activePreset.id) { mutableStateOf(activePreset.tankMaxDamage.toString()) }
    var tankEnergy by remember(activePreset.id) { mutableStateOf(activePreset.tankMaxEnergy.toString()) }
    var tankRange by remember(activePreset.id) { mutableStateOf(activePreset.tankAttackRange.toString()) }
    var tankVision by remember(activePreset.id) { mutableStateOf(activePreset.tankVisionRange.toString()) }

    var engineerCount by remember(activePreset.id) { mutableStateOf(activePreset.engineerCount.toString()) }
    var engineerHp by remember(activePreset.id) { mutableStateOf(activePreset.engineerMaxHp.toString()) }
    var engineerEnergy by remember(activePreset.id) { mutableStateOf(activePreset.engineerMaxEnergy.toString()) }
    var engineerVision by remember(activePreset.id) { mutableStateOf(activePreset.engineerVisionRange.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Preset Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFFE6E1E5),
                    unfocusedTextColor = Color(0xFFE6E1E5),
                    focusedBorderColor = Color(0xFFD0BCFF),
                    unfocusedBorderColor = Color(0xFF49454F),
                    focusedLabelColor = Color(0xFFD0BCFF),
                    unfocusedLabelColor = Color(0xFFCAC4D0)
                )
            )
        }

        // Section: MAP CONFIG
        item {
            Text("1. MAP RATIOS & SIZES", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Map Grid Dimensions: ${width.toInt()} x ${height.toInt()}", color = Color(0xFFE6E1E5), fontSize = 14.sp)
                    Slider(
                        value = width,
                        onValueChange = { width = it },
                        valueRange = 5f..21f,
                        steps = 16,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )
                    Slider(
                        value = height,
                        onValueChange = { height = it },
                        valueRange = 5f..21f,
                        steps = 16,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Terrain Mix (Grass: ${grassPct.toInt()}%, Hills: ${hillsPct.toInt()}%, Mountains: ${mountainsPct.toInt()}%, Water: ${waterPct.toInt()}%)", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                    Slider(
                        value = grassPct,
                        onValueChange = { grassPct = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )
                    Slider(
                        value = hillsPct,
                        onValueChange = { hillsPct = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )
                    Slider(
                        value = mountainsPct,
                        onValueChange = { mountainsPct = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )
                    Slider(
                        value = waterPct,
                        onValueChange = { waterPct = it },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )
                }
            }
        }

        // Section: CASTLE & GENERAL
        item {
            Text("2. CASTLES & DEFENSIVE CONFIGS", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = castleMaxHp,
                            onValueChange = { castleMaxHp = it },
                            label = { Text("Castle HP") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                        OutlinedTextField(
                            value = castleMaxDmg,
                            onValueChange = { castleMaxDmg = it },
                            label = { Text("Castle DMG") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = castleRange,
                            onValueChange = { castleRange = it },
                            label = { Text("Castle Range") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                        OutlinedTextField(
                            value = castleVision,
                            onValueChange = { castleVision = it },
                            label = { Text("Castle Vision") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Fog of War Mode", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = fogMode == VisibilityMode.FOG_OF_WAR,
                                onClick = { fogMode = VisibilityMode.FOG_OF_WAR },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD0BCFF), unselectedColor = Color(0xFF49454F))
                            )
                            Text("Fog Active", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = fogMode == VisibilityMode.FULL_VISIBILITY,
                                onClick = { fogMode = VisibilityMode.FULL_VISIBILITY },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD0BCFF), unselectedColor = Color(0xFF49454F))
                            )
                            Text("Full Vision", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Trench Shield: ${(trenchReduction * 100).toInt()}%", color = Color(0xFFE6E1E5), fontSize = 13.sp)
                    Slider(
                        value = trenchReduction,
                        onValueChange = { trenchReduction = it },
                        valueRange = 0f..0.9f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFD0BCFF), activeTrackColor = Color(0xFFD0BCFF))
                    )
                }
            }
        }

        // Section: ACTION COSTS
        item {
            Text("3. ENERGY COSTS", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costMoveGrass,
                            onValueChange = { costMoveGrass = it },
                            label = { Text("Move Grass") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                        OutlinedTextField(
                            value = costMoveHills,
                            onValueChange = { costMoveHills = it },
                            label = { Text("Move Hills") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costTrench,
                            onValueChange = { costTrench = it },
                            label = { Text("Build Trench") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                        OutlinedTextField(
                            value = costBridge,
                            onValueChange = { costBridge = it },
                            label = { Text("Build Bridge") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                    }
                }
            }
        }

        // Section: MILITARY STATS (TANK / INFANTRY)
        item {
            Text("4. COMBAT MILITARY UNIT STATS (Tank)", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tankCount,
                            onValueChange = { tankCount = it },
                            label = { Text("Count") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                        OutlinedTextField(
                            value = tankHp,
                            onValueChange = { tankHp = it },
                            label = { Text("Max HP") },
                            modifier = Modifier.weight(1.2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tankDmg,
                            onValueChange = { tankDmg = it },
                            label = { Text("Max DMG") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                        OutlinedTextField(
                            value = tankEnergy,
                            onValueChange = { tankEnergy = it },
                            label = { Text("Max Energy") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tankRange,
                            onValueChange = { tankRange = it },
                            label = { Text("Range") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                        OutlinedTextField(
                            value = tankVision,
                            onValueChange = { tankVision = it },
                            label = { Text("Vision") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                    }
                }
            }
        }

        // Section: ENGINEER SUPPORT STATS
        item {
            Text("5. SUPPORT UNIT STATS (Engineer)", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = engineerCount,
                            onValueChange = { engineerCount = it },
                            label = { Text("Count") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                        OutlinedTextField(
                            value = engineerHp,
                            onValueChange = { engineerHp = it },
                            label = { Text("Max HP") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = engineerEnergy,
                            onValueChange = { engineerEnergy = it },
                            label = { Text("Max Energy") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                        OutlinedTextField(
                            value = engineerVision,
                            onValueChange = { engineerVision = it },
                            label = { Text("Vision") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE6E1E5),
                                unfocusedTextColor = Color(0xFFE6E1E5),
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color(0xFF49454F),
                                focusedLabelColor = Color(0xFFD0BCFF),
                                unfocusedLabelColor = Color(0xFFCAC4D0)
                            )
                        )
                    }
                }
            }
        }

        // SAVE BUTTON
        item {
            Button(
                onClick = {
                    // Normalise percentages so they sum to 100%
                    val sum = grassPct + hillsPct + mountainsPct + waterPct
                    val scaleFactor = 100f / if (sum > 0) sum else 1f
                    
                    val finalPreset = activePreset.copy(
                        name = name.ifBlank { "My Custom Game" },
                        mapWidth = width.toInt(),
                        mapHeight = height.toInt(),
                        grassPercent = (grassPct * scaleFactor).toInt(),
                        hillsPercent = (hillsPct * scaleFactor).toInt(),
                        mountainsPercent = (mountainsPct * scaleFactor).toInt(),
                        waterPercent = (waterPct * scaleFactor).toInt(),
                        castleMaxHp = castleMaxHp.toIntOrNull() ?: 100,
                        castleMaxDamage = castleMaxDmg.toIntOrNull() ?: 30,
                        castleAttackRange = castleRange.toIntOrNull() ?: 2,
                        castleVisionRange = castleVision.toIntOrNull() ?: 3,
                        visibilityMode = fogMode,
                        trenchDamageReduction = trenchReduction,
                        costMoveGrass = costMoveGrass.toIntOrNull() ?: 10,
                        costMoveHills = costMoveHills.toIntOrNull() ?: 20,
                        costBuildTrench = costTrench.toIntOrNull() ?: 30,
                        costBuildBridge = costBridge.toIntOrNull() ?: 40,
                        
                        tankCount = tankCount.toIntOrNull() ?: 2,
                        tankMaxHp = tankHp.toIntOrNull() ?: 100,
                        tankMaxDamage = tankDmg.toIntOrNull() ?: 45,
                        tankMaxEnergy = tankEnergy.toIntOrNull() ?: 100,
                        tankAttackRange = tankRange.toIntOrNull() ?: 2,
                        tankVisionRange = tankVision.toIntOrNull() ?: 3,

                        engineerCount = engineerCount.toIntOrNull() ?: 2,
                        engineerMaxHp = engineerHp.toIntOrNull() ?: 40,
                        engineerMaxEnergy = engineerEnergy.toIntOrNull() ?: 80,
                        engineerVisionRange = engineerVision.toIntOrNull() ?: 2
                    )
                    viewModel.savePreset(finalPreset)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_preset_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                )
            ) {
                Text("Save Game Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// --- SCREEN 4: MAP EDITOR ---

@Composable
fun MapEditorScreen(viewModel: MainViewModel) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    var brushTerrain by remember { mutableStateOf(TerrainType.GRASSLAND) }
    var selectedCastlePlayer by remember { mutableStateOf<Int?>(null) } // 1 or 2 to place castle

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Hybrid Editor: Tap a tile on map below to paint or place Castle",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE6E1E5),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Terrains painting
                    Button(
                        onClick = {
                            brushTerrain = TerrainType.GRASSLAND
                            selectedCastlePlayer = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (brushTerrain == TerrainType.GRASSLAND && selectedCastlePlayer == null) Color(0xFF2E7D32) else Color(0xFF49454F),
                            contentColor = Color(0xFFE6E1E5)
                        ),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Grass", fontSize = 11.sp) }

                    Button(
                        onClick = {
                            brushTerrain = TerrainType.HILLS
                            selectedCastlePlayer = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (brushTerrain == TerrainType.HILLS && selectedCastlePlayer == null) Color(0xFFF9A825) else Color(0xFF49454F),
                            contentColor = if (brushTerrain == TerrainType.HILLS && selectedCastlePlayer == null) Color(0xFF381E72) else Color(0xFFE6E1E5)
                        ),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Hills", fontSize = 11.sp) }

                    Button(
                        onClick = {
                            brushTerrain = TerrainType.MOUNTAINS
                            selectedCastlePlayer = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (brushTerrain == TerrainType.MOUNTAINS && selectedCastlePlayer == null) Color(0xFF5D4037) else Color(0xFF49454F),
                            contentColor = Color(0xFFE6E1E5)
                        ),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Mount", fontSize = 11.sp) }

                    Button(
                        onClick = {
                            brushTerrain = TerrainType.WATER
                            selectedCastlePlayer = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (brushTerrain == TerrainType.WATER && selectedCastlePlayer == null) Color(0xFF0277BD) else Color(0xFF49454F),
                            contentColor = Color(0xFFE6E1E5)
                        ),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Water", fontSize = 11.sp) }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Castle placement triggers
                    Button(
                        onClick = { selectedCastlePlayer = 1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCastlePlayer == 1) Color(0xFF3F51B5) else Color(0xFF49454F),
                            contentColor = Color(0xFFE6E1E5)
                        ),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Place P1 Castle", fontSize = 11.sp) }

                    Button(
                        onClick = { selectedCastlePlayer = 2 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCastlePlayer == 2) Color(0xFFF44336) else Color(0xFF49454F),
                            contentColor = Color(0xFFE6E1E5)
                        ),
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Place P2 Castle", fontSize = 11.sp) }
                }
            }
        }

        // Central Map Area
        Box(modifier = Modifier.weight(1f)) {
            HexMap(
                tiles = state.tiles,
                castles = state.castles,
                units = emptyList(),
                visibleTiles = state.tiles.map { Pair(it.row, it.col) }.toSet(),
                selectedCoord = null,
                reachableTiles = emptySet(),
                attackRangeTiles = emptySet(),
                selectedAttackTargets = emptySet(),
                selectedCastleAttackTarget = null,
                pendingPath = null,
                isAttackMode = false,
                isMapEditing = true,
                onTileClick = { r, c ->
                    val targetCastle = selectedCastlePlayer
                    if (targetCastle != null) {
                        viewModel.placeCastle(targetCastle, r, c)
                        viewModel.showToast("Placed Player $targetCastle Castle at ($r, $c)")
                    } else {
                        viewModel.paintTileTerrain(r, c, brushTerrain)
                    }
                }
            )
        }

        // Action confirmation footers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.generateNewRandomMap(state.preset) },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE6E1E5))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFE6E1E5))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Regen Random", fontSize = 12.sp)
            }

            Button(
                onClick = { viewModel.finalizeMapAndContinue() },
                modifier = Modifier.weight(1.2f).testTag("confirm_map_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD0BCFF),
                    contentColor = Color(0xFF381E72)
                )
            ) {
                Text("Accept Map", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// --- SCREEN 5: LAN SETUP / WAIT LOBBY ---

@Composable
fun LANSetupScreen(viewModel: MainViewModel) {
    val networkRole by viewModel.networkRole.collectAsStateWithLifecycle()
    val clientIp by viewModel.connectedClientIp.collectAsStateWithLifecycle()
    val isConnecting by viewModel.isNetworkConnecting.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (networkRole == MainViewModel.NetworkRole.HOST) {
            Icon(Icons.Default.SettingsCell, contentDescription = null, modifier = Modifier.size(70.dp), tint = Color(0xFFD0BCFF))
            Spacer(modifier = Modifier.height(16.dp))
            Text("WAITING FOR CLIENT", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFE6E1E5))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Give this IP to the Client player on the same WiFi network:",
                color = Color(0xFFCAC4D0),
                textAlign = TextAlign.Center
            )
            Text(
                text = viewModel.localIp,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = Color(0xFFD0BCFF),
                modifier = Modifier.padding(12.dp),
                fontFamily = FontFamily.Monospace
            )
            Text("Port: 9090", color = Color(0xFFCAC4D0), fontSize = 14.sp)

            Spacer(modifier = Modifier.height(40.dp))
            CircularProgressIndicator(color = Color(0xFFD0BCFF))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Status: Waiting for Client connection...", color = Color(0xFFCAC4D0), fontSize = 13.sp)
        } else {
            // Client Connecting screen
            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(70.dp), tint = Color(0xFFD0BCFF))
            Spacer(modifier = Modifier.height(16.dp))
            Text("CONNECTING TO HOST", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFE6E1E5))
            Spacer(modifier = Modifier.height(24.dp))
            if (isConnecting) {
                CircularProgressIndicator(color = Color(0xFFD0BCFF))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Searching for host socket server...", color = Color(0xFFCAC4D0))
            } else {
                Text("Failed to establish socket connection.", color = Color(0xFFF2B8B5))
            }
        }
    }
}

// --- SCREEN 6: DEPLOYMENT ---

@Composable
fun DeploymentScreen(viewModel: MainViewModel) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val selectedUnit by viewModel.selectedUnit.collectAsStateWithLifecycle()
    val networkRole by viewModel.networkRole.collectAsStateWithLifecycle()
    val perspective by viewModel.sandboxActivePerspective.collectAsStateWithLifecycle()

    // Filter units that belong to the active perspective/player
    val playerNumber = viewModel.myPlayerNumber
    val playerUnits = state.units.filter { it.owner == playerNumber }
    val deployedCount = playerUnits.count { it.row != -1 }
    val totalCount = playerUnits.size

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deploy Units ($deployedCount / $totalCount)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE6E1E5),
                        fontSize = 14.sp
                    )
                    
                    if (networkRole == MainViewModel.NetworkRole.SANDBOX) {
                        Button(
                            onClick = { viewModel.toggleSandboxPerspective() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72)
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Swap to P${if (perspective == 1) 2 else 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "Tap a unit from reserves below, then click a highlighted hex in your Deployment Zone (nearest 2 rows).",
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Map area: Deployment highlight zones
        Box(modifier = Modifier.weight(1f)) {
            // Get deployment zone visible/clickable overlay
            val deploymentHighlight = mutableSetOf<Pair<Int, Int>>()
            for (r in 0 until state.mapHeight) {
                for (c in 0 until state.mapWidth) {
                    if (GameEngine.isCoordinateInDeploymentZone(playerNumber, r, state.mapHeight)) {
                        deploymentHighlight.add(Pair(r, c))
                    }
                }
            }

            HexMap(
                tiles = state.tiles,
                castles = state.castles,
                units = state.units,
                visibleTiles = state.tiles.map { Pair(it.row, it.col) }.toSet(),
                selectedCoord = null,
                reachableTiles = if (selectedUnit != null && selectedUnit!!.row == -1) deploymentHighlight else emptySet(),
                attackRangeTiles = emptySet(),
                selectedAttackTargets = emptySet(),
                selectedCastleAttackTarget = null,
                pendingPath = null,
                isAttackMode = false,
                isMapEditing = true, // Force full visibility of other units to avoid deployment conflicts
                onTileClick = { r, c ->
                    if (selectedUnit != null) {
                        if (selectedUnit!!.row == -1) {
                            viewModel.deployUnit(selectedUnit!!.id, r, c)
                        }
                    } else {
                        // Undeploy unit
                        val clickedUnit = state.units.firstOrNull { it.row == r && it.col == c && it.owner == playerNumber }
                        if (clickedUnit != null) {
                            viewModel.undeployUnit(clickedUnit.id)
                        }
                    }
                }
            )
        }

        // Reserve Bank
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
            border = BorderStroke(1.dp, Color(0xFF49454F))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Reserve Units Bank (Taps to select):", color = Color(0xFFCAC4D0), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                
                // Horizontal list of reserves
                val reserves = playerUnits.filter { it.row == -1 }
                if (reserves.isEmpty()) {
                    Text("All units successfully deployed! Click READY below.", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (resUnit in reserves) {
                            val isSelected = selectedUnit?.id == resUnit.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFD0BCFF) else Color(0xFF49454F))
                                    .clickable { viewModel.onTileSelected(resUnit.row, resUnit.col) } // Taps to select in VM
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = resUnit.type.displayName,
                                    color = if (isSelected) Color(0xFF381E72) else Color(0xFFE6E1E5),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ready trigger
        Button(
            onClick = { viewModel.setReady() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .height(48.dp)
                .testTag("ready_deployment_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Ready to Battle!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// --- SCREEN 7: BATTLE SCREEN ---

@Composable
fun BattleScreen(viewModel: MainViewModel) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val selectedCoord by viewModel.selectedCoord.collectAsStateWithLifecycle()
    val selectedUnit by viewModel.selectedUnit.collectAsStateWithLifecycle()
    val reachableTiles by viewModel.reachableTiles.collectAsStateWithLifecycle()
    val attackTargets by viewModel.attackTargets.collectAsStateWithLifecycle()
    val selectedAttackTargets by viewModel.selectedAttackTargets.collectAsStateWithLifecycle()
    val selectedCastleTarget by viewModel.selectedCastleAttackTarget.collectAsStateWithLifecycle()
    val isAttackMode by viewModel.isAttackMode.collectAsStateWithLifecycle()

    val pendingMovePath by viewModel.pendingMovePath.collectAsStateWithLifecycle()
    val pendingMoveCost by viewModel.pendingMoveCost.collectAsStateWithLifecycle()

    val networkRole by viewModel.networkRole.collectAsStateWithLifecycle()
    val perspective by viewModel.sandboxActivePerspective.collectAsStateWithLifecycle()

    val myPlayer = viewModel.myPlayerNumber
    val isMyTurn = state.activePlayer == myPlayer || networkRole == MainViewModel.NetworkRole.SANDBOX

    // Vision coordinates mapping
    val visibleCoords = remember(state.units, state.castles, state.activePlayer, perspective, state.tiles) {
        val viewer = if (networkRole == MainViewModel.NetworkRole.SANDBOX) perspective else myPlayer
        GameEngine.getVisibleCoordinates(viewer, state.tiles, state.units, state.castles, state.preset)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Turn banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isMyTurn) Color(0xFF2E7D32) else Color(0xFF3B292C))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isMyTurn) "YOUR TURN (Player ${state.activePlayer})" else "WAITING FOR ENEMY (Player ${state.activePlayer})",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (networkRole == MainViewModel.NetworkRole.SANDBOX) {
                    Button(
                        onClick = { viewModel.toggleSandboxPerspective() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD0BCFF),
                            contentColor = Color(0xFF381E72)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Swap view: P$perspective", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Castle Status Bars HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2B2930))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val castle1 = state.castles.first { it.owner == 1 }
            val castle2 = state.castles.first { it.owner == 2 }

            Column {
                Text("P1 Castle", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("HP: ${castle1.hp} / ${castle1.maxHp}", color = Color(0xFFE6E1E5), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("P2 Castle", color = Color(0xFFF2B8B5), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("HP: ${castle2.hp} / ${castle2.maxHp}", color = Color(0xFFE6E1E5), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Map Render Area
        Box(modifier = Modifier.weight(1f)) {
            HexMap(
                tiles = state.tiles,
                castles = state.castles,
                units = state.units,
                visibleTiles = visibleCoords,
                selectedCoord = selectedCoord,
                reachableTiles = reachableTiles.keys,
                attackRangeTiles = attackTargets.filterIsInstance<GameUnit>().map { Pair(it.row, it.col) }.toSet() +
                        attackTargets.filterIsInstance<Castle>().map { Pair(it.row, it.col) }.toSet(),
                selectedAttackTargets = selectedAttackTargets,
                selectedCastleAttackTarget = selectedCastleTarget,
                pendingPath = pendingMovePath,
                isAttackMode = isAttackMode,
                onTileClick = { r, c ->
                    if (isMyTurn) {
                        viewModel.onTileSelected(r, c)
                    } else {
                        viewModel.showToast("It is not your turn!")
                    }
                }
            )
        }

        // Contextual Action Panel & HUD
        AnimatedVisibility(visible = isMyTurn) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                border = BorderStroke(1.dp, Color(0xFF49454F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (selectedUnit != null) {
                        val unit = selectedUnit!!
                        // 1. Show Unit Statistics Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${unit.type.displayName} (Player ${unit.owner})",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE6E1E5),
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "HP: ${unit.hp} | Energy: ${unit.energy} | Attacked: ${if (unit.energy == 0) "Yes" else "No"}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFCAC4D0)
                                )
                            }

                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Default.Close, contentDescription = "Deselect", tint = Color(0xFFCAC4D0))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Action Buttons depending on confirmation / mode
                        when {
                            pendingMovePath != null -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.clearSelection() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF49454F),
                                            contentColor = Color(0xFFE6E1E5)
                                        )
                                    ) { Text("Cancel", fontSize = 13.sp) }

                                    Button(
                                        onClick = { viewModel.confirmPendingMove() },
                                        modifier = Modifier.weight(1.3f).testTag("confirm_move_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD0BCFF),
                                            contentColor = Color(0xFF381E72)
                                        )
                                    ) {
                                        Text("Confirm Move (-$pendingMoveCost E)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            isAttackMode -> {
                                val currentDamage = if (selectedAttackTargets.isNotEmpty()) {
                                    GameEngine.calculateCombatDamage(unit, selectedAttackTargets.size, state.preset)
                                } else if (selectedCastleTarget != null) {
                                    GameEngine.calculateCombatDamage(unit, 1, state.preset)
                                } else 0

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Targets: ${selectedAttackTargets.size + (if (selectedCastleTarget != null) 1 else 0)}", color = Color(0xFFE6E1E5), fontSize = 12.sp)
                                        Text("Est DMG: $currentDamage", color = Color(0xFFF2B8B5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.clearSelection() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF49454F),
                                            contentColor = Color(0xFFE6E1E5)
                                        )
                                    ) { Text("Cancel") }

                                    Button(
                                        onClick = { viewModel.confirmAttack() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFF2B8B5),
                                            contentColor = Color(0xFF601410)
                                        ),
                                        modifier = Modifier.testTag("confirm_attack_button")
                                    ) {
                                        Text("Fire! (-All Energy)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            else -> {
                                // Default actions block
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Move Action Hint
                                    if (unit.energy > 0) {
                                        Button(
                                            onClick = { viewModel.showToast("Tap any green highlighted hex to select destination!") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF381E72),
                                                contentColor = Color(0xFFD0BCFF)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Move", fontSize = 12.sp)
                                        }
                                    }

                                    // Attack Trigger
                                    if (unit.type != UnitType.ENGINEER && unit.energy > 0 && attackTargets.isNotEmpty()) {
                                        Button(
                                            onClick = { viewModel.startAttackMode() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFF2B8B5),
                                                contentColor = Color(0xFF601410)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Attack Mode", fontSize = 12.sp)
                                        }
                                    }

                                    // Engineer Trench ability
                                    if (unit.type == UnitType.ENGINEER && unit.energy >= state.preset.costBuildTrench) {
                                        // Verify if current tile has trench
                                        val standingTile = state.tiles.firstOrNull { it.row == unit.row && it.col == unit.col }
                                        if (standingTile?.hasTrench == false) {
                                            Button(
                                                onClick = { viewModel.buildTrench() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8D6E63)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Dig Trench (-${state.preset.costBuildTrench} E)", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    // Engineer Bridge ability
                                    if (unit.type == UnitType.ENGINEER && unit.energy >= state.preset.costBuildBridge) {
                                        // Find adjacent unbridged water tiles
                                        val neighbors = GameEngine.getNeighbors(unit.row, unit.col, state.mapWidth, state.mapHeight)
                                        val bridgeableWaterNeighbor = state.tiles.firstOrNull { t ->
                                            neighbors.any { it.first == t.row && it.second == t.col } &&
                                                    t.terrain == TerrainType.WATER && !t.hasBridge
                                        }

                                        if (bridgeableWaterNeighbor != null) {
                                            Button(
                                                onClick = {
                                                    viewModel.buildBridge(bridgeableWaterNeighbor.row, bridgeableWaterNeighbor.col)
                                                    viewModel.showToast("Constructing Bridge on Water tile (${bridgeableWaterNeighbor.row}, ${bridgeableWaterNeighbor.col})!")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF795548)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Build Bridge (-${state.preset.costBuildBridge} E)", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 3. No selected unit: standard Turn End
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Select any friendly unit (blue glow) to perform actions.", color = Color(0xFFCAC4D0), fontSize = 12.sp)
                            Button(
                                onClick = { viewModel.endTurn() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD0BCFF),
                                    contentColor = Color(0xFF381E72)
                                ),
                                modifier = Modifier.testTag("end_turn_button")
                            ) {
                                Icon(Icons.Default.NavigateNext, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("End Turn")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN 8: VICTORY SCREEN ---

@Composable
fun VictoryScreen(viewModel: MainViewModel) {
    val state by viewModel.gameState.collectAsStateWithLifecycle()
    val winner = state.winnerPlayer ?: 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.EmojiEvents, contentDescription = "Trophy", modifier = Modifier.size(100.dp), tint = Color(0xFFD0BCFF))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "MATCH COMPLETE",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFFCAC4D0),
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "PLAYER $winner\nVICTORIOUS!",
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            color = Color(0xFFD0BCFF),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text(
            text = "The enemy castle has been reduced to rubble! Tactical superiority achieved.",
            color = Color(0xFFE6E1E5),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { viewModel.exitGame() },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD0BCFF),
                contentColor = Color(0xFF381E72)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Return to Main Menu", fontWeight = FontWeight.Bold)
        }
    }
}
