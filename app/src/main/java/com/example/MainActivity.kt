@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.data.database.HistoryItem
import com.example.data.repository.HistoryRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.RngTab
import com.example.ui.RngViewModel
import com.example.ui.RngViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core room persistence instantiations
        val database = AppDatabase.getDatabase(this)
        val repository = HistoryRepository(database.historyDao())

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RngAppScreen(
                        repository = repository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun RngAppScreen(
    repository: HistoryRepository,
    modifier: Modifier = Modifier,
    viewModel: RngViewModel = viewModel(factory = RngViewModelFactory(repository))
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val history by viewModel.historyList.collectAsStateWithLifecycle()

    var showHistoryConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // App Header
        AppHeaderSection()

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Control Tabs
        TabSelectionRow(
            activeTab = viewModel.activeTab,
            onTabSelected = { viewModel.activeTab = it }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Main Core Interactive Cards & Mode UIs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (viewModel.activeTab) {
                RngTab.RANGE -> RangeGeneratorView(viewModel = viewModel)
                RngTab.DICE -> DiceRollerView(viewModel = viewModel)
                RngTab.COIN -> CoinFlipperView(viewModel = viewModel)
                RngTab.LIST_DRAW -> ListDrawerView(viewModel = viewModel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Collapsible History Panel
        HistorySection(
            history = history,
            onDeleteItem = { viewModel.deleteHistoryItem(it) },
            onClearAll = { showHistoryConfirmDialog = true },
            onCopyResult = { result ->
                clipboardManager.setText(AnnotatedString(result))
                Toast.makeText(context, "Copied: $result", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showHistoryConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryConfirmDialog = false },
            title = { Text("Clear All History") },
            text = { Text("Are you sure you want to delete all generated history permanently?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showHistoryConfirmDialog = false
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_clear_history_button")
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AppHeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Randomizer",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Your ultimate tool for random generation",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.Casino,
            contentDescription = "Dice Icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
fun TabSelectionRow(
    activeTab: RngTab,
    onTabSelected: (RngTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RngTab.values().forEach { tab ->
                val isSelected = activeTab == tab
                val tabColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    animationSpec = tween(durationMillis = 250)
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(durationMillis = 250)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tabColor)
                        .clickable(
                            role = Role.Tab,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (tab) {
                            RngTab.RANGE -> "Range"
                            RngTab.DICE -> "Dice"
                            RngTab.COIN -> "Coin"
                            RngTab.LIST_DRAW -> "Draw"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun RangeGeneratorView(viewModel: RngViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Configuration Row
            Column {
                Text(
                    text = "Custom Range Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.rangeMinText,
                        onValueChange = { viewModel.rangeMinText = it },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("range_min_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = viewModel.rangeMaxText,
                        onValueChange = { viewModel.rangeMaxText = it },
                        label = { Text("Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("range_max_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Number Count Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Numbers to Generate: ${viewModel.rangeCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Slider(
                    value = viewModel.rangeCount.toFloat(),
                    onValueChange = { viewModel.rangeCount = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.fillMaxWidth()
                )

                // Allow Duplicates Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Allow Duplicates",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Generate repeating values",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = viewModel.rangeAllowDuplicates,
                        onCheckedChange = { viewModel.rangeAllowDuplicates = it },
                        modifier = Modifier.testTag("duplicates_switch")
                    )
                }
            }

            // Results Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.rangeResults.isEmpty() && !viewModel.isRangeGenerating) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Empty Roll",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap Generate to Roll Numbers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        AnimatedContent(
                            targetState = viewModel.rangeResults,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                            }
                        ) { results ->
                            if (results.size <= 2) {
                                // Large display for single/double numbers
                                Text(
                                    text = results.joinToString(", "),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                // Flow chips for multiple numbers
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    results.forEach { num ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = num.toString(),
                                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!viewModel.isRangeGenerating && viewModel.rangeResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            IconButton(
                                onClick = {
                                    val text = viewModel.rangeResults.joinToString(", ")
                                    clipboardManager.setText(AnnotatedString(text))
                                    Toast.makeText(context, "Copied range results!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy range result",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }

            // Big Glowing Button
            Button(
                onClick = { viewModel.generateNumbers() },
                enabled = !viewModel.isRangeGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_numbers_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (viewModel.isRangeGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Autorenew, contentDescription = "Roll")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GENERATE NUMBERS",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiceRollerView(viewModel: RngViewModel) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Select Number of Dice",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (1..6).forEach { num ->
                        val isSelected = viewModel.diceCount == num
                        val cardBg by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                            animationSpec = tween(200)
                        )
                        val textColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                            animationSpec = tween(200)
                        )

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardBg)
                                .clickable { viewModel.diceCount = num }
                                .testTag("dice_selector_$num"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = num.toString(),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = textColor
                            )
                        }
                    }
                }
            }

            // Dice tray with custom pips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.diceResults.isEmpty() && !viewModel.isDiceRolling) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Dice Roll",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Roll the Dice tray!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Display Dice Grid
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            viewModel.diceResults.forEach { value ->
                                CustomDieFace(
                                    value = value,
                                    isRolling = viewModel.isDiceRolling,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }

                        if (!viewModel.isDiceRolling) {
                            Text(
                                text = "Total Score: ${viewModel.diceResults.sum()}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.rollDice() },
                enabled = !viewModel.isDiceRolling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("roll_dice_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (viewModel.isDiceRolling) {
                    Text(
                        text = "ROLLING...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = "Roll")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ROLL DICE",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomDieFace(
    value: Int,
    isRolling: Boolean,
    modifier: Modifier = Modifier
) {
    val shakeOffset by animateFloatAsState(
        targetValue = if (isRolling) (-15..15).random().toFloat() else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh)
    )

    Card(
        modifier = modifier
            .size(68.dp)
            .graphicsLayer {
                translationX = shakeOffset
                translationY = shakeOffset
                rotationZ = shakeOffset * 0.5f
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = 5.dp.toPx()
                val dotColor = Color(0xFF1E293B) // Dark charcoal dots

                val width = size.width
                val height = size.height

                val left = width * 0.25f
                val centerX = width * 0.5f
                val right = width * 0.75f

                val top = height * 0.25f
                val centerY = height * 0.5f
                val bottom = height * 0.75f

                when (value) {
                    1 -> {
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(centerX, centerY))
                    }
                    2 -> {
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(left, top))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(right, bottom))
                    }
                    3 -> {
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(left, top))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(centerX, centerY))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(right, bottom))
                    }
                    4 -> {
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(left, top))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(right, top))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(left, bottom))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(right, bottom))
                    }
                    5 -> {
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(left, top))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(right, top))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(centerX, centerY))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(left, bottom))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(right, bottom))
                    }
                    6 -> {
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(left, top))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(right, top))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(left, centerY))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(right, centerY))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(left, bottom))
                        drawCircle(color = dotColor, radius = radius, center = androidx.compose.ui.geometry.Offset(right, bottom))
                    }
                }
            }
        }
    }
}

