package com.my.forintern.HomeScreen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.my.forintern.Message.ChatMessage
import kotlinx.coroutines.launch
import java.nio.file.Files.size
import kotlin.io.path.Path
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    username: String,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    //pipeline for the messages
    val pipelineState by viewModel.messagePipeline.collectAsState()


    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setHasMicPermission(isGranted)
        if (isGranted) {
            viewModel.toggleListening()
        }
    }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setHasMicPermission(isGranted)
    }

    // Parallax & Swipe logic
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }


    val dragOffset = remember { Animatable(0f) }

    // Progress of scroll: 0f = Aura visible, 1f = Chat History visible
    val scrollProgress = (-dragOffset.value / screenHeightPx).coerceIn(0f, 1f)

    // Modern dark theme background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13)) // Deep dark space color
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (dragOffset.value < -screenHeightPx / 4) {
                                // Snap to Chat History
                                dragOffset.animateTo(-screenHeightPx, tween(300))
                                viewModel.setChatHistoryVisibility(true)
                            } else {
                                // Snap back to Aura
                                dragOffset.animateTo(0f, tween(300))
                                viewModel.setChatHistoryVisibility(false)
                            }
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        val newOffset = (dragOffset.value + dragAmount).coerceIn(-screenHeightPx, 0f)
                        dragOffset.snapTo(newOffset)
                    }
                }
            }
    ) {
        // --- LAYER 1: AURA CIRCLE (Fades out and moves up as we scroll up) ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (dragOffset.value * 0.5f).roundToInt()) } // Parallax
                .alpha(1f - scrollProgress)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Greeting
            Text(
                text = "Hello, ${if (username.isNotBlank()) username else userProfile.name}",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (uiState.auraState == AuraState.IDLE) "Your aura is resting." else "I'm listening...",
                color = Color(0xFFA0A0B0),
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Traits display
            if (userProfile.traits.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    userProfile.traits.forEach { trait ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A2A35))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = trait, color = Color(0xFFD0D0E0), fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // The Aura Circle Composable
            AuraCircle(
                state = uiState.auraState,
                amplitude = uiState.amplitude,
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Controls Row (Mic & Keyboard)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Microphone Toggle Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(
                            elevation = if (uiState.auraState == AuraState.LISTENING) 20.dp else 8.dp,
                            shape = CircleShape,
                            spotColor = if (uiState.auraState == AuraState.LISTENING) Color(0xFFE91E63) else Color(0xFF6200EA)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = if (uiState.auraState == AuraState.LISTENING) {
                                    listOf(Color(0xFFFF4081), Color(0xFFC2185B))
                                } else {
                                    listOf(Color(0xFF7C4DFF), Color(0xFF512DA8))
                                }
                            )
                        )
                        .clickable {
                            if (uiState.hasMicPermission) {
                                viewModel.toggleListening()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.auraState == AuraState.LISTENING) "■" else "🎤",
                        fontSize = 24.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Keyboard Button
                IconButton(
                    onClick = { viewModel.toggleKeyboard() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A35))
                ) {
                    Text(text = "⌨", fontSize = 24.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- LAYER 2: CHAT HISTORY (Slides up from the bottom) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (screenHeightPx + dragOffset.value).roundToInt()) }
                .background(Color(0xFF0F0F13)) // Same dark background
        ) {
            ChatHistoryScreen(
                messages = uiState.visibleMessages,
                onLoadMore = { viewModel.loadMoreMessages() },
                onClose = {
                    scope.launch {
                        dragOffset.animateTo(0f, tween(300))
                        viewModel.setChatHistoryVisibility(false)
                    }
                },
                onEdit = { viewModel.setEditingMessage(it) },
                onDelete = { viewModel.deleteMessage(it) }
            )
        }

        // --- LAYER 3: CUSTOM KEYBOARD TEXT INPUT (Slides up from the bottom when requested) ---
        AnimatedVisibility(
            visible = uiState.isKeyboardVisible && scrollProgress < 0.5f,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E28), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(16.dp)
                    .navigationBarsPadding() // Keep above bottom nav
            ) {
                //pipeline UI
                Column {
                    if (uiState.editingMessage != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Editing message...", color = Color(0xFF7C4DFF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = { viewModel.setEditingMessage(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text("✕", color = Color.Gray, fontSize = 16.sp)
                            }
                        }
                    }

                    if (pipelineState !is MessagePipeline.Idle && pipelineState !is MessagePipeline.Typing) {
                        val statusText = when (pipelineState) {
                            is MessagePipeline.Validating -> "Validating message..."
                            is MessagePipeline.Processing -> "Processing..."
                            is MessagePipeline.Responding -> "Responding..."
                            is MessagePipeline.Error -> "Error: ${(pipelineState as MessagePipeline.Error).reason}"
                            else -> ""
                        }
                        val statusColor = if (pipelineState is MessagePipeline.Error) Color.Red else Color.LightGray

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = statusText, color = statusColor, fontSize = 14.sp)
                            if (pipelineState is MessagePipeline.Error) {
                                Text(
                                    text = "Retry",
                                    color = Color(0xFF7C4DFF),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.retryMessage((pipelineState as MessagePipeline.Error).lastMessage)
                                    }
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = { viewModel.updateInputText(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF7C4DFF),
                                unfocusedBorderColor = Color.Gray,
                                focusedContainerColor = Color(0xFF2A2A35),
                                unfocusedContainerColor = Color(0xFF2A2A35)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFF512DA8))))
                        ) {
                            Text(text = "➤", fontSize = 20.sp, color = Color.White)
                        }
                    }
                } // close Column
            }
        }
    }
}

