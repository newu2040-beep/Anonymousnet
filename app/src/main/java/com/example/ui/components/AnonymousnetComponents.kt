package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.MainTab
import com.example.ui.theme.AnonHeartRed

@Composable
fun HoodedAnonymousHeroIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 186.dp
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = w * 0.45f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.28f),
                        primaryColor.copy(alpha = 0.0f)
                    ),
                    center = center,
                    radius = w * 0.5f
                ),
                radius = w * 0.5f,
                center = center
            )

            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.7f),
                        primaryColor.copy(alpha = 0.5f),
                        Color(0xFFD9D4FF)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                radius = radius,
                center = center
            )

            val cloakPath = Path().apply {
                moveTo(w * 0.20f, h * 0.80f)
                quadraticTo(w * 0.25f, h * 0.62f, w * 0.39f, h * 0.58f)
                lineTo(w * 0.61f, h * 0.58f)
                quadraticTo(w * 0.75f, h * 0.62f, w * 0.80f, h * 0.80f)
                quadraticTo(w * 0.50f, h * 0.94f, w * 0.20f, h * 0.80f)
                close()
            }
            drawPath(
                path = cloakPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF312A6E), Color(0xFF1A1542))
                )
            )

            val hoodPath = Path().apply {
                moveTo(w * 0.50f, h * 0.18f)
                cubicTo(w * 0.66f, h * 0.22f, w * 0.72f, h * 0.42f, w * 0.68f, h * 0.62f)
                lineTo(w * 0.32f, h * 0.62f)
                cubicTo(w * 0.28f, h * 0.42f, w * 0.34f, h * 0.22f, w * 0.50f, h * 0.18f)
                close()
            }
            drawPath(
                path = hoodPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4C4398), Color(0xFF241E54))
                )
            )

            val innerHood = Path().apply {
                moveTo(w * 0.50f, h * 0.25f)
                cubicTo(w * 0.61f, h * 0.29f, w * 0.63f, h * 0.45f, w * 0.60f, h * 0.58f)
                lineTo(w * 0.40f, h * 0.58f)
                cubicTo(w * 0.37f, h * 0.45f, w * 0.39f, h * 0.29f, w * 0.50f, h * 0.25f)
                close()
            }
            drawPath(path = innerHood, color = Color(0xFF171338))

            val maskPath = Path().apply {
                moveTo(w * 0.41f, h * 0.38f)
                lineTo(w * 0.59f, h * 0.38f)
                quadraticTo(w * 0.58f, h * 0.53f, w * 0.50f, h * 0.56f)
                quadraticTo(w * 0.42f, h * 0.53f, w * 0.41f, h * 0.38f)
                close()
            }
            drawPath(path = maskPath, color = Color(0xFFF6F5FF))

            drawRoundRect(
                color = Color(0xFF241E54),
                topLeft = Offset(w * 0.435f, h * 0.42f),
                size = Size(w * 0.05f, h * 0.025f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = Color(0xFF241E54),
                topLeft = Offset(w * 0.515f, h * 0.42f),
                size = Size(w * 0.05f, h * 0.025f),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-12).dp)
                .size(54.dp)
                .shadow(10.dp, CircleShape, spotColor = primaryColor.copy(alpha = 0.35f)),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.5.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (idx == 1) primaryColor else secondaryColor)
                    )
                }
            }
        }
    }
}

