package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BlockedUserRef
import com.example.data.ConfessionPost
import com.example.data.PostComment
import com.example.data.SavedOrLikedPostRef
import com.example.data.UserNotification
import com.example.ui.components.AnonymousHatAvatar
import com.example.ui.theme.AnonHeartRed
import com.example.ui.theme.AnonPrimaryViolet

/**
 * Activity / Notifications Screen (opened via bottom Activity icon or top-right bell on Home)
 */
@Composable
fun ActivityNotificationsScreen(
    notifications: List<UserNotification>,
    onMarkRead: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("activity_notifications_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Activity & Safety Notices",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (notifications.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = AnonPrimaryViolet,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "You're all caught up",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Replies to your confessions and safety updates appear here without exposing sensitive text on your lock screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(notifications, key = { it.id }) { notif ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onMarkRead(notif.id) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (notif.isRead) MaterialTheme.colorScheme.surface else AnonPrimaryViolet.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(AnonPrimaryViolet.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = AnonPrimaryViolet
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notif.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notif.body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full Post Detail View with Real-Time Anonymous Comments & Engagement Actions
 */
@Composable
fun PostDetailSubScreen(
    post: ConfessionPost?,
    comments: List<PostComment>,
    isLiked: Boolean,
    isSaved: Boolean,
    currentUserId: String,
    onBack: () -> Unit,
    onToggleLike: (ConfessionPost) -> Unit,
    onToggleSave: (ConfessionPost) -> Unit,
    onAddComment: (ConfessionPost, String) -> Unit,
    onDeleteComment: (ConfessionPost, String) -> Unit,
    onReportPost: (ConfessionPost) -> Unit,
    onBlockAuthor: (ConfessionPost) -> Unit,
    onDeleteOwnPost: (String) -> Unit,
    onRecordView: (ConfessionPost) -> Unit
) {
    var commentInput by remember { mutableStateOf("") }

    LaunchedEffect(post?.id) {
        if (post != null) {
            onRecordView(post)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        SubScreenTopBar(title = "Confession Thread", onBack = onBack)

        if (post == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Confession is no longer available.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ConfessionPostCard(
                        post = post,
                        isLiked = isLiked,
                        isSaved = isSaved,
                        isOwnPost = post.authorUid == currentUserId,
                        onCardClick = {},
                        onLikeClick = { onToggleLike(post) },
                        onCommentClick = {},
                        onShareClick = {},
                        onSaveClick = { onToggleSave(post) },
                        onHideClick = onBack,
                        onBlockAuthorClick = { onBlockAuthor(post) },
                        onReportClick = { onReportPost(post) },
                        onDeleteClick = { onDeleteOwnPost(post.id) }
                    )
                }

                item {
                    Text(
                        text = "Anonymous Replies (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "No replies yet. Share an encouraging anonymous reply below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                AnonymousHatAvatar(size = 34.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comment.anonymousLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = comment.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (comment.authorUid == currentUserId) {
                                    IconButton(
                                        onClick = { onDeleteComment(post, comment.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete comment",
                                            tint = AnonHeartRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Comment Composer Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 96.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { if (it.length <= 500) commentInput = it },
                    placeholder = { Text("Add an anonymous reply...") },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field")
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = {
                        if (commentInput.isNotBlank()) {
                            onAddComment(post, commentInput)
                            commentInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AnonPrimaryViolet)
                        .testTag("submit_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send Reply",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Report Flow SubScreen (Section 9: harassment, threats, self-harm, sexual exploitation, hate, spam, impersonation, privacy, illegal, other)
 */
@Composable
fun ReportFlowSubScreen(
    targetType: String,
    targetId: String,
    targetLabel: String,
    onBack: () -> Unit,
    onSubmitReport: (reason: String, details: String) -> Unit
) {
    val categories = listOf(
        "harassment" to "Harassment or Bullying",
        "threats" to "Credible Threats or Violence",
        "self_harm" to "Self-Harm Concern",
        "sexual_exploitation" to "Sexual Exploitation or Abuse",
        "hate" to "Hate Speech",
        "spam" to "Spam or Scam",
        "impersonation" to "Impersonation",
        "privacy" to "Privacy / Personal Data Exposure",
        "illegal" to "Illegal Content",
        "other" to "Other Safety Issue"
    )
    var selectedReason by remember { mutableStateOf("harassment") }
    var details by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SubScreenTopBar(title = "Report $targetLabel", onBack = onBack)
        }
        item {
            Text(
                text = "Select a reason so our safety team can review this content promptly. Reports are strictly confidential.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(categories) { (key, label) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { selectedReason = key },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedReason == key,
                        onClick = { selectedReason = key }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        item {
            OutlinedTextField(
                value = details,
                onValueChange = { if (it.length <= 500) details = it },
                label = { Text("Additional details (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
        item {
            Button(
                onClick = { onSubmitReport(selectedReason, details) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_report_confirm_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AnonPrimaryViolet)
            ) {
                Text("Submit Safety Report", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Reusable Sub-Screens for My Posts, Saved Posts, Liked Posts, Blocked Users, Stay Anonymous Always, Help & Support, and About
 */
@Composable
fun SavedOrLikedListSubScreen(
    title: String,
    items: List<SavedOrLikedPostRef>,
    onBack: () -> Unit,
    onOpenPostId: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SubScreenTopBar(title = title, onBack = onBack) }
        if (items.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "No posts in $title yet. This list is private to your account and never visible to other users.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(items, key = { it.postId }) { ref ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenPostId(ref.postId) },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = ref.anonymousLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = AnonPrimaryViolet
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ref.postText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockedUsersSubScreen(
    blockedUsers: List<BlockedUserRef>,
    onBack: () -> Unit,
    onUnblock: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SubScreenTopBar(title = "Blocked Authors", onBack = onBack) }
        if (blockedUsers.isEmpty()) {
            item {
                Text(
                    text = "You have not blocked any authors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(blockedUsers, key = { it.blockedUid }) { blocked ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${blocked.blockedLabel} (${blocked.blockedUid.take(8)})")
                        OutlinedButton(onClick = { onUnblock(blocked.blockedUid) }) {
                            Text("Unblock")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoDetailSubScreen(
    title: String,
    paragraphs: List<String>,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SubScreenTopBar(title = title, onBack = onBack) }
        items(paragraphs) { text ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AnonPrimaryViolet,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SubScreenTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .testTag("subscreen_back_button"),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
