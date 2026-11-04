package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AnonymousnetRepository
import com.example.ui.AnonymousnetViewModel
import com.example.ui.MainTab
import com.example.ui.SubScreen
import com.example.ui.UiState
import com.example.ui.components.AnonymousnetBottomBar
import com.example.ui.components.PushNotificationHelper
import com.example.ui.screens.ActivityNotificationsScreen
import com.example.ui.screens.BlockedUsersSubScreen
import com.example.ui.screens.ConversationChatSubScreen
import com.example.ui.screens.CreateConfessionScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.InfoDetailSubScreen
import com.example.ui.screens.PostDetailSubScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.ReportFlowSubScreen
import com.example.ui.screens.SavedOrLikedListSubScreen
import com.example.ui.screens.WelcomeAuthScreen
import com.example.ui.screens.signOutUser
import com.example.ui.theme.AnonymousnetTheme
import com.example.ui.theme.AppColorTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        PushNotificationHelper.ensureNotificationChannel(this)
        setContent {
            AnonymousnetAppRoot()
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AnonymousnetAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var localDarkTheme by rememberSaveable { mutableStateOf(false) }
    var selectedColorThemeName by rememberSaveable { mutableStateOf(AppColorTheme.VIOLET.name) }
    var useSystemFont by rememberSaveable { mutableStateOf(false) }
    var enteredAsGuest by rememberSaveable { mutableStateOf(false) }
    var requestedInitialPermissions by rememberSaveable { mutableStateOf(false) }

    val selectedColorTheme = remember(selectedColorThemeName) {
        AppColorTheme.entries.firstOrNull { it.name == selectedColorThemeName } ?: AppColorTheme.VIOLET
    }

    val initialPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        PushNotificationHelper.ensureNotificationChannel(context)
    }

    LaunchedEffect(Unit) {
        if (!requestedInitialPermissions) {
            requestedInitialPermissions = true
            val permissionsToRequest = mutableListOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            initialPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    val user = currentUser
    if (user == null) {
        // Screen 1 — Welcome and Authentication (no bottom navigation bar, matching reference)
        AnonymousnetTheme(
            darkTheme = localDarkTheme,
            colorTheme = selectedColorTheme,
            useSystemFont = useSystemFont
        ) {
            WelcomeAuthScreen(
                isDarkTheme = localDarkTheme,
                onToggleDarkTheme = { localDarkTheme = !localDarkTheme },
                onAuthSuccess = { guestMode ->
                    enteredAsGuest = guestMode
                }
            )
        }
    } else {
        // Authenticated session: Initialize ViewModel keyed by user.uid with custom Firestore database ID
        val viewModel: AnonymousnetViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val app = checkNotNull(this[APPLICATION_KEY]) {
                        "APPLICATION_KEY missing from CreationExtras"
                    }
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    AnonymousnetViewModel(
                        repository = AnonymousnetRepository(db),
                        currentUserId = user.uid,
                        initialGuestMode = enteredAsGuest
                    )
                }
            }
        )

        val userAccountState by viewModel.userAccountState.collectAsStateWithLifecycle()
        val userAccount = (userAccountState as? UiState.Success)?.data
        val isDark = if (userAccount != null) {
            userAccount.themeMode == "dark"
        } else {
            localDarkTheme
        }
        val activeColorTheme = remember(userAccount?.colorTheme, selectedColorThemeName) {
            if (!userAccount?.colorTheme.isNullOrBlank()) {
                AppColorTheme.fromKey(userAccount.colorTheme)
            } else {
                AppColorTheme.entries.firstOrNull { it.name == selectedColorThemeName } ?: AppColorTheme.VIOLET
            }
        }
        val activeUseSystemFont = userAccount?.useSystemFont ?: useSystemFont

        AnonymousnetTheme(
            darkTheme = isDark,
            colorTheme = activeColorTheme,
            useSystemFont = activeUseSystemFont
        ) {
            AuthenticatedMainScaffold(
                viewModel = viewModel,
                currentUserId = user.uid,
                isDarkTheme = isDark,
                selectedColorTheme = activeColorTheme,
                useSystemFont = activeUseSystemFont,
                onToggleDarkTheme = {
                    localDarkTheme = !isDark
                    viewModel.toggleThemeMode(isDark)
                },
                onSelectColorTheme = { newTheme ->
                    selectedColorThemeName = newTheme.name
                    viewModel.setColorTheme(newTheme.key)
                },
                onToggleSystemFont = { enabled ->
                    useSystemFont = enabled
                    viewModel.setUseSystemFont(enabled)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedMainScaffold(
    viewModel: AnonymousnetViewModel,
    currentUserId: String,
    isDarkTheme: Boolean,
    selectedColorTheme: AppColorTheme,
    useSystemFont: Boolean,
    onToggleDarkTheme: () -> Unit,
    onSelectColorTheme: (AppColorTheme) -> Unit,
    onToggleSystemFont: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val subScreenStack by viewModel.subScreenStack.collectAsStateWithLifecycle()
    val feedSegment by viewModel.feedSegment.collectAsStateWithLifecycle()
    val exploreFilter by viewModel.exploreFilter.collectAsStateWithLifecycle()
    val exploreSort by viewModel.exploreSort.collectAsStateWithLifecycle()
    val exploreSearchQuery by viewModel.exploreSearchQuery.collectAsStateWithLifecycle()
    val isExploreSearchVisible by viewModel.isExploreSearchVisible.collectAsStateWithLifecycle()
    val composerState by viewModel.composerState.collectAsStateWithLifecycle()

    val userAccountState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val publicProfileState by viewModel.publicProfileState.collectAsStateWithLifecycle()
    val filteredFeedPosts by viewModel.filteredFeedPosts.collectAsStateWithLifecycle()
    val filteredExplorePosts by viewModel.filteredExplorePosts.collectAsStateWithLifecycle()
    val publishedPostsState by viewModel.publishedPostsState.collectAsStateWithLifecycle()
    val myPostsState by viewModel.myPostsState.collectAsStateWithLifecycle()
    val likedPosts by viewModel.likedPostsState.collectAsStateWithLifecycle()
    val savedPosts by viewModel.savedPostsState.collectAsStateWithLifecycle()
    val blockedUsers by viewModel.blockedUsersState.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationsState.collectAsStateWithLifecycle()
    val followedUids by viewModel.followedUids.collectAsStateWithLifecycle()
    val followingList by viewModel.followingListState.collectAsStateWithLifecycle()
    val conversations by viewModel.conversationsState.collectAsStateWithLifecycle()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val savedPostIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    val activePostComments by viewModel.activePostComments.collectAsStateWithLifecycle()
    val usernameAvailability by viewModel.usernameAvailability.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val userAccount = (userAccountState as? UiState.Success)?.data

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            if (msg.contains("Confession posted", ignoreCase = true) && (userAccount?.notificationsEnabled ?: true)) {
                PushNotificationHelper.sendPushNotification(
                    context = context,
                    title = "Confession Published Anonymously",
                    body = "Your confession is now live on the Anonymousnet feed."
                )
            }
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val activeSubScreen = subScreenStack.lastOrNull()
    val unreadNotificationsCount = notifications.count { !it.isRead }
    val allPublishedPosts = (publishedPostsState as? UiState.Success)?.data.orEmpty()
    val myPostsList = (myPostsState as? UiState.Success)?.data.orEmpty()
    val draftsList = myPostsList.filter { it.status == "draft" }

    // Mandatory BackHandler for custom state navigation
    BackHandler(enabled = activeSubScreen != null || currentTab != MainTab.HOME) {
        viewModel.popSubScreen()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            if (activeSubScreen != null) {
                when (activeSubScreen) {
                    is SubScreen.PostDetail -> {
                        val targetPost = (allPublishedPosts + myPostsList).firstOrNull { it.id == activeSubScreen.postId }
                        PostDetailSubScreen(
                            post = targetPost,
                            comments = activePostComments,
                            isLiked = activeSubScreen.postId in likedPostIds,
                            isSaved = activeSubScreen.postId in savedPostIds,
                            isFollowingAuthor = targetPost?.let { it.authorUid in followedUids } ?: false,
                            currentUserId = currentUserId,
                            onBack = { viewModel.popSubScreen() },
                            onToggleLike = { viewModel.toggleLike(it) },
                            onToggleSave = { viewModel.toggleSave(it) },
                            onToggleFollow = { uid, handle -> viewModel.toggleFollowUser(uid, handle) },
                            onSendMessageRequest = { msg ->
                                targetPost?.let { p ->
                                    viewModel.startPrivateMessageRequest(p.authorUid, p.anonymousHandle, msg)
                                }
                            },
                            onAddComment = { post, text -> viewModel.addCommentToPost(post, text) },
                            onDeleteComment = { post, cmtId -> viewModel.deleteComment(post, cmtId) },
                            onReportPost = { post ->
                                viewModel.pushSubScreen(
                                    SubScreen.ReportDialog("post", post.id, "Confession")
                                )
                            },
                            onBlockAuthor = { post -> viewModel.blockAuthor(post.authorUid, post.anonymousLabel) },
                            onDeleteOwnPost = { viewModel.deleteOwnPost(it) },
                            onRecordView = { viewModel.recordPostView(it) }
                        )
                    }
                    is SubScreen.ConversationChat -> {
                        ConversationChatSubScreen(
                            conversationId = activeSubScreen.conversationId,
                            otherUserHandle = activeSubScreen.otherUserHandle,
                            otherUserUid = activeSubScreen.otherUserUid,
                            currentUserId = currentUserId,
                            messages = activeChatMessages,
                            onBack = { viewModel.popSubScreen() },
                            onSendMessage = { text ->
                                viewModel.sendChatMessage(activeSubScreen.conversationId, activeSubScreen.otherUserUid, text)
                            }
                        )
                    }
                    is SubScreen.MyPosts -> {
                        SavedOrLikedListSubScreen(
                            title = "My Posts (${myPostsList.size})",
                            items = myPostsList.map { post ->
                                com.example.data.SavedOrLikedPostRef(
                                    id = post.id,
                                    userId = currentUserId,
                                    postId = post.id,
                                    postText = "[${post.status.uppercase()}] ${post.text}",
                                    postType = post.type,
                                    postMediaUri = post.mediaUri,
                                    anonymousLabel = post.anonymousLabel,
                                    createdAt = post.createdAt
                                )
                            },
                            onBack = { viewModel.popSubScreen() },
                            onOpenPostId = { postId ->
                                val draft = draftsList.firstOrNull { it.id == postId }
                                if (draft != null) {
                                    viewModel.loadDraftIntoComposer(draft)
                                } else {
                                    viewModel.pushSubScreen(SubScreen.PostDetail(postId))
                                }
                            }
                        )
                    }
                    is SubScreen.SavedPosts -> {
                        SavedOrLikedListSubScreen(
                            title = "Saved Confessions",
                            items = savedPosts,
                            onBack = { viewModel.popSubScreen() },
                            onOpenPostId = { postId ->
                                viewModel.pushSubScreen(SubScreen.PostDetail(postId))
                            }
                        )
                    }
                    is SubScreen.LikedPosts -> {
                        SavedOrLikedListSubScreen(
                            title = "Liked Confessions",
                            items = likedPosts,
                            onBack = { viewModel.popSubScreen() },
                            onOpenPostId = { postId ->
                                viewModel.pushSubScreen(SubScreen.PostDetail(postId))
                            }
                        )
                    }
                    is SubScreen.BlockedUsers -> {
                        BlockedUsersSubScreen(
                            blockedUsers = blockedUsers,
                            onBack = { viewModel.popSubScreen() },
                            onUnblock = { viewModel.unblockAuthor(it) }
                        )
                    }
                    is SubScreen.ReportDialog -> {
                        ReportFlowSubScreen(
                            targetType = activeSubScreen.targetType,
                            targetId = activeSubScreen.targetId,
                            targetLabel = activeSubScreen.targetLabel,
                            onBack = { viewModel.popSubScreen() },
                            onSubmitReport = { reason, details ->
                                viewModel.submitSafetyReport(
                                    activeSubScreen.targetType,
                                    activeSubScreen.targetId,
                                    reason,
                                    details
                                )
                            }
                        )
                    }
                    is SubScreen.StayAnonymousInfo -> {
                        InfoDetailSubScreen(
                            title = "Stay Anonymous Always",
                            paragraphs = listOf(
                                "Your posts appear under an anonymous identity (@anon_user). We never show your sign-in email, legal name, phone number, or Firebase UID on public posts.",
                                "Location tagging is disabled by default and camera EXIF metadata is stripped from uploaded media.",
                                "Saved posts and liked confessions are scoped strictly to your private account via zero-trust Firestore security rules and are never visible to other users.",
                                "We process limited internal account and technical data to keep Anonymousnet secure, enforce ownership on your posts, prevent abuse, and operate moderation."
                            ),
                            onBack = { viewModel.popSubScreen() }
                        )
                    }
                    is SubScreen.HelpAndSupport -> {
                        InfoDetailSubScreen(
                            title = "Help & Support",
                            paragraphs = listOf(
                                "How do I post a confession? Tap the violet '+' button in the center of the bottom bar, pick Text, Image, Video, or Audio, ensure 'Keep it anonymous' is toggled on, and tap 'Post Anonymously'.",
                                "In-Built Video & Audio Players: Tap any Video or Audio confession card to play, pause, scrub with the seek bar, adjust playback speed (1x / 1.25x / 1.5x / 2x), or expand video to fullscreen.",
                                "How do I report or block harmful content? Tap the '...' menu on any confession card or open 'Report a Post' from your Profile to submit a categorized safety report or block an author."
                            ),
                            onBack = { viewModel.popSubScreen() }
                        )
                    }
                    is SubScreen.AboutAnonymousnet -> {
                        InfoDetailSubScreen(
                            title = "About Anonymousnet",
                            paragraphs = listOf(
                                "Anonymousnet is a calm, mobile-first anonymous community for honest confessions, thoughts, photos, videos, and recorded voice moments.",
                                "Features an in-built smooth Video Player & Audio Waveform Player, multi-theme customization, system font support, and live push notifications.",
                                "Made with ❤️ by Rahul Shah • Lead Developer & Designer • Version 1.0 • No judgment, just vibes."
                            ),
                            onBack = { viewModel.popSubScreen() }
                        )
                    }
                    is SubScreen.TermsAndPrivacy -> {
                        InfoDetailSubScreen(
                            title = "Terms & Privacy Policy",
                            paragraphs = listOf(
                                "Community Guidelines: Treat others with empathy. Harassment, doxxing, non-consensual media, hate speech, and illegal content are strictly prohibited.",
                                "Data Retention: You can edit or delete your confessions or permanently delete your entire account and associated data at any time from Profile Settings."
                            ),
                            onBack = { viewModel.popSubScreen() }
                        )
                    }
                }
            } else {
                // Main 5-Tab Navigation
                when (currentTab) {
                    MainTab.HOME -> {
                        HomeFeedScreen(
                            feedState = filteredFeedPosts,
                            selectedSegment = feedSegment,
                            selectedCategory = selectedCategoryFilter,
                            availableCategories = availableCategories,
                            likedPostIds = likedPostIds,
                            savedPostIds = savedPostIds,
                            followedUids = followedUids,
                            unreadNotificationCount = unreadNotificationsCount,
                            currentUserId = currentUserId,
                            onSelectSegment = { viewModel.setFeedSegment(it) },
                            onSelectCategory = { viewModel.setCategoryFilter(it) },
                            onOpenNotifications = { viewModel.selectTab(MainTab.ACTIVITY) },
                            onOpenCreateConfession = { viewModel.selectTab(MainTab.CREATE) },
                            onOpenPostDetail = { post ->
                                viewModel.pushSubScreen(SubScreen.PostDetail(post.id))
                            },
                            onToggleLike = { viewModel.toggleLike(it) },
                            onToggleSave = { viewModel.toggleSave(it) },
                            onToggleFollow = { authorUid, authorHandle ->
                                viewModel.toggleFollowUser(authorUid, authorHandle)
                            },
                            onSendMessageRequest = { recipientUid, recipientHandle, msg ->
                                viewModel.startPrivateMessageRequest(recipientUid, recipientHandle, msg)
                            },
                            onHidePost = { viewModel.hidePostFromFeed(it) },
                            onBlockAuthor = { post ->
                                viewModel.blockAuthor(post.authorUid, post.anonymousLabel)
                            },
                            onReportPost = { post ->
                                viewModel.pushSubScreen(
                                    SubScreen.ReportDialog("post", post.id, "Confession")
                                )
                            },
                            onDeleteOwnPost = { viewModel.deleteOwnPost(it) }
                        )
                    }
                    MainTab.EXPLORE -> {
                        ExploreScreen(
                            explorePostsState = filteredExplorePosts,
                            selectedFilter = exploreFilter,
                            selectedSort = exploreSort,
                            selectedCategory = selectedCategoryFilter,
                            availableCategories = availableCategories,
                            searchQuery = exploreSearchQuery,
                            isSearchVisible = isExploreSearchVisible,
                            onSelectFilter = { viewModel.setExploreFilter(it) },
                            onSelectSort = { viewModel.setExploreSort(it) },
                            onSelectCategory = { viewModel.setCategoryFilter(it) },
                            onSearchQueryChange = { viewModel.setExploreSearchQuery(it) },
                            onToggleSearch = { viewModel.toggleExploreSearch() },
                            onOpenPostDetail = { post ->
                                viewModel.pushSubScreen(SubScreen.PostDetail(post.id))
                            },
                            onLikePost = { viewModel.toggleLike(it) }
                        )
                    }
                    MainTab.CREATE -> {
                        CreateConfessionScreen(
                            composerState = composerState,
                            drafts = draftsList,
                            availableCategories = availableCategories,
                            onBackClick = { viewModel.selectTab(MainTab.HOME) },
                            onUpdateText = { viewModel.updateComposerText(it) },
                            onSelectType = { viewModel.updateComposerType(it) },
                            onSelectCategory = { viewModel.updateComposerCategory(it) },
                            onUpdateKeepAnonymous = { viewModel.updateKeepAnonymous(it) },
                            onAttachMedia = { uri, cap, dur, mime ->
                                viewModel.updateComposerMedia(uri, cap, dur, mime)
                            },
                            onUpdateTopics = { viewModel.updateComposerTopics(it) },
                            onClearMedia = { viewModel.clearComposerMedia() },
                            onSetAudioRecording = { rec, secs ->
                                viewModel.setAudioRecordingState(rec, secs)
                            },
                            onPublish = { asDraft -> viewModel.publishConfession(asDraft) },
                            onLoadDraft = { viewModel.loadDraftIntoComposer(it) }
                        )
                    }
                    MainTab.ACTIVITY -> {
                        ActivityNotificationsScreen(
                            notifications = notifications,
                            conversations = conversations,
                            currentUserId = currentUserId,
                            onMarkRead = { viewModel.markNotificationRead(it) },
                            onOpenConversation = { conv ->
                                val otherUid = if (conv.initiatorUid == currentUserId) conv.recipientUid else conv.initiatorUid
                                val otherHandle = if (conv.initiatorUid == currentUserId) conv.recipientHandle else conv.initiatorHandle
                                viewModel.pushSubScreen(
                                    SubScreen.ConversationChat(
                                        conversationId = conv.id,
                                        otherUserHandle = otherHandle,
                                        otherUserUid = otherUid
                                    )
                                )
                            },
                            onRespondToDmRequest = { convId, accept, block ->
                                viewModel.respondToDmRequest(convId, accept, block)
                            }
                        )
                    }
                    MainTab.PROFILE -> {
                        ProfileSettingsScreen(
                            userAccount = userAccount,
                            publicProfile = (publicProfileState as? UiState.Success)?.data,
                            myPostsCount = myPostsList.size,
                            followingCount = followingList.size,
                            isDarkTheme = isDarkTheme,
                            selectedColorTheme = selectedColorTheme,
                            useSystemFont = useSystemFont,
                            onToggleDarkTheme = onToggleDarkTheme,
                            onSelectColorTheme = onSelectColorTheme,
                            onToggleSystemFont = onToggleSystemFont,
                            onUpdatePrivacyDefaults = { keepAnon, notifs ->
                                viewModel.updatePrivacyDefaults(keepAnon, notifs)
                            },
                            onLinkGuestAccount = { viewModel.linkGuestSessionToPermanentGoogle() },
                            onNavigateSubScreen = { viewModel.pushSubScreen(it) },
                            onDeleteAccount = {
                                viewModel.deleteAccountData {
                                    signOutUser(credentialManager, {}, scope)
                                }
                            },
                            onLogOut = {
                                signOutUser(credentialManager, {}, scope)
                            },
                            usernameAvailability = usernameAvailability,
                            onCheckUsername = { viewModel.checkUsername(it) },
                            onResetUsernameCheck = { viewModel.resetUsernameCheck() },
                            onSaveUsernameAndDisplayName = { handle, displayName, onComplete ->
                                viewModel.updateProfileAndUsername(handle, displayName, onComplete)
                            }
                        )
                    }
                }
            }

            // Floating Rounded Bottom Bar (consistent geometry and icon order across Feed, Create, Explore, Activity, Profile)
            AnonymousnetBottomBar(
                currentTab = currentTab,
                unreadNotificationCount = unreadNotificationsCount,
                onSelectTab = { viewModel.selectTab(it) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
