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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.example.ui.theme.AppFontOption
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
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (_: Throwable) {
        }
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
    try {
        addAuthStateListener(listener)
    } catch (_: Throwable) {
        trySend(null)
    }
    awaitClose {
        try {
            removeAuthStateListener(listener)
        } catch (_: Throwable) {
        }
    }
}

@Composable
fun AnonymousnetAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var localDarkTheme by rememberSaveable { mutableStateOf(false) }
    var localCompactMode by rememberSaveable { mutableStateOf(false) }
    var selectedColorThemeName by rememberSaveable { mutableStateOf(AppColorTheme.CUSTOM_ACCENT.name) }
    var selectedFontOptionKey by rememberSaveable { mutableStateOf(AppFontOption.PLUS_JAKARTA_SANS.key) }
    var useSystemFont by rememberSaveable { mutableStateOf(false) }
    var enteredAsGuest by rememberSaveable { mutableStateOf(false) }
    var requestedInitialPermissions by rememberSaveable { mutableStateOf(false) }

    val selectedColorTheme = remember(selectedColorThemeName) {
        AppColorTheme.entries.firstOrNull { it.name == selectedColorThemeName } ?: AppColorTheme.CUSTOM_ACCENT
    }

    val initialPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        PushNotificationHelper.ensureNotificationChannel(context)
    }

    val user = currentUser

    // Request notification permission only after authentication so it never collides with CredentialManager on cold start
    LaunchedEffect(user?.uid) {
        if (user != null && !requestedInitialPermissions) {
            requestedInitialPermissions = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                try {
                    initialPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                } catch (_: Throwable) {
                }
            }
        }
    }

    if (user == null) {
        // Screen 1 — Welcome and Authentication
        AnonymousnetTheme(
            darkTheme = localDarkTheme,
            colorTheme = selectedColorTheme,
            fontOption = AppFontOption.fromKey(selectedFontOptionKey),
            compactMode = localCompactMode
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
        val appContext = context.applicationContext ?: context
        // Authenticated session: Initialize ViewModel keyed by user.uid with custom Firestore database ID
        val viewModel: AnonymousnetViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val app = this[APPLICATION_KEY] ?: appContext
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
        val isCompact = userAccount?.compactMode ?: localCompactMode
        val activeColorTheme = remember(userAccount?.colorTheme, selectedColorThemeName) {
            if (!userAccount?.colorTheme.isNullOrBlank()) {
                AppColorTheme.fromKey(userAccount.colorTheme)
            } else {
                AppColorTheme.entries.firstOrNull { it.name == selectedColorThemeName } ?: AppColorTheme.CUSTOM_ACCENT
            }
        }
        val activeFontOption = remember(userAccount?.fontFamily, userAccount?.useSystemFont, selectedFontOptionKey) {
            if (userAccount?.useSystemFont == true) {
                AppFontOption.SYSTEM_DEFAULT
            } else if (!userAccount?.fontFamily.isNullOrBlank()) {
                AppFontOption.fromKey(userAccount.fontFamily)
            } else {
                AppFontOption.fromKey(selectedFontOptionKey)
            }
        }

        AnonymousnetTheme(
            darkTheme = isDark,
            colorTheme = activeColorTheme,
            fontOption = activeFontOption,
            compactMode = isCompact
        ) {
            AuthenticatedMainScaffold(
                viewModel = viewModel,
                currentUserId = user.uid,
                isDarkTheme = isDark,
                isCompactMode = isCompact,
                selectedColorTheme = activeColorTheme,
                selectedFontOption = activeFontOption,
                useSystemFont = activeFontOption == AppFontOption.SYSTEM_DEFAULT,
                onToggleDarkTheme = {
                    localDarkTheme = !isDark
                    viewModel.toggleThemeMode(isDark)
                },
                onToggleCompactMode = {
                    localCompactMode = !isCompact
                    viewModel.toggleCompactMode(isCompact)
                },
                onSelectColorTheme = { newTheme ->
                    selectedColorThemeName = newTheme.name
                    viewModel.setColorTheme(newTheme.key)
                },
                onSelectFontOption = { newFont ->
                    selectedFontOptionKey = newFont.key
                    viewModel.setFontOption(newFont.key)
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
    isCompactMode: Boolean,
    selectedColorTheme: AppColorTheme,
    selectedFontOption: AppFontOption,
    useSystemFont: Boolean,
    onToggleDarkTheme: () -> Unit,
    onToggleCompactMode: () -> Unit,
    onSelectColorTheme: (AppColorTheme) -> Unit,
    onSelectFontOption: (AppFontOption) -> Unit,
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

    // Trigger real status-bar push notifications when posting confessions or performing actions
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            if (userAccount?.notificationsEnabled ?: true) {
                val shouldPush = msg.contains("Confession", ignoreCase = true) ||
                    msg.contains("published", ignoreCase = true) ||
                    msg.contains("Draft", ignoreCase = true) ||
                    msg.contains("comment", ignoreCase = true) ||
                    msg.contains("Liked", ignoreCase = true) ||
                    msg.contains("Following", ignoreCase = true) ||
                    msg.contains("updated", ignoreCase = true) ||
                    msg.contains("Message", ignoreCase = true)
                if (shouldPush) {
                    PushNotificationHelper.sendPushNotification(
                        context = context,
                        title = "Anonymousnet v3.0",
                        body = msg
                    )
                }
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
                AnimatedContent(
                    targetState = activeSubScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "subscreen_transition"
                ) { subScreen ->
                    when (subScreen) {
                        is SubScreen.PostDetail -> {
                            val targetPost = (allPublishedPosts + myPostsList).firstOrNull { it.id == subScreen.postId }
                            PostDetailSubScreen(
                                post = targetPost,
                                comments = activePostComments,
                                isLiked = subScreen.postId in likedPostIds,
                                isSaved = subScreen.postId in savedPostIds,
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
                                conversationId = subScreen.conversationId,
                                otherUserHandle = subScreen.otherUserHandle,
                                otherUserUid = subScreen.otherUserUid,
                                currentUserId = currentUserId,
                                messages = activeChatMessages,
                                onBack = { viewModel.popSubScreen() },
                                onSendMessage = { text ->
                                    viewModel.sendChatMessage(subScreen.conversationId, subScreen.otherUserUid, text)
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
                                targetType = subScreen.targetType,
                                targetId = subScreen.targetId,
                                targetLabel = subScreen.targetLabel,
                                onBack = { viewModel.popSubScreen() },
                                onSubmitReport = { reason, details ->
                                    viewModel.submitSafetyReport(
                                        subScreen.targetType,
                                        subScreen.targetId,
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
                                    "How do I post a confession? Tap the '+' button in the center of the bottom bar, pick any of the 18+ categories, write with zero word limits, and tap 'Post Anonymously'.",
                                    "Compact Mode for Smaller Displays: Toggle Compact Mode in Profile or Settings to dynamically scale the entire UI in real time for compact phone screens.",
                                    "Custom Profile & Gallery Avatar: Tap your profile picture or 'Edit Profile & Details' to customize your name, surname, @handle, gender (Male or Female), age, interests, and gallery avatar."
                                ),
                                onBack = { viewModel.popSubScreen() }
                            )
                        }
                        is SubScreen.AboutAnonymousnet -> {
                            InfoDetailSubScreen(
                                title = "About Anonymousnet v3.0",
                                paragraphs = listOf(
                                    "Anonymousnet v3.0 is a real-time, mobile-first anonymous community for honest confessions, thoughts, photos, videos, and recorded voice moments with no word limits.",
                                    "Includes real-time Compact Mode for smaller phone displays, 16 curated color themes, 7 typography fonts, custom gallery avatars, and real-time push notifications.",
                                    "Made with ❤️ by Rahul Shah • Lead Developer & Designer • Version 3.0 • No judgment, just vibes."
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
                }
            } else {
                // Main 5-Tab Navigation with smooth transitions
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(160))
                    },
                    label = "main_tab_transition"
                ) { tab ->
                    when (tab) {
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
                                isCompactMode = isCompactMode,
                                selectedColorTheme = selectedColorTheme,
                                selectedFontOption = selectedFontOption,
                                useSystemFont = useSystemFont,
                                onToggleDarkTheme = onToggleDarkTheme,
                                onToggleCompactMode = onToggleCompactMode,
                                onSelectColorTheme = onSelectColorTheme,
                                onSelectFontOption = onSelectFontOption,
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
                                onUpdateProfileAvatarOnly = { uri ->
                                    viewModel.updateProfileAvatarOnly(uri)
                                },
                                onSaveFullProfile = { handle, displayName, surname, gender, age, interests, avatarUri, onComplete ->
                                    viewModel.updateFullProfileDetails(
                                        newHandle = handle,
                                        newDisplayName = displayName,
                                        newSurname = surname,
                                        newGender = gender,
                                        newAge = age,
                                        newInterests = interests,
                                        newAvatarUri = avatarUri,
                                        onComplete = onComplete
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Floating Rounded Bottom Bar (only shown on the 5 main tabs so it never covers sub-screens or action buttons)
            if (activeSubScreen == null) {
                AnonymousnetBottomBar(
                    currentTab = currentTab,
                    unreadNotificationCount = unreadNotificationsCount,
                    onSelectTab = { viewModel.selectTab(it) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
