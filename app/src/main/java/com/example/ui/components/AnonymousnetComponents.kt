package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.MainTab
import com.example.ui.theme.AnonHeartRed
import com.example.ui.theme.LocalCompactMode

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
            if (w <= 0f || h <= 0f) return@Canvas
            val center = Offset(w / 2f, h / 2f)
            val radius = (w * 0.45f).coerceAtLeast(1f)
            val outerRadius = (w * 0.5f).coerceAtLeast(1f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.28f),
                        primaryColor.copy(alpha = 0.0f)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
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
    showVerifiedBadge: Boolean = false,
    avatarUri: String? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUri.isNullOrBlank()) {
            AsyncImage(
                model = avatarUri,
                contentDescription = "Anonymous Profile Picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .border(2.dp, primaryColor.copy(alpha = 0.4f), CircleShape)
            )
        } else {
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
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Canvas(modifier = modifier) {
        val colors = when (postType) {
            "audio" -> listOf(primary, secondary, Color(0xFF1E1B4B))
            "video" -> listOf(secondary, primary, Color(0xFF0F172A))
            else -> listOf(primary, secondary, Color(0xFF2E266D))
        }
        drawRect(brush = Brush.verticalGradient(colors = colors))
    }
}

/**
 * Floating Rounded Bottom Navigation Bar (shared across Feed, Create, Explore, Activity, and Profile)
 * Synchronized with the active MaterialTheme primary color and Compact Mode.
 */
@Composable
fun AnonymousnetBottomBar(
    currentTab: MainTab,
    unreadNotificationCount: Int,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalCompactMode.current
    val activeColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val activePillBg = activeColor.copy(alpha = 0.14f)
    val barHeight by animateDpAsState(
        targetValue = if (isCompact) 58.dp else 64.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bottom_bar_height"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = if (isCompact) 14.dp else 20.dp, vertical = if (isCompact) 6.dp else 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
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
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                BottomBarIconItem(
                    selected = currentTab == MainTab.HOME,
                    activeBg = activePillBg,
                    onClick = { onSelectTab(MainTab.HOME) },
                    testTag = "nav_home_button"
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
                    testTag = "nav_explore_button"
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
                    testTag = "nav_activity_button"
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
                    testTag = "nav_profile_button"
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
    icon: @Composable () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) activeBg else Color.Transparent,
        label = "nav_item_bg"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
