package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ConfessionPost
import com.example.ui.DraftComposerState
import com.example.ui.components.CreateConfessionHeroIcon
import com.example.ui.components.InbuiltAudioPlayer
import com.example.ui.components.InbuiltVideoPlayer
import com.example.ui.components.VoiceConfessionRecorder
import kotlinx.coroutines.delay

/**
 * Screen 3 — Create Confession
 * Supports Gallery Photo/Video Picker, Full Audio/Video/Document File Picker with persistent read URI access,
 * real VoiceConfessionRecorder (.m4a), and live InbuiltVideoPlayer / InbuiltAudioPlayer preview before publishing!
 */
@Composable
fun CreateConfessionScreen(
    composerState: DraftComposerState,
    drafts: List<ConfessionPost>,
    onBackClick: () -> Unit,
    onUpdateText: (String) -> Unit,
    onSelectType: (String) -> Unit,
    onUpdateKeepAnonymous: (Boolean) -> Unit,
    onAttachMedia: (uri: String?, caption: String, durationSec: Long?, mimeType: String?) -> Unit,
    onUpdateTopics: (String) -> Unit,
    onClearMedia: () -> Unit,
    onSetAudioRecording: (Boolean, Int) -> Unit,
    onPublish: (asDraft: Boolean) -> Unit,
    onLoadDraft: (ConfessionPost) -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val voiceRecorder = remember(context) { VoiceConfessionRecorder(context) }
    var recordedUri by remember { mutableStateOf<String?>(null) }

    var showDraftsAndOptionsDialog by remember { mutableStateOf(false) }
    var showAudioRecorderModal by remember { mutableStateOf(false) }
    var showOtherMetadataDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.stopRecording()
        }
    }

    // Gallery Photo Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            onAttachMedia(uri.toString(), "Gallery Photo", null, "image/jpeg")
        }
    }

    // Gallery Video Picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            onAttachMedia(uri.toString(), "Gallery Video Moment", 45L, "video/mp4")
        }
    }

    // Full Files / Audio / Video / Document Picker (OpenDocument with persistable URI access)
    val openAnyFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val detectedMime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            when {
                detectedMime.startsWith("video") -> {
                    onSelectType("video")
                    onAttachMedia(uri.toString(), "Video File", 60L, detectedMime)
                }
                detectedMime.startsWith("audio") -> {
                    onSelectType("audio")
                    onAttachMedia(uri.toString(), "Audio File", 45L, detectedMime)
                }
                detectedMime.startsWith("image") -> {
                    onSelectType("image")
                    onAttachMedia(uri.toString(), "Image File", null, detectedMime)
                }
                else -> {
                    onSelectType("other")
                    onAttachMedia(uri.toString(), "Attached File ($detectedMime)", null, detectedMime)
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        showAudioRecorderModal = true
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    // Live timer while recording audio confession
    LaunchedEffect(composerState.isRecordingAudio) {
        if (composerState.isRecordingAudio) {
            var secs = composerState.recordedSeconds
            while (composerState.isRecordingAudio && secs < 300) {
                delay(1000L)
                secs += 1
                onSetAudioRecording(true, secs)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Row: Circular Back `<` at left, Circular Settings/Drafts Gear at right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.06f))
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick)
                    .testTag("create_back_button"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.06f))
                    .clip(CircleShape)
                    .clickable { showDraftsAndOptionsDialog = true }
                    .testTag("create_settings_draft_button"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Drafts and Composer Options",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Hero Illustration: Tilted Violet Document + Padlock
        CreateConfessionHeroIcon()

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Heading & Subtitle
        Text(
            text = "Create Confession",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Share anything anonymously",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Content Type Selector Row: Image, Video, Audio, Other
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ContentTypeTile(
                label = "Image",
                icon = Icons.Outlined.Image,
                selected = composerState.selectedType == "image",
                onClick = {
                    onSelectType("image")
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("type_tile_image")
            )
            ContentTypeTile(
                label = "Video",
                icon = Icons.Outlined.PlayArrow,
                selected = composerState.selectedType == "video",
                onClick = {
                    onSelectType("video")
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("type_tile_video")
            )
            ContentTypeTile(
                label = "Audio",
                icon = Icons.Outlined.MicNone,
                selected = composerState.selectedType == "audio",
                onClick = {
                    onSelectType("audio")
                    val hasAudioPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasAudioPerm) {
                        showAudioRecorderModal = true
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("type_tile_audio")
            )
            ContentTypeTile(
                label = "Other",
                icon = Icons.Filled.MoreHoriz,
                selected = composerState.selectedType == "other",
                onClick = {
                    onSelectType("other")
                    showOtherMetadataDialog = true
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("type_tile_other")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick File & Gallery Access Row (Browse any Audio, Video, Photo, or Document file)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    openAnyFileLauncher.launch(arrayOf("video/*", "audio/*", "image/*", "application/pdf"))
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("browse_any_media_file_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browse Files / Audio / Video", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    val perms = mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    multiPermissionLauncher.launch(perms.toTypedArray())
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("composer_grant_permissions_button")
            ) {
                Text("Allow All Access", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Large Rounded Multi-line Confession Editor with Live Counter (0/500)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.06f)
                ),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                BasicTextField(
                    value = composerState.text,
                    onValueChange = onUpdateText,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(primaryColor),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp)
                        .testTag("confession_text_input"),
                    decorationBox = { innerTextField ->
                        if (composerState.text.isEmpty()) {
                            Text(
                                text = "Your confession...",
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        innerTextField()
                    }
                )

                Text(
                    text = "${composerState.text.length}/500",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }

        // Attached Media Live Preview with In-Built Video / Audio Player
        if (composerState.mediaUri != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (composerState.selectedType == "audio") Icons.Filled.GraphicEq else Icons.Outlined.Image,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = composerState.mediaCaption.ifEmpty { "Media attached (${composerState.selectedType})" },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove attachment",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable { onClearMedia() }
                        )
                    }

                    // Live Inbuilt Player Preview inside Create Confession
                    if (composerState.selectedType == "video") {
                        InbuiltVideoPlayer(
                            videoUri = composerState.mediaUri,
                            caption = composerState.mediaCaption,
                            durationSecHint = composerState.mediaDurationSec ?: 45L
                        )
                    } else if (composerState.selectedType == "audio") {
                        InbuiltAudioPlayer(
                            audioUri = composerState.mediaUri,
                            title = composerState.mediaCaption.ifEmpty { "Recorded Voice Confession" },
                            durationSecHint = composerState.mediaDurationSec ?: 20L
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. "Keep it anonymous" Toggle Row matching Screen 3 reference
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = "Anonymous Mask",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Keep it anonymous",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = composerState.keepAnonymous,
                    onCheckedChange = onUpdateKeepAnonymous,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = primaryColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("keep_anonymous_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 7. Vivid Pill Publish Button: Paper Airplane + "Post Anonymously"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = primaryColor.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(primaryColor, MaterialTheme.colorScheme.secondary)
                    )
                )
                .clickable(enabled = !composerState.isPublishing) {
                    onPublish(false)
                }
                .testTag("post_anonymously_button"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (composerState.isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Publishing...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(-25f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (composerState.keepAnonymous) "Post Anonymously" else "Post Confession",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }

    // Audio Recording & Audio File Picker Modal
    if (showAudioRecorderModal) {
        AlertDialog(
            onDismissRequest = {
                if (composerState.isRecordingAudio) {
                    recordedUri = voiceRecorder.stopRecording()
                    onSetAudioRecording(false, composerState.recordedSeconds)
                }
                showAudioRecorderModal = false
            },
            title = {
                Text("Record or Pick Audio Confession", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (composerState.isRecordingAudio) {
                            "Recording... 00:${String.format("%02d", composerState.recordedSeconds)}"
                        } else if (composerState.recordedSeconds > 0) {
                            "Recorded 00:${String.format("%02d", composerState.recordedSeconds)} • Ready to preview & attach"
                        } else {
                            "Record a live voice confession or choose any audio file (.mp3, .m4a, .wav) from your device."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (composerState.isRecordingAudio) {
                                    recordedUri = voiceRecorder.stopRecording()
                                    onSetAudioRecording(false, composerState.recordedSeconds)
                                } else {
                                    recordedUri = voiceRecorder.startRecording()
                                    onSetAudioRecording(true, 0)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (composerState.isRecordingAudio) Color(0xFFEF4444) else primaryColor
                            )
                        ) {
                            Icon(
                                imageVector = if (composerState.isRecordingAudio) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (composerState.isRecordingAudio) "Stop" else "Record Mic")
                        }

                        OutlinedButton(
                            onClick = {
                                showAudioRecorderModal = false
                                openAnyFileLauncher.launch(arrayOf("audio/*"))
                            }
                        ) {
                            Text("Pick Audio File")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUri = if (composerState.isRecordingAudio) {
                            voiceRecorder.stopRecording()
                        } else {
                            recordedUri ?: "preset://birds_sky"
                        }
                        val duration = composerState.recordedSeconds.coerceAtLeast(12).toLong()
                        onSetAudioRecording(false, duration.toInt())
                        onAttachMedia(
                            finalUri,
                            "Voice Confession (${duration}s)",
                            duration,
                            "audio/mp4"
                        )
                        showAudioRecorderModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Attach Audio")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        voiceRecorder.stopRecording()
                        onSetAudioRecording(false, 0)
                        showAudioRecorderModal = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Other / Preset Media & Topic Selector Modal
    if (showOtherMetadataDialog) {
        var topicsInput by remember { mutableStateOf(composerState.topics) }
        AlertDialog(
            onDismissRequest = { showOtherMetadataDialog = false },
            title = { Text("Topics, Presets & Files", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = topicsInput,
                        onValueChange = { topicsInput = it },
                        label = { Text("Topic tags (comma-separated)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Attach a sample video/audio scene or browse device files:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSelectType("video")
                                onAttachMedia("preset://dream_silhouette", "Dream Big Video ✨", 42L, "video/mp4")
                                showOtherMetadataDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sample Video", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                onSelectType("audio")
                                onAttachMedia("preset://birds_sky", "Calm Breeze Audio ❤️", 28L, "audio/mp4")
                                showOtherMetadataDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sample Audio", fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = {
                            showOtherMetadataDialog = false
                            openAnyFileLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Full File & Media Picker")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateTopics(topicsInput)
                        showOtherMetadataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtherMetadataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Drafts & Save Draft Dialog
    if (showDraftsAndOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showDraftsAndOptionsDialog = false },
            title = { Text("Drafts & Options", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Drafts are private to your account and never visible in public feeds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            showDraftsAndOptionsDialog = false
                            onPublish(true)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Text("Save Current Confession as Draft")
                    }
                    if (drafts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Your Saved Drafts:", style = MaterialTheme.typography.titleMedium)
                        drafts.take(3).forEach { draft ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showDraftsAndOptionsDialog = false
                                        onLoadDraft(draft)
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = draft.text.take(60),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDraftsAndOptionsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun ContentTypeTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .height(74.dp)
            .shadow(
                elevation = if (selected) 8.dp else 4.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = if (selected) primaryColor.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, primaryColor, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
