package com.example.ui.components

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.sin

/**
 * Helper for recording real voice confessions with high quality (.m4a AAC 256kbps 48kHz).
 * Zero simulated audio synthesis.
 */
class VoiceConfessionRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): String? {
        stopRecording()
        return try {
            val file = File(context.cacheDir, "anon_voice_${System.currentTimeMillis()}.m4a")
            outputFile = file
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(256000)
            mr.setAudioSamplingRate(48000)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            Log.e("VoiceRecorder", "Microphone recording failed: ${e.message}", e)
            recorder = null
            null
        }
    }

    fun stopRecording(): String? {
        val currentRecorder = recorder
        recorder = null
        if (currentRecorder != null) {
            try {
                currentRecorder.stop()
            } catch (_: Exception) {
            } finally {
                try {
                    currentRecorder.release()
                } catch (_: Exception) {
                }
            }
        }
        val file = outputFile
        return if (file != null && file.exists() && file.length() > 128L) {
            Uri.fromFile(file).toString()
        } else {
            null
        }
    }
}

/**
 * Smooth In-Built Video Player Component
 * Strictly preserves the video's original aspect ratio without stretching, cropping, or distortion.
 */
@Composable
fun InbuiltVideoPlayer(
    videoUri: String,
    caption: String?,
    durationSecHint: Long = 30L,
    modifier: Modifier = Modifier
) {
    var isFullscreen by remember { mutableStateOf(false) }

    VideoPlayerSurface(
        videoUri = videoUri,
        caption = caption,
        durationSecHint = durationSecHint,
        isFullscreen = false,
        onToggleFullscreen = { isFullscreen = true },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    )

    if (isFullscreen) {
        Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoPlayerSurface(
                    videoUri = videoUri,
                    caption = caption,
                    durationSecHint = durationSecHint,
                    isFullscreen = true,
                    onToggleFullscreen = { isFullscreen = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerSurface(
    videoUri: String,
    caption: String?,
    durationSecHint: Long,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf((durationSecHint.coerceAtLeast(1L) * 1000L).toInt()) }
    var isBuffering by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(isPlaying, playbackSpeed) {
        while (isActive && isPlaying) {
            val vv = videoViewRef
            if (vv != null && vv.isPlaying) {
                currentPositionMs = vv.currentPosition
                if (vv.duration > 0) {
                    totalDurationMs = vv.duration
                }
            }
            delay(200L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (_: Exception) {
            }
        }
    }

    BoxWithConstraints(
        modifier = if (isFullscreen) {
            modifier.fillMaxSize().background(Color.Black)
        } else {
            modifier
                .fillMaxWidth()
                .heightIn(min = 190.dp, max = 320.dp)
                .aspectRatio(videoAspectRatio.coerceIn(0.65f, 1.95f), matchHeightConstraintsFirst = false)
                .background(Color(0xFF0E0D18))
        }.testTag("inbuilt_video_player"),
        contentAlignment = Alignment.Center
    ) {
        if (!loadError && videoUri.isNotBlank()) {
            AndroidView(
                factory = { ctx ->
                    val frameLayout = FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    val videoView = VideoView(ctx).apply {
                        videoViewRef = this
                        isBuffering = true
                        setVideoURI(Uri.parse(videoUri))
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            isBuffering = false
                            if (mp.duration > 0) {
                                totalDurationMs = mp.duration
                            }
                            val vw = mp.videoWidth
                            val vh = mp.videoHeight
                            if (vw > 0 && vh > 0) {
                                videoAspectRatio = vw.toFloat() / vh.toFloat()
                            }
                            val vol = if (isMuted) 0f else 1f
                            mp.setVolume(vol, vol)
                            if (isPlaying) {
                                start()
                            }
                        }
                        setOnCompletionListener {
                            isPlaying = false
                            currentPositionMs = 0
                        }
                        setOnErrorListener { _, _, _ ->
                            isBuffering = false
                            loadError = true
                            true
                        }
                    }
                    val lp = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        android.view.Gravity.CENTER
                    )
                    frameLayout.addView(videoView, lp)
                    frameLayout
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF151426)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Video stream unavailable on this device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.70f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = caption?.takeIf { it.isNotBlank() } ?: "Anonymous Video",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            playbackSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            try {
                                val mp = mediaPlayerRef
                                if (mp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                }
                            } catch (_: Exception) {
                            }
                        }
                        .testTag("video_speed_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.50f)
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        val vol = if (isMuted) 0f else 1f
                        try {
                            mediaPlayerRef?.setVolume(vol, vol)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("video_mute_toggle")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute Video" else "Mute Video",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("video_fullscreen_toggle")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit Fullscreen" else "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Center Controls
        if (!loadError) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.50f))
                        .clickable {
                            val target = (currentPositionMs - 10_000).coerceAtLeast(0)
                            currentPositionMs = target
                            try {
                                videoViewRef?.seekTo(target)
                            } catch (_: Exception) {
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay10,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.92f))
                        .clickable {
                            isPlaying = !isPlaying
                            try {
                                if (isPlaying) videoViewRef?.start() else videoViewRef?.pause()
                            } catch (_: Exception) {
                            }
                        }
                        .testTag("video_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Video" else "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.50f))
                        .clickable {
                            val target = (currentPositionMs + 10_000).coerceAtMost(totalDurationMs)
                            currentPositionMs = target
                            try {
                                videoViewRef?.seekTo(target)
                            } catch (_: Exception) {
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forward10,
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Bottom Scrubber Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .align(Alignment.BottomCenter)
        ) {
            Slider(
                value = if (totalDurationMs > 0) {
                    (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f,
                onValueChange = { fraction ->
                    val seekMs = (fraction * totalDurationMs).toInt()
                    currentPositionMs = seekMs
                    try {
                        videoViewRef?.seekTo(seekMs)
                    } catch (_: Exception) {
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                ),
                modifier = Modifier.height(22.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMilliseconds(currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Text(
                    text = formatMilliseconds(totalDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * In-Built Audio Player Component (Real MediaPlayer only — zero simulated audio)
 */
@Composable
fun InbuiltAudioPlayer(
    audioUri: String,
    title: String?,
    durationSecHint: Long = 15L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember {
        mutableIntStateOf((durationSecHint.coerceAtLeast(1L) * 1000L).toInt())
    }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(audioUri) {
        if (audioUri.isNotBlank()) {
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(audioUri))
                    prepareAsync()
                    setOnPreparedListener { player ->
                        if (player.duration > 0) {
                            totalDurationMs = player.duration
                        }
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPositionMs = 0
                    }
                    setOnErrorListener { _, _, _ ->
                        isPlaying = false
                        true
                    }
                }
                mediaPlayerRef = mp
            } catch (_: Exception) {
            }
        }
        onDispose {
            try {
                mediaPlayerRef?.stop()
            } catch (_: Exception) {
            }
            try {
                mediaPlayerRef?.release()
            } catch (_: Exception) {
            }
            mediaPlayerRef = null
        }
    }

    LaunchedEffect(isPlaying, playbackSpeed) {
        while (isActive && isPlaying) {
            val mp = mediaPlayerRef
            if (mp != null && mp.isPlaying) {
                currentPositionMs = mp.currentPosition
                if (mp.duration > 0) totalDurationMs = mp.duration
            }
            delay(150L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "audio_waveform")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveform_phase"
    )

    val progressFraction = if (totalDurationMs > 0) {
        (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("inbuilt_audio_player"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            try {
                                val mp = mediaPlayerRef
                                if (mp != null) {
                                    if (isPlaying) {
                                        mp.pause()
                                        isPlaying = false
                                    } else {
                                        mp.start()
                                        isPlaying = true
                                    }
                                }
                            } catch (_: Exception) {
                                isPlaying = false
                            }
                        }
                        .testTag("audio_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title?.takeIf { it.isNotBlank() } ?: "Anonymous Voice Note",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    playbackSpeed = when (playbackSpeed) {
                                        1.0f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 1.0f
                                    }
                                    try {
                                        val mp = mediaPlayerRef
                                        if (mp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                                        }
                                    } catch (_: Exception) {
                                    }
                                }
                                .testTag("audio_speed_button"),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Speed,
                                    contentDescription = "Playback speed",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${playbackSpeed}x",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val inactiveBarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    ) {
                        val barCount = 28
                        val spacing = 4.dp.toPx()
                        val totalSpacing = spacing * (barCount - 1)
                        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(3f)

                        for (i in 0 until barCount) {
                            val barFraction = i.toFloat() / barCount.toFloat()
                            val baseHeightRatio = 0.30f + 0.55f * kotlin.math.abs(sin(i * 0.65f))
                            val dynamicRatio = if (isPlaying) {
                                (baseHeightRatio + 0.25f * sin(animPhase + i * 0.45f)).coerceIn(0.2f, 1.0f)
                            } else {
                                baseHeightRatio
                            }
                            val barH = size.height * dynamicRatio
                            val topY = (size.height - barH) / 2f
                            val leftX = i * (barWidth + spacing)

                            drawRoundRect(
                                color = if (barFraction <= progressFraction) primaryColor else inactiveBarColor,
                                topLeft = Offset(leftX, topY),
                                size = Size(barWidth, barH),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }
            }

            Slider(
                value = progressFraction,
                onValueChange = { fraction ->
                    val targetMs = (fraction * totalDurationMs).toInt()
                    currentPositionMs = targetMs
                    try {
                        mediaPlayerRef?.seekTo(targetMs)
                    } catch (_: Exception) {
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                ),
                modifier = Modifier.height(22.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMilliseconds(currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMilliseconds(totalDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatMilliseconds(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}
