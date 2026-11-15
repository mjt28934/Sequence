package com.sequence.watchorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.AppTab
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.Sheet
import com.sequence.watchorder.data.LibView
import com.sequence.watchorder.data.LibraryItem
import com.sequence.watchorder.data.Logic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val state = vm.progress
    val today = vm.today()
    val all = remember(state, today) { Logic.libraryList(vm.catalog, state, "all", today) }
    val items = if (vm.libraryFilter == "all") {
        all
    } else {
        all.filter { Logic.statusOf(it.seen) == vm.libraryFilter }
    }
    val countInProgress = all.count { Logic.statusOf(it.seen) == "in-progress" }
    val countNotStarted = all.count { Logic.statusOf(it.seen) == "not-started" }
    val countCompleted = all.count { Logic.statusOf(it.seen) == "completed" }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Library") },
                actions = {
                    BarIconButton(SeqIcons.Sort, "Sort") { vm.openSheet(Sheet.Sort) }
                    BarIconButton(
                        if (state.libView == LibView.GRID) SeqIcons.List else SeqIcons.Grid,
                        if (state.libView == LibView.GRID) "Show as list" else "Show as grid",
                    ) { vm.toggleLibView() }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.openSheet(Sheet.Sort) },
                icon = { Icon(SeqIcons.Tune, contentDescription = null) },
                text = { Text("Sort & filter") },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            ChipRow(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)) {
                item {
                    SeqFilterChip("All", vm.libraryFilter == "all", { vm.selectLibraryFilter("all") }, count = all.size)
                }
                item {
                    SeqFilterChip(
                        "In progress",
                        vm.libraryFilter == "in-progress",
                        { vm.selectLibraryFilter("in-progress") },
                        count = countInProgress,
                    )
                }
                item {
                    SeqFilterChip(
                        "Not started",
                        vm.libraryFilter == "not-started",
                        { vm.selectLibraryFilter("not-started") },
                        count = countNotStarted,
                    )
                }
                item {
                    SeqFilterChip(
                        "Completed",
                        vm.libraryFilter == "completed",
                        { vm.selectLibraryFilter("completed") },
                        count = countCompleted,
                    )
                }
            }

            when {
                all.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon = SeqIcons.Library,
                        title = "Your library is empty",
                        body = "Franchises you start, or save with the bookmark, show up here with a count of what is left.",
                        actionLabel = "Browse franchises",
                        onAction = { vm.selectTab(AppTab.HOME) },
                    )
                }

                items.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon = SeqIcons.Filter,
                        title = "Nothing here",
                        body = "No franchise in your library matches that filter.",
                    )
                }

                state.libView == LibView.GRID -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(152.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(items, key = { it.franchise.id }) { item ->
                        PosterTile(
                            franchise = item.franchise,
                            subtitle = subtitleFor(item),
                            progress = item.seen,
                            accent = vm.catalog.accent(item.franchise.id).toPalette(),
                            onClick = { vm.openFranchise(item.franchise.id) },
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                ) {
                    items(items, key = { it.franchise.id }) { item ->
                        LibraryRow(vm, item)
                    }
                }
            }
        }
    }
}

private fun subtitleFor(item: LibraryItem): String = when {
    item.seen.total > 0 && item.seen.done >= item.seen.total -> "Completed"
    item.unseen > 0 -> "${item.unseen} left"
    else -> "Saved"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryRow(vm: MainViewModel, item: LibraryItem) {
    val accent = vm.catalog.accent(item.franchise.id).toPalette()
    Card(
        onClick = { vm.openFranchise(item.franchise.id) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
            headlineContent = {
                Text(item.franchise.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Column {
                    Text(
                        "${item.franchise.years} · ${item.seen.done} of ${item.seen.total} watched",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ProgressStrip(
                        pct = item.seen.pct,
                        color = accent.accent,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            },
            leadingContent = { RowThumb(item.franchise, width = 72.dp) },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}
