package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AnonymousnetRepository(
    private val db: FirebaseFirestore
) {
    private val auth get() = Firebase.auth

    private fun handleFirestoreError(
        error: Exception,
        operationType: OperationType,
        path: String?
    ) {
        val currentUser = try {
            auth.currentUser
        } catch (_: Exception) {
            null
        }
        val providerInfo = JSONArray().apply {
            currentUser?.providerData?.forEach { profile ->
                put(
                    JSONObject().apply {
                        put("providerId", profile.providerId)
                        put("displayName", profile.displayName)
                        put("email", profile.email)
                        put("photoUrl", profile.photoUrl?.toString())
                    }
                )
            }
        }

        val authInfo = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("isAnonymous", currentUser?.isAnonymous)
            put(
                "tenantId",
                try {
                    currentUser?.tenantId
                } catch (_: Throwable) {
                    null
                }
            )
            put("providerInfo", providerInfo)
        }

        val errInfo = JSONObject().apply {
            put("error", error.message ?: error.toString())
            put("operationType", operationType.name.lowercase())
            put("path", path)
            put("authInfo", authInfo)
        }

        Log.e("FirestoreError", errInfo.toString(), error)
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to perform Firestore operations.")
    }

    // --- 1. User Account & Public Profile Initialization ---

    suspend fun ensureUserAndPublicProfile(
        guestMode: Boolean = false,
        preferredHandle: String = "@anon_user"
    ): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        val profileRef = db.collection("publicProfiles").document(uid)
        return try {
            val userSnap = userRef.get().await()
            val profileSnap = profileRef.get().await()

            val batch = db.batch()
            var needsCommit = false

            if (!userSnap.exists()) {
                val account = UserAccount(
                    id = uid,
                    uid = uid,
                    anonymousHandle = preferredHandle,
                    guest = guestMode,
                    themeMode = "light",
                    colorTheme = "custom_accent",
                    fontFamily = "plus_jakarta_sans",
                    useSystemFont = false,
                    compactMode = false,
                    keepAnonymousDefault = true,
                    notificationsEnabled = true,
                    safetyGuidelinesAccepted = true
                )
                batch.set(userRef, account.toCreateMap())
                needsCommit = true
            }

            if (!profileSnap.exists()) {
                val publicProfile = PublicProfile(
                    id = uid,
                    uid = uid,
                    displayName = "Anonymousnet",
                    surname = "",
                    anonymousHandle = preferredHandle,
                    gender = "",
                    age = 0,
                    interests = "",
                    avatarKey = "hat_glasses",
                    avatarUri = "",
                    postCount = 0L,
                    viewCount = 0L,
                    likeCount = 0L,
                    followersCount = 0L,
                    followingCount = 0L
                )
                batch.set(profileRef, publicProfile.toCreateMap())
                needsCommit = true
            }

            if (needsCommit) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    fun observeUserAccount(userId: String): Flow<UserAccount?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    fun observePublicProfile(userId: String): Flow<PublicProfile?> {
        val path = "publicProfiles/$userId"
        return db.collection("publicProfiles").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun updateUserPreferences(
        themeMode: String? = null,
        colorTheme: String? = null,
        fontFamily: String? = null,
        useSystemFont: Boolean? = null,
        compactMode: Boolean? = null,
        keepAnonymousDefault: Boolean? = null,
        notificationsEnabled: Boolean? = null,
        guest: Boolean? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val userRef = db.collection("users").document(uid)
        return try {
            val snap = userRef.get().await()
            if (!snap.exists()) {
                ensureUserAndPublicProfile()
            }
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (themeMode != null) updates["themeMode"] = themeMode
            if (colorTheme != null) updates["colorTheme"] = colorTheme
            if (fontFamily != null) updates["fontFamily"] = fontFamily
            if (useSystemFont != null) updates["useSystemFont"] = useSystemFont
            if (compactMode != null) updates["compactMode"] = compactMode
            if (keepAnonymousDefault != null) updates["keepAnonymousDefault"] = keepAnonymousDefault
            if (notificationsEnabled != null) updates["notificationsEnabled"] = notificationsEnabled
            if (guest != null) updates["guest"] = guest

            userRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            Result.failure(e)
        }
    }

    fun normalizeHandle(raw: String): String {
        val cleaned = raw.trim()
            .removePrefix("@")
            .lowercase()
            .filter { it.isLetterOrDigit() || it == '_' }
            .take(30)
        return cleaned
    }

    suspend fun checkUsernameAvailability(rawHandle: String): Result<UsernameAvailability> {
        val uid = requireUserId()
        val normalized = normalizeHandle(rawHandle)
        if (normalized.isEmpty()) {
            return Result.success(UsernameAvailability.Empty)
        }
        if (normalized.length < 3) {
            return Result.success(
                UsernameAvailability.InvalidFormat("Username must be at least 3 characters (letters, numbers, or _).")
            )
        }
        val formattedHandle = "@$normalized"
        return try {
            val existingSnap = db.collection("publicProfiles")
                .whereEqualTo("anonymousHandle", formattedHandle)
                .get()
                .await()

            val takenByOther = existingSnap.documents.any { doc -> doc.id != uid }
            val ownedByCurrent = existingSnap.documents.any { doc -> doc.id == uid }

            val status = when {
                takenByOther -> UsernameAvailability.Taken(normalized)
                ownedByCurrent -> UsernameAvailability.IsCurrentUser(normalized)
                else -> UsernameAvailability.Available(normalized)
            }
            Result.success(status)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "publicProfiles")
            Result.failure(e)
        }
    }

    suspend fun updateFullProfile(
        rawHandle: String,
        newDisplayName: String,
        newSurname: String = "",
        newGender: String = "",
        newAge: Int = 0,
        newInterests: String = "",
        newAvatarUri: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val normalized = normalizeHandle(rawHandle)
        if (normalized.length < 3) {
            return Result.failure(IllegalArgumentException("Username must be at least 3 characters."))
        }
        val formattedHandle = "@$normalized"
        val cleanDisplayName = newDisplayName.trim().take(60).ifEmpty { "Anonymousnet" }
        val cleanSurname = newSurname.trim().take(60)
        val cleanGender = if (newGender in listOf("Male", "Female")) newGender else ""
        val cleanAge = newAge.coerceIn(0, 120)
        val cleanInterests = newInterests.trim().take(500)
        val cleanAvatarUri = newAvatarUri.trim().take(2048)

        val userRef = db.collection("users").document(uid)
        val profileRef = db.collection("publicProfiles").document(uid)

        return try {
            ensureUserAndPublicProfile()
            val availability = checkUsernameAvailability(normalized).getOrThrow()
            if (availability is UsernameAvailability.Taken) {
                return Result.failure(IllegalStateException("Username $formattedHandle is already taken."))
            }

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "anonymousHandle" to formattedHandle,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                profileRef,
                mapOf(
                    "displayName" to cleanDisplayName,
                    "surname" to cleanSurname,
                    "anonymousHandle" to formattedHandle,
                    "gender" to cleanGender,
                    "age" to cleanAge,
                    "interests" to cleanInterests,
                    "avatarUri" to cleanAvatarUri,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Propagate the updated handle, display name, and optional avatar to user's authored posts
            val myPostsSnap = db.collection("posts")
                .whereEqualTo("authorUid", uid)
                .get()
                .await()

            val fullLabel = if (cleanSurname.isNotEmpty()) "$cleanDisplayName $cleanSurname".take(60) else cleanDisplayName
            for (postDoc in myPostsSnap.documents) {
                val isAnon = postDoc.getBoolean("isAnonymous") ?: true
                val updatedLabel = if (isAnon) "Anonymous" else fullLabel
                val postUpdates = mutableMapOf<String, Any>(
                    "anonymousHandle" to formattedHandle,
                    "anonymousLabel" to updatedLabel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanAvatarUri.isNotEmpty()) {
                    postUpdates["authorAvatarUri"] = cleanAvatarUri
                }
                batch.update(postDoc.reference, postUpdates)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, profileRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateUsernameAndDisplayName(
        rawHandle: String,
        newDisplayName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val existingProfile = try {
            db.collection("publicProfiles").document(uid).get().await()
                .toObject(PublicProfile::class.java)
        } catch (_: Exception) {
            null
        }
        return updateFullProfile(
            rawHandle = rawHandle,
            newDisplayName = newDisplayName,
            newSurname = existingProfile?.surname ?: "",
            newGender = existingProfile?.gender ?: "",
            newAge = existingProfile?.age ?: 0,
            newInterests = existingProfile?.interests ?: "",
            newAvatarUri = existingProfile?.avatarUri ?: ""
        )
    }

    // --- 2. Confession Feed & Creation (No Word Limits) ---

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
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createPost(
        text: String,
        type: String,
        category: String = "General",
        isAnonymous: Boolean = true,
        anonymousLabel: String = "Anonymous",
        anonymousHandle: String = "@anon_user",
        authorAvatarUri: String? = null,
        mediaUri: String? = null,
        mediaCaption: String? = null,
        mediaDurationSec: Long? = null,
        mediaMimeType: String? = null,
        topics: String? = null,
        contentWarning: String? = null,
        status: String = "published"
    ): Result<String> {
        val uid = requireUserId()
        val postId = "post_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val postRef = db.collection("posts").document(postId)
        val profileRef = db.collection("publicProfiles").document(uid)

        return try {
            ensureUserAndPublicProfile()
            val post = ConfessionPost(
                id = postId,
                authorUid = uid,
                anonymousLabel = if (isAnonymous) "Anonymous" else anonymousLabel,
                anonymousHandle = anonymousHandle,
                authorAvatarUri = authorAvatarUri?.takeIf { it.isNotBlank() },
                isAnonymous = isAnonymous,
                type = type,
                category = category.trim().take(60).ifEmpty { "General" },
                text = text.trim().take(50000),
                mediaUri = mediaUri,
                mediaCaption = mediaCaption?.trim()?.take(500),
                mediaDurationSec = mediaDurationSec,
                mediaMimeType = mediaMimeType,
                topics = topics?.trim()?.take(300),
                contentWarning = contentWarning?.trim()?.take(120),
                status = status,
                likeCount = 0L,
                commentCount = 0L,
                viewCount = 0L
            )

            val batch = db.batch()
            batch.set(postRef, post.toCreateMap())
            if (status == "published") {
                batch.update(
                    profileRef,
                    mapOf(
                        "postCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(postId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        return try {
            postRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/$postId (by $uid)")
            Result.failure(e)
        }
    }

    // --- 3. Likes, Saves, and Comments ---

    fun observeLikedPosts(userId: String): Flow<List<SavedOrLikedPostRef>> {
        val path = "users/$userId/likedPosts"
        return db.collection("users").document(userId).collection("likedPosts")
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
        val path = "users/$userId/savedPosts"
        return db.collection("users").document(userId).collection("savedPosts")
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
        val userLikedRef = db.collection("users").document(uid).collection("likedPosts").document(post.id)
        val postRef = db.collection("posts").document(post.id)
        val authorProfileRef = db.collection("publicProfiles").document(post.authorUid)

        return try {
            val batch = db.batch()
            if (isCurrentlyLiked) {
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
                val refObj = SavedOrLikedPostRef(
                    userId = uid,
                    postId = post.id,
                    postText = post.text.take(50000),
                    postType = post.type,
                    postMediaUri = post.mediaUri,
                    anonymousLabel = post.anonymousLabel
                )
                batch.set(userLikedRef, refObj.toCreateMap())
                val nextLikes = post.likeCount + 1L
                batch.update(
                    postRef,
                    mapOf(
                        "likeCount" to nextLikes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // Notify post author if not self
                if (post.authorUid != uid) {
                    val notifRef = db.collection("users").document(post.authorUid)
                        .collection("notifications").document("like_${post.id}_${uid.take(8)}")
                    val notif = UserNotification(
                        userId = post.authorUid,
                        type = "like",
                        title = "New Anonymous Like",
                        body = "Someone liked your confession: \"${post.text.take(60)}\"",
                        targetId = post.id,
                        isRead = false
                    )
                    batch.set(notifRef, notif.toCreateMap())
                }
            }
            batch.commit().await()

            // Best-effort update on author's public profile like counter
            try {
                authorProfileRef.update(
                    mapOf(
                        "likeCount" to FieldValue.increment(if (isCurrentlyLiked) -1L else 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userLikedRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleSave(post: ConfessionPost, isCurrentlySaved: Boolean): Result<Unit> {
        val uid = requireUserId()
        val userSavedRef = db.collection("users").document(uid).collection("savedPosts").document(post.id)
        return try {
            if (isCurrentlySaved) {
                userSavedRef.delete().await()
            } else {
                val refObj = SavedOrLikedPostRef(
                    userId = uid,
                    postId = post.id,
                    postText = post.text.take(50000),
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
                text = text.trim().take(10000),
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
            ensureUserAndPublicProfile()
            if (isCurrentlyFollowing) {
                followRef.delete().await()
                try {
                    myProfileRef.update(
                        mapOf(
                            "followingCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {}
                try {
                    targetProfileRef.update(
                        mapOf(
                            "followersCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {}
            } else {
                val follow = FollowRef(
                    followerUid = uid,
                    targetUid = targetUid,
                    targetHandle = targetHandle
                )
                followRef.set(follow.toCreateMap()).await()
                try {
                    myProfileRef.update(
                        mapOf(
                            "followingCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {}
                try {
                    targetProfileRef.update(
                        mapOf(
                            "followersCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } catch (_: Exception) {}

                try {
                    val notifRef = db.collection("users").document(targetUid).collection("notifications").document("follow_${uid.take(12)}")
                    val notif = UserNotification(
                        userId = targetUid,
                        type = "follow",
                        title = "New Anonymous Follower",
                        body = "Someone started following your anonymous confessions.",
                        targetId = uid,
                        isRead = false
                    )
                    notifRef.set(notif.toCreateMap()).await()
                } catch (_: Exception) {}
            }
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
                lastMessageText = initialText.trim().take(2000),
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
                text = initialText.trim().take(2000),
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
                text = text.trim().take(2000),
                mediaUri = mediaUri
            )
            batch.set(msgRef, msg.toCreateMap())
            batch.update(
                convRef,
                mapOf(
                    "lastMessageText" to text.trim().take(2000),
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
                details = details.trim().take(2000).ifEmpty { "Reported via Anonymousnet Safety Center" },
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
