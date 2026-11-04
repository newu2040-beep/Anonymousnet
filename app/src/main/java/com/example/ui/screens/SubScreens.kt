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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.Conversation
import com.example.data.DirectMessage
import com.example.data.PostComment
import com.example.data.SavedOrLikedPostRef
import com.example.data.UserNotification
import com.example.ui.components.AnonymousHatAvatar
import com.example.ui.theme.AnonHeartRed

/**
 * Activity & Inbox Screen: Shows real-time Activity & Safety Notices,
 * as well as Direct Message Conversations with Request & Acceptance system.
 */
@Composable
fun ActivityNotificationsScreen(
    notifications: List<UserNotification>,
    conversations: List<Conversation>,
    currentUserId: String,
    onMarkRead: (String) -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onRespondToDmRequest: (conversationId: String, accept: Boolean, block: Boolean) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("activity_notifications_list"),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Direct Messages Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Direct Messages (${conversations.size})",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (conversations.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mail,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No private messages yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp)
                        )
                        Text(
                            text = "Tap the three dots on any confession to send a private message request.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(conversations, key = { it.id }) { conv ->
                val isInitiator = conv.initiatorUid == currentUserId
                val otherHandle = if (isInitiator) conv.recipientHandle else conv.initiatorHandle
                val otherUid = if (isInitiator) conv.recipientUid else conv.initiatorUid

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (conv.status == "accepted" || isInitiator) {
                                onOpenConversation(conv)
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnonymousHatAvatar(size = 38.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = otherHandle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = conv.lastMessageText.ifBlank { "Message request" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (conv.status) {
                                    "accepted" -> primaryColor.copy(alpha = 0.15f)
                                    "pending" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = conv.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = when (conv.status) {
                                        "accepted" -> primaryColor
                                        "pending" -> Color(0xFFD97706)
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Message Request Accept/Decline/Block Controls for Recipient
                        if (!isInitiator && conv.status == "pending") {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRespondToDmRequest(conv.id, true, false) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Accept", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onRespondToDmRequest(conv.id, false, false) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Decline", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onRespondToDmRequest(conv.id, false, true) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Block, contentDescription = null, tint = AnonHeartRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Block", color = AnonHeartRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Activity & Safety Notices Section Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Activity & Safety Notices",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (notifications.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You're all caught up",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Replies, likes, follows, and direct message alerts appear here in real time.",
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
                    color = if (notif.isRead) MaterialTheme.colorScheme.surface else primaryColor.copy(alpha = 0.08f)
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
                                .background(primaryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = primaryColor
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
 * Real-Time Direct Message Chat SubScreen
 */
@Composable
fun ConversationChatSubScreen(
    conversationId: String,
    otherUserHandle: String,
    otherUserUid: String,
    currentUserId: String,
    messages: List<DirectMessage>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        SubScreenTopBar(title = otherUserHandle, onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Start of confidential conversation with $otherUserHandle.\nZero legal identities are exposed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    val isMe = msg.senderUid == currentUserId
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            ),
                            color = if (isMe) primaryColor else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Message Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 96.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { if (it.length <= 1000) messageInput = it },
                placeholder = { Text("Write private message...") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("chat_input_field")
            )
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        onSendMessage(messageInput)
                        messageInput = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Post Detail SubScreen
 */
@Composable
fun PostDetailSubScreen(
    post: ConfessionPost?,
    comments: List<PostComment>,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowingAuthor: Boolean = false,
    currentUserId: String,
    onBack: () -> Unit,
    onToggleLike: (ConfessionPost) -> Unit,
    onToggleSave: (ConfessionPost) -> Unit,
    onToggleFollow: (authorUid: String, authorHandle: String) -> Unit = { _, _ -> },
    onSendMessageRequest: (String) -> Unit = {},
    onAddComment: (ConfessionPost, String) -> Unit,
    onDeleteComment: (ConfessionPost, String) -> Unit,
    onReportPost: (ConfessionPost) -> Unit,
    onBlockAuthor: (ConfessionPost) -> Unit,
    onDeleteOwnPost: (String) -> Unit,
    onRecordView: (ConfessionPost) -> Unit
) {
    var commentInput by remember { mutableStateOf("") }
    val primaryColor = MaterialTheme.colorScheme.primary

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
                        isFollowingAuthor = isFollowingAuthor,
                        isOwnPost = post.authorUid == currentUserId,
                        onCardClick = {},
                        onLikeClick = { onToggleLike(post) },
                        onCommentClick = {},
                        onToggleFollow = { onToggleFollow(post.authorUid, post.anonymousHandle) },
                        onSendMessageRequest = onSendMessageRequest,
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
                        .background(primaryColor)
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
 * Report Flow SubScreen
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
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SubScreenTopBar(title = "Report $targetLabel", onBack = onBack) }
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
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("Submit Safety Report", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Reusable Sub-Screens
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
                            color = MaterialTheme.colorScheme.primary
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
    val primaryColor = MaterialTheme.colorScheme.primary

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
                        tint = primaryColor,
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