@Composable
fun CoinFlipperView(viewModel: RngViewModel) {
    val rotationY by animateFloatAsState(
        targetValue = if (viewModel.isCoinFlipping) 1080f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Coin Flip Mode",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Custom Metallic Coin Canvas with rotation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer {
                                this.rotationY = rotationY
                                cameraDistance = 12f * density
                            }
                            .shadow(8.dp, CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700), // Gold
                                        Color(0xFFDAA520)  // Goldenrod
                                    )
                                ),
                                shape = CircleShape
                            )
                            .border(6.dp, Color(0xFFB8860B), CircleShape)
                            .clickable(enabled = !viewModel.isCoinFlipping) { viewModel.flipCoin() }
                            .testTag("interactive_coin"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Coin inner design
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0x33000000),
                                radius = size.minDimension / 2.3f
                            )
                        }

                        // Text based on coin side
                        val sideText = viewModel.coinResult ?: "Heads"
                        Text(
                            text = if (sideText == "Heads") "H" else "T",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = Color(0xFF5C4033) // Deep dark brown metallic shade
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = viewModel.coinResult?.uppercase() ?: "TAP TO FLIP",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = { viewModel.flipCoin() },
                enabled = !viewModel.isCoinFlipping,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("flip_coin_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (viewModel.isCoinFlipping) {
                    Text(
                        text = "FLIPPING...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = "Coin")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FLIP COIN",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListDrawerView(viewModel: RngViewModel) {
    val itemsCount = remember(viewModel.listInputText) {
        viewModel.listInputText.split(Regex("[,\\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .size
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Pick Random Winners",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = viewModel.listInputText,
                    onValueChange = { viewModel.listInputText = it },
                    label = { Text("List items (separated by commas or newlines)") },
                    placeholder = { Text("e.g. Alice, Bob, Charlie") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("list_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Number to pick
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pick count: ${viewModel.listPickCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Slider(
                    value = viewModel.listPickCount.toFloat(),
                    onValueChange = { viewModel.listPickCount = it.toInt() },
                    valueRange = 1f..maxOf(1f, itemsCount.toFloat()),
                    steps = maxOf(0, itemsCount - 2),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Drawn result
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.listDrawResults.isEmpty() && !viewModel.isListDrawing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "List Draw",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Awaiting Winners Draw",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🎉 WINNERS 🎉",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            ),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.listDrawResults.forEach { result ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = result,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.drawFromList() },
                enabled = !viewModel.isListDrawing && itemsCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("draw_list_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (viewModel.isListDrawing) {
                    Text(
                        text = "DRAWING...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = "Draw")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DRAW FROM LIST",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistorySection(
    history: List<HistoryItem>,
    onDeleteItem: (HistoryItem) -> Unit,
    onClearAll: () -> Unit,
    onCopyResult: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Recent History (${history.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExpanded && history.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier.testTag("clear_history_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Expanded List
            if (isExpanded) {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No history recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .padding(bottom = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(history) { item ->
                            HistoryRow(
                                item = item,
                                onDelete = { onDeleteItem(item) },
                                onCopy = { onCopyResult(item.result) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRow(
    item: HistoryItem,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val formattedTime = remember(item.timestamp) {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mode Indicator Icon
            val icon = when (item.mode) {
                "Range" -> Icons.Default.Numbers
                "Dice" -> Icons.Default.Casino
                "Coin" -> Icons.Default.MonetizationOn
                else -> Icons.Default.List
            }
            Icon(
                imageVector = icon,
                contentDescription = item.mode,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.mode,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.result,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (item.details.isNotEmpty()) {
                    Text(
                        text = item.details,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions (Copy & Delete)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Item Result",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