@Composable
fun ChatHistoryScreen(
    messages: List<ChatMessage>,
    onLoadMore: () -> Unit,
    onClose: () -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit
) {
    val listState = rememberLazyListState()

    // Detect when user scrolls to top to load more (since messages are usually bottom-up)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { firstVisibleIndex ->
                if (firstVisibleIndex == 0 && messages.isNotEmpty()) {
                    onLoadMore()
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Text("▼", color = Color.White, fontSize = 24.sp) // Custom down arrow
            }
            Text(
                "Chat History",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            reverseLayout = false // or true depending on how you want to show it. Assuming normal top-to-bottom for older-to-newer
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    onEdit = { onEdit(message) },
                    onDelete = { onDelete(message) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val alignment = if (message.issentByme) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.issentByme) Color(0xFF7C4DFF) else Color(0xFF2A2A35)
    val textColor = Color.White

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.issentByme) 16.dp else 0.dp,
                    bottomEnd = if (message.issentByme) 0.dp else 16.dp
                ))
                .background(bgColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            if (message.issentByme) {
                                showMenu = true
                            }
                        }
                    )
                }
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(text = message.text, color = textColor, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.time,
                color = textColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.End)
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun AuraCircle(
    state: AuraState,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "auraTransition")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Smooth amplitude for visual drawing
    val animatedAmplitude by animateFloatAsState(
        targetValue = if (state == AuraState.LISTENING) amplitude else 0.05f, // 0.05f provides a slight idle breath
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "amplitude"
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = size.minDimension / 3f

        // We will draw 3 morphing blob layers
        val layers = listOf(
            AuraLayer(
                colorStart = Color(0xFF9C27B0).copy(alpha = 0.6f),
                colorEnd = Color.Transparent,
                frequency = 3f,
                timeMultiplier = 1.0f,
                amplitudeMultiplier = 0.3f,
                phaseOffset = 0f
            ),
            AuraLayer(
                colorStart = Color(0xFF00BCD4).copy(alpha = 0.5f),
                colorEnd = Color.Transparent,
                frequency = 4f,
                timeMultiplier = -1.2f,
                amplitudeMultiplier = 0.25f,
                phaseOffset = 1.5f
            ),
            AuraLayer(
                colorStart = Color(0xFFE91E63).copy(alpha = 0.4f),
                colorEnd = Color.Transparent,
                frequency = 5f,
                timeMultiplier = 1.5f,
                amplitudeMultiplier = 0.2f,
                phaseOffset = 3f
            )
        )

        drawIntoCanvas { canvas ->
            for (layer in layers) {
                val path = Path()
                val points = 120
                val angleStep = (2.0 * Math.PI) / points

                for (i in 0..points) {
                    val angle = i * angleStep
                    // radius modulation: base + wave * amplitude effect
                    val wave = sin(layer.frequency * angle + time * layer.timeMultiplier + layer.phaseOffset).toFloat()

                    // overall breathing effect
                    val breath = sin(time).toFloat() * 0.05f

                    val baseR = baseRadius * (1f + animatedAmplitude * 0.5f)
                    val r = baseR * (1f + breath + wave * (0.05f + animatedAmplitude * layer.amplitudeMultiplier * 2.5f))

                    val x = centerX + r * cos(angle).toFloat()
                    val y = centerY + r * sin(angle).toFloat()

                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()

                val brush = Brush.radialGradient(
                    colors = listOf(layer.colorStart, layer.colorEnd),
                    center = Offset(centerX, centerY),
                    radius = baseRadius * 2f
                )

                drawPath(
                    path = path,
                    brush = brush,
                    blendMode = BlendMode.Screen
                )
            }

            // Draw a bright core to make it look like a glowing orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = (0.8f + animatedAmplitude * 0.2f).coerceIn(0f, 1f)), Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = baseRadius * 0.6f * (1f + animatedAmplitude * 0.6f)
                ),
                center = Offset(centerX, centerY),
                blendMode = BlendMode.Plus
            )
        }
    }
}

private data class AuraLayer(
    val colorStart: Color,
    val colorEnd: Color,
    val frequency: Float,
    val timeMultiplier: Float,
    val amplitudeMultiplier: Float,
    val phaseOffset: Float
)
