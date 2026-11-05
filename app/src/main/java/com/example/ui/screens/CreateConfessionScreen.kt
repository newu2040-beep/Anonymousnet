package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Description
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.ConfessionPost
import com.example.ui.DraftComposerState
import com.example.ui.components.AnonymousHatAvatar
import com.example.ui.components.CreateConfessionHeroIcon
import com.example.ui.components.InbuiltAudioPlayer
import com.example.ui.components.InbuiltVideoPlayer
import com.example.ui.components.VoiceConfessionRecorder
import kotlinx.coroutines.delay

/**
 * Screen 3 — Create Confession (Anonymousnet v3.0)
 * - Unlimited words & characters (no word limits)
 * - Category Selector (18+ categories + custom category input)
 * - Photo Picker, Video Picker, Voice Recorder, and File Picker
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
    onSetAudioRecording: (isRecording: Boolean, seconds: Int) -> Unit,
    onPublish: (asDraft: Boolean) -> Unit,
    onLoadDraft: (ConfessionPost) -> Unit
) {
    val context = LocalContext.current
    var showDraftsDialog by remember { mutableStateOf(false) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryInput by remember { mutableStateOf("") }

    val voiceRecorder = remember(context) { VoiceConfessionRecorder(context) }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.stopRecording()
        }
    }

    // 1. Android Photo Picker for Images
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            onSelectType("image")
            onAttachMedia(
                uri.toString(),
                "Original Photo Moment",
                null,
                context.contentResolver.getType(uri) ?: "image/jpeg"
            )
        }
    }

    // 2. Android Photo Picker for Videos
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            onSelectType("video")
            onAttachMedia(
                uri.toString(),
                "Original Video Confession",
                null,
                context.contentResolver.getType(uri) ?: "video/mp4"
            )
        }
    }

    // 3. System Document Picker for Audio or Other files
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val detectedType = when {
                mime.startsWith("audio") -> "audio"
                mime.startsWith("video") -> "video"
                mime.startsWith("image") -> "image"
                else -> "other"
            }
            onSelectType(detectedType)
            onAttachMedia(
                uri.toString(),
                uri.lastPathSegment ?: "Attached Confession Media",
                if (detectedType == "audio") 30L else null,
                mime
            )
        }
    }

    // 4. Runtime Microphone Permission for Voice Recording
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecorder.startRecording()
            onSelectType("audio")
            onSetAudioRecording(true, 0)
        }
    }

    LaunchedEffect(composerState.isRecordingAudio) {
        if (composerState.isRecordingAudio) {
            var secs = composerState.recordedSeconds
            while (composerState.isRecordingAudio) {
                delay(1000L)
                secs += 1
                onSetAudioRecording(true, secs)
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val wordCount = remember(composerState.text) {
        val trimmed = composerState.text.trim()
        if (trimmed.isEmpty()) 0 else trimmed.split("\\s+".toRegex()).size
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .align(Alignment.TopEnd)
        ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val safeRadius = (size.width * 0.55f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.28f),
                        primaryColor.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.22f),
                    radius = safeRadius
                ),
                radius = safeRadius,
                center = Offset(size.width * 0.88f, size.height * 0.22f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 114.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(42.dp)
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

                Text(
                    text = "Create Confession",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (drafts.isNotEmpty()) {
                    TextButton(
                        onClick = { showDraftsDialog = true },
                        modifier = Modifier.testTag("open_drafts_button")
                    ) {
                        Text(
                            text = "Drafts (${drafts.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryColor
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(42.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Hero emblem
            CreateConfessionHeroIcon()

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Category Selector Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Category",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { showCustomCategoryDialog = true }) {
                        Text("+ Custom Category", style = MaterialTheme.typography.labelSmall, color = primaryColor)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableCategories.forEach { cat ->
                        val isSelected = composerState.selectedCategory.equals(cat, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectCategory(cat) }
                                .testTag("composer_cat_${cat.lowercase()}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Main Composer Card (Unlimited words & characters)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    ),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        AnonymousHatAvatar(size = 38.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = composerState.text,
                            onValueChange = onUpdateText,
                            placeholder = {
                                Text(
                                    text = "Share your confession freely with no word limits...",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .testTag("confession_text_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 23.sp
                            )
                        )
                    }

                    // Live Word & Character Count (No Word Limits)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Category: ${composerState.selectedCategory}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "$wordCount words • ${composerState.text.length} chars (No limit)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("character_counter_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4-Tile Media Attachment Selector Row: Image, Video, Audio, Other
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MediaAttachmentTile(
                            label = "Image",
                            icon = Icons.Filled.Image,
                            selected = composerState.selectedType == "image",
                            modifier = Modifier.weight(1f),
                            testTag = "media_tile_image",
                            onClick = {
                                onSelectType("image")
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        MediaAttachmentTile(
                            label = "Video",
                            icon = Icons.Filled.Videocam,
                            selected = composerState.selectedType == "video",
                            modifier = Modifier.weight(1f),
                            testTag = "media_tile_video",
                            onClick = {
                                onSelectType("video")
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        )
                        MediaAttachmentTile(
                            label = "Audio",
                            icon = Icons.Filled.Mic,
                            selected = composerState.selectedType == "audio",
                            modifier = Modifier.weight(1f),
                            testTag = "media_tile_audio",
                            onClick = {
                                onSelectType("audio")
                                val hasMicPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasMicPerm) {
                                    if (!composerState.isRecordingAudio) {
                                        val recordedUri = voiceRecorder.startRecording()
                                        if (recordedUri != null) {
                                            onSetAudioRecording(true, 0)
                                        }
                                    } else {
                                        val savedUri = voiceRecorder.stopRecording()
                                        val duration = composerState.recordedSeconds.coerceAtLeast(1).toLong()
                                        onSetAudioRecording(false, 0)
                                        if (savedUri != null) {
                                            onAttachMedia(
                                                savedUri,
                                                "Recorded Voice Confession (${duration}s)",
                                                duration,
                                                "audio/mp4"
                                            )
                                        }
                                    }
                                } else {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        )
                        MediaAttachmentTile(
                            label = "Other",
                            icon = Icons.Filled.AttachFile,
                            selected = composerState.selectedType == "other",
                            modifier = Modifier.weight(1f),
                            testTag = "media_tile_other",
                            onClick = {
                                onSelectType("other")
                                documentPickerLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }

                    // Active Voice Recording Banner
                    if (composerState.isRecordingAudio) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = primaryColor.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Mic,
                                        contentDescription = "Recording",
                                        tint = primaryColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Recording Voice (${composerState.recordedSeconds}s)...",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = primaryColor
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        val savedUri = voiceRecorder.stopRecording()
                                        val dur = composerState.recordedSeconds.coerceAtLeast(1).toLong()
                                        onSetAudioRecording(false, 0)
                                        if (savedUri != null) {
                                            onAttachMedia(
                                                savedUri,
                                                "Recorded Voice Confession (${dur}s)",
                                                dur,
                                                "audio/mp4"
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Stop,
                                        contentDescription = "Stop Recording",
                                        tint = primaryColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Finish", color = primaryColor)
                                }
                            }
                        }
                    }

                    // Attached Media Preview
                    if (composerState.mediaUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            when (composerState.selectedType) {
                                "video" -> {
                                    InbuiltVideoPlayer(
                                        videoUri = composerState.mediaUri,
                                        caption = composerState.mediaCaption,
                                        durationSecHint = composerState.mediaDurationSec ?: 24L
                                    )
                                }
                                "audio" -> {
                                    InbuiltAudioPlayer(
                                        audioUri = composerState.mediaUri,
                                        title = composerState.mediaCaption,
                                        durationSecHint = composerState.mediaDurationSec ?: 15L
                                    )
                                }
                                "image" -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    ) {
                                        AsyncImage(
                                            model = composerState.mediaUri,
                                            contentDescription = "Selected photo",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Description,
                                                contentDescription = null,
                                                tint = primaryColor
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = composerState.mediaCaption.ifEmpty { "Attached File" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearMedia),
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove attachment",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. "Keep it anonymous" Card with Toggle Switch
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnonymousHatAvatar(size = 36.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Keep it anonymous",
                            style = MaterialTheme.typography.titleMedium.copy(
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

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Primary CTA: "Post Anonymously"
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
                            colors = listOf(secondaryColor, primaryColor)
                        )
                    )
                    .clickable(enabled = !composerState.isPublishing) {
                        onPublish(false)
                    }
                    .testTag("post_anonymously_submit_button"),
                contentAlignment = Alignment.Center
            ) {
                if (composerState.isPublishing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Publishing...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                } else {
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

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Action: Save as Private Draft
            OutlinedButton(
                onClick = { onPublish(true) },
                enabled = !composerState.isPublishing && composerState.text.isNotBlank(),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("save_draft_button")
            ) {
                Text(
                    text = "Save Private Draft",
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryColor
                )
            }
        }
    }

    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            title = { Text("Custom Category", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = customCategoryInput,
                    onValueChange = { if (it.length <= 30) customCategoryInput = it },
                    placeholder = { Text("e.g., Late Night, College, Music") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customCategoryInput.isNotBlank()) {
                            onSelectCategory(customCategoryInput.trim())
                            customCategoryInput = ""
                            showCustomCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDraftsDialog) {
        AlertDialog(
            onDismissRequest = { showDraftsDialog = false },
            title = { Text("Saved Private Drafts", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    drafts.forEach { draft ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onLoadDraft(draft)
                                    showDraftsDialog = false
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = draft.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Category: ${draft.category} • Tap to load",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primaryColor
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDraftsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MediaAttachmentTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = if (selected) {
        primaryColor.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.8.dp,
                        color = primaryColor,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
