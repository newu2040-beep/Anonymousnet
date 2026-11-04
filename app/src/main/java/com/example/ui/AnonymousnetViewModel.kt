package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AnonymousnetRepository
import com.example.data.BlockedUserRef
import com.example.data.ConfessionPost
import com.example.data.Conversation
import com.example.data.DirectMessage
import com.example.data.FollowRef
import com.example.data.PostComment
import com.example.data.PublicProfile
import com.example.data.SavedOrLikedPostRef
import com.example.data.UserAccount
import com.example.data.UserNotification
import com.example.data.UsernameAvailability
import com.google.firebase.Timestamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainTab {
    HOME,
    EXPLORE,
    CREATE,
    ACTIVITY,
    PROFILE
}

enum class FeedSegment(val label: String) {
    FOR_YOU("For You"),
    POPULAR("Popular"),
    FOLLOWING("Following")
}

enum class ExploreFilter(val label: String, val typeKey: String?) {
    ALL("All", null),
    TEXT("Text", "text"),
    IMAGE("Image", "image"),
    VIDEO("Video", "video"),
    AUDIO("Audio", "audio")
}

enum class ExploreSort(val label: String) {
    POPULAR("Popular"),
    RECENT("Recent")
}

sealed interface SubScreen {
    data class PostDetail(val postId: String) : SubScreen
    data class ConversationChat(val conversationId: String, val otherUserUid: String, val otherUserHandle: String) : SubScreen
    data object MyPosts : SubScreen
    data object SavedPosts : SubScreen
    data object LikedPosts : SubScreen
    data object BlockedUsers : SubScreen
    data object StayAnonymousInfo : SubScreen
    data class ReportDialog(val targetType: String, val targetId: String, val targetLabel: String) : SubScreen
    data object HelpAndSupport : SubScreen
    data object AboutAnonymousnet : SubScreen
    data object TermsAndPrivacy : SubScreen
}

data class DraftComposerState(
    val text: String = "",
    val selectedType: String = "image", // image, video, audio, other, text
    val selectedCategory: String = "General",
    val keepAnonymous: Boolean = true,
    val mediaUri: String? = null,
    val mediaCaption: String = "",
    val mediaDurationSec: Long? = null,
    val mediaMimeType: String? = null,
    val topics: String = "vibes,confession",
    val contentWarning: String = "",
    val isPublishing: Boolean = false,
    val isRecordingAudio: Boolean = false,
    val recordedSeconds: Int = 0
)

