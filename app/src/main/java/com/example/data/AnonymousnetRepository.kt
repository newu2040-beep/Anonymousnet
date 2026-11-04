package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AnonymousnetRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- 1. User Account & Public Pseudonymous Profile ---

    suspend fun ensureUserAndPublicProfile(guestMode: Boolean = false): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val profileRef = db.collection("publicProfiles").document(uid)
        return try {
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                val account = UserAccount(
                    userId = uid,
                    authProvider = if (guestMode) "guest_google" else "google",
                    accountState = "active",
                    guest = guestMode,
                    themeMode = "light",
                    colorTheme = "violet",
                    useSystemFont = false,
                    keepAnonymousDefault = true,
                    notificationsEnabled = true,
                    mutedTopics = ""
                )
                userRef.set(account.toCreateMap()).await()
            }
            val profileSnap = profileRef.get().await()
            if (!profileSnap.exists()) {
                val suffix = uid.take(4).lowercase().filter { it.isLetterOrDigit() }.ifEmpty { "user" }
                val initialHandle = "anon_$suffix"
                val profile = PublicProfile(
                    userId = uid,
                    displayName = "Anonymousnet",
                    anonymousHandle = "@$initialHandle",
                    avatarKey = "hooded_violet",
                    isVerified = true,
                    postCount = 24L,
                    viewCount = 12500L,
                    likeCount = 8700L,
                    followersCount = 142L,
                    followingCount = 38L
                )
                profileRef.set(profile.toCreateMap()).await()

                val usernameRef = db.collection("usernames").document(initialHandle)
                val usernameSnap = usernameRef.get().await()
                if (!usernameSnap.exists()) {
                    usernameRef.set(
                        mapOf(
                            "uid" to uid,
                            "handle" to initialHandle,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }

                val notifId = "welcome_${uid.take(8)}"
                val notifRef = userRef.collection("notifications").document(notifId)
                val welcomeNotif = UserNotification(
                    userId = uid,
                    type = "security",
                    title = "Stay Anonymous Always",
                    body = "Your public identity is hidden on all confessions. You are protected by pseudonymous safety controls.",
                    targetId = "privacy",
                    isRead = false
                )
                notifRef.set(welcomeNotif.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path)
            Result.failure(e)
        }
    }

    fun observeUserAccount(userId: String): Flow<UserAccount?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    fun observePublicProfile(userId: String): Flow<PublicProfile?> {
        val docRef = db.collection("publicProfiles").document(userId)
        return docRef.snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else null
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                throw error
            }
    }

    suspend fun updateUserPreferences(
        themeMode: String? = null,
        colorTheme: String? = null,
        useSystemFont: Boolean? = null,
        keepAnonymousDefault: Boolean? = null,
        notificationsEnabled: Boolean? = null,
        mutedTopics: String? = null,
        guest: Boolean? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (themeMode != null) updates["themeMode"] = themeMode
            if (colorTheme != null) updates["colorTheme"] = colorTheme
            if (useSystemFont != null) updates["useSystemFont"] = useSystemFont
            if (keepAnonymousDefault != null) updates["keepAnonymousDefault"] = keepAnonymousDefault
            if (notificationsEnabled != null) updates["notificationsEnabled"] = notificationsEnabled
            if (mutedTopics != null) updates["mutedTopics"] = mutedTopics
            if (guest != null) {
                updates["guest"] = guest
                updates["authProvider"] = if (guest) "guest_google" else "google"
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun normalizeHandle(raw: String): String {
        return raw.trim()
            .removePrefix("@")
            .lowercase()
            .filter { it.isLetterOrDigit() || it == '_' }
    }

    fun isValidHandleFormat(handle: String): Boolean {
        return handle.length in 3..30 && handle.all { it.isLetterOrDigit() || it == '_' }
    }

    suspend fun checkUsernameAvailability(rawHandle: String): Result<UsernameAvailability> {
        val uid = requireUserId()
        val normalized = normalizeHandle(rawHandle)
        if (normalized.isEmpty()) {
            return Result.success(UsernameAvailability.Empty)
        }
        if (normalized.length < 3) {
            return Result.success(UsernameAvailability.InvalidFormat("Must be at least 3 characters"))
        }
        if (normalized.length > 30) {
            return Result.success(UsernameAvailability.InvalidFormat("Must be 30 characters or fewer"))
        }
        return try {
            val snap = db.collection("usernames").document(normalized).get().await()
            if (!snap.exists()) {
                Result.success(UsernameAvailability.Available(normalized))
            } else {
                val ownerUid = snap.getString("uid")
                if (ownerUid == uid) {
                    Result.success(UsernameAvailability.IsCurrentUser(normalized))
                } else {
                    Result.success(UsernameAvailability.Taken(normalized))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "usernames/$normalized")
            Result.failure(e)
        }
    }

    suspend fun updateUsernameAndDisplayName(
        newRawHandle: String,
        newDisplayName: String,
        newAvatarKey: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val newNormalized = normalizeHandle(newRawHandle)
        if (!isValidHandleFormat(newNormalized)) {
            return Result.failure(IllegalArgumentException("Username must be 3-30 letters, numbers, or underscores."))
        }
        val cleanDisplayName = newDisplayName.trim().take(60).ifEmpty { "Anonymousnet" }
        val profileRef = db.collection("publicProfiles").document(uid)

        return try {
            val profileSnap = profileRef.get().await()
            val currentHandle = profileSnap.getString("anonymousHandle")?.removePrefix("@")?.lowercase() ?: ""

            if (currentHandle == newNormalized) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanDisplayName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (newAvatarKey != null) updates["avatarKey"] = newAvatarKey
                profileRef.update(updates).await()
                return Result.success(Unit)
            }

            val targetDocRef = db.collection("usernames").document(newNormalized)
            val targetSnap = targetDocRef.get().await()
            if (targetSnap.exists() && targetSnap.getString("uid") != uid) {
                return Result.failure(IllegalStateException("Username '@$newNormalized' is already taken."))
            }

            val batch = db.batch()
            batch.set(
                targetDocRef,
                mapOf(
                    "uid" to uid,
                    "handle" to newNormalized,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            if (currentHandle.isNotEmpty() && currentHandle != "anon_user" && currentHandle != newNormalized) {
                val oldDocRef = db.collection("usernames").document(currentHandle)
                batch.delete(oldDocRef)
            }
            val profileUpdates = mutableMapOf<String, Any>(
                "displayName" to cleanDisplayName,
                "anonymousHandle" to "@$newNormalized",
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newAvatarKey != null) profileUpdates["avatarKey"] = newAvatarKey
            batch.update(profileRef, profileUpdates)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "usernames/$newNormalized")
            Result.failure(e)
        }
    }

    suspend fun updatePublicProfileHandle(
        displayName: String,
        anonymousHandle: String,
        avatarKey: String
    ): Result<Unit> {
        return updateUsernameAndDisplayName(
            newRawHandle = anonymousHandle,
            newDisplayName = displayName,
            newAvatarKey = avatarKey
        )
    }

    // --- 2. Confession Posts (Feed, Explore, Drafts, My Posts) ---

    fun observePublishedPosts(): Flow<List<ConfessionPost>> {
        val path = "posts"
        return db.collection(path)
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ConfessionPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMyPosts(userId: String): Flow<List<ConfessionPost>> {
        val path = "posts"
        return db.collection(path)
            .whereEqualTo("authorUid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ConfessionPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.status != "deleted" }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun getPostById(postId: String): Result<ConfessionPost?> {
        return try {
            val snapshot = db.collection("posts").document(postId).get().await()
            if (snapshot.exists()) {
                Result.success(snapshot.toObject(ConfessionPost::class.java)?.copy(id = snapshot.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            handleFirestoreError(e, OperationType.GET, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun createPost(
        text: String,
        type: String = "text",
        category: String = "General",
        isAnonymous: Boolean = true,
        anonymousLabel: String = "Anonymous",
        anonymousHandle: String = "@anon_user",
        mediaUri: String? = null,
        mediaCaption: String? = null,
        mediaDurationSec: Long? = null,
        mediaMimeType: String? = null,
        topics: String? = null,
        contentWarning: String? = null,
        status: String = "published",
        initialLikes: Long = 0L,
        initialComments: Long = 0L,
        initialViews: Long = 1L,
        customDocId: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val postId = customDocId ?: "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("posts").document(postId)
        return try {
            val lower = text.lowercase()
            val flaggedKeywords = listOf("kill yourself", "bomb threat", "doxx", "ssn:")
            val finalStatus = if (status == "published" && flaggedKeywords.any { lower.contains(it) }) {
                "under_review"
            } else {
                status
            }

            val post = ConfessionPost(
                id = postId,
                authorUid = uid,
                anonymousLabel = if (isAnonymous) "Anonymous" else anonymousLabel,
                anonymousHandle = anonymousHandle,
                isAnonymous = isAnonymous,
                text = text.trim().take(2000),
                type = type,
                category = category.trim().take(60).ifBlank { "General" },
                mediaUri = mediaUri?.takeIf { it.isNotBlank() },
                mediaCaption = mediaCaption?.takeIf { it.isNotBlank() },
                mediaDurationSec = mediaDurationSec,
                mediaMimeType = mediaMimeType?.takeIf { it.isNotBlank() },
                topics = topics?.takeIf { it.isNotBlank() },
                contentWarning = contentWarning?.takeIf { it.isNotBlank() },
                status = finalStatus,
                likeCount = initialLikes,
                commentCount = initialComments,
                viewCount = initialViews
            )
            docRef.set(post.toCreateMap()).await()
            Result.success(postId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("posts").document(postId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- 3. Engagement: Likes, Saves, Comments, Views ---

    fun observeLikedPosts(userId: String): Flow<List<SavedOrLikedPostRef>> {
        val path = "users/$userId/liked"
        return db.collection("users").document(userId).collection("liked")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SavedOrLikedPostRef::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeSavedPosts(userId: String): Flow<List<SavedOrLikedPostRef>> {
        val path = "users/$userId/saved"
        return db.collection("users").document(userId).collection("saved")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SavedOrLikedPostRef::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun toggleLike(post: ConfessionPost, isCurrentlyLiked: Boolean): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(post.id)
        val postLikeRef = postRef.collection("likes").document(uid)
        val userLikedRef = db.collection("users").document(uid).collection("liked").document(post.id)

        return try {
            val batch = db.batch()
            if (isCurrentlyLiked) {
                batch.delete(postLikeRef)
                batch.delete(userLikedRef)
                val nextLikes = (post.likeCount - 1L).coerceAtLeast(0L)
                batch.update(
                    postRef,
                    mapOf(
                        "likeCount" to nextLikes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    postLikeRef,
                    mapOf(
                        "postId" to post.id,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                val refObj = SavedOrLikedPostRef(
                    userId = uid,
                    postId = post.id,
                    postText = post.text.take(2000),
                    postType = post.type,
                    postMediaUri = post.mediaUri,
                    anonymousLabel = post.anonymousLabel
                )
                batch.set(userLikedRef, refObj.toCreateMap())
                batch.update(
                    postRef,
                    mapOf(
                        "likeCount" to (post.likeCount + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Notify post author if not self
                if (post.authorUid != uid) {
                    val notifRef = db.collection("users").document(post.authorUid).collection("notifications").document("like_${post.id.take(8)}_$uid")
                    val notif = UserNotification(
                        userId = post.authorUid,
                        type = "like",
                        title = "New Confession Like",
                        body = "Someone resonated with your anonymous confession.",
                        targetId = post.id,
                        isRead = false
                    )
                    batch.set(notifRef, notif.toCreateMap())
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleSave(post: ConfessionPost, isCurrentlySaved: Boolean): Result<Unit> {
        val uid = requireUserId()
        val userSavedRef = db.collection("users").document(uid).collection("saved").document(post.id)
        return try {
            if (isCurrentlySaved) {
                userSavedRef.delete().await()
            } else {
                val refObj = SavedOrLikedPostRef(
                    userId = uid,
                    postId = post.id,
                    postText = post.text.take(2000),
                    postType = post.type,
                    postMediaUri = post.mediaUri,
                    anonymousLabel = post.anonymousLabel
                )
                userSavedRef.set(refObj.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userSavedRef.path)
            Result.failure(e)
        }
    }

    fun observeComments(postId: String): Flow<List<PostComment>> {
        val path = "posts/$postId/comments"
        return db.collection("posts").document(postId).collection("comments")
            .whereEqualTo("status", "published")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun addComment(
        post: ConfessionPost,
        text: String,
        anonymousLabel: String = "Anonymous"
    ): Result<String> {
        val uid = requireUserId()
        val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val postRef = db.collection("posts").document(post.id)
        val commentRef = postRef.collection("comments").document(commentId)
        return try {
            val comment = PostComment(
                id = commentId,
                postId = post.id,
                authorUid = uid,
                anonymousLabel = anonymousLabel,
                text = text.trim().take(500),
                status = "published"
            )
            val batch = db.batch()
            batch.set(commentRef, comment.toCreateMap())
            batch.update(
                postRef,
                mapOf(
                    "commentCount" to (post.commentCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Notify post author if not self
            if (post.authorUid != uid) {
                val notifRef = db.collection("users").document(post.authorUid).collection("notifications").document("cmt_${commentId.take(12)}")
                val notif = UserNotification(
                    userId = post.authorUid,
                    type = "comment",
                    title = "New Anonymous Reply",
                    body = "Someone left an anonymous reply on your confession: \"${text.trim().take(60)}\"",
                    targetId = post.id,
                    isRead = false
                )
                batch.set(notifRef, notif.toCreateMap())
            }

            batch.commit().await()
            Result.success(commentId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(post: ConfessionPost, commentId: String): Result<Unit> {
        requireUserId()
        val postRef = db.collection("posts").document(post.id)
        val commentRef = postRef.collection("comments").document(commentId)
        return try {
            val batch = db.batch()
            batch.delete(commentRef)
            batch.update(
                postRef,
                mapOf(
                    "commentCount" to (post.commentCount - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementPostView(post: ConfessionPost): Result<Unit> {
        requireUserId()
        val postRef = db.collection("posts").document(post.id)
        return try {
            postRef.update(
                mapOf(
                    "viewCount" to (post.viewCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            Result.failure(e)
        }
    }

    // --- 4. Follow System ---

    fun observeFollowing(userId: String): Flow<List<FollowRef>> {
        val path = "users/$userId/following"
        return db.collection("users").document(userId).collection("following")
            .whereEqualTo("followerUid", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FollowRef::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun toggleFollow(targetUid: String, targetHandle: String, isCurrentlyFollowing: Boolean): Result<Unit> {
        val uid = requireUserId()
        if (targetUid == uid) return Result.failure(IllegalArgumentException("Cannot follow self"))

        val followRef = db.collection("users").document(uid).collection("following").document(targetUid)
        val myProfileRef = db.collection("publicProfiles").document(uid)
        val targetProfileRef = db.collection("publicProfiles").document(targetUid)

        return try {
            val batch = db.batch()
            if (isCurrentlyFollowing) {
                batch.delete(followRef)
                batch.update(myProfileRef, "followingCount", FieldValue.increment(-1))
                batch.update(targetProfileRef, "followersCount", FieldValue.increment(-1))
            } else {
                val follow = FollowRef(
                    followerUid = uid,
                    targetUid = targetUid,
                    targetHandle = targetHandle
                )
                batch.set(followRef, follow.toCreateMap())
                batch.update(myProfileRef, "followingCount", FieldValue.increment(1))
                batch.update(targetProfileRef, "followersCount", FieldValue.increment(1))

                val notifRef = db.collection("users").document(targetUid).collection("notifications").document("follow_${uid.take(12)}")
                val notif = UserNotification(
                    userId = targetUid,
                    type = "follow",
                    title = "New Anonymous Follower",
                    body = "Someone started following your anonymous confessions.",
                    targetId = uid,
                    isRead = false
                )
                batch.set(notifRef, notif.toCreateMap())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followRef.path)
            Result.failure(e)
        }
    }

    // --- 5. Real-Time Private Messaging ---

    fun observeConversations(userId: String): Flow<List<Conversation>> {
        val path = "conversations"
        return db.collection(path)
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    fun observeMessages(conversationId: String): Flow<List<DirectMessage>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations").document(conversationId).collection("messages")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DirectMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun sendDirectMessageRequest(
        recipientUid: String,
        recipientHandle: String,
        initialText: String,
        mediaUri: String? = null
    ): Result<String> {
        val uid = requireUserId()
        if (recipientUid == uid) return Result.failure(IllegalArgumentException("Cannot message self"))

        val conversationId = if (uid < recipientUid) "conv_${uid.take(8)}_${recipientUid.take(8)}" else "conv_${recipientUid.take(8)}_${uid.take(8)}"
        val convRef = db.collection("conversations").document(conversationId)

        return try {
            val myProfile = db.collection("publicProfiles").document(uid).get().await()
            val senderHandle = myProfile.getString("anonymousHandle") ?: "@anon_user"

            val existing = convRef.get().await()
            val batch = db.batch()

            val status = if (existing.exists()) {
                existing.getString("status") ?: "pending"
            } else {
                "pending"
            }

            if (status == "declined" || status == "blocked") {
                return Result.failure(IllegalStateException("Conversation request is $status."))
            }

            val conv = Conversation(
                id = conversationId,
                participantUids = listOf(uid, recipientUid),
                initiatorUid = uid,
                recipientUid = recipientUid,
                status = status,
                lastMessageText = initialText.trim().take(1000),
                lastMessageSenderUid = uid,
                initiatorHandle = senderHandle,
                recipientHandle = recipientHandle
            )
            batch.set(convRef, conv.toCreateMap())

            val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            val msgRef = convRef.collection("messages").document(messageId)
            val msg = DirectMessage(
                id = messageId,
                conversationId = conversationId,
                senderUid = uid,
                recipientUid = recipientUid,
                text = initialText.trim().take(1000),
                mediaUri = mediaUri
            )
            batch.set(msgRef, msg.toCreateMap())

            // Notification to recipient
            val notifRef = db.collection("users").document(recipientUid).collection("notifications").document("dm_${messageId.take(12)}")
            val notif = UserNotification(
                userId = recipientUid,
                type = if (status == "pending") "dm_request" else "dm_message",
                title = if (status == "pending") "Private Message Request" else "New Message",
                body = "New message from $senderHandle: \"${initialText.trim().take(60)}\"",
                targetId = conversationId,
                isRead = false
            )
            batch.set(notifRef, notif.toCreateMap())

            batch.commit().await()
            Result.success(conversationId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, convRef.path)
            Result.failure(e)
        }
    }

    suspend fun respondToMessageRequest(conversationId: String, accept: Boolean, block: Boolean = false): Result<Unit> {
        val uid = requireUserId()
        val convRef = db.collection("conversations").document(conversationId)
        return try {
            val nextStatus = when {
                block -> "blocked"
                accept -> "accepted"
                else -> "declined"
            }
            convRef.update(
                mapOf(
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (block) {
                val conv = convRef.get().await().toObject(Conversation::class.java)
                val targetUid = conv?.participantUids?.firstOrNull { it != uid }
                if (targetUid != null) {
                    blockAuthor(targetUid, "Anonymous User")
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, convRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendMessageInConversation(
        conversationId: String,
        recipientUid: String,
        text: String,
        mediaUri: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val convRef = db.collection("conversations").document(conversationId)
        val messageId = "msg_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val msgRef = convRef.collection("messages").document(messageId)

        return try {
            val conv = convRef.get().await().toObject(Conversation::class.java)
            if (conv?.status != "accepted") {
                return Result.failure(IllegalStateException("Cannot send message until conversation request is accepted."))
            }

            val batch = db.batch()
            val msg = DirectMessage(
                id = messageId,
                conversationId = conversationId,
                senderUid = uid,
                recipientUid = recipientUid,
                text = text.trim().take(1000),
                mediaUri = mediaUri
            )
            batch.set(msgRef, msg.toCreateMap())
            batch.update(
                convRef,
                mapOf(
                    "lastMessageText" to text.trim().take(1000),
                    "lastMessageSenderUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val notifRef = db.collection("users").document(recipientUid).collection("notifications").document("msg_${messageId.take(12)}")
            val notif = UserNotification(
                userId = recipientUid,
                type = "dm_message",
                title = "New Private Message",
                body = "New message: \"${text.trim().take(60)}\"",
                targetId = conversationId,
                isRead = false
            )
            batch.set(notifRef, notif.toCreateMap())

            batch.commit().await()
            Result.success(messageId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    // --- 6. Safety: Reports, Blocks, and Notifications ---

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String
    ): Result<String> {
        val uid = requireUserId()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val reportRef = db.collection("reports").document(reportId)
        return try {
            val report = ContentReport(
                id = reportId,
                reporterUid = uid,
                targetType = targetType,
                targetId = targetId,
                reason = reason,
                details = details.trim().take(1000).ifEmpty { "Reported via Anonymousnet Safety Center" },
                status = "submitted"
            )
            reportRef.set(report.toCreateMap()).await()
            Result.success(reportId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportRef.path)
            Result.failure(e)
        }
    }

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUserRef>> {
        val path = "users/$userId/blocks"
        return db.collection("users").document(userId).collection("blocks")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(BlockedUserRef::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun blockAuthor(blockedUid: String, blockedLabel: String = "Anonymous"): Result<Unit> {
        val uid = requireUserId()
        val safeBlockedId = blockedUid.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_").take(120).ifEmpty { "anon_blocked" }
        val blockRef = db.collection("users").document(uid).collection("blocks").document(safeBlockedId)
        return try {
            val ref = BlockedUserRef(
                userId = uid,
                blockedUid = safeBlockedId,
                blockedLabel = blockedLabel.take(60)
            )
            blockRef.set(ref.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, blockRef.path)
            Result.failure(e)
        }
    }

    suspend fun unblockAuthor(blockedUid: String): Result<Unit> {
        val uid = requireUserId()
        val blockRef = db.collection("users").document(uid).collection("blocks").document(blockedUid)
        return try {
            blockRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, blockRef.path)
            Result.failure(e)
        }
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> {
        val path = "users/$userId/notifications"
        return db.collection("users").document(userId).collection("notifications")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val uid = requireUserId()
        val notifRef = db.collection("users").document(uid).collection("notifications").document(notificationId)
        return try {
            notifRef.update(
                mapOf(
                    "isRead" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, notifRef.path)
            Result.failure(e)
        }
    }

    suspend fun seedInitialConfessionsIfNeeded(): Result<Unit> {
        val uid = requireUserId()
        val postsPath = "posts"
        return try {
            val existing = db.collection(postsPath)
                .whereEqualTo("status", "published")
                .limit(1)
                .get()
                .await()
            if (!existing.isEmpty) {
                return Result.success(Unit)
            }

            val seedItems = listOf(
                ConfessionPost(
                    id = "seed_post_1",
                    authorUid = uid,
                    anonymousLabel = "Anonymous",
                    anonymousHandle = "@anon_2h",
                    isAnonymous = true,
                    text = "Sometimes I just want to run away from everything... 💔",
                    type = "text",
                    category = "Feelings",
                    topics = "life,feelings,honest",
                    status = "published",
                    likeCount = 842L,
                    commentCount = 214L,
                    viewCount = 3400L
                ),
                ConfessionPost(
                    id = "seed_post_2",
                    authorUid = uid,
                    anonymousLabel = "Anonymous",
                    anonymousHandle = "@anon_3h",
                    isAnonymous = true,
                    text = "Life is better with good coffee and good vibes. ☕✨",
                    type = "text",
                    category = "Coffee",
                    topics = "coffee,vibes,gratitude",
                    status = "published",
                    likeCount = 627L,
                    commentCount = 128L,
                    viewCount = 2150L
                ),
                ConfessionPost(
                    id = "seed_explore_1",
                    authorUid = uid,
                    anonymousLabel = "Anonymous",
                    anonymousHandle = "@anon_sunset",
                    isAnonymous = true,
                    text = "Watching the purple sunset sky made me realize it is okay to let go of old chapters and start fresh.",
                    type = "image",
                    category = "Life",
                    mediaUri = "preset://sunset_sky",
                    mediaCaption = "Just let it go... ❤️",
                    topics = "healing,sunset,peace",
                    status = "published",
                    likeCount = 1200L,
                    commentCount = 389L,
                    viewCount = 4800L
                ),
                ConfessionPost(
                    id = "seed_explore_2",
                    authorUid = uid,
                    anonymousLabel = "Anonymous",
                    anonymousHandle = "@anon_latte",
                    isAnonymous = true,
                    text = "Quiet mornings with warm latte art before the world wakes up are my secret therapy.",
                    type = "image",
                    category = "Coffee",
                    mediaUri = "preset://coffee_cup",
                    mediaCaption = "Coffee heals everything ☕",
                    topics = "coffee,morning,calm",
                    status = "published",
                    likeCount = 892L,
                    commentCount = 176L,
                    viewCount = 2900L
                ),
                ConfessionPost(
                    id = "seed_explore_3",
                    authorUid = uid,
                    anonymousLabel = "Anonymous",
                    anonymousHandle = "@anon_dreamer",
                    isAnonymous = true,
                    text = "No one in my circle knows I have been secretly building my own studio after work every night. Dream big... ✨",
                    type = "video",
                    category = "Dreams",
                    mediaUri = "preset://dream_silhouette",
                    mediaCaption = "Dream big... ✨",
                    mediaDurationSec = 42L,
                    topics = "dreams,ambition,night",
                    status = "published",
                    likeCount = 764L,
                    commentCount = 132L,
                    viewCount = 2400L
                ),
                ConfessionPost(
                    id = "seed_explore_4",
                    authorUid = uid,
                    anonymousLabel = "Anonymous",
                    anonymousHandle = "@anon_birds",
                    isAnonymous = true,
                    text = "Recorded the calm evening breeze by the coast today. Choosing happiness one breath at a time.",
                    type = "audio",
                    category = "Mindfulness",
                    mediaUri = "preset://birds_sky",
                    mediaCaption = "Be happy ❤️",
                    mediaDurationSec = 28L,
                    topics = "happiness,mindfulness,audio",
                    status = "published",
                    likeCount = 621L,
                    commentCount = 98L,
                    viewCount = 1950L
                )
            )

            val batch = db.batch()
            for (item in seedItems) {
                val ref = db.collection(postsPath).document(item.id)
                batch.set(ref, item.toCreateMap())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, postsPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAccountAndData(): Result<Unit> {
        val uid = requireUserId()
        return try {
            val myPosts = db.collection("posts")
                .whereEqualTo("authorUid", uid)
                .get()
                .await()
            val batch = db.batch()
            for (doc in myPosts.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(db.collection("publicProfiles").document(uid))
            batch.delete(db.collection("users").document(uid))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