@Composable
fun AnonymousHatAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    iconTint: Color = MaterialTheme.colorScheme.primary,
    showVerifiedBadge: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(backgroundColor)
        ) {
            val w = this.size.width
            val h = this.size.height

            val shoulderPath = Path().apply {
                moveTo(w * 0.22f, h * 0.96f)
                quadraticTo(w * 0.50f, h * 0.72f, w * 0.78f, h * 0.96f)
                close()
            }
            drawPath(path = shoulderPath, color = iconTint.copy(alpha = 0.85f))

            val crownPath = Path().apply {
                moveTo(w * 0.33f, h * 0.44f)
                lineTo(w * 0.38f, h * 0.24f)
                quadraticTo(w * 0.50f, h * 0.20f, w * 0.62f, h * 0.24f)
                lineTo(w * 0.67f, h * 0.44f)
                close()
            }
            drawPath(path = crownPath, color = iconTint)

            drawRoundRect(
                color = iconTint,
                topLeft = Offset(w * 0.22f, h * 0.42f),
                size = Size(w * 0.56f, h * 0.065f),
                cornerRadius = CornerRadius(12f, 12f)
            )

            drawRoundRect(
                color = iconTint,
                topLeft = Offset(w * 0.30f, h * 0.53f),
                size = Size(w * 0.17f, h * 0.11f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = iconTint,
                topLeft = Offset(w * 0.53f, h * 0.53f),
                size = Size(w * 0.17f, h * 0.11f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawLine(
                color = iconTint,
                start = Offset(w * 0.46f, h * 0.56f),
                end = Offset(w * 0.54f, h * 0.56f),
                strokeWidth = w * 0.035f,
                cap = StrokeCap.Round
            )
        }

        if (showVerifiedBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-2).dp, y = (-2).dp)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Verified Anonymous Identity",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.16f)
                )
            }
        }
    }
}

@Composable
fun CreateConfessionHeroIcon(
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier.size(104.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 74.dp)
                .rotate(-10f)
                .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = primaryColor)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(secondaryColor, primaryColor)
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-6).dp, y = (-6).dp)
                .size(40.dp)
                .shadow(8.dp, CircleShape, spotColor = primaryColor.copy(alpha = 0.3f)),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Encrypted & Anonymous",
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ExplorePresetArtwork(
    presetUri: String?,
    postType: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (presetUri) {
            "preset://sunset_sky" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4A3B7C),
                            Color(0xFF9E6B9E),
                            Color(0xFFE8998D),
                            Color(0xFF2D2346)
                        )
                    )
                )
                drawOval(
                    color = Color(0xFFFFB5A7).copy(alpha = 0.32f),
                    topLeft = Offset(w * 0.1f, h * 0.28f),
                    size = Size(w * 0.8f, h * 0.16f)
                )
                drawOval(
                    color = Color(0xFF3D2E5E).copy(alpha = 0.55f),
                    topLeft = Offset(-w * 0.1f, h * 0.45f),
                    size = Size(w * 0.9f, h * 0.22f)
                )
            }
            "preset://coffee_cup" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF5C4033),
                            Color(0xFF8B654E),
                            Color(0xFF3A261B)
                        )
                    )
                )
                drawOval(
                    color = Color(0xFFEFECE6),
                    topLeft = Offset(w * 0.16f, h * 0.40f),
                    size = Size(w * 0.68f, h * 0.34f)
                )
                drawCircle(
                    color = Color(0xFFFDFBF7),
                    radius = w * 0.24f,
                    center = Offset(w * 0.50f, h * 0.50f)
                )
                drawCircle(
                    color = Color(0xFFC68B59),
                    radius = w * 0.20f,
                    center = Offset(w * 0.50f, h * 0.50f)
                )
                drawCircle(
                    color = Color(0xFFFDFBF7),
                    radius = w * 0.06f,
                    center = Offset(w * 0.50f, h * 0.49f)
                )
            }
            "preset://dream_silhouette" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF5E648E),
                            Color(0xFFB3889B),
                            Color(0xFFE6A17E),
                            Color(0xFF1B1926)
                        )
                    )
                )
                drawCircle(
                    color = Color(0xFF0E0D14),
                    radius = w * 0.11f,
                    center = Offset(w * 0.50f, h * 0.46f)
                )
                val body = Path().apply {
                    moveTo(w * 0.30f, h * 0.85f)
                    quadraticTo(w * 0.34f, h * 0.58f, w * 0.50f, h * 0.58f)
                    quadraticTo(w * 0.66f, h * 0.58f, w * 0.70f, h * 0.85f)
                    close()
                }
                drawPath(path = body, color = Color(0xFF0E0D14))
            }
            "preset://birds_sky" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF8CA6DB),
                            Color(0xFFD4A5B8),
                            Color(0xFFF3C6A8),
                            Color(0xFF2B283A)
                        )
                    )
                )
                val birdPositions = listOf(
                    Offset(w * 0.35f, h * 0.35f),
                    Offset(w * 0.52f, h * 0.28f),
                    Offset(w * 0.64f, h * 0.42f),
                    Offset(w * 0.44f, h * 0.48f)
                )
                birdPositions.forEach { pos ->
                    drawArc(
                        color = Color(0xFF2B283A),
                        startAngle = 200f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = pos,
                        size = Size(18f, 10f),
                        style = Stroke(width = 3f)
                    )
                }
            }
            else -> {
                val colors = when (postType) {
                    "audio" -> listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFF1E1B4B))
                    "video" -> listOf(Color(0xFF3730A3), Color(0xFF6366F1), Color(0xFF0F172A))
                    else -> listOf(Color(0xFF6E66FF), Color(0xFF9B8AFB), Color(0xFF2E266D))
                }
                drawRect(brush = Brush.verticalGradient(colors = colors))
            }
        }
    }
}

