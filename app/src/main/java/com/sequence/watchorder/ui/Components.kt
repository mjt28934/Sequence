package com.sequence.watchorder.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.data.Franchise
import com.sequence.watchorder.data.Progress
import com.sequence.watchorder.data.Work

fun plural(n: Int, one: String, many: String): String = "$n ${if (n == 1) one else many}"

/** "24 titles · 17 books", short enough to fit a two-column poster tile. */
fun countLine(titles: Int, books: Int): String = buildString {
    append(plural(titles, "title", "titles"))
    if (books > 0) append(" · ").append(plural(books, "book", "books"))
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = modifier.size(26.dp)) {
        val y = size.height / 2f
        drawLine(
            color = color,
            start = Offset(size.width * 0.16f, y),
            end = Offset(size.width * 0.88f, y),
            strokeWidth = size.width * 0.065f,
            cap = StrokeCap.Round,
        )
        drawCircle(color, radius = size.width * 0.13f, center = Offset(size.width * 0.18f, y))
        drawCircle(color, radius = size.width * 0.13f, center = Offset(size.width * 0.5f, y))
        drawCircle(
            color = color,
            radius = size.width * 0.105f,
            center = Offset(size.width * 0.82f, y),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.06f),
        )
    }
}

/** Section title with an optional trailing text action, the shelf idiom Android media apps use. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Thin progress strip used on cards; Material 3 colours, no full-width chrome. */
@Composable
fun ProgressStrip(
    pct: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    height: Dp = 4.dp,
) {
    val target = pct.coerceIn(0, 100) / 100f
    // Marking something seen moves the bar rather than teleporting it, which is what makes the
    // action feel like it did something. Reduce motion keeps the bar honest and still.
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = if (Motion.enabled) {
            tween(durationMillis = SeqMotion.PROGRESS_MS, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "progress",
    )
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        color = color,
        trackColor = track,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/**
 * The primary tap target for marking something seen. Uses a filled icon button so it reads as a
 * real control, and fires a haptic tick exactly like the system toggles.
 */
@Composable
fun SeenToggle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 40.dp,
) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    // The fill and the tick ease between states instead of hard-swapping, and the tick lands with
    // a short spring so the action feels acknowledged.
    val container by animateColorAsState(
        targetValue = if (checked) scheme.primary else Color.Transparent,
        animationSpec = seqTween(SeqMotion.SHORT_MS),
        label = "seenContainer",
    )
    val content by animateColorAsState(
        targetValue = if (checked) scheme.onPrimary else scheme.onSurfaceVariant,
        animationSpec = seqTween(SeqMotion.SHORT_MS),
        label = "seenContent",
    )
    val pop = rememberFlagPop(checked)

    IconButton(
        onClick = {
            Haptics.tick(haptics)
            onToggle()
        },
        enabled = enabled,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = if (checked) "Mark as unseen" else "Mark as seen" },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = container,
            contentColor = content,
        ),
    ) {
        Icon(
            imageVector = if (checked) SeqIcons.Check else SeqIcons.CheckOutline,
            contentDescription = null,
            modifier = Modifier
                .size(size * 0.55f)
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                },
        )
    }
}

