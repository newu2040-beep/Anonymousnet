package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.PublicProfile
import com.example.data.UserAccount
import com.example.data.UsernameAvailability
import com.example.ui.SubScreen
import com.example.ui.components.AnonymousHatAvatar
import com.example.ui.components.PushNotificationHelper
import com.example.ui.theme.AnonHeartRed
import com.example.ui.theme.AnonStarGold
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppFontOption

private val SUGGESTED_INTERESTS = listOf(
    "Music", "Gaming", "Photography", "Movies", "Fitness",
    "Coding", "Travel", "Art", "Coffee", "Night Owl", "Books", "Anime"
)

/**
 * Screen 5 — Profile and Settings (Anonymousnet v3.0)
 * - Compact Mode toggle for smaller phone displays
 * - Custom Anonymous Gallery Profile Picture picker
 * - Full Profile & Identity Customization: Name, Surname, @handle, Gender (Male or Female only), Age, and Interests
 * - 16 Color Themes & 7 Fonts
 * - Real-time statistics (zero fake/demo counters)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    userAccount: UserAccount?,
    publicProfile: PublicProfile?,
    myPostsCount: Int,
    followingCount: Int,
    isDarkTheme: Boolean,
    isCompactMode: Boolean = false,
    selectedColorTheme: AppColorTheme,
    selectedFontOption: AppFontOption = AppFontOption.PLUS_JAKARTA_SANS,
    useSystemFont: Boolean = false,
    onToggleDarkTheme: () -> Unit,
    onToggleCompactMode: () -> Unit = {},
    onSelectColorTheme: (AppColorTheme) -> Unit,
    onSelectFontOption: (AppFontOption) -> Unit = {},
    onToggleSystemFont: (Boolean) -> Unit = {},
    onUpdatePrivacyDefaults: (keepAnon: Boolean, notifs: Boolean) -> Unit,
    onLinkGuestAccount: () -> Unit,
    onNavigateSubScreen: (SubScreen) -> Unit,
    onDeleteAccount: () -> Unit,
    onLogOut: () -> Unit,
    usernameAvailability: UsernameAvailability = UsernameAvailability.Idle,
    onCheckUsername: (String) -> Unit = {},
    onResetUsernameCheck: () -> Unit = {},
    onUpdateProfileAvatarOnly: (String) -> Unit = {},
    onSaveFullProfile: (
        handle: String,
        displayName: String,
        surname: String,
        gender: String,
        age: Int,
        interests: String,
        avatarUri: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    var showSettingsModal by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmModal by remember { mutableStateOf(false) }
    var permissionStatusText by remember { mutableStateOf("") }

    fun checkPermissionsGranted(): Boolean {
        val mic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val cam = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        return mic && cam && notif
    }

    var allPermissionsGranted by remember { mutableStateOf(checkPermissionsGranted()) }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allPermissionsGranted = checkPermissionsGranted()
        val grantedCount = results.values.count { it }
        permissionStatusText = "Permissions updated ($grantedCount granted)."
        PushNotificationHelper.ensureNotificationChannel(context)
    }

    // Launcher for choosing Anonymous Profile Picture from Gallery directly on Profile
    val profileAvatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            onUpdateProfileAvatarOnly(uri.toString())
        }
    }

    val galleryVerifyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            permissionStatusText = "Gallery media access verified: ${uri.lastPathSegment ?: "media"}"
        }
    }

    val filesVerifyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            permissionStatusText = "Full file access verified: ${uri.lastPathSegment ?: "document"}"
        }
    }

    fun requestAllAppPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        multiPermissionLauncher.launch(perms.toTypedArray())
    }

    val displayPosts = (publicProfile?.postCount ?: myPostsCount.toLong()).coerceAtLeast(myPostsCount.toLong())
    val displayViews = publicProfile?.viewCount ?: 0L
    val displayLikes = publicProfile?.likeCount ?: 0L
    val displayFollowers = publicProfile?.followersCount ?: 0L
    val displayFollowing = (publicProfile?.followingCount ?: followingCount.toLong()).coerceAtLeast(followingCount.toLong())

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val safeRadius = (size.width * 0.55f).coerceAtLeast(1f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        selectedColorTheme.primary.copy(alpha = 0.28f),
                        selectedColorTheme.secondaryLight.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.25f),
                    radius = safeRadius
                ),
                radius = safeRadius,
                center = Offset(size.width * 0.18f, size.height * 0.25f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 114.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Row: Version 3.0 badge on left, Settings gear icon on top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = selectedColorTheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Anonymousnet v3.0",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = selectedColorTheme.primaryDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                IconButton(
                    onClick = { showSettingsModal = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("profile_settings_gear")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Account and App Settings",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 2. Centered Anonymous Avatar (Tap to pick custom gallery avatar)
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .clickable {
                        profileAvatarPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("profile_avatar_gallery_picker")
            ) {
                AnonymousHatAvatar(
                    size = 96.dp,
                    backgroundColor = selectedColorTheme.primary.copy(alpha = 0.16f),
                    iconTint = selectedColorTheme.primaryDark,
                    showVerifiedBadge = true,
                    avatarUri = publicProfile?.avatarUri
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Choose Anonymous Profile Picture from Gallery",
                            tint = selectedColorTheme.primaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Identity: Full Name (First + Surname) & "@handle"
            Text(
                text = publicProfile?.fullDisplayName() ?: "Anonymousnet",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = publicProfile?.anonymousHandle ?: "@anon_user",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Gender, Age, and Interests Badges
            val hasGender = !publicProfile?.gender.isNullOrBlank()
            val hasAge = (publicProfile?.age ?: 0) > 0
            val interestsList = remember(publicProfile?.interests) {
                publicProfile?.interests
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty()
            }

            if (hasGender || hasAge || interestsList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasGender) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = selectedColorTheme.primary.copy(alpha = 0.14f),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        ) {
                            Text(
                                text = publicProfile!!.gender,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = selectedColorTheme.primaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (hasAge) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = selectedColorTheme.secondaryLight.copy(alpha = 0.18f),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        ) {
                            Text(
                                text = "${publicProfile!!.age} yrs",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = selectedColorTheme.primaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    interestsList.forEach { interest ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        ) {
                            Text(
                                text = "#$interest",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Customize Profile & Choose Gallery Picture
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showEditProfileDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = selectedColorTheme.primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        selectedColorTheme.primary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("customize_username_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Customize Profile & Details",
                            tint = selectedColorTheme.primaryDark,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Edit Profile & Details",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = selectedColorTheme.primaryDark
                        )
                    }
                }

                Surface(
                    onClick = {
                        profileAvatarPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("choose_gallery_avatar_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Gallery Photo",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Gallery Photo",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Compact Stats: Posts, Views, Likes, Followers, Following
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileStatCard(
                    value = formatCompactCount(displayPosts),
                    label = "Posts",
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    value = formatCompactCount(displayViews),
                    label = "Views",
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    value = formatCompactCount(displayLikes),
                    label = "Likes",
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    value = formatCompactCount(displayFollowers),
                    label = "Followers",
                    modifier = Modifier.weight(1f)
                )
                ProfileStatCard(
                    value = formatCompactCount(displayFollowing),
                    label = "Following",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Highlighted Gradient Privacy Card: "Stay Anonymous Always"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                selectedColorTheme.primary,
                                selectedColorTheme.secondaryLight
                            )
                        )
                    )
                    .clickable { onNavigateSubScreen(SubScreen.StayAnonymousInfo) }
                    .padding(horizontal = 18.dp, vertical = 15.dp)
                    .testTag("stay_anonymous_always_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = AnonStarGold,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Stay Anonymous\nAlways",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open Privacy Controls",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Profile Customization Section: Compact Mode, Dark Mode, Typography, 16 Color Themes
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppearanceCustomizationCardContent(
                        isDarkTheme = isDarkTheme,
                        isCompactMode = isCompactMode,
                        selectedColorTheme = selectedColorTheme,
                        selectedFontOption = selectedFontOption,
                        onToggleDarkTheme = onToggleDarkTheme,
                        onToggleCompactMode = onToggleCompactMode,
                        onSelectColorTheme = onSelectColorTheme,
                        onSelectFontOption = onSelectFontOption
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7. Clean Rounded Navigation Menu
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileMenuRow(
                        icon = Icons.AutoMirrored.Outlined.Article,
                        title = "My Posts",
                        testTag = "profile_row_my_posts",
                        onClick = { onNavigateSubScreen(SubScreen.MyPosts) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ProfileMenuRow(
                        icon = Icons.Outlined.BookmarkBorder,
                        title = "Saved",
                        testTag = "profile_row_saved",
                        onClick = { onNavigateSubScreen(SubScreen.SavedPosts) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ProfileMenuRow(
                        icon = Icons.Outlined.FavoriteBorder,
                        title = "Liked",
                        testTag = "profile_row_liked",
                        onClick = { onNavigateSubScreen(SubScreen.LikedPosts) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ProfileMenuRow(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Report a Post",
                        testTag = "profile_row_report",
                        onClick = {
                            onNavigateSubScreen(
                                SubScreen.ReportDialog(
                                    targetType = "post",
                                    targetId = "general_feed",
                                    targetLabel = "Community Confession"
                                )
                            )
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ProfileMenuRow(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        title = "Help & Support",
                        testTag = "profile_row_help",
                        onClick = { onNavigateSubScreen(SubScreen.HelpAndSupport) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ProfileMenuRow(
                        icon = Icons.Outlined.Info,
                        title = "About Anonymousnet v3.0",
                        testTag = "profile_row_about",
                        onClick = { onNavigateSubScreen(SubScreen.AboutAnonymousnet) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 8. Permissions & Hardware Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Permissions & Device Access",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { requestAllAppPermissions() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Allow All Permissions", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                PushNotificationHelper.sendPushNotification(
                                    context = context,
                                    title = "Anonymousnet v3.0 Push Active",
                                    body = "Real-time push notifications are verified and active!"
                                )
                                permissionStatusText = "Push notification triggered on your status bar!"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Push", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                galleryVerifyLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Gallery Access", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { filesVerifyLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Files Access", fontSize = 12.sp)
                        }
                    }

                    if (permissionStatusText.isNotBlank()) {
                        Text(
                            text = permissionStatusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 9. Developer Credit Card ("Made with ❤️ by Rahul Shah • Anonymousnet v3.0")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                    .testTag("developer_credit_card"),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Made with ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Love",
                            tint = AnonHeartRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = " by Rahul Shah",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lead Developer & Designer • Rahul Shah • Anonymousnet v3.0",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 10. Centered Log Out Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onLogOut)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .testTag("profile_logout_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Log Out",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Settings Modal (Anonymousnet v3.0)
    if (showSettingsModal) {
        var keepAnon by remember { mutableStateOf(userAccount?.keepAnonymousDefault ?: true) }
        var notifsEnabled by remember { mutableStateOf(userAccount?.notificationsEnabled ?: true) }

        AlertDialog(
            onDismissRequest = { showSettingsModal = false },
            title = { Text("Settings • Anonymousnet v3.0", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anonymous by Default", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Keep your handle hidden when composing confessions",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = keepAnon,
                            onCheckedChange = {
                                keepAnon = it
                                onUpdatePrivacyDefaults(keepAnon, notifsEnabled)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Push Notifications", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Receive real-time status bar alerts when posting confessions, comments, and likes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notifsEnabled,
                            onCheckedChange = {
                                notifsEnabled = it
                                onUpdatePrivacyDefaults(keepAnon, notifsEnabled)
                            }
                        )
                    }

                    if (userAccount?.guest == true) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Guest Mode Active",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Uninstalling or clearing app data may lose access unless linked.",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = {
                                        onLinkGuestAccount()
                                        showSettingsModal = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Link Guest Session to Google")
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showSettingsModal = false
                            showEditProfileDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_customize_username_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AlternateEmail,
                            contentDescription = null,
                            tint = selectedColorTheme.primaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Customize Name, Gender, Age & Interests")
                    }

                    OutlinedButton(
                        onClick = {
                            showSettingsModal = false
                            profileAvatarPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = selectedColorTheme.primaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Anonymous Avatar from Gallery")
                    }

                    OutlinedButton(
                        onClick = {
                            showSettingsModal = false
                            onNavigateSubScreen(SubScreen.BlockedUsers)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Manage Blocked Users")
                    }

                    OutlinedButton(
                        onClick = {
                            showSettingsModal = false
                            showDeleteConfirmModal = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Account & Data", color = AnonHeartRed)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Dedicated Appearance & Compact Mode Customization Section
                    AppearanceCustomizationCardContent(
                        isDarkTheme = isDarkTheme,
                        isCompactMode = isCompactMode,
                        selectedColorTheme = selectedColorTheme,
                        selectedFontOption = selectedFontOption,
                        onToggleDarkTheme = onToggleDarkTheme,
                        onToggleCompactMode = onToggleCompactMode,
                        onSelectColorTheme = onSelectColorTheme,
                        onSelectFontOption = onSelectFontOption
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Made with ❤️ by Rahul Shah",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Anonymousnet v3.0 • Compact Mode & 16 Themes Edition",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsModal = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showDeleteConfirmModal) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmModal = false },
            title = { Text("Delete Account?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "This permanently deletes your private account profile, public pseudonymous handle, and all confessions you authored from Firestore.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmModal = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AnonHeartRed)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full Profile Customization Dialog (Name, Surname, Handle, Gender [Male/Female only], Age, Interests, Gallery Avatar)
    if (showEditProfileDialog) {
        var inputDisplayName by remember {
            mutableStateOf(publicProfile?.displayName ?: "Anonymousnet")
        }
        var inputSurname by remember {
            mutableStateOf(publicProfile?.surname ?: "")
        }
        var inputHandle by remember {
            mutableStateOf(publicProfile?.anonymousHandle?.removePrefix("@") ?: "anon_user")
        }
        var selectedGender by remember {
            mutableStateOf(publicProfile?.gender ?: "")
        }
        var inputAgeText by remember {
            val existingAge = publicProfile?.age ?: 0
            mutableStateOf(if (existingAge > 0) existingAge.toString() else "")
        }
        var inputInterests by remember {
            mutableStateOf(publicProfile?.interests ?: "")
        }
        var inputAvatarUri by remember {
            mutableStateOf(publicProfile?.avatarUri ?: "")
        }
        var isSaving by remember { mutableStateOf(false) }

        val dialogAvatarPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
                inputAvatarUri = uri.toString()
            }
        }

        LaunchedEffect(inputHandle) {
            onCheckUsername(inputHandle)
        }

        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    onResetUsernameCheck()
                    showEditProfileDialog = false
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(selectedColorTheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AlternateEmail,
                            contentDescription = null,
                            tint = selectedColorTheme.primaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Customize Profile & Identity",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "Name, surname, gender, age, interests & gallery avatar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Anonymous Avatar Picker Row
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AnonymousHatAvatar(
                                size = 56.dp,
                                backgroundColor = selectedColorTheme.primary.copy(alpha = 0.16f),
                                iconTint = selectedColorTheme.primaryDark,
                                showVerifiedBadge = true,
                                avatarUri = inputAvatarUri
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Anonymous Profile Picture",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            dialogAvatarPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = selectedColorTheme.primary)
                                    ) {
                                        Text("From Gallery", fontSize = 11.sp)
                                    }
                                    if (inputAvatarUri.isNotBlank()) {
                                        TextButton(onClick = { inputAvatarUri = "" }) {
                                            Text("Reset Hat", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // First / Display Name
                    OutlinedTextField(
                        value = inputDisplayName,
                        onValueChange = { if (it.length <= 60) inputDisplayName = it },
                        label = { Text("First / Display Name") },
                        placeholder = { Text("e.g. Midnight") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_display_name_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Surname
                    OutlinedTextField(
                        value = inputSurname,
                        onValueChange = { if (it.length <= 60) inputSurname = it },
                        label = { Text("Surname (Optional)") },
                        placeholder = { Text("e.g. Phantom / Shah") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_surname_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Unique @Handle
                    OutlinedTextField(
                        value = inputHandle,
                        onValueChange = { raw ->
                            val filtered = raw.trim()
                                .removePrefix("@")
                                .lowercase()
                                .filter { it.isLetterOrDigit() || it == '_' }
                                .take(30)
                            inputHandle = filtered
                        },
                        label = { Text("Unique Handle") },
                        prefix = {
                            Text(
                                text = "@",
                                fontWeight = FontWeight.Bold,
                                color = selectedColorTheme.primaryDark
                            )
                        },
                        placeholder = { Text("username") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.AlternateEmail,
                                contentDescription = null,
                                tint = selectedColorTheme.primaryDark
                            )
                        },
                        singleLine = true,
                        isError = usernameAvailability is UsernameAvailability.Taken || usernameAvailability is UsernameAvailability.InvalidFormat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_handle_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Real-time Handle Availability Status Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = when (usernameAvailability) {
                            is UsernameAvailability.Available -> Color(0xFFE8F5E9)
                            is UsernameAvailability.IsCurrentUser -> selectedColorTheme.primary.copy(alpha = 0.12f)
                            is UsernameAvailability.Taken -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            is UsernameAvailability.InvalidFormat -> Color(0xFFFFF3E0)
                            is UsernameAvailability.Checking -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            UsernameAvailability.Empty, UsernameAvailability.Idle -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (usernameAvailability) {
                                is UsernameAvailability.Checking -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = selectedColorTheme.primary
                                    )
                                    Text(
                                        text = "Checking real-time availability...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                is UsernameAvailability.Available -> {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "@${usernameAvailability.handle} is available!",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                                is UsernameAvailability.IsCurrentUser -> {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = selectedColorTheme.primaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "This is your current active handle",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = selectedColorTheme.primaryDark
                                    )
                                }
                                is UsernameAvailability.Taken -> {
                                    Icon(
                                        imageVector = Icons.Filled.Cancel,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "@${usernameAvailability.handle} is already taken",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                is UsernameAvailability.InvalidFormat -> {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = usernameAvailability.reason,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFBF360C)
                                    )
                                }
                                UsernameAvailability.Empty, UsernameAvailability.Idle -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Handles must be 3-30 letters, numbers, or underscores.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Gender Selection (Male or Female only)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Gender (Male or Female)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("Male", "Female").forEach { genderOption ->
                                val isSelected = selectedGender == genderOption
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            selectedGender = if (isSelected) "" else genderOption
                                        }
                                        .testTag("gender_option_${genderOption.lowercase()}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) {
                                        selectedColorTheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = genderOption,
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Age Input
                    OutlinedTextField(
                        value = inputAgeText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(3)
                            inputAgeText = digits
                        },
                        label = { Text("Age (Years)") },
                        placeholder = { Text("e.g. 21") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Cake,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_age_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Interests Input + Interactive Quick Chips
                    OutlinedTextField(
                        value = inputInterests,
                        onValueChange = { if (it.length <= 500) inputInterests = it },
                        label = { Text("Interests (comma-separated)") },
                        placeholder = { Text("e.g. Music, Coding, Photography, Coffee") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Interests,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_interests_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val currentList = inputInterests.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        SUGGESTED_INTERESTS.forEach { tag ->
                            val active = currentList.any { it.equals(tag, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) selectedColorTheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (active) androidx.compose.foundation.BorderStroke(1.dp, selectedColorTheme.primary) else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val updated = if (active) {
                                            currentList.filterNot { it.equals(tag, ignoreCase = true) }
                                        } else {
                                            currentList + tag
                                        }
                                        inputInterests = updated.joinToString(", ")
                                    }
                            ) {
                                Text(
                                    text = if (active) "✓ $tag" else "+ $tag",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (active) selectedColorTheme.primaryDark else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                val canSave = !isSaving &&
                    inputDisplayName.isNotBlank() &&
                    inputHandle.length in 3..30 &&
                    (usernameAvailability is UsernameAvailability.Available || usernameAvailability is UsernameAvailability.IsCurrentUser)

                Button(
                    onClick = {
                        isSaving = true
                        val parsedAge = inputAgeText.toIntOrNull()?.coerceIn(0, 120) ?: 0
                        onSaveFullProfile(
                            inputHandle,
                            inputDisplayName,
                            inputSurname,
                            selectedGender,
                            parsedAge,
                            inputInterests,
                            inputAvatarUri
                        ) { success ->
                            isSaving = false
                            if (success) {
                                onResetUsernameCheck()
                                showEditProfileDialog = false
                            }
                        }
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedColorTheme.primary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("save_username_confirm_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onResetUsernameCheck()
                        showEditProfileDialog = false
                    },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(68.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatCompactCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@Composable
fun AppearanceCustomizationCardContent(
    isDarkTheme: Boolean,
    isCompactMode: Boolean = false,
    selectedColorTheme: AppColorTheme,
    selectedFontOption: AppFontOption,
    onToggleDarkTheme: () -> Unit,
    onToggleCompactMode: () -> Unit = {},
    onSelectColorTheme: (AppColorTheme) -> Unit,
    onSelectFontOption: (AppFontOption) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Display, Theme & Appearance (v3.0)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Compact Mode Toggle for Smaller Phone Displays
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Compact Mode (Small Screens)",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Resizes the entire UI in real time to fit smaller phone displays",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isCompactMode,
                onCheckedChange = { onToggleCompactMode() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("settings_compact_mode_switch")
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        // Dark & Light Mode Toggle Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isDarkTheme) "Dark Mode" else "Light Mode",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Consistent theme across entire application",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { onToggleDarkTheme() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("settings_dark_light_switch")
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        // Typography Selection (7 options)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.FontDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Typography (${selectedFontOption.displayName})",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFontOption.entries.forEach { fontOption ->
                    val isSelected = fontOption == selectedFontOption
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectFontOption(fontOption) }
                            .testTag("font_chip_${fontOption.key}"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            selectedColorTheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, selectedColorTheme.primary)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = selectedColorTheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = fontOption.displayName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = fontOption.fontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) selectedColorTheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

        // App Color Theme Swatches (16 options)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Color Palette (${selectedColorTheme.displayName} • 16 Themes)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppColorTheme.entries.forEach { themeOption ->
                    val isSelected = themeOption == selectedColorTheme
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectColorTheme(themeOption) }
                            .testTag("theme_chip_${themeOption.key}"),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            themeOption.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(1.5.dp, themeOption.primary)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                themeOption.primary,
                                                themeOption.secondaryLight
                                            )
                                        )
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = themeOption.displayName,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) themeOption.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