class AnonymousnetViewModel(
    private val repository: AnonymousnetRepository,
    private val currentUserId: String,
    initialGuestMode: Boolean = false
) : ViewModel() {

    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _subScreenStack = MutableStateFlow<List<SubScreen>>(emptyList())
    val subScreenStack: StateFlow<List<SubScreen>> = _subScreenStack.asStateFlow()

    private val _feedSegment = MutableStateFlow(FeedSegment.FOR_YOU)
    val feedSegment: StateFlow<FeedSegment> = _feedSegment.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _exploreFilter = MutableStateFlow(ExploreFilter.ALL)
    val exploreFilter: StateFlow<ExploreFilter> = _exploreFilter.asStateFlow()

    private val _exploreSort = MutableStateFlow(ExploreSort.POPULAR)
    val exploreSort: StateFlow<ExploreSort> = _exploreSort.asStateFlow()

    private val _exploreSearchQuery = MutableStateFlow("")
    val exploreSearchQuery: StateFlow<String> = _exploreSearchQuery.asStateFlow()

    private val _isExploreSearchVisible = MutableStateFlow(false)
    val isExploreSearchVisible: StateFlow<Boolean> = _isExploreSearchVisible.asStateFlow()

    private val _composerState = MutableStateFlow(DraftComposerState())
    val composerState: StateFlow<DraftComposerState> = _composerState.asStateFlow()

    private val _hiddenPostIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenPostIds: StateFlow<Set<String>> = _hiddenPostIds.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _activePostComments = MutableStateFlow<List<PostComment>>(emptyList())
    val activePostComments: StateFlow<List<PostComment>> = _activePostComments.asStateFlow()
    private var commentsJob: Job? = null

    private val _activeChatMessages = MutableStateFlow<List<DirectMessage>>(emptyList())
    val activeChatMessages: StateFlow<List<DirectMessage>> = _activeChatMessages.asStateFlow()
    private var chatJob: Job? = null

    private val _usernameAvailability = MutableStateFlow<UsernameAvailability>(UsernameAvailability.Idle)
    val usernameAvailability: StateFlow<UsernameAvailability> = _usernameAvailability.asStateFlow()
    private var checkUsernameJob: Job? = null

    // Two-tier Flow error handling for all Firestore streams
    val userAccountState: StateFlow<UiState<UserAccount?>> = repository.observeUserAccount(currentUserId)
        .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing user account", e)
            emit(UiState.Error(e.message ?: "Failed to load user account"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val publicProfileState: StateFlow<UiState<PublicProfile?>> = repository.observePublicProfile(currentUserId)
        .map<PublicProfile?, UiState<PublicProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing public profile", e)
            emit(UiState.Error(e.message ?: "Failed to load public profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val publishedPostsState: StateFlow<UiState<List<ConfessionPost>>> = repository.observePublishedPosts()
        .map<List<ConfessionPost>, UiState<List<ConfessionPost>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing published posts", e)
            emit(UiState.Error(e.message ?: "Failed to load confession feed"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val myPostsState: StateFlow<UiState<List<ConfessionPost>>> = repository.observeMyPosts(currentUserId)
        .map<List<ConfessionPost>, UiState<List<ConfessionPost>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing user posts", e)
            emit(UiState.Error(e.message ?: "Failed to load your posts"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    val likedPostsState: StateFlow<List<SavedOrLikedPostRef>> = repository.observeLikedPosts(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing liked posts", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val savedPostsState: StateFlow<List<SavedOrLikedPostRef>> = repository.observeSavedPosts(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing saved posts", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val followingListState: StateFlow<List<FollowRef>> = repository.observeFollowing(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing following", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val followedUids: StateFlow<Set<String>> = followingListState
        .map { list -> list.map { it.targetUid }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val conversationsState: StateFlow<List<Conversation>> = repository.observeConversations(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing conversations", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val blockedUsersState: StateFlow<List<BlockedUserRef>> = repository.observeBlockedUsers(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing blocked users", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val notificationsState: StateFlow<List<UserNotification>> = repository.observeNotifications(currentUserId)
        .catch { e ->
            Log.w(TAG, "Error observing notifications", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val likedPostIds: StateFlow<Set<String>> = likedPostsState
        .map { list -> list.map { it.postId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val savedPostIds: StateFlow<Set<String>> = savedPostsState
        .map { list -> list.map { it.postId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val blockedUids: StateFlow<Set<String>> = blockedUsersState
        .map { list -> list.map { it.blockedUid }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    // All available post categories derived from published posts
    val availableCategories: StateFlow<List<String>> = publishedPostsState.map { state ->
        val defaults = listOf("General", "Life", "Feelings", "Coffee", "Dreams", "Mindfulness", "Vibes")
        if (state is UiState.Success) {
            val fromPosts = state.data.map { it.category.trim() }.filter { it.isNotBlank() }
            (defaults + fromPosts).distinct()
        } else defaults
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), listOf("General", "Life", "Feelings", "Coffee", "Dreams", "Mindfulness", "Vibes"))

    private data class FeedFilterParams(
        val segment: FeedSegment,
        val category: String?,
        val followed: Set<String>,
        val blocked: Set<String>,
        val hidden: Set<String>
    )

    private val feedFilterParams = combine(
        _feedSegment,
        _selectedCategoryFilter,
        followedUids,
        blockedUids,
        _hiddenPostIds
    ) { segment, category, followed, blocked, hidden ->
        FeedFilterParams(segment, category, followed, blocked, hidden)
    }

    val filteredFeedPosts: StateFlow<UiState<List<ConfessionPost>>> = combine(
        publishedPostsState,
        feedFilterParams
    ) { postsState, params ->
        when (postsState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> postsState
            is UiState.Success -> {
                val visible = postsState.data.filter { post ->
                    post.id !in params.hidden && post.authorUid !in params.blocked && post.status == "published" &&
                        (params.category == null || post.category.equals(params.category, ignoreCase = true))
                }
                val sorted = when (params.segment) {
                    FeedSegment.FOR_YOU -> visible
                    FeedSegment.POPULAR -> visible.sortedByDescending { it.likeCount + it.commentCount * 2 }
                    FeedSegment.FOLLOWING -> {
                        val followedMatches = visible.filter { it.authorUid in params.followed }
                        followedMatches.ifEmpty {
                            // If user is not following anyone yet, show active community creators
                            visible
                        }
                    }
                }
                UiState.Success(sorted)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    private data class ExploreFilterParams(
        val filter: ExploreFilter,
        val sort: ExploreSort,
        val query: String,
        val category: String?,
        val blocked: Set<String>
    )

    private val exploreFilterParams = combine(
        _exploreFilter,
        _exploreSort,
        _exploreSearchQuery,
        _selectedCategoryFilter,
        blockedUids
    ) { filter, sort, query, category, blocked ->
        ExploreFilterParams(filter, sort, query, category, blocked)
    }

    val filteredExplorePosts: StateFlow<UiState<List<ConfessionPost>>> = combine(
        publishedPostsState,
        exploreFilterParams
    ) { postsState, params ->
        when (postsState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> postsState
            is UiState.Success -> {
                val q = params.query.trim().lowercase()
                val filtered = postsState.data.filter { post ->
                    post.authorUid !in params.blocked &&
                        post.status == "published" &&
                        (params.filter.typeKey == null || post.type.equals(params.filter.typeKey, ignoreCase = true)) &&
                        (params.category == null || post.category.equals(params.category, ignoreCase = true)) &&
                        (q.isEmpty() ||
                            post.text.lowercase().contains(q) ||
                            post.category.lowercase().contains(q) ||
                            (post.mediaCaption?.lowercase()?.contains(q) == true) ||
                            (post.topics?.lowercase()?.contains(q) == true))
                }
                val sorted = when (params.sort) {
                    ExploreSort.POPULAR -> filtered.sortedByDescending { it.likeCount + it.commentCount }
                    ExploreSort.RECENT -> filtered.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                UiState.Success(sorted)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

    init {
        viewModelScope.launch {
            repository.ensureUserAndPublicProfile(guestMode = initialGuestMode)
            repository.seedInitialConfessionsIfNeeded()
        }
    }

    fun selectTab(tab: MainTab) {
        _subScreenStack.value = emptyList()
        _currentTab.value = tab
    }

    fun pushSubScreen(subScreen: SubScreen) {
        _subScreenStack.update { it + subScreen }
        if (subScreen is SubScreen.PostDetail) {
            observePostComments(subScreen.postId)
        } else if (subScreen is SubScreen.ConversationChat) {
            observeChatMessages(subScreen.conversationId)
        }
    }

    fun popSubScreen(): Boolean {
        val current = _subScreenStack.value
        if (current.isNotEmpty()) {
            _subScreenStack.value = current.dropLast(1)
            return true
        }
        if (_currentTab.value != MainTab.HOME) {
            _currentTab.value = MainTab.HOME
            return true
        }
        return false
    }

    private fun observePostComments(postId: String) {
        commentsJob?.cancel()
        commentsJob = viewModelScope.launch {
            repository.observeComments(postId)
                .catch { e -> Log.w(TAG, "Error observing comments", e) }
                .collect { comments ->
                    _activePostComments.value = comments
                }
        }
    }

    private fun observeChatMessages(conversationId: String) {
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            repository.observeMessages(conversationId)
                .catch { e -> Log.w(TAG, "Error observing chat messages", e) }
                .collect { msgs ->
                    _activeChatMessages.value = msgs
                }
        }
    }

    fun setFeedSegment(segment: FeedSegment) {
        _feedSegment.value = segment
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setExploreFilter(filter: ExploreFilter) {
        _exploreFilter.value = filter
    }

    fun setExploreSort(sort: ExploreSort) {
        _exploreSort.value = sort
    }

    fun setExploreSearchQuery(query: String) {
        _exploreSearchQuery.value = query
    }

    fun toggleExploreSearch() {
        _isExploreSearchVisible.update { !it }
        if (!_isExploreSearchVisible.value) {
            _exploreSearchQuery.value = ""
        }
    }

    // --- Composer Actions ---

    fun updateComposerText(text: String) {
        if (text.length <= 500) {
            _composerState.update { it.copy(text = text) }
        }
    }

    fun updateComposerType(type: String) {
        _composerState.update { it.copy(selectedType = type) }
    }

    fun updateComposerCategory(category: String) {
        _composerState.update { it.copy(selectedCategory = category) }
    }

    fun updateKeepAnonymous(keepAnon: Boolean) {
        _composerState.update { it.copy(keepAnonymous = keepAnon) }
    }

    fun updateComposerMedia(
        uri: String?,
        caption: String = "",
        durationSec: Long? = null,
        mimeType: String? = null
    ) {
        _composerState.update {
            it.copy(
                mediaUri = uri,
                mediaCaption = caption.ifEmpty { it.mediaCaption },
                mediaDurationSec = durationSec,
                mediaMimeType = mimeType
            )
        }
    }

    fun updateComposerTopics(topics: String) {
        _composerState.update { it.copy(topics = topics.take(120)) }
    }

    fun clearComposerMedia() {
        _composerState.update {
            it.copy(
                mediaUri = null,
                mediaCaption = "",
                mediaDurationSec = null,
                mediaMimeType = null,
                isRecordingAudio = false,
                recordedSeconds = 0
            )
        }
    }

    fun setAudioRecordingState(isRecording: Boolean, seconds: Int) {
        _composerState.update {
            it.copy(isRecordingAudio = isRecording, recordedSeconds = seconds)
        }
    }

    fun publishConfession(asDraft: Boolean = false) {
        val state = _composerState.value
        val trimmed = state.text.trim()
        if (trimmed.isEmpty()) {
            showSnackbar("Please write your confession before publishing.")
            return
        }
        _composerState.update { it.copy(isPublishing = true) }

        val profile = (publicProfileState.value as? UiState.Success)?.data
        val handle = profile?.anonymousHandle ?: "@anon_user"
        val displayLabel = if (state.keepAnonymous) "Anonymous" else (profile?.displayName ?: "Anonymousnet")
        val finalType = if (state.mediaUri != null) state.selectedType else state.selectedType.ifEmpty { "text" }

        viewModelScope.launch {
            val result = repository.createPost(
                text = trimmed,
                type = finalType,
                category = state.selectedCategory.ifBlank { "General" },
                isAnonymous = state.keepAnonymous,
                anonymousLabel = displayLabel,
                anonymousHandle = handle,
                mediaUri = state.mediaUri,
                mediaCaption = state.mediaCaption.ifBlank { trimmed.take(40) },
                mediaDurationSec = state.mediaDurationSec,
                mediaMimeType = state.mediaMimeType,
                topics = state.topics,
                contentWarning = state.contentWarning.ifBlank { null },
                status = if (asDraft) "draft" else "published"
            )
            _composerState.update { it.copy(isPublishing = false) }
            result.onSuccess {
                _composerState.value = DraftComposerState()
                if (asDraft) {
                    showSnackbar("Draft saved privately to your account.")
                } else {
                    showSnackbar("Confession published anonymously!")
                    _currentTab.value = MainTab.HOME
                }
            }.onFailure { err ->
                showSnackbar(err.localizedMessage ?: "Failed to publish confession. Draft preserved.")
            }
        }
    }

    fun loadDraftIntoComposer(draft: ConfessionPost) {
        _composerState.value = DraftComposerState(
            text = draft.text,
            selectedType = draft.type,
            selectedCategory = draft.category,
            keepAnonymous = draft.isAnonymous,
            mediaUri = draft.mediaUri,
            mediaCaption = draft.mediaCaption ?: "",
            mediaDurationSec = draft.mediaDurationSec,
            mediaMimeType = draft.mediaMimeType,
            topics = draft.topics ?: "vibes,confession",
            contentWarning = draft.contentWarning ?: ""
        )
        _subScreenStack.value = emptyList()
        _currentTab.value = MainTab.CREATE
        showSnackbar("Draft loaded into composer.")
    }

    // --- Post Engagement Actions ---

    fun toggleLike(post: ConfessionPost) {
        val isLiked = post.id in likedPostIds.value
        viewModelScope.launch {
            repository.toggleLike(post, isLiked).onFailure {
                showSnackbar("Could not update like state.")
            }
        }
    }

    fun toggleSave(post: ConfessionPost) {
        val isSaved = post.id in savedPostIds.value
        viewModelScope.launch {
            repository.toggleSave(post, isSaved).onSuccess {
                showSnackbar(if (isSaved) "Removed from Saved" else "Saved privately to your profile")
            }.onFailure {
                showSnackbar("Could not update saved state.")
            }
        }
    }

    fun toggleFollowUser(targetUid: String, targetHandle: String) {
        val isFollowing = targetUid in followedUids.value
        viewModelScope.launch {
            repository.toggleFollow(targetUid, targetHandle, isFollowing).onSuccess {
                showSnackbar(if (isFollowing) "Unfollowed $targetHandle" else "Following $targetHandle")
            }.onFailure {
                showSnackbar("Could not update follow state.")
            }
        }
    }

    fun startPrivateMessageRequest(recipientUid: String, recipientHandle: String, messageText: String) {
        val trimmed = messageText.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.sendDirectMessageRequest(recipientUid, recipientHandle, trimmed).onSuccess { convId ->
                showSnackbar("Message request sent to $recipientHandle")
                pushSubScreen(SubScreen.ConversationChat(convId, recipientUid, recipientHandle))
            }.onFailure { e ->
                showSnackbar(e.message ?: "Could not send message request.")
            }
        }
    }

    fun respondToDmRequest(conversationId: String, accept: Boolean, block: Boolean = false) {
        viewModelScope.launch {
            repository.respondToMessageRequest(conversationId, accept, block).onSuccess {
                showSnackbar(
                    when {
                        block -> "Conversation blocked"
                        accept -> "Message request accepted"
                        else -> "Message request declined"
                    }
                )
            }.onFailure {
                showSnackbar("Failed to update message request.")
            }
        }
    }

    fun sendChatMessage(conversationId: String, recipientUid: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.sendMessageInConversation(conversationId, recipientUid, trimmed).onFailure { e ->
                showSnackbar(e.message ?: "Could not send message.")
            }
        }
    }

    fun addCommentToPost(post: ConfessionPost, commentText: String) {
        val trimmed = commentText.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addComment(post, trimmed, "Anonymous").onSuccess {
                showSnackbar("Anonymous comment posted")
            }.onFailure {
                showSnackbar("Failed to post comment")
            }
        }
    }

    fun deleteComment(post: ConfessionPost, commentId: String) {
        viewModelScope.launch {
            repository.deleteComment(post, commentId).onSuccess {
                showSnackbar("Comment deleted")
            }
        }
    }

    fun deleteOwnPost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).onSuccess {
                showSnackbar("Confession deleted")
                if (_subScreenStack.value.lastOrNull() is SubScreen.PostDetail) {
                    popSubScreen()
                }
            }.onFailure {
                showSnackbar("Could not delete confession")
            }
        }
    }

    fun hidePostFromFeed(postId: String) {
        _hiddenPostIds.update { it + postId }
        showSnackbar("Post hidden from your feed")
    }

    fun blockAuthor(authorUid: String, authorLabel: String) {
        if (authorUid == currentUserId) {
            showSnackbar("You cannot block your own anonymous identity.")
            return
        }
        viewModelScope.launch {
            repository.blockAuthor(authorUid, authorLabel).onSuccess {
                showSnackbar("Blocked author. Their confessions are now hidden.")
            }
        }
    }

    fun unblockAuthor(blockedUid: String) {
        viewModelScope.launch {
            repository.unblockAuthor(blockedUid).onSuccess {
                showSnackbar("Unblocked user")
            }
        }
    }

    fun submitSafetyReport(targetType: String, targetId: String, reason: String, details: String) {
        viewModelScope.launch {
            repository.submitReport(targetType, targetId, reason, details).onSuccess {
                popSubScreen()
                showSnackbar("Report received. Our safety team will review this content.")
            }.onFailure {
                showSnackbar("Failed to submit report. Please try again.")
            }
        }
    }

    fun toggleThemeMode(currentlyDark: Boolean) {
        val nextMode = if (currentlyDark) "light" else "dark"
        viewModelScope.launch {
            repository.updateUserPreferences(themeMode = nextMode)
        }
    }

    fun setColorTheme(themeKey: String) {
        viewModelScope.launch {
            repository.updateUserPreferences(colorTheme = themeKey)
        }
    }

    fun setUseSystemFont(useSys: Boolean) {
        viewModelScope.launch {
            repository.updateUserPreferences(useSystemFont = useSys)
        }
    }

    fun updatePrivacyDefaults(keepAnonymousDefault: Boolean, notificationsEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateUserPreferences(
                keepAnonymousDefault = keepAnonymousDefault,
                notificationsEnabled = notificationsEnabled
            ).onSuccess {
                showSnackbar("Privacy preferences updated")
            }
        }
    }

    fun linkGuestSessionToPermanentGoogle() {
        viewModelScope.launch {
            repository.updateUserPreferences(guest = false).onSuccess {
                showSnackbar("Guest session linked to your Google account!")
            }
        }
    }

    fun checkUsername(rawHandle: String) {
        checkUsernameJob?.cancel()
        val trimmed = rawHandle.trim()
        if (trimmed.isEmpty()) {
            _usernameAvailability.value = UsernameAvailability.Empty
            return
        }
        _usernameAvailability.value = UsernameAvailability.Checking
        checkUsernameJob = viewModelScope.launch {
            delay(280L)
            val result = repository.checkUsernameAvailability(trimmed)
            _usernameAvailability.value = result.getOrElse {
                UsernameAvailability.InvalidFormat(it.message ?: "Failed to verify username")
            }
        }
    }

    fun resetUsernameCheck() {
        checkUsernameJob?.cancel()
        _usernameAvailability.value = UsernameAvailability.Idle
    }

    fun updateProfileAndUsername(
        newHandle: String,
        newDisplayName: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            repository.updateUsernameAndDisplayName(newHandle, newDisplayName).onSuccess {
                val normalized = repository.normalizeHandle(newHandle)
                showSnackbar("Username successfully updated to @$normalized!")
                onComplete(true)
            }.onFailure { err ->
                showSnackbar(err.message ?: "Could not update username.")
                onComplete(false)
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun recordPostView(post: ConfessionPost) {
        viewModelScope.launch {
            repository.incrementPostView(post)
        }
    }

    fun deleteAccountData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAccountAndData().onSuccess {
                onComplete()
            }.onFailure {
                showSnackbar("Failed to delete account data")
            }
        }
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private companion object {
        const val TAG = "AnonymousnetVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
