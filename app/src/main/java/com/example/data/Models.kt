package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val uid: String = "",
    val anonymousHandle: String = "@anon_user",
    val guest: Boolean = false,
    val themeMode: String = "light",
    val colorTheme: String = "custom_accent",
    val fontFamily: String = "plus_jakarta_sans",
    val useSystemFont: Boolean = false,
    val compactMode: Boolean = false,
    val keepAnonymousDefault: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val safetyGuidelinesAccepted: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "anonymousHandle" to anonymousHandle,
        "guest" to guest,
        "themeMode" to themeMode,
        "colorTheme" to colorTheme,
        "fontFamily" to fontFamily,
        "useSystemFont" to useSystemFont,
        "compactMode" to compactMode,
        "keepAnonymousDefault" to keepAnonymousDefault,
        "notificationsEnabled" to notificationsEnabled,
        "safetyGuidelinesAccepted" to safetyGuidelinesAccepted,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "Anonymousnet",
    val surname: String = "",
    val anonymousHandle: String = "@anon_user",
    val gender: String = "",
    val age: Int = 0,
    val interests: String = "",
    val avatarKey: String = "hat_glasses",
    val avatarUri: String = "",
    val postCount: Long = 0L,
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun fullDisplayName(): String {
        val first = displayName.trim().ifEmpty { "Anonymousnet" }
        val last = surname.trim()
        return if (last.isNotEmpty()) "$first $last" else first
    }

    fun toCreateMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "surname" to surname,
        "anonymousHandle" to anonymousHandle,
        "gender" to gender,
        "age" to age,
        "interests" to interests,
        "avatarKey" to avatarKey,
        "avatarUri" to avatarUri,
        "postCount" to postCount,
        "viewCount" to viewCount,
        "likeCount" to likeCount,
        "followersCount" to followersCount,
        "followingCount" to followingCount,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

sealed interface UsernameAvailability {
    data object Idle : UsernameAvailability
    data object Empty : UsernameAvailability
    data object Checking : UsernameAvailability
    data class Available(val handle: String) : UsernameAvailability
    data class Taken(val handle: String) : UsernameAvailability
    data class InvalidFormat(val reason: String) : UsernameAvailability
    data class IsCurrentUser(val handle: String) : UsernameAvailability
}

@IgnoreExtraProperties
data class ConfessionPost(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val anonymousLabel: String = "Anonymous",
    val anonymousHandle: String = "@anon_user",
    val authorAvatarUri: String? = null,
    val isAnonymous: Boolean = true,
    val type: String = "text", // text | image | video | audio | other
    val category: String = "General",
    val text: String = "",
    val mediaUri: String? = null,
    val mediaCaption: String? = null,
    val mediaDurationSec: Long? = null,
    val mediaMimeType: String? = null,
    val topics: String? = null,
    val contentWarning: String? = null,
    val status: String = "published", // published | draft | hidden
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    val viewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "authorUid" to authorUid,
            "anonymousLabel" to anonymousLabel,
            "anonymousHandle" to anonymousHandle,
            "isAnonymous" to isAnonymous,
            "type" to type,
            "category" to category,
            "text" to text,
            "status" to status,
            "likeCount" to likeCount,
            "commentCount" to commentCount,
            "viewCount" to viewCount,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!authorAvatarUri.isNullOrBlank()) map["authorAvatarUri"] = authorAvatarUri
        if (mediaUri != null) map["mediaUri"] = mediaUri
        if (mediaCaption != null) map["mediaCaption"] = mediaCaption
        if (mediaDurationSec != null) map["mediaDurationSec"] = mediaDurationSec
        if (mediaMimeType != null) map["mediaMimeType"] = mediaMimeType
        if (topics != null) map["topics"] = topics
        if (contentWarning != null) map["contentWarning"] = contentWarning
        return map
    }
}

@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorUid: String = "",
    val anonymousLabel: String = "Anonymous",
    val text: String = "",
    val status: String = "published",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> = mapOf(
        "postId" to postId,
        "authorUid" to authorUid,
        "anonymousLabel" to anonymousLabel,
        "text" to text,
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class SavedOrLikedPostRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val postText: String = "",
    val postType: String = "text",
    val postMediaUri: String? = null,
    val anonymousLabel: String = "Anonymous",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "postId" to postId,
            "postText" to postText,
            "postType" to postType,
            "anonymousLabel" to anonymousLabel,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (postMediaUri != null) map["postMediaUri"] = postMediaUri
        return map
    }
}

