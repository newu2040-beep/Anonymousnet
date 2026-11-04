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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material3.IconButton
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
 * Supports Customizable Category System (select existing or type custom category),
 * uncompressed original quality media attachment, Gallery Photo/Video Picker,
 * and high-bitrate voice note recorder.
 */
@Composable
fun CreateConfessionScreen(
    composerState: DraftComposerState,
    drafts: List<ConfessionPost>,
    availableCategories: List<String>,
    onBackClick: () -> Unit,
    onUpdateText: (String) -> Unit,
    onSelectType: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
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
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryInput by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.stopRecording()
        }
    }

    // Gallery Photo Picker (preserves original quality)
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
            onAttachMedia(uri.toString(), "Original Quality Photo", null, "image/jpeg")
        }
    }

    // Gallery Video Picker (preserves original resolution and bitrate)
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
            onAttachMedia(uri.toString(), "Original Quality Video", 45L, "video/mp4")
        }
    }

    // Document & Audio File Picker (preserves original audio/video files)
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
                    onAttachMedia(uri.toString(), "Original Video File", 60L, detectedMime)
                }
                detectedMime.startsWith("audio") -> {
                    onSelectType("audio")
                    onAttachMedia(uri.toString(), "Original Audio File", 45L, detectedMime)
                }
                detectedMime.startsWith("image") -> {
                    onSelectType("image")
                    onAttachMedia(uri.toString(), "Original Image File", null, detectedMime)
                }
                else -> {
                    onSelectType("other")
                    onAttachMedia(uri.toString(), "Original File ($detectedMime)", null, detectedMime)
                }
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        showAudioRecorderModal = true
    }

    // Live timer while recording audio confession
    LaunchedEffect(composerState.isRecordingAudio) {
        if (composerState.isRecordingAudio) {
            var secs = 0
            while (composerState.isRecordingAudio) {
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
            .padding(top = 12.dp, bottom = 108.dp)
            .testTag("create_confession_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Navigation Bar: Back Arrow `<` on left, Settings Gear on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.05f))
                    .clip(CircleShape)
                    .clickable(onClick = onBackClick)
                    .testTag("create_back_button"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back to Feed",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.05f))
                    .clip(CircleShape)
                    .clickable { showDraftsAndOptionsDialog = true }
                    .testTag("create_options_gear"),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Composer Options & Drafts",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Centered Illustration
        CreateConfessionHeroIcon()

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Header Texts
        Text(
            text = "Create Confession",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Share anything anonymously",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 4. Media Type Selector Pills (Image, Video, Audio, Other)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ComposerMediaTypePill(
                label = "Image",
                icon = Icons.Outlined.Image,
                isSelected = composerState.selectedType == "image",
                modifier = Modifier.weight(1f),
                onClick = {
                    onSelectType("image")
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            ComposerMediaTypePill(
                label = "Video",
                icon = Icons.Outlined.PlayArrow,
                isSelected = composerState.selectedType == "video",
                modifier = Modifier.weight(1f),
                onClick = {
                    onSelectType("video")
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )

            ComposerMediaTypePill(
                label = "Audio",
                icon = Icons.Outlined.MicNone,
                isSelected = composerState.selectedType == "audio",
                modifier = Modifier.weight(1f),
                onClick = {
                    onSelectType("audio")
                    val hasMic = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasMic) {
                        showAudioRecorderModal = true
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            )

            ComposerMediaTypePill(
                label = "Other",
                icon = Icons.Filled.MoreHoriz,
                isSelected = composerState.selectedType == "other",
                modifier = Modifier.weight(1f),
                onClick = {
                    openAnyFileLauncher.launch(arrayOf("*/*"))
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Category Selection Row (+ Custom Category button)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category: ${composerState.selectedCategory}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "+ Add Custom",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor,
                    modifier = Modifier.clickable { showCustomCategoryDialog = true }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { cat ->
                    val isCatSelected = composerState.selectedCategory.equals(cat, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectCategory(cat) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCatSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Confession Input Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = Color.Black.copy(alpha = 0.05f)
                ),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    if (composerState.text.isEmpty()) {
                        Text(
                            text = "Your confession...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    }
                    BasicTextField(
                        value = composerState.text,
                        onValueChange = onUpdateText,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("confession_input_field"),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(primaryColor)
                    )
                }

                Text(
                    text = "${composerState.text.length}/500",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        // Live attached media preview
        if (!composerState.mediaUri.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Attached Media (${composerState.selectedType.uppercase()})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = primaryColor
                        )
                        IconButton(onClick = onClearMedia, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear attachment", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (composerState.selectedType) {
                        "video" -> {
                            InbuiltVideoPlayer(
                                videoUri = composerState.mediaUri,
                                caption = composerState.mediaCaption.ifBlank { "Original Video Preview" }
                            )
                        }
                        "audio" -> {
                            InbuiltAudioPlayer(
                                audioUri = composerState.mediaUri,
                                title = composerState.mediaCaption.ifBlank { "Original Audio Voice Note" }
                            )
                        }
                        else -> {
                            Text(
                                text = "Attached: ${composerState.mediaCaption}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Privacy Toggle Card: "Keep it anonymous"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = Color.Black.copy(alpha = 0.04f)
                ),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Keep it anonymous",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
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
                        checkedTrackColor = primaryColor
                    ),
                    modifier = Modifier.testTag("keep_anonymous_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 8. Bottom Action Button: "Post Anonymously"
        Button(
            onClick = { onPublish(false) },
            enabled = !composerState.isPublishing && composerState.text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(27.dp),
                    spotColor = primaryColor.copy(alpha = 0.40f)
                )
                .testTag("publish_confession_button"),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                contentColor = Color.White
            )
        ) {
            if (composerState.isPublishing) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (composerState.keepAnonymous) "Post Anonymously" else "Post Confession",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }

    // Custom Category Input Modal
    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            title = { Text("Create Custom Category", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = customCategoryInput,
                    onValueChange = { if (it.length <= 40) customCategoryInput = it },
                    placeholder = { Text("e.g. Campus Life, Secret Crush, Coding") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = customCategoryInput.trim()
                        if (trimmed.isNotBlank()) {
                            onSelectCategory(trimmed)
                            showCustomCategoryDialog = false
                            customCategoryInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Select Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Audio Voice Note Recording Dialog (High quality original voice capture)
    if (showAudioRecorderModal) {
        AlertDialog(
            onDismissRequest = {
                voiceRecorder.stopRecording()
                onSetAudioRecording(false, 0)
                showAudioRecorderModal = false
            },
            title = { Text("Record Anonymous Voice Note", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Original high-bitrate voice capture (48kHz AAC). Your voice can be played smoothly by anyone on the community feed.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (composerState.isRecordingAudio) MaterialTheme.colorScheme.error else primaryColor)
                            .clickable {
                                if (composerState.isRecordingAudio) {
                                    val uri = voiceRecorder.stopRecording()
                                    onSetAudioRecording(false, 0)
                                    recordedUri = uri
                                } else {
                                    val uri = voiceRecorder.startRecording()
                                    recordedUri = uri
                                    onSetAudioRecording(true, 0)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (composerState.isRecordingAudio) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (composerState.isRecordingAudio) "Stop Recording" else "Start Recording",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    if (composerState.isRecordingAudio) {
                        Text(
                            text = "Recording: ${composerState.recordedSeconds}s",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (recordedUri != null) {
                        Text(
                            text = "Voice note ready to attach!",
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryColor
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileUri = recordedUri ?: voiceRecorder.stopRecording()
                        onSetAudioRecording(false, 0)
                        if (fileUri != null) {
                            onAttachMedia(fileUri, "Anonymous Voice Note", 30L, "audio/mp4")
                        }
                        showAudioRecorderModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Attach Voice Note")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    voiceRecorder.stopRecording()
                    onSetAudioRecording(false, 0)
                    showAudioRecorderModal = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDraftsAndOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showDraftsAndOptionsDialog = false },
            title = { Text("Drafts & Options", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onPublish(true)
                            showDraftsAndOptionsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Current Confession as Draft")
                    }

                    if (drafts.isNotEmpty()) {
                        Text("Your Saved Drafts (${drafts.size}):", style = MaterialTheme.typography.labelLarge)
                        drafts.forEach { draft ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onLoadDraft(draft)
                                        showDraftsAndOptionsDialog = false
                                    },
                                shape = RoundedCornerShape(12.dp),
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
private fun ComposerMediaTypePill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .height(50.dp)
            .shadow(
                elevation = if (isSelected) 6.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isSelected) primaryColor.copy(alpha = 0.35f) else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("composer_pill_${label.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else primaryColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
