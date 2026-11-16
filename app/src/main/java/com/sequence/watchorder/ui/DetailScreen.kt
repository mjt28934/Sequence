package com.sequence.watchorder.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.DetailTab
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.Sheet
import com.sequence.watchorder.data.BookFilter
import com.sequence.watchorder.data.Entry
import com.sequence.watchorder.data.Franchise
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.OrderMode
import com.sequence.watchorder.data.WatchFilter
import com.sequence.watchorder.data.Work
import kotlinx.coroutines.launch

private sealed interface DetailRow {
    data class EraHeader(val label: String) : DetailRow
    data class WatchRow(val entry: Entry, val position: Int) : DetailRow
    data class PathHeader(val label: String) : DetailRow
    data class CoverRow(val items: List<Pair<Work, Int>>) : DetailRow
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(vm: MainViewModel, franchise: Franchise) {
    val state = vm.progress
    val today = vm.today()
    val accent = vm.catalog.accent(franchise.id).toPalette()
    val works = vm.catalog.worksFor(franchise.id)
    val hasBooks = works.isNotEmpty()
    val showingBooks = vm.detailTab == DetailTab.BOOKS && hasBooks

    val watched = vm.watchedFor(franchise.id).toSet()
    val read = vm.readFor(franchise.id).toSet()
    val seen = Logic.franchiseProgress(franchise, watched, today)
    val bookProgress = Logic.readProgress(works, read)
    val saved = franchise.id in state.saved
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val rows: List<DetailRow> = remember(
        franchise,
        showingBooks,
        watched,
        read,
        today,
        state.mode,
        vm.watchFilter,
        vm.bookFilter,
    ) {
        if (showingBooks) {
            buildBookRows(vm, franchise, read)
        } else {
            buildWatchRows(vm, franchise, watched, today)
        }
    }

    val nextWatch = Logic.nextUp(
        Logic.orderEntries(franchise, state.mode),
        watched,
        today,
        vm.watchFilter,
    )
    val nextBook = Logic.nextToRead(Logic.orderWorks(works, state.mode), read, vm.bookFilter)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        franchise.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    BarIconButton(SeqIcons.Back, "Back") { vm.closeDetail() }
                },
                actions = {
                    BarIconButton(
                        if (saved) SeqIcons.Bookmark else SeqIcons.BookmarkOff,
                        if (saved) "Remove from library" else "Save to library",
                    ) { vm.toggleSaved(franchise.id) }
                    BarIconButton(SeqIcons.More, "More") { vm.openSheet(Sheet.More) }
                    Spacer(Modifier.width(4.dp))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            val target = if (showingBooks) nextBook else nextWatch
            if (target != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val index = rows.indexOfFirst {
                            when (it) {
                                is DetailRow.WatchRow -> it.entry.id == (target as? Entry)?.id
                                is DetailRow.CoverRow -> it.items.any { pair -> pair.first.id == (target as? Work)?.id }
                                else -> false
                            }
                        }
                        if (index >= 0) scope.launch { listState.animateScrollToItem(index + 1) }
                    },
                    icon = { Icon(SeqIcons.PlayArrow, contentDescription = null) },
                    text = { Text(if (showingBooks) "Next book" else "Next up") },
                    containerColor = accent.accent,
                    contentColor = accent.ink,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 96.dp,
            ),
        ) {
            item("hero") {
                Hero(
                    franchise = franchise,
                    accent = accent,
                    seen = seen,
                    saved = saved,
                    onSave = { vm.toggleSaved(franchise.id) },
                    scrollState = listState,
                )
            }

            item("blurb") {
                BlurbCard(
                    franchise = franchise,
                    accent = accent,
                    open = vm.blurbOpen,
                    onToggle = { vm.toggleBlurb() },
                )
            }

            item("actions") {
                ActionRow(
                    accent = accent,
                    onOrder = { vm.openSheet(Sheet.Order) },
                    onFilter = { vm.openSheet(Sheet.Filter) },
                    onMore = { vm.openSheet(Sheet.More) },
                    saved = saved,
                    onSave = { vm.toggleSaved(franchise.id) },
                )
            }

            if (hasBooks) {
                item("tabs") {
                    DetailTabs(
                        showingBooks = showingBooks,
                        onWatch = { vm.selectDetailTab(DetailTab.WATCH) },
                        onBooks = { vm.selectDetailTab(DetailTab.BOOKS) },
                    )
                }
            }

            if (showingBooks) {
                item("bookmeta") {
                    FilterSummary(
                        text = describeBookFilter(vm.bookFilter, works),
                        progress = bookProgress,
                        accent = accent,
                        onOpen = { vm.openSheet(Sheet.Filter) },
                    )
                }
                itemsIndexed(rows, key = { index, row ->
                    when (row) {
                        is DetailRow.PathHeader -> "ph-$index-${row.label}"
                        is DetailRow.CoverRow -> "cr-$index-${row.items.first().first.id}"
                        else -> "row-$index"
                    }
                }) { index, row ->
                    Box(seqItem()) {
                        when (row) {
                            is DetailRow.PathHeader -> EraLabel(row.label)
                            is DetailRow.CoverRow -> CoverRow(
                                vm = vm,
                                franchise = franchise,
                                items = row.items,
                                accent = accent,
                                read = read,
                                nextBookId = nextBook?.id,
                            )
                            else -> Unit
                        }
                    }
                }
            } else {
                item("watchmeta") {
                    FilterSummary(
                        text = describeWatchFilter(vm.watchFilter, franchise),
                        progress = seen,
                        accent = accent,
                        onOpen = { vm.openSheet(Sheet.Filter) },
                    )
                }
                itemsIndexed(rows, key = { index, row ->
                    when (row) {
                        is DetailRow.EraHeader -> "eh-$index-${row.label}"
                        is DetailRow.WatchRow -> "wr-${row.entry.id}"
                        else -> "row-$index"
                    }
                }) { _, row ->
                    Box(seqItem()) {
                        when (row) {
                            is DetailRow.EraHeader -> EraLabel(row.label)
                            is DetailRow.WatchRow -> SwipeableWatchRow(
                                vm = vm,
                                franchise = franchise,
                                entry = row.entry,
                                position = row.position,
                                accent = accent,
                                watched = row.entry.id in watched,
                                upcoming = Logic.isUpcoming(row.entry, today),
                            )
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(
    franchise: Franchise,
    accent: AccentPalette,
    seen: com.sequence.watchorder.data.Progress,
    saved: Boolean,
    onSave: () -> Unit,
    scrollState: LazyListState,
) {
    val drift by remember(scrollState) {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex == 0) {
                scrollState.firstVisibleItemScrollOffset.toFloat()
            } else {
                0f
            }
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clipToBounds(),
    ) {
        ArtImage(
            ArtKind.POSTER,
            franchise.id,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (Motion.enabled) {
                        // The art trails the page as it scrolls, and grows just enough that the
                        // parallax never exposes an edge of the box.
                        val lift = drift * 0.38f
                        translationY = -lift
                        val grow = if (size.height > 0f) 2f * lift / size.height else 0f
                        scaleX = 1.02f + grow
                        scaleY = 1.02f + grow
                    }
                },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color(0xF20B0C10),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                franchise.name,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${franchise.years} · ${franchise.genre} · ${Logic.describeCounts(franchise)}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetaPill(
                    text = "${seen.done} of ${seen.total} watched",
                    container = accent.accent,
                    content = accent.ink,
                )
                if (saved) {
                    MetaPill(text = "In library", container = Color.White.copy(alpha = 0.18f), content = Color.White)
                }
            }
            ProgressStrip(
                pct = seen.pct,
                color = accent.accent,
                modifier = Modifier.padding(top = 12.dp),
                track = Color.White.copy(alpha = 0.22f),
            )
        }
    }
}

@Composable
private fun BlurbCard(franchise: Franchise, accent: AccentPalette, open: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .then(
                    if (Motion.enabled) {
                        Modifier.animateContentSize(
                            animationSpec = tween(SeqMotion.STANDARD_MS, easing = FastOutSlowInEasing),
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            Text(
                franchise.hook,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                franchise.blurb,
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (open) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (franchise.storyHint.isNotEmpty() && open) {
                Text(
                    franchise.storyHint,
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = accent.accent,
                )
            }
            TextButton(
                onClick = onToggle,
                modifier = Modifier.padding(top = 4.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text(if (open) "Show less" else "Read more")
            }
        }
    }
}

@Composable
private fun ActionRow(
    accent: AccentPalette,
    onOrder: () -> Unit,
    onFilter: () -> Unit,
    onMore: () -> Unit,
    saved: Boolean,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SmallAction("Order", SeqIcons.Sort, onOrder)
        SmallAction("Filter", SeqIcons.Tune, onFilter)
        SmallAction(if (saved) "Saved" else "Save", if (saved) SeqIcons.Bookmark else SeqIcons.BookmarkOff, onSave)
        SmallAction("More", SeqIcons.More, onMore)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmallAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    androidx.compose.material3.AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

@Composable
private fun DetailTabs(showingBooks: Boolean, onWatch: () -> Unit, onBooks: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        SingleChoiceSegmentedButtonRow(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SegmentedButton(
                selected = !showingBooks,
                onClick = onWatch,
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Watch order") }
            SegmentedButton(
                selected = showingBooks,
                onClick = onBooks,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text("Books") }
        }
    }
}

@Composable
private fun FilterSummary(
    text: String,
    progress: com.sequence.watchorder.data.Progress,
    accent: AccentPalette,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProgressStrip(
                pct = progress.pct,
                color = accent.accent,
                modifier = Modifier.padding(top = 6.dp, end = 8.dp),
            )
        }
        TextButton(onClick = onOpen) { Text("Filter") }
    }
}

@Composable
private fun EraLabel(label: String) {
    Text(
        label,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableWatchRow(
    vm: MainViewModel,
    franchise: Franchise,
    entry: Entry,
    position: Int,
    accent: AccentPalette,
    watched: Boolean,
    upcoming: Boolean,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd && !upcoming && !watched) {
                vm.toggleSeen(franchise.id, entry)
                true
            } else {
                false
            }
        },
    )
    LaunchedEffect(watched) {
        if (watched && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.accent.copy(alpha = 0.9f))
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(SeqIcons.Check, contentDescription = null, tint = accent.ink)
                    Text("Mark seen", color = accent.ink, style = MaterialTheme.typography.labelLarge)
                }
            }
        },
    ) {
        WatchRow(
            vm = vm,
            franchise = franchise,
            entry = entry,
            position = position,
            accent = accent,
            watched = watched,
            upcoming = upcoming,
        )
    }
}

