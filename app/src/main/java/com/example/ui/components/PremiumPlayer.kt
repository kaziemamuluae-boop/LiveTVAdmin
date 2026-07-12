package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(UnstableApi::class)
@Composable
fun PremiumPlayer(
    streamUrl: String,
    serverName: String,
    quality: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // Position persistence key
    val sharedPrefs = remember { context.getSharedPreferences("kazi_player_prefs", Context.MODE_PRIVATE) }
    val autoResumePosition = remember(streamUrl) { sharedPrefs.getLong(streamUrl, 0L) }

    // ExoPlayer Instance
    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            val mediaItem = MediaItem.fromUri(streamUrl)
            setMediaItem(mediaItem)
            if (autoResumePosition > 0) {
                seekTo(autoResumePosition)
            }
            prepare()
        }
    }

    // Audio Manager for Volume control
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }

    // States
    var isPlaying by remember { mutableStateOf(true) }
    var playbackState by remember { mutableStateOf(Player.STATE_IDLE) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isBufferIndicatorVisible by remember { mutableStateOf(false) }

    var areControlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var isSpeedDialogVisible by remember { mutableStateOf(false) }

    // Swipe feedback overlay states
    var gestureVolumeValue by remember { mutableStateOf(0f) }
    var gestureBrightnessValue by remember { mutableStateOf(0f) }
    var isVolumeGestureVisible by remember { mutableStateOf(false) }
    var isBrightnessGestureVisible by remember { mutableStateOf(false) }
    var doubleTapFeedbackText by remember { mutableStateOf<String?>(null) }

    // Track state changes
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                isBufferIndicatorVisible = state == Player.STATE_BUFFERING
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }

            override fun onPlayerError(error: PlaybackException) {
                isBufferIndicatorVisible = false
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            // Persist position for auto resume
            val currentPos = exoPlayer.currentPosition
            sharedPrefs.edit().putLong(streamUrl, currentPos).apply()
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Coroutine to update seek progress
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    // Coroutine to auto-hide controls
    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible) {
            delay(4000)
            if (!isSpeedDialogVisible) {
                areControlsVisible = false
            }
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Android Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Gesture Detector Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            if (!isLocked) {
                                val width = size.width
                                if (offset.x < width / 2) {
                                    // Rewind
                                    val newPos = max(0L, exoPlayer.currentPosition - 10000L)
                                    exoPlayer.seekTo(newPos)
                                    doubleTapFeedbackText = "⏪ -10s"
                                } else {
                                    // Fast Forward
                                    val newPos = min(exoPlayer.duration, exoPlayer.currentPosition + 10000L)
                                    exoPlayer.seekTo(newPos)
                                    doubleTapFeedbackText = "⏩ +10s"
                                }
                                scope.launch {
                                    delay(800)
                                    doubleTapFeedbackText = null
                                }
                            }
                        },
                        onTap = {
                            areControlsVisible = !areControlsVisible
                        }
                    )
                }
                .pointerInput(Unit) {
                    // Vertical Swipe for Volume (right) and Brightness (left)
                    var totalDragY = 0f
                    var initialVolume = 0f
                    var initialBrightness = 0f
                    var isRightSide = false

                    detectDragGestures(
                        onDragStart = { offset ->
                            totalDragY = 0f
                            isRightSide = offset.x > size.width / 2

                            if (isRightSide) {
                                initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                                isVolumeGestureVisible = true
                            } else {
                                activity?.let { act ->
                                    val attrs = act.window.attributes
                                    initialBrightness = if (attrs.screenBrightness < 0) 0.5f else attrs.screenBrightness
                                }
                                isBrightnessGestureVisible = true
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragY -= dragAmount.y // invert coordinate system

                            val height = size.height
                            val deltaFraction = totalDragY / height

                            if (isRightSide) {
                                val deltaVolume = deltaFraction * maxVolume
                                val newVolume = (initialVolume + deltaVolume).coerceIn(0f, maxVolume)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume.toInt(), 0)
                                gestureVolumeValue = (newVolume / maxVolume) * 100
                            } else {
                                activity?.let { act ->
                                    val newBrightness = (initialBrightness + deltaFraction).coerceIn(0.01f, 1.0f)
                                    val attrs = act.window.attributes
                                    attrs.screenBrightness = newBrightness
                                    act.window.attributes = attrs
                                    gestureBrightnessValue = newBrightness * 100
                                }
                            }
                        },
                        onDragEnd = {
                            isVolumeGestureVisible = false
                            isBrightnessGestureVisible = false
                        },
                        onDragCancel = {
                            isVolumeGestureVisible = false
                            isBrightnessGestureVisible = false
                        }
                    )
                }
        )

        // 3. Playback Controls Layer
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                if (isLocked) {
                    // Locked Mode overlay: only shows unlock button
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { isLocked = false },
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(32.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Unlock Controls",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                } else {
                    // Full controls when unlocked
                    // TOP BAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = serverName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quality: $quality",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        // Playback Speed Button
                        IconButton(onClick = { isSpeedDialogVisible = true }) {
                            Icon(
                                imageVector = Icons.Default.SlowMotionVideo,
                                contentDescription = "Playback Speed",
                                tint = Color.White
                            )
                        }

                        // Picture in Picture Button
                        if (activity != null) {
                            IconButton(onClick = {
                                try {
                                    activity.enterPictureInPictureMode()
                                } catch (e: Exception) {
                                    // Handle PiP failure
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White
                               )
                            }
                        }

                        // Lock Controls Button
                        IconButton(onClick = { isLocked = true }) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Lock Controls",
                                tint = Color.White
                            )
                        }
                    }

                    // CENTER PLAY/PAUSE
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(36.dp))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // BOTTOM CONTROLS (SeekBar, Timers, Volume/Audio track indicators)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        // Progress / Seekbar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Slider(
                                value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                onValueChange = { fraction ->
                                    val targetPos = (fraction * duration).toLong()
                                    exoPlayer.seekTo(targetPos)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                )
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Buffering circular indicator
        if (isBufferIndicatorVisible) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
        }

        // 5. Brightness Indicator overlay
        AnimatedVisibility(
            visible = isBrightnessGestureVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 32.dp)
        ) {
            GestureOverlay(
                icon = Icons.Default.Brightness5,
                title = "Brightness",
                value = gestureBrightnessValue.toInt()
            )
        }

        // 6. Volume Indicator overlay
        AnimatedVisibility(
            visible = isVolumeGestureVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 32.dp)
        ) {
            GestureOverlay(
                icon = Icons.Default.VolumeUp,
                title = "Volume",
                value = gestureVolumeValue.toInt()
            )
        }

        // 7. Double Tap seek HUD popup overlay
        doubleTapFeedbackText?.let { feedback ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 96.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = feedback,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Playback Speed Dialog
        if (isSpeedDialogVisible) {
            AlertDialog(
                onDismissRequest = { isSpeedDialogVisible = false },
                title = { Text("Playback Speed", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playbackSpeed = speed
                                        exoPlayer.playbackParameters = PlaybackParameters(speed)
                                        isSpeedDialogVisible = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = playbackSpeed == speed,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = if (speed == 1.0f) "Normal" else "${speed}x", fontSize = 16.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isSpeedDialogVisible = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun GestureOverlay(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$value%",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
