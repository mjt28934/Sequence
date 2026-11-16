package com.sequence.watchorder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.Sheet
import com.sequence.watchorder.data.ContinueCard
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.Progress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state = vm.progress
    val today = vm.today()
    // These read the catalogue and the whole watched map, so they only re-run when something
    // they depend on changes, not on every scroll tick or search keystroke.
    val cards = remember(state, today) { Logic.continueList(vm.catalog, state, today) }
    val seenTotal = remember(state, today) {
        Logic.overallProgress(vm.catalog.franchises, { state.watched[it].orEmpty() }, today)
    }
    val readTotal = remember(state) {
        Logic.overallReadProgress(
            vm.catalog.franchises,
            { vm.catalog.worksFor(it) },
            { state.read[it].orEmpty() },
        )
    }
    val allFranchises = remember(state, today) {
        vm.catalog.franchises
            .filter {
                !state.hideFinished ||
                    Logic.franchiseProgress(it, state.watched[it.id].orEmpty().toSet(), today).pct < 100
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // Saveable, so returning to Home after a tab change keeps the page settled instead of
    // replaying the entrance from the top.
    val enter = rememberSaveable(saver = EnterTrackerSaver) { EnterTracker() }
    val searchOpen = vm.searchOpen
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val readNext = remember(cards) { Logic.readNextList(cards) }
    // Held outside the search switcher, so opening and closing search returns to the same place.
    val listState = rememberLazyListState()

    // Opening search grows the field under the app bar and parks the cursor in it. The focus
    // request can land before the field is attached; that throws, so it stays a no-op.
    LaunchedEffect(searchOpen, vm.searchFocusRequest) {
        if (searchOpen) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    androidx.compose.material3.Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
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
                        BarIconButton(
                            icon = if (searchOpen) SeqIcons.Close else SeqIcons.Search,
                            contentDescription = if (searchOpen) "Close search" else "Search",
                        ) {
                            if (searchOpen) vm.closeSearch() else vm.openSearch()
                        }
                        BarIconButton(SeqIcons.Settings, "Settings") { vm.openSettings() }
                        Spacer(Modifier.width(4.dp))
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
                // The field unrolls from under the title instead of replacing the screen.
                if (Motion.enabled) {
                    AnimatedVisibility(
                        visible = searchOpen,
                        enter = expandVertically(tween(SeqMotion.STANDARD_MS), expandFrom = Alignment.Top) +
                            fadeIn(tween(SeqMotion.SHORT_MS)),
                        exit = shrinkVertically(tween(SeqMotion.SHORT_MS), shrinkTowards = Alignment.Top) +
                            fadeOut(tween(SeqMotion.PRESS_MS)),
                    ) {
                        HomeSearchField(
                            value = vm.query,
                            onValueChange = vm::updateQuery,
                            focusRequester = focusRequester,
                        )
                    }
                } else if (searchOpen) {
                    // Settled state for snapshots, which have no time to run an enter transition.
                    HomeSearchField(
                        value = vm.query,
                        onValueChange = vm::updateQuery,
                        focusRequester = focusRequester,
                    )
                }
            }
        },
    ) { innerPadding ->
        val top = innerPadding.calculateTopPadding()
        SectionSwitcher(
            target = searchOpen,
            modifier = Modifier.fillMaxSize(),
            axis = SectionAxis.VERTICAL,
            direction = { from, _ -> if (from) -1 else 1 },
        ) { searching ->
            if (searching) {
                HomeSearchResults(vm, Modifier.fillMaxSize().padding(top = top))
                return@SectionSwitcher
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = top,
                    bottom = 24.dp,
                ),
            ) {
                item { EnterSection(enter, "totals", 1) { OverallProgressCard(seenTotal, readTotal) } }

                if (cards.isNotEmpty()) {
                    item {
                        EnterSection(enter, "continue-header", 2) {
                            SectionHeader(
                                title = "Continue",
                                subtitle = plural(cards.size, "franchise in progress", "franchises in progress"),
                            )
                        }
                    }
                    item {
                        EnterSection(enter, "continue-rail", 3) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(cards, key = { it.franchise.id }) { card ->
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

                if (readNext.isNotEmpty()) {
                    item {
                        EnterSection(enter, "reading-header", 4) { SectionHeader(title = "Keep reading") }
                    }
                    item {
                        EnterSection(enter, "reading-rail", 5) {
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

                // No genre rails on Home any more: one flat A-Z grid of everything. Categories live
                // on the Tags tab, so browsing here never hides a franchise behind its genre.
                item {
                    EnterSection(enter, "all-header", 6) {
                        SectionHeader(
                            title = "All franchises",
                            subtitle = plural(allFranchises.size, "franchise", "franchises"),
                        )
                    }
                }
                items(allFranchises.chunked(2), key = { "all-${it.first().id}" }) { row ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .then(seqItem())
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { f ->
                            val p = Logic.franchiseProgress(
                                f,
                                state.watched[f.id].orEmpty().toSet(),
                                today,
                            )
                            PosterTile(
                                franchise = f,
                                subtitle = countLine(
                                    f.entries.size,
                                    vm.catalog.worksFor(f.id).size,
                                ),
                                progress = p,
                                accent = vm.catalog.accent(f.id).toPalette(),
                                onClick = { vm.openFranchise(f.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (allFranchises.isEmpty()) {
                    item {
                        EmptyState(
                            icon = SeqIcons.Check,
                            title = "Every franchise is finished",
                            body = "You have completed everything in the catalog. Show finished " +
                                "franchises again to browse them, or clear a few marks to replay.",
                            actionLabel = "Show finished franchises",
                            onAction = { vm.setHideFinished(false) },
                        )
                    }
                }

                item { FooterHint(vm.catalog.dataAsOf) }
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
            ProgressColumn("Seen", seen, Modifier.weight(1f))
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
private fun FooterHint(dataAsOf: String) {
    Text(
        "Dates and orders last checked ${Logic.formatDate(dataAsOf)}. Progress stays on this phone.",
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