/** Small tonal label for year, kind, format and similar metadata. */
@Composable
fun MetaPill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Horizontal, scrollable filter chip row with consistent edge padding. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeqFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    count: Int? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Text(
                if (count == null) label else "$label · $count",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Wide shelf card: artwork with the title laid over it, used by the horizontal rails. */
@Composable
fun ShelfCard(
    franchise: Franchise,
    title: String,
    subtitle: String,
    progress: Progress,
    accent: AccentPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .width(268.dp)
            .pressScale(interaction),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
            ) {
                ArtImage(ArtKind.POSTER, franchise.id, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.35f to Color.Transparent,
                                1f to Color(0xE60B0C10),
                            ),
                        ),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ProgressStrip(
                pct = progress.pct,
                color = accent.accent,
                track = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/** Grid tile used by the library: artwork, name and progress, sized for adaptive columns. */
@Composable
fun PosterTile(
    franchise: Franchise,
    subtitle: String,
    progress: Progress,
    accent: AccentPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    longClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val complete = progress.total > 0 && progress.done >= progress.total
    Column(modifier = modifier.pressScale(interaction)) {
        Card(
            onClick = onClick,
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f),
            ) {
                ArtImage(ArtKind.POSTER, franchise.id, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1f to Color(0x99000000),
                            ),
                        ),
                )
                if (Motion.enabled) {
                    // The check arrives with the progress, rather than being there from the start.
                    FinishedBadgeSlot(
                        complete = complete,
                        accent = accent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    )
                } else if (complete) {
                    FinishedBadge(accent, Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ProgressStrip(
                pct = progress.pct,
                color = accent.accent,
                track = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }
        Text(
            text = franchise.name,
            modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (longClickLabel != null) {
            Text(
                text = longClickLabel,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The "finished" tick in the corner of a poster. */
@Composable
private fun FinishedBadge(accent: AccentPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(accent.accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            SeqIcons.Check,
            contentDescription = "Finished",
            tint = accent.ink,
            modifier = Modifier.size(15.dp),
        )
    }
}

/**
 * Scrolls the finished tick in and out with the poster. A separate composable so the top-level
 * [AnimatedVisibility] overload is used rather than a scoped one from the surrounding layout.
 */
@Composable
private fun FinishedBadgeSlot(complete: Boolean, accent: AccentPalette, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = complete,
        modifier = modifier,
        enter = fadeIn(tween(SeqMotion.SHORT_MS)) +
            scaleIn(
                initialScale = 0.7f,
                animationSpec = spring(
                    dampingRatio = SeqMotion.BADGE_DAMPING,
                    stiffness = SeqMotion.BADGE_STIFFNESS,
                ),
            ),
        exit = fadeOut(tween(SeqMotion.PRESS_MS)) +
            scaleOut(targetScale = 0.85f, animationSpec = tween(SeqMotion.PRESS_MS)),
    ) {
        FinishedBadge(accent)
    }
}

/** Book cover tile with a check badge, used on shelves and reading paths. */
@Composable
fun CoverTile(
    work: Work,
    accent: AccentPalette,
    read: Boolean,
    isNext: Boolean,
    onClick: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    // Dimming a read cover is a state change, so ease it instead of snapping to half opacity.
    val artAlpha by animateFloatAsState(
        targetValue = if (read) 0.45f else 1f,
        animationSpec = seqTween(SeqMotion.STANDARD_MS),
        label = "coverRead",
    )
    val badgeFill by animateColorAsState(
        targetValue = if (read) accent.accent else MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
        animationSpec = seqTween(SeqMotion.SHORT_MS),
        label = "badgeFill",
    )
    val badgeStroke by animateColorAsState(
        targetValue = if (read) accent.accent else Color.White.copy(alpha = 0.75f),
        animationSpec = seqTween(SeqMotion.SHORT_MS),
        label = "badgeStroke",
    )
    val badgeInk by animateColorAsState(
        targetValue = if (read) accent.ink else Color.White,
        animationSpec = seqTween(SeqMotion.SHORT_MS),
        label = "badgeInk",
    )
    val nextBorder by animateColorAsState(
        targetValue = if (isNext) accent.accent else Color.Transparent,
        animationSpec = seqTween(SeqMotion.STANDARD_MS),
        label = "nextBorder",
    )
    val pop = rememberFlagPop(read)
    Column(modifier = modifier.pressScale(interaction)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onClick,
                ),
        ) {
            ArtImage(
                ArtKind.COVER,
                work.id,
                Modifier
                    .fillMaxSize(),
                alpha = artAlpha,
            )
            if (isNext || nextBorder.alpha > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .border(2.dp, nextBorder, MaterialTheme.shapes.medium),
                )
            }
            val haptics = LocalHapticFeedback.current
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .graphicsLayer {
                        scaleX = pop
                        scaleY = pop
                    }
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(badgeFill)
                    .border(1.5.dp, badgeStroke, CircleShape)
                    .clickable {
                        Haptics.tick(haptics)
                        onToggleRead()
                    }
                    .semantics { contentDescription = if (read) "Mark unread" else "Mark read" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (read) SeqIcons.Check else SeqIcons.CheckOutline,
                    contentDescription = null,
                    tint = badgeInk,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (showTitle) {
            Text(
                work.title,
                modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                work.by,
                modifier = Modifier.padding(start = 2.dp, end = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A full-width tappable row with the same feedback as a card: ripple, tonal surface and the shared
 * press scale, all driven by one interaction source so they react to a single gesture.
 */
@Composable
fun PressableRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction, pressedScale = 0.98f),
        shape = shape,
        color = color,
        contentColor = contentColor,
        content = content,
    )
}

@Composable
fun RowThumb(franchise: Franchise, modifier: Modifier = Modifier, width: Dp = 88.dp) {
    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(16f / 10f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        ArtImage(ArtKind.POSTER, franchise.id, Modifier.fillMaxSize())
    }
}

@Composable
fun CoverThumb(work: Work, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(44.dp)
            .aspectRatio(2f / 3f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        ArtImage(ArtKind.COVER, work.id, Modifier.fillMaxSize())
    }
}

@Composable
fun GenreTile(
    genre: String,
    franchise: Franchise,
    accent: AccentPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int = 0,
) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = accent.deep),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(108.dp),
        ) {
            ArtImage(
                ArtKind.POSTER,
                franchise.id,
                Modifier
                    .fillMaxSize()
                    .align(Alignment.CenterEnd),
                alpha = 0.55f,
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to accent.deep,
                            1f to accent.deep.copy(alpha = 0.35f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp, end = 40.dp),
            ) {
                Text(
                    genre,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (count > 0) {
                    Text(
                        plural(count, "franchise", "franchises"),
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                SeqIcons.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(18.dp),
            )
        }
    }
}

/** Navigation rail/sheet style header. */
@Composable
fun SheetHeader(title: String, subtitle: String? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SheetOptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            RadioButton(selected = selected, onClick = null)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
    )
}

@Composable
fun SheetToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp),
    )
}

@Composable
fun SheetActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false,
    subtitle: String? = null,
) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        headlineContent = {
            Text(
                title,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
    )
}