@Composable
private fun WatchRow(
    vm: MainViewModel,
    franchise: Franchise,
    entry: Entry,
    position: Int,
    accent: AccentPalette,
    watched: Boolean,
    upcoming: Boolean,
) {
    val interaction = remember { MutableInteractionSource() }
    // Watching a title dims its row and fills its position chip; both ease so the list reads as
    // one state change instead of two hard cuts.
    val rowColor by animateColorAsState(
        targetValue = if (watched) {
            MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = seqTween(SeqMotion.STANDARD_MS),
        label = "rowColor",
    )
    val chipColor by animateColorAsState(
        targetValue = if (watched) {
            accent.accent
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = seqTween(SeqMotion.STANDARD_MS),
        label = "chipColor",
    )
    val chipInk by animateColorAsState(
        targetValue = if (watched) accent.ink else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = seqTween(SeqMotion.STANDARD_MS),
        label = "chipInk",
    )
    Surface(
        onClick = { vm.openSheet(Sheet.EntrySheet(entry.id)) },
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .pressScale(interaction, pressedScale = 0.98f),
        shape = RoundedCornerShape(16.dp),
        color = rowColor,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = chipColor,
                modifier = Modifier.size(30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        position.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = chipInk,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(entry.year)
                        append(" · ")
                        append(entry.kindLabel)
                        if (!entry.era.isNullOrEmpty()) append(" · ").append(entry.era)
                        if (entry.optional) append(" · Optional")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (upcoming) {
                MetaPill("Soon")
            } else {
                SeenToggle(
                    checked = watched,
                    onToggle = { vm.toggleSeen(franchise.id, entry) },
                )
            }
        }
    }
}

@Composable
private fun CoverRow(
    vm: MainViewModel,
    franchise: Franchise,
    items: List<Pair<Work, Int>>,
    accent: AccentPalette,
    read: Set<String>,
    nextBookId: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEach { (work, _) ->
            CoverTile(
                work = work,
                accent = accent,
                read = work.id in read,
                isNext = work.id == nextBookId,
                onClick = { vm.openSheet(Sheet.WorkSheet(work.id)) },
                onToggleRead = { vm.toggleRead(franchise.id, work) },
                modifier = Modifier.weight(1f),
            )
        }
        repeat(3 - items.size) { Spacer(Modifier.weight(1f)) }
    }
}

private fun buildWatchRows(
    vm: MainViewModel,
    franchise: Franchise,
    watched: Set<String>,
    today: String,
): List<DetailRow> {
    val ordered = Logic.orderEntries(franchise, vm.progress.mode)
    val positions = Logic.withPositions(ordered)
    val visible = Logic.visibleItems(positions, watched, effectiveWatchFilter(vm, franchise))
    val groups = Logic.groupItems(visible, vm.progress.mode)
    return buildList {
        for (group in groups) {
            if (!group.label.isNullOrEmpty()) add(DetailRow.EraHeader(group.label))
            for (item in group.items) add(DetailRow.WatchRow(item.entry, item.position))
        }
    }
}

private fun buildBookRows(
    vm: MainViewModel,
    franchise: Franchise,
    read: Set<String>,
): List<DetailRow> {
    val works = vm.catalog.worksFor(franchise.id)
    val ordered = Logic.orderWorks(works, vm.progress.mode)
    val positions = Logic.withWorkPositions(ordered)
    val visible = Logic.visibleWorks(positions, read, effectiveBookFilter(vm, works))
    val groups = Logic.groupWorks(visible, vm.progress.mode)
    return buildList {
        for (group in groups) {
            if (!group.label.isNullOrEmpty()) add(DetailRow.PathHeader(group.label))
            for (chunk in group.items.chunked(3)) {
                add(DetailRow.CoverRow(chunk.map { it.work to it.position }))
            }
        }
    }
}

private fun effectiveWatchFilter(vm: MainViewModel, franchise: Franchise): WatchFilter = WatchFilter(
    kind = if (Logic.hasMixedKinds(franchise)) vm.watchFilter.kind else "all",
    hideSeen = vm.watchFilter.hideSeen,
    essentials = vm.watchFilter.essentials,
)

private fun effectiveBookFilter(vm: MainViewModel, works: List<Work>): BookFilter {
    val formats = Logic.formatCounts(works)
    val active = Work.FORMATS.filter { (formats[it] ?: 0) > 0 }
    return BookFilter(
        format = if (active.size > 1) vm.bookFilter.format else "all",
        hideRead = vm.bookFilter.hideRead,
        essentials = vm.bookFilter.essentials,
    )
}

private fun describeWatchFilter(filter: WatchFilter, franchise: Franchise): String {
    val parts = mutableListOf<String>()
    if (Logic.hasMixedKinds(franchise)) {
        parts += when (filter.kind) {
            "film" -> "Films only"
            "series" -> "Series and specials"
            else -> "Everything"
        }
    } else {
        parts += "Everything"
    }
    if (filter.essentials) parts += "essentials only"
    if (filter.hideSeen) parts += "seen hidden"
    return parts.joinToString(" · ")
}

private fun describeBookFilter(filter: BookFilter, works: List<Work>): String {
    val parts = mutableListOf<String>()
    parts += if (filter.format == "all") {
        "All formats"
    } else {
        Work.FORMAT_LABELS[filter.format] ?: filter.format
    }
    if (filter.essentials) parts += "essentials only"
    if (filter.hideRead) parts += "read hidden"
    return parts.joinToString(" · ")
}