/**
 * Floating Rounded Bottom Navigation Bar (shared across Feed, Create, Explore, Activity, and Profile)
 * Synchronized with the active MaterialTheme primary color so any theme changes immediately apply to the navigation bar.
 */
@Composable
fun AnonymousnetBottomBar(
    currentTab: MainTab,
    unreadNotificationCount: Int,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val activePillBg = activeColor.copy(alpha = 0.14f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = activeColor.copy(alpha = 0.22f)
                ),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                BottomBarIconItem(
                    selected = currentTab == MainTab.HOME,
                    activeBg = activePillBg,
                    onClick = { onSelectTab(MainTab.HOME) },
                    testTag = "nav_home_button",
                    contentDescription = "Home Feed"
                ) {
                    Icon(
                        imageVector = if (currentTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home Feed",
                        tint = if (currentTab == MainTab.HOME) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Explore
                BottomBarIconItem(
                    selected = currentTab == MainTab.EXPLORE,
                    activeBg = activePillBg,
                    onClick = { onSelectTab(MainTab.EXPLORE) },
                    testTag = "nav_explore_button",
                    contentDescription = "Explore"
                ) {
                    Icon(
                        imageVector = if (currentTab == MainTab.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore,
                        contentDescription = "Explore",
                        tint = if (currentTab == MainTab.EXPLORE) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 3. Center Create (+) Active Themed Circle Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(10.dp, CircleShape, spotColor = activeColor.copy(alpha = 0.5f))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(secondaryColor, activeColor)
                            )
                        )
                        .clickable { onSelectTab(MainTab.CREATE) }
                        .testTag("nav_create_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Create Confession",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // 4. Activity / Inbox / Direct Messages
                BottomBarIconItem(
                    selected = currentTab == MainTab.ACTIVITY,
                    activeBg = activePillBg,
                    onClick = { onSelectTab(MainTab.ACTIVITY) },
                    testTag = "nav_activity_button",
                    contentDescription = "Activity and Messages"
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Activity",
                            tint = if (currentTab == MainTab.ACTIVITY) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(23.dp)
                        )
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AnonHeartRed)
                            )
                        }
                    }
                }

                // 5. Profile
                BottomBarIconItem(
                    selected = currentTab == MainTab.PROFILE,
                    activeBg = activePillBg,
                    onClick = { onSelectTab(MainTab.PROFILE) },
                    testTag = "nav_profile_button",
                    contentDescription = "Profile and Settings"
                ) {
                    Icon(
                        imageVector = if (currentTab == MainTab.PROFILE) Icons.Filled.Person else Icons.Outlined.PersonOutline,
                        contentDescription = "Profile",
                        tint = if (currentTab == MainTab.PROFILE) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarIconItem(
    selected: Boolean,
    activeBg: Color,
    onClick: () -> Unit,
    testTag: String,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) activeBg else Color.Transparent)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
