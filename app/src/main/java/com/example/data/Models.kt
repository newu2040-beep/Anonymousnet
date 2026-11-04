package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authProvider: String = "google",
    val accountState: String = "active",
    val guest: Boolean = false,
    val themeMode: String = "light",
    val keepAnonymousDefault: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val mutedTopics: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "authProvider" to authProvider,
        "accountState" to accountState,
        "guest" to guest,
        "themeMode" to themeMode,
        "keepAnonymousDefault" to keepAnonymousDefault,
        "notificationsEnabled" to notificationsEnabled,
        "mutedTopics" to mutedTopics,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Anonymousnet",
    val anonymousHandle: String = "@anon_user",
    val avatarKey: String = "hooded_violet",
    val isVerified: Boolean = true,
    val postCount: Long = 0L,
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "anonymousHandle" to anonymousHandle,
        "avatarKey" to avatarKey,
        "isVerified" to isVerified,
        "postCount" to postCount,
        "viewCount" to viewCount,
        "likeCount" to likeCount,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ConfessionPost(
    @DocumentId val id: String = "",
    val authorUid: String = "",
    val anonymousLabel: String = "Anonymous",
    val anonymousHandle: String = "@anon_user",
    val isAnonymous: Boolean = true,
    val text: String = "",
    val type: String = "text", // text, image, video, audio, other
    val mediaUri: String? = null,
    val mediaCaption: String? = null,
    val mediaDurationSec: Long? = null,
    val mediaMimeType: String? = null,
    val topics: String? = null,
    val contentWarning: String? = null,
    val status: String = "published", // draft, processing, published, under_review, removed, deleted
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    val viewCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> = mapOf(
        "authorUid" to authorUid,
        "anonymousLabel" to anonymousLabel,
        "anonymousHandle" to anonymousHandle,
        "isAnonymous" to isAnonymous,
        "text" to text,
        "type" to type,
        "mediaUri" to mediaUri,
        "mediaCaption" to mediaCaption,
        "mediaDurationSec" to mediaDurationSec,
        "mediaMimeType" to mediaMimeType,
        "topics" to topics,
        "contentWarning" to contentWarning,
        "status" to status,
        "likeCount" to likeCount,
        "commentCount" to commentCount,
        "viewCount" to viewCount,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
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
    fun toCreateMap(): Map<String, Any> = mapOf(
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
data class PostLike(
    @DocumentId val id: String = "",
    val postId: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SavedOrLikedPostRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val postText: String = "",
    val postType: String = "text",
    val postMediaUri: String? = null,
    val anonymousLabel: String = "Anonymous",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    @Suppress("UNCHECKED_CAST")
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "postId" to postId,
        "postText" to postText,
        "postType" to postType,
        "postMediaUri" to postMediaUri,
        "anonymousLabel" to anonymousLabel,
        "createdAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class BlockedUserRef(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedUid: String = "",
    val blockedLabel: String = "Anonymous",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "blockedUid" to blockedUid,
        "blockedLabel" to blockedLabel,
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "system", // comment, like, moderation, security, system
    val title: String = "",
    val body: String = "",
    val targetId: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "type" to type,
        "title" to title,
        "body" to body,
        "targetId" to targetId,
        "isRead" to isRead,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ContentReport(
    @DocumentId val id: String = "",
    val reporterUid: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val reason: String = "other",
    val details: String = "",
    val status: String = "submitted",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "reporterUid" to reporterUid,
        "targetType" to targetType,
        "targetId" to targetId,
        "reason" to reason,
        "details" to details,
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp()
    )
}
