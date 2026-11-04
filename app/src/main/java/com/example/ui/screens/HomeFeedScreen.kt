package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ConfessionPost
import com.example.ui.FeedSegment
import com.example.ui.UiState
import com.example.ui.components.AnonymousHatAvatar
import com.example.ui.components.ExplorePresetArtwork
import com.example.ui.components.InbuiltAudioPlayer
import com.example.ui.components.InbuiltVideoPlayer
import com.example.ui.components.MediaSaveHelper
import com.example.ui.theme.AnonHeartRed
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Screen 2 — Home / Confession Feed
 * Real-time synchronization for likes, comments, categories, video aspect ratio preservation,
 * follow/unfollow toggle, and Save to Device option in the 3-dot overflow menu.
 */
@Composable
fun HomeFeedScreen(
    feedState: UiState<List<ConfessionPost>>,
    selectedSegment: FeedSegment,
    selectedCategory: String?,
    availableCategories: List<String>,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    followedUids: Set<String>,
    unreadNotificationCount: Int,
    currentUserId: String,
    onSelectSegment: (FeedSegment) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCreateConfession: () -> Unit,
    onOpenPostDetail: (ConfessionPost) -> Unit,
    onToggleLike: (ConfessionPost) -> Unit,
    onToggleSave: (ConfessionPost) -> Unit,
    onToggleFollow: (authorUid: String, authorHandle: String) -> Unit,
    onSendMessageRequest: (recipientUid: String, recipientHandle: String, messageText: String) -> Unit,
    onHidePost: (String) -> Unit,
    onBlockAuthor: (ConfessionPost) -> Unit,
    onReportPost: (ConfessionPost) -> Unit,
    onDeleteOwnPost: (String) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_feed_list"),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 12.dp,
            bottom = 104.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Compact Header: Anonymousnet wordmark on left; notification bell on right
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Anonymousnet",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("feed_notifications_button")
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(25.dp)
                        )
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 1.dp, y = (-1).dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(AnonHeartRed)
                            )
                        }
                    }
                }
            }
        }

        // 2. Segmented Feed Tabs: For You, Popular, Following
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedSegment.entries.forEach { segment ->
                    val isSelected = selectedSegment == segment
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onSelectSegment(segment) }
                            .testTag("feed_tab_${segment.name.lowercase()}"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = segment.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Category Filter Chips (Category-Based Browsing)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val allSelected = selectedCategory == null
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectCategory(null) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (allSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "All Topics",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (allSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }

                availableCategories.forEach { cat ->
                    val isCatSelected = selectedCategory.equals(cat, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectCategory(cat) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCatSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // 4. Featured CTA Banner Card: "Share anything anonymously..." + "+ Post Confession"
        item {
            FeaturedConfessionCtaCard(
                onPostConfessionClick = onOpenCreateConfession
            )
        }

        // 5. Stream of Confession Post Cards
        when (feedState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = feedState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            is UiState.Success -> {
                if (feedState.data.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (selectedCategory != null) "No confessions in category '$selectedCategory'" else "No confessions in this view yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Be the first to share an anonymous thought with the community.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onOpenCreateConfession,
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text("+ Post Confession")
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = feedState.data,
                        key = { it.id }
                    ) { post ->
                        ConfessionPostCard(
                            post = post,
                            isLiked = post.id in likedPostIds,
                            isSaved = post.id in savedPostIds,
                            isFollowingAuthor = post.authorUid in followedUids,
                            isOwnPost = post.authorUid == currentUserId,
                            onCardClick = { onOpenPostDetail(post) },
                            onLikeClick = { onToggleLike(post) },
                            onCommentClick = { onOpenPostDetail(post) },
                            onToggleFollow = { onToggleFollow(post.authorUid, post.anonymousHandle) },
                            onSendMessageRequest = { msg -> onSendMessageRequest(post.authorUid, post.anonymousHandle, msg) },
                            onSaveClick = { onToggleSave(post) },
                            onHideClick = { onHidePost(post.id) },
                            onBlockAuthorClick = { onBlockAuthor(post) },
                            onReportClick = { onReportPost(post) },
                            onDeleteClick = { onDeleteOwnPost(post.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedConfessionCtaCard(
    onPostConfessionClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = primaryColor.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 500f)
                )
            )
            .padding(20.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.width * 0.32f,
                center = Offset(size.width * 0.85f, size.height * 0.25f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.width * 0.24f,
                center = Offset(size.width * 0.12f, size.height * 0.90f)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Share anything\nanonymously...",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            lineHeight = 25.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No judgement, Just vibes.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = Color.White.copy(alpha = 0.86f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .offset(x = 4.dp, y = (-2).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier
                            .size(44.dp)
                            .rotate(-28f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onPostConfessionClick)
                    .testTag("featured_post_confession_button"),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Post Confession",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF1E1B3A)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfessionPostCard(
    post: ConfessionPost,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowingAuthor: Boolean = false,
    isOwnPost: Boolean,
    onCardClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onToggleFollow: () -> Unit = {},
    onSendMessageRequest: (String) -> Unit = {},
    onSaveClick: () -> Unit,
    onHideClick: () -> Unit,
    onBlockAuthorClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var showMessageRequestDialog by remember { mutableStateOf(false) }
    var dmMessageText by remember { mutableStateOf("") }
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onCardClick)
            .testTag("post_card_${post.id}"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Top Row: Avatar + Anonymous identity + Category chip + Follow + Overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnonymousHatAvatar(size = 42.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.anonymousLabel.ifEmpty { "Anonymous" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Category Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = primaryColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = post.category.ifBlank { "General" },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "${post.anonymousHandle} • ${formatRelativeTimestamp(post.createdAt, post.id)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isOwnPost) {
                    // Follow / Unfollow Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleFollow)
                            .testTag("follow_author_${post.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFollowingAuthor) MaterialTheme.colorScheme.surfaceVariant else primaryColor.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isFollowingAuthor) Icons.Filled.PersonRemove else Icons.Filled.PersonAdd,
                                contentDescription = if (isFollowingAuthor) "Unfollow" else "Follow",
                                tint = if (isFollowingAuthor) MaterialTheme.colorScheme.onSurface else primaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFollowingAuthor) "Following" else "Follow",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isFollowingAuthor) MaterialTheme.colorScheme.onSurface else primaryColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("post_overflow_${post.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "Post Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        // Save to Device Option (Preserves original quality video, audio, image)
                        if (!post.mediaUri.isNullOrBlank() || post.type == "video" || post.type == "audio") {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save to Device (Original Quality)")
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    scope.launch {
                                        MediaSaveHelper.saveMediaToDevice(
                                            context = context,
                                            mediaUriString = post.mediaUri ?: "preset://${post.type}",
                                            mimeType = post.mediaMimeType,
                                            type = post.type,
                                            caption = post.mediaCaption ?: post.text
                                        )
                                    }
                                }
                            )
                        }

                        if (!isOwnPost) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Send Private Message Request")
                                    }
                                },
                                onClick = {
                                    menuExpanded = false
                                    showMessageRequestDialog = true
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text("Report Confession") },
                            onClick = {
                                menuExpanded = false
                                onReportClick()
                            }
                        )
                        if (!isOwnPost) {
                            DropdownMenuItem(
                                text = { Text("Block Anonymous Author") },
                                onClick = {
                                    menuExpanded = false
                                    onBlockAuthorClick()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Hide Post") },
                            onClick = {
                                menuExpanded = false
                                onHideClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Link") },
                            onClick = {
                                menuExpanded = false
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Anonymous confession on Anonymousnet:\n\"${post.text}\"\nhttps://anonymousnet.app/post/${post.id}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Confession"))
                            }
                        )
                        if (isOwnPost) {
                            DropdownMenuItem(
                                text = { Text("Delete My Confession", color = AnonHeartRed) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confession Body Text
            Text(
                text = post.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // In-built Video Player (preserves original aspect ratio), Audio Player, or Image Viewer
            if (!post.mediaUri.isNullOrBlank() || post.type == "video" || post.type == "audio") {
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    post.type.equals("video", ignoreCase = true) ||
                        post.mediaMimeType?.startsWith("video") == true -> {
                        InbuiltVideoPlayer(
                            videoUri = post.mediaUri ?: "preset://dream_silhouette",
                            caption = post.mediaCaption ?: post.text,
                            durationSecHint = post.mediaDurationSec ?: 42L
                        )
                    }
                    post.type.equals("audio", ignoreCase = true) ||
                        post.mediaMimeType?.startsWith("audio") == true -> {
                        InbuiltAudioPlayer(
                            audioUri = post.mediaUri ?: "preset://birds_sky",
                            title = post.mediaCaption ?: "Anonymous Voice Confession",
                            durationSecHint = post.mediaDurationSec ?: 28L
                        )
                    }
                    !post.mediaUri.isNullOrBlank() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(18.dp))
                        ) {
                            if (post.mediaUri.startsWith("preset://")) {
                                ExplorePresetArtwork(
                                    presetUri = post.mediaUri,
                                    postType = post.type,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = post.mediaUri,
                                    contentDescription = post.mediaCaption ?: "Confession attachment",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Engagement Bar: Likes, Comments, Share, Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button + count
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onLikeClick)
                            .testTag("post_like_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) AnonHeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = post.likeCount.toString(),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Comment button + count
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onCommentClick)
                            .testTag("post_comment_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = post.commentCount.toString(),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Anonymous confession:\n\"${post.text}\"\nhttps://anonymousnet.app/post/${post.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Confession"))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Bookmark / Save
                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("post_save_${post.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Remove from Saved" else "Save Post",
                        tint = if (isSaved) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }

    // Direct Message Request Prompt Dialog
    if (showMessageRequestDialog) {
        AlertDialog(
            onDismissRequest = { showMessageRequestDialog = false },
            title = { Text("Send Message Request", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Send a private direct message request to ${post.anonymousHandle}. The recipient must explicitly accept before real-time chat begins.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = dmMessageText,
                        onValueChange = { if (it.length <= 500) dmMessageText = it },
                        placeholder = { Text("Write your message...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dmMessageText.isNotBlank()) {
                            onSendMessageRequest(dmMessageText)
                            showMessageRequestDialog = false
                            dmMessageText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Send Request")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMessageRequestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatRelativeTimestamp(timestamp: Timestamp?, fallbackId: String): String {
    if (timestamp == null) {
        return when {
            fallbackId.contains("2h") -> "2h ago"
            fallbackId.contains("3h") -> "3h ago"
            fallbackId.contains("explore_1") -> "4h ago"
            fallbackId.contains("explore_2") -> "5h ago"
            fallbackId.contains("explore_3") -> "6h ago"
            fallbackId.contains("explore_4") -> "8h ago"
            else -> "Just now"
        }
    }
    val diffMs = System.currentTimeMillis() - timestamp.toDate().time
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
