package com.sequence.watchorder.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.data.ContinueCard
import com.sequence.watchorder.data.Entry
import com.sequence.watchorder.data.Franchise
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.OrderMode
import com.sequence.watchorder.data.Progress
import com.sequence.watchorder.data.UpNextItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state = vm.progress
    val today = vm.today()
    val cards = Logic.continueList(vm.catalog, state, today)
    val seenTotal = Logic.overallProgress(vm.catalog.franchises, { state.watched[it].orEmpty() }, today)
    val readTotal = Logic.overallReadProgress(
        vm.catalog.franchises,
        { vm.catalog.worksFor(it) },
        { state.read[it].orEmpty() },
    )
    val shelves = Logic.genreShelves(vm.catalog.franchises, vm.catalog.genres)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val enter = remember { EnterTracker() }

    androidx.compose.material3.Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BrandMark()
                        Text("Sequence", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                actions = {
                    BarIconButton(SeqIcons.Search, "Search") { vm.goSearch() }
                    BarIconButton(SeqIcons.Settings, "Settings") { vm.openSettings() }
                    Spacer(Modifier.width(4.dp))
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        // Switching genre cross-fades the whole feed up or down instead of replacing it in place.
        SectionSwitcher(
            target = vm.genre,
            modifier = Modifier.fillMaxSize(),
            axis = SectionAxis.VERTICAL,
            direction = { _, to -> if (to == "all") -1 else 1 },
        ) { genre ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 24.dp,
                ),
            ) {
                item { EnterSection(enter, "genres", 0) { GenreFilterRow(vm) } }

                if (genre != "all") {
                    val list = vm.catalog.franchises.filter { it.genre == genre }
                    item {
                        EnterSection(enter, "genre-header", 1) {
                            SectionHeader(genre, subtitle = plural(list.size, "franchise", "franchises"))
                        }
                    }
                    items(list.chunked(2), key = { row -> row.first().id }) { row ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { f ->
                                val p = Logic.franchiseProgress(f, state.watched[f.id].orEmpty().toSet(), today)
                                PosterTile(
                                    franchise = f,
                                    // Two narrow columns: keep the line short enough to never ellipsise.
                                    subtitle = buildString {
                                        append(plural(f.entries.size, "title", "titles"))
                                        val books = vm.catalog.worksFor(f.id).size
                                        if (books > 0) append(" · ").append(plural(books, "book", "books"))
                                    },
                                    progress = p,
                                    accent = vm.catalog.accent(f.id).toPalette(),
                                    onClick = { vm.openFranchise(f.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    item { FooterHint(vm.catalog.dataAsOf) }
                    return@LazyColumn
                }

                if (cards.isNotEmpty()) {
                    item {
                        EnterSection(enter, "hero", 1) {
                            ContinueHero(
                                card = cards.first(),
                                accent = vm.catalog.accent(cards.first().franchise.id).toPalette(),
                                onOpen = { vm.openFranchise(cards.first().franchise.id) },
                                onMarkSeen = {
                                    val first = cards.first()
                                    first.next?.let { vm.toggleSeen(first.franchise.id, it) }
                                        ?: first.nextBook?.let { vm.toggleRead(first.franchise.id, it) }
                                },
                            )
                        }
                    }
                }

                if (seenTotal.done == 0 && readTotal.done == 0) {
                    item { EnterSection(enter, "demo", 2) { OrderDemoCard(vm) } }
                } else {
                    item { EnterSection(enter, "totals", 2) { OverallProgressCard(seenTotal, readTotal) } }
                }

                if (cards.size > 1) {
                    item {
                        EnterSection(enter, "continue-header", 3) {
                            SectionHeader(
                                title = "Continue",
                                subtitle = plural(cards.size, "franchise in progress", "franchises in progress"),
                            )
                        }
                    }
                    item {
                        EnterSection(enter, "continue-rail", 4) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(cards.drop(1), key = { it.franchise.id }) { card ->
                                    ShelfCard(
                                        franchise = card.franchise,
                                        title = card.franchise.name,
                                        subtitle = cardSubtitle(card),
                                        progress = card.seen,
                                        accent = vm.catalog.accent(card.franchise.id).toPalette(),
                                        onClick = { vm.openFranchise(card.franchise.id) },
                                    )
                                }
                            }
                        }
                    }
                }

                val upNext = Logic.upNextList(cards)
                if (upNext.isNotEmpty()) {
                    item {
                        EnterSection(enter, "upnext-header", 5) {
                            SectionHeader(
                                title = "Up next",
                                subtitle = "Tap the tick when you have watched it",
                            )
                        }
                    }
                    items(upNext, key = { "up-${it.franchise.id}-${it.entry.id}" }) { item ->
                        UpNextRow(vm, item)
                    }
                }

                val readNext = Logic.readNextList(cards)
                if (readNext.isNotEmpty()) {
                    item {
                        EnterSection(enter, "reading-header", 6) { SectionHeader(title = "Keep reading") }
                    }
                    item {
                        EnterSection(enter, "reading-rail", 7) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(readNext, key = { "rn-${it.franchise.id}-${it.work.id}" }) { item ->
                                    CoverTile(
                                        work = item.work,
                                        accent = vm.catalog.accent(item.franchise.id).toPalette(),
                                        read = false,
                                        isNext = true,
                                        onClick = { vm.openFranchise(item.franchise.id, books = true) },
                                        onToggleRead = { vm.toggleRead(item.franchise.id, item.work) },
                                        modifier = Modifier.width(116.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                shelves.forEach { (genre, franchises) ->
                    item { EnterSection(enter, "shelf-$genre", 8) { SectionHeader(title = genre) } }
                    item {
                        EnterSection(enter, "rail-$genre", 9) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(franchises, key = { "$genre-${it.id}" }) { f ->
                                    val p = Logic.franchiseProgress(f, state.watched[f.id].orEmpty().toSet(), today)
                                    PosterTile(
                                        franchise = f,
                                        subtitle = if (p.done > 0) "${p.done} of ${p.total}" else f.years,
                                        progress = p,
                                        accent = vm.catalog.accent(f.id).toPalette(),
                                        onClick = { vm.openFranchise(f.id) },
                                        modifier = Modifier.width(180.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                item { FooterHint(vm.catalog.dataAsOf) }
            }
        }
    }
}

@Composable
private fun GenreFilterRow(vm: MainViewModel) {
    ChipRow(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
    ) {
        item {
            SeqFilterChip("All", vm.genre == "all", { vm.selectGenre("all") })
        }
        items(vm.catalog.genres) { genre ->
            SeqFilterChip(genre, vm.genre == genre, { vm.selectGenre(genre) })
        }
    }
}

@Composable
private fun ContinueHero(
    card: ContinueCard,
    accent: AccentPalette,
    onOpen: () -> Unit,
    onMarkSeen: () -> Unit,
) {
    val next = card.next
    val nextBook = card.nextBook
    val headline = next?.title ?: nextBook?.title ?: "All caught up"
    val kicker = when {
        next != null -> "Continue watching"
        nextBook != null -> "Continue reading"
        else -> "Finished"
    }
    val interaction = remember { MutableInteractionSource() }

    Card(
        onClick = onOpen,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pressScale(interaction, pressedScale = 0.976f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f),
        ) {
            ArtImage(ArtKind.POSTER, card.franchise.id, Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.25f to Color.Transparent,
                            1f to Color(0xF20A0B0F),
                        ),
                    ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            ) {
                Text(
                    kicker.uppercase(),
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    headline,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${card.franchise.name} · ${card.seen.done} of ${card.seen.total}",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onMarkSeen) {
                        Icon(SeqIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (next == null && nextBook != null) "Read it" else "Watched it")
                    }
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                ProgressStrip(pct = card.seen.pct, color = accent.accent, height = 4.dp)
            }
        }
    }
}

private fun cardSubtitle(card: ContinueCard): String = when {
    card.next != null -> "Up next · ${card.next.title}"
    card.nextBook != null -> "Read next · ${card.nextBook.title}"
    else -> "All caught up"
}

@Composable
private fun OverallProgressCard(seen: Progress, read: Progress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ProgressColumn("Watched", seen, Modifier.weight(1f))
            ProgressColumn("Read", read, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProgressColumn(label: String, progress: Progress, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                progress.done.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "of ${progress.total}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        ProgressStrip(progress.pct, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun UpNextRow(vm: MainViewModel, item: UpNextItem) {
    val accent = vm.catalog.accent(item.franchise.id).toPalette()
    val seen = item.entry.id in vm.progress.watched[item.franchise.id].orEmpty()

    ListItem(
        headlineContent = {
            Text(
                item.entry.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                "${item.franchise.name} · #${item.position}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { RowThumb(item.franchise, width = 64.dp) },
        trailingContent = {
            SeenToggle(
                checked = seen,
                onToggle = { vm.toggleSeen(item.franchise.id, item.entry) },
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderDemoCard(vm: MainViewModel) {
    val saga = vm.catalog.franchise("star-wars") ?: return
    val ids = listOf("sw-1", "sw-2", "sw-3", "sw-4", "sw-5", "sw-6")
    val entries = ids.mapNotNull { id -> saga.entries.firstOrNull { it.id == id } }
    var mode by remember { mutableIntStateOf(1) }
    val ordered: List<Entry> = remember(mode) {
        Logic.orderEntries(saga.copy(entries = entries), if (mode == 1) OrderMode.STORY else OrderMode.RELEASE)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Release order or story order?",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                if (mode == 1) "The order the story happens." else "The order they reached cinemas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
            ) {
                SegmentedButton(
                    selected = mode == 0,
                    onClick = { mode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Release") }
                SegmentedButton(
                    selected = mode == 1,
                    onClick = { mode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Story") }
            }
            ordered.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(26.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                (index + 1).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Text(
                        entry.title.removePrefix("Episode "),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        entry.year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterHint(dataAsOf: String) {
    Text(
        "Dates and orders last checked ${Logic.formatDate(dataAsOf)}. Progress stays on this phone.",
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
