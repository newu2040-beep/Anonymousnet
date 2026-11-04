package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ConfessionPost
import com.example.ui.ExploreFilter
import com.example.ui.ExploreSort
import com.example.ui.UiState
import com.example.ui.components.ExplorePresetArtwork
import com.example.ui.theme.AnonHeartRed
import com.example.ui.theme.AnonPrimaryIndigo
import com.example.ui.theme.AnonPrimaryViolet
import com.example.ui.theme.AnonStarGold

/**
 * Screen 4 — Explore
 * Matches the fourth reference panel:
 * - Header: "Explore" title on left, Search icon on right
 * - Horizontal filter chips: All, Text, Image, Video, Audio (+ Popular / Recent sort toggle)
 * - Two-column visual grid cards with overlaid caption on media thumbnail and compact engagement counts beneath each card
 * - Bottom violet banner CTA: gold star + "Discover more amazing confessions" + `>` button
 */
@Composable
fun ExploreScreen(
    explorePostsState: UiState<List<ConfessionPost>>,
    selectedFilter: ExploreFilter,
    selectedSort: ExploreSort,
    searchQuery: String,
    isSearchVisible: Boolean,
    onSelectFilter: (ExploreFilter) -> Unit,
    onSelectSort: (ExploreSort) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onOpenPostDetail: (ConfessionPost) -> Unit,
    onLikePost: (ConfessionPost) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_screen_list"),
        contentPadding = PaddingValues(
            start = 18.dp,
            end = 18.dp,
            top = 12.dp,
            bottom = 104.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: Explore title + Search icon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.05f))
                        .clip(CircleShape)
                        .clickable(onClick = onToggleSearch)
                        .testTag("explore_search_button"),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "Search Confessions",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        // Expandable Search Input + Sort Toggle
        item {
            AnimatedVisibility(visible = isSearchVisible) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search text, topics (#coffee, #healing)...") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("explore_search_input")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort by:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ExploreSort.entries.forEach { sort ->
                            val active = selectedSort == sort
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectSort(sort) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (active) AnonPrimaryViolet else MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = sort.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Horizontal Filter Chips: All, Text, Image, Video, Audio
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(ExploreFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .clickable { onSelectFilter(filter) }
                            .testTag("explore_chip_${filter.name.lowercase()}"),
                        shape = RoundedCornerShape(19.dp),
                        color = if (isSelected) AnonPrimaryViolet else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Two-Column Visual Grid of Confessions
        when (explorePostsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AnonPrimaryViolet)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = explorePostsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is UiState.Success -> {
                // Prioritize cards with visual/caption styling first so the 2x2 grid mirrors Screen 4 reference
                val sortedForGrid = explorePostsState.data.sortedByDescending {
                    if (!it.mediaUri.isNullOrBlank()) 1 else 0
                }
                val rows = sortedForGrid.chunked(2)

                if (rows.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No matching confessions found",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try selecting 'All' or clearing your search filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(rows) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            pair.forEach { post ->
                                ExploreGridCardItem(
                                    post = post,
                                    onClick = { onOpenPostDetail(post) },
                                    onLikeClick = { onLikePost(post) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 4. Bottom Violet Discovery Banner CTA ("Discover more amazing confessions")
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = AnonPrimaryViolet.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF7B73FF), AnonPrimaryViolet, Color(0xFF8E7CFF))
                        )
                    )
                    .clickable {
                        onSelectFilter(ExploreFilter.ALL)
                        onSelectSort(
                            if (selectedSort == ExploreSort.POPULAR) ExploreSort.RECENT else ExploreSort.POPULAR
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .testTag("explore_discover_more_cta")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = AnonStarGold,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Discover more\namazing confessions",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Discover More",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreGridCardItem(
    post: ConfessionPost,
    onClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("explore_card_${post.id}")
    ) {
        // Thumbnail Card with Overlaid Short Caption at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(22.dp))
        ) {
            if (post.mediaUri == null || post.mediaUri.startsWith("preset://")) {
                ExplorePresetArtwork(
                    presetUri = post.mediaUri,
                    postType = post.type,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = post.mediaUri,
                    contentDescription = post.mediaCaption ?: post.text,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom gradient scrim for caption legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )

            // Overlaid short caption matching Screen 4 reference
            Text(
                text = post.mediaCaption?.takeIf { it.isNotBlank() } ?: post.text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Engagement counts beneath card: ❤️ 1.2K   💬 389
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onLikeClick)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Likes",
                    tint = AnonHeartRed,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatCompactCount(post.likeCount),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatCompactCount(post.commentCount),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