@IgnoreExtraProperties
data class FollowRef(
    @DocumentId val id: String = "",
    val followerUid: String = "",
    val targetUid: String = "",
    val targetHandle: String = "@anon_user",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> = mapOf(
        "followerUid" to followerUid,
        "targetUid" to targetUid,
        "targetHandle" to targetHandle,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val initiatorUid: String = "",
    val recipientUid: String = "",
    val status: String = "pending", // pending | accepted | declined | blocked
    val lastMessageText: String = "",
    val lastMessageSenderUid: String = "",
    val initiatorHandle: String = "@anon_user",
    val recipientHandle: String = "@anon_user",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> = mapOf(
        "participantUids" to participantUids,
        "initiatorUid" to initiatorUid,
        "recipientUid" to recipientUid,
        "status" to status,
        "lastMessageText" to lastMessageText,
        "lastMessageSenderUid" to lastMessageSenderUid,
        "initiatorHandle" to initiatorHandle,
        "recipientHandle" to recipientHandle,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val senderUid: String = "",
    val recipientUid: String = "",
    val text: String = "",
    val mediaUri: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "conversationId" to conversationId,
            "senderUid" to senderUid,
            "recipientUid" to recipientUid,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (mediaUri != null) map["mediaUri"] = mediaUri
        return map
    }
}

@IgnoreExtraProperties
data class ContentReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val reason: String = "harassment",
    val details: String = "",
    val status: String = "submitted",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> = mapOf(
        "reporterUid" to reporterUid,
        "targetType" to targetType,
        "targetId" to targetId,
        "reason" to reason,
        "details" to details,
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class BlockedUserRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedUid: String = "",
    val blockedLabel: String = "Anonymous",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "blockedUid" to blockedUid,
        "blockedLabel" to blockedLabel,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "activity",
    val title: String = "",
    val body: String = "",
    val targetId: String? = null,
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "type" to type,
            "title" to title,
            "body" to body,
            "isRead" to isRead,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (targetId != null) map["targetId"] = targetId
        return map
    }
}

// --- Fault-Tolerant DocumentSnapshot Deserializers (prevents crashes on legacy/mismatched Firestore fields) ---

private fun DocumentSnapshot.str(key: String, default: String = ""): String {
    val raw = try { get(key) } catch (_: Throwable) { null } ?: return default
    return when (raw) {
        is String -> raw
        is Iterable<*> -> raw.joinToString(",") { it?.toString().orEmpty() }
        else -> raw.toString()
    }
}

private fun DocumentSnapshot.strOrNull(key: String): String? {
    val raw = try { get(key) } catch (_: Throwable) { null } ?: return null
    return when (raw) {
        is String -> raw
        is Iterable<*> -> raw.joinToString(",") { it?.toString().orEmpty() }
        else -> raw.toString()
    }
}

private fun DocumentSnapshot.longVal(key: String, default: Long = 0L): Long {
    val raw = try { get(key) } catch (_: Throwable) { null } ?: return default
    return when (raw) {
        is Number -> raw.toLong()
        is String -> raw.toLongOrNull() ?: default
        else -> default
    }
}

private fun DocumentSnapshot.longOrNull(key: String): Long? {
    val raw = try { get(key) } catch (_: Throwable) { null } ?: return null
    return when (raw) {
        is Number -> raw.toLong()
        is String -> raw.toLongOrNull()
        else -> null
    }
}

private fun DocumentSnapshot.boolVal(key: String, default: Boolean = false): Boolean {
    val raw = try { get(key) } catch (_: Throwable) { null } ?: return default
    return when (raw) {
        is Boolean -> raw
        is String -> raw.equals("true", ignoreCase = true)
        is Number -> raw.toInt() != 0
        else -> default
    }
}

