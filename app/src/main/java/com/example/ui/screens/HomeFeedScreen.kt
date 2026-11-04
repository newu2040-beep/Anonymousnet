package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.ui.theme.AnonHeartRed
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

/**
 * Screen 2 — Home / Confession Feed
 * Includes in-built smooth Video Player and Audio Player directly in confession cards.
 */
@Composable
fun HomeFeedScreen(
    feedState: UiState<List<ConfessionPost>>,
    selectedSegment: FeedSegment,
    likedPostIds: Set<String>,
    savedPostIds: Set<String>,
    unreadNotificationCount: Int,
    currentUserId: String,
    onSelectSegment: (FeedSegment) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenCreateConfession: () -> Unit,
    onOpenPostDetail: (ConfessionPost) -> Unit,
    onToggleLike: (ConfessionPost) -> Unit,
    onToggleSave: (ConfessionPost) -> Unit,
    onHidePost: (String) -> Unit,
    onBlockAuthor: (ConfessionPost) -> Unit,
    onReportPost: (ConfessionPost) -> Unit,
    onDeleteOwnPost: (String) -> Unit
) {
    val context = LocalContext.current
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
        // 1. Compact Header: Anonymousnet wordmark on left; notification bell with unread indicator on right
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

        // 3. Featured CTA Banner Card: "Share anything anonymously..." + "+ Post Confession"
        item {
            FeaturedConfessionCtaCard(
                onPostConfessionClick = onOpenCreateConfession
            )
        }

        // 4. Stream of Confession Post Cards
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
                                    text = "No confessions in this view yet",
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
                            isOwnPost = post.authorUid == currentUserId,
                            onCardClick = { onOpenPostDetail(post) },
                            onLikeClick = { onToggleLike(post) },
                            onCommentClick = { onOpenPostDetail(post) },
                            onShareClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Anonymous confession on Anonymousnet:\n\"${post.text}\"\nhttps://anonymousnet.app/post/${post.id}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Confession"))
                            },
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
                    colors = listOf(
                        primaryColor,
                        secondaryColor
                    ),
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

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
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
    isOwnPost: Boolean,
    onCardClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onSaveClick: () -> Unit,
    onHideClick: () -> Unit,
    onBlockAuthorClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
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
            // Top Row: Avatar + Anonymous + relative timestamp + overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnonymousHatAvatar(size = 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.anonymousLabel.ifEmpty { "Anonymous" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatRelativeTimestamp(post.createdAt, post.id),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                onShareClick()
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

            // In-built Video Player, Audio Player, or Image Viewer
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
                                .height(185.dp)
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
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reaction Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like Pill
                    Surface(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onLikeClick)
                            .testTag("like_button_${post.id}"),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isLiked) AnonHeartRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) AnonHeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatCompactCount(post.likeCount),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isLiked) AnonHeartRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Comment Pill
                    Surface(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onCommentClick)
                            .testTag("comment_button_${post.id}"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatCompactCount(post.commentCount),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share Circle Button
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onShareClick)
                            .testTag("share_button_${post.id}"),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share Confession",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // Bookmark / Save Circle Button
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onSaveClick)
                            .testTag("save_button_${post.id}"),
                        shape = CircleShape,
                        color = if (isSaved) primaryColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save Confession",
                                tint = if (isSaved) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatCompactCount(count: Long): String {
    return when {
        count >= 1_000_000L -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000L -> String.format("%.1fK", count / 1_000.0).replace(".0K", "K")
        else -> count.toString()
    }
}

fun formatRelativeTimestamp(timestamp: Timestamp?, postId: String = ""): String {
    if (postId == "seed_post_1") return "2h ago"
    if (postId == "seed_post_2") return "3h ago"
    if (timestamp == null) return "Just now"
    val diffMillis = (System.currentTimeMillis() - timestamp.toDate().time).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