/** Full-width action button used at the bottom of sheets. */
@Composable
fun SheetButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    container: Color = Color.Unspecified,
    content: Color = Color.Unspecified,
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor = when {
        container != Color.Unspecified -> container
        danger -> scheme.error
        tonal -> scheme.secondaryContainer
        else -> scheme.primary
    }
    val contentColor = when {
        content != Color.Unspecified -> content
        danger -> scheme.onError
        tonal -> scheme.onSecondaryContainer
        else -> scheme.onPrimary
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SheetNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun SeqSnackbar(data: SnackbarData) = Snackbar(
    snackbarData = data,
    shape = MaterialTheme.shapes.small,
    containerColor = MaterialTheme.colorScheme.inverseSurface,
    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    actionContentColor = MaterialTheme.colorScheme.inversePrimary,
)

/** A divider that respects the Material 3 inset convention. */
@Composable
fun InsetDivider(start: Dp = 16.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

/** Filled tonal icon button used in top app bars. */
@Composable
fun BarIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        // Swapping between two meanings (search/close, save/unsave, grid/list) is a small state
        // change, so the glyph crossfades instead of snapping.
        if (!Motion.enabled) {
            Icon(icon, contentDescription = contentDescription)
        } else {
            AnimatedContent(
                targetState = icon,
                transitionSpec = {
                    (fadeIn(tween(SeqMotion.SHORT_MS)) + scaleIn(initialScale = 0.84f)) togetherWith
                        (fadeOut(tween(SeqMotion.PRESS_MS)) + scaleOut(targetScale = 0.9f))
                },
                label = "barIcon",
            ) { shown ->
                Icon(shown, contentDescription = contentDescription)
            }
        }
    }
}

@Composable
fun TonalIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledIconButton(onClick = onClick, modifier = modifier.size(40.dp)) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}