private fun DocumentSnapshot.tsOrNull(key: String): Timestamp? {
    return try {
        getTimestamp(key, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (_: Throwable) {
        val raw = try { get(key) } catch (_: Throwable) { null }
        when (raw) {
            is Timestamp -> raw
            is Date -> Timestamp(raw)
            is Number -> Timestamp(Date(raw.toLong()))
            else -> null
        }
    }
}

fun DocumentSnapshot.toUserAccountSafe(): UserAccount? {
    if (!exists()) return null
    return try {
        UserAccount(
            id = id,
            uid = str("uid", id),
            anonymousHandle = str("anonymousHandle", "@anon_user"),
            guest = boolVal("guest", false),
            themeMode = str("themeMode", "light"),
            colorTheme = str("colorTheme", "custom_accent"),
            fontFamily = str("fontFamily", "plus_jakarta_sans"),
            useSystemFont = boolVal("useSystemFont", false),
            compactMode = boolVal("compactMode", false),
            keepAnonymousDefault = boolVal("keepAnonymousDefault", true),
            notificationsEnabled = boolVal("notificationsEnabled", true),
            safetyGuidelinesAccepted = boolVal("safetyGuidelinesAccepted", true),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}

fun DocumentSnapshot.toPublicProfileSafe(): PublicProfile? {
    if (!exists()) return null
    return try {
        PublicProfile(
            id = id,
            uid = str("uid", id),
            displayName = str("displayName", "Anonymousnet"),
            surname = str("surname", ""),
            anonymousHandle = str("anonymousHandle", "@anon_user"),
            gender = str("gender", ""),
            age = longVal("age", 0L).toInt().coerceIn(0, 120),
            interests = str("interests", ""),
            avatarKey = str("avatarKey", "hat_glasses"),
            avatarUri = str("avatarUri", ""),
            postCount = longVal("postCount", 0L),
            viewCount = longVal("viewCount", 0L),
            likeCount = longVal("likeCount", 0L),
            followersCount = longVal("followersCount", 0L),
            followingCount = longVal("followingCount", 0L),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}

fun DocumentSnapshot.toConfessionPostSafe(): ConfessionPost? {
    if (!exists()) return null
    return try {
        ConfessionPost(
            id = id,
            authorUid = str("authorUid", ""),
            anonymousLabel = str("anonymousLabel", "Anonymous"),
            anonymousHandle = str("anonymousHandle", "@anon_user"),
            authorAvatarUri = strOrNull("authorAvatarUri"),
            isAnonymous = boolVal("isAnonymous", true),
            type = str("type", "text"),
            category = str("category", "General").ifBlank { "General" },
            text = str("text", ""),
            mediaUri = strOrNull("mediaUri"),
            mediaCaption = strOrNull("mediaCaption"),
            mediaDurationSec = longOrNull("mediaDurationSec"),
            mediaMimeType = strOrNull("mediaMimeType"),
            topics = strOrNull("topics"),
            contentWarning = strOrNull("contentWarning"),
            status = str("status", "published"),
            likeCount = longVal("likeCount", 0L),
            commentCount = longVal("commentCount", 0L),
            viewCount = longVal("viewCount", 0L),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}

fun DocumentSnapshot.toPostCommentSafe(): PostComment? {
    if (!exists()) return null
    return try {
        PostComment(
            id = id,
            postId = str("postId", ""),
            authorUid = str("authorUid", ""),
            anonymousLabel = str("anonymousLabel", "Anonymous"),
            text = str("text", ""),
            status = str("status", "published"),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}

fun DocumentSnapshot.toSavedOrLikedPostRefSafe(): SavedOrLikedPostRef? {
    if (!exists()) return null
    return try {
        SavedOrLikedPostRef(
            id = id,
            userId = str("userId", ""),
            postId = str("postId", id),
            postText = str("postText", ""),
            postType = str("postType", "text"),
            postMediaUri = strOrNull("postMediaUri"),
            anonymousLabel = str("anonymousLabel", "Anonymous"),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}

fun DocumentSnapshot.toFollowRefSafe(): FollowRef? {
    if (!exists()) return null
    return try {
        FollowRef(
            id = id,
            followerUid = str("followerUid", ""),
            targetUid = str("targetUid", id),
            targetHandle = str("targetHandle", "@anon_user"),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}

fun DocumentSnapshot.toConversationSafe(): Conversation? {
    if (!exists()) return null
    return try {
        val rawParticipants = try { get("participantUids") } catch (_: Throwable) { null }
        val participants = (rawParticipants as? Iterable<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        Conversation(
            id = id,
            participantUids = participants,
            initiatorUid = str("initiatorUid", ""),
            recipientUid = str("recipientUid", ""),
            status = str("status", "pending"),
            lastMessageText = str("lastMessageText", ""),
            lastMessageSenderUid = str("lastMessageSenderUid", ""),
            initiatorHandle = str("initiatorHandle", "@anon_user"),
            recipientHandle = str("recipientHandle", "@anon_user"),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}

fun DocumentSnapshot.toDirectMessageSafe(): DirectMessage? {
    if (!exists()) return null
    return try {
        DirectMessage(
            id = id,
            conversationId = str("conversationId", ""),
            senderUid = str("senderUid", ""),
            recipientUid = str("recipientUid", ""),
            text = str("text", ""),
            mediaUri = strOrNull("mediaUri"),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}

fun DocumentSnapshot.toBlockedUserRefSafe(): BlockedUserRef? {
    if (!exists()) return null
    return try {
        BlockedUserRef(
            id = id,
            userId = str("userId", ""),
            blockedUid = str("blockedUid", id),
            blockedLabel = str("blockedLabel", "Anonymous"),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}

fun DocumentSnapshot.toUserNotificationSafe(): UserNotification? {
    if (!exists()) return null
    return try {
        UserNotification(
            id = id,
            userId = str("userId", ""),
            type = str("type", "activity"),
            title = str("title", ""),
            body = str("body", ""),
            targetId = strOrNull("targetId"),
            isRead = boolVal("isRead", false),
            createdAt = tsOrNull("createdAt"),
            updatedAt = tsOrNull("updatedAt")
        )
    } catch (_: Throwable) {
        null
    }
}
