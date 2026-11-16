package com.sequence.watchorder.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.Sheet
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.UpcomingItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UpcomingScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val today = vm.today()
    val soon = remember(today) { Logic.upcomingList(vm.catalog.franchises, today) }
    val recent = remember(today) { Logic.recentReleases(vm.catalog.franchises, today, 90) }
    // Months keep their order, so the sticky label matches the rows underneath it.
    val months = remember(soon) { soon.groupBy { Logic.monthLabel(it.entry.date) } }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Upcoming") },
                actions = {
                    BarIconButton(SeqIcons.Settings, "Settings") { vm.openSettings() }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 32.dp,
            ),
        ) {
            if (soon.isEmpty() && recent.isEmpty()) {
                item {
                    EmptyState(
                        icon = SeqIcons.Event,
                        title = "Nothing on the calendar",
                        body = "Every title in the catalog is already out, with nothing dated ahead " +
                            "and nothing released in the last 90 days.",
                    )
                }
            } else {
                item {
                    SectionHeader(
                        title = "Coming soon",
                        subtitle = plural(soon.size, "dated title", "dated titles"),
                    )
                }
                if (soon.isEmpty()) {
                    item { MutedLine("Nothing is dated in the future right now.") }
                } else {
                    months.forEach { (month, rows) ->
                        stickyHeader(key = "month-$month") { MonthLabel(month) }
                        items(rows, key = { "soon-${it.franchise.id}-${it.entry.id}" }) { row ->
                            UpcomingRow(vm, row, modifier = seqItem())
                        }
                    }
                }

                item {
                    SectionHeader(
                        title = "Just released",
                        subtitle = "The last 90 days",
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
                if (recent.isEmpty()) {
                    item { MutedLine("Nothing new in the last 90 days.") }
                } else {
                    items(recent, key = { "recent-${it.franchise.id}-${it.entry.id}" }) { row ->
                        UpcomingRow(vm, row, modifier = seqItem())
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthLabel(label: String) {
    // A sticky header has to hide the rows sliding under it, so it brings its own background.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Text(
            label,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MutedLine(text: String) {
    Text(
        text,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingRow(
    vm: MainViewModel,
    item: UpcomingItem,
    modifier: Modifier = Modifier,
) {
    val accent = vm.catalog.accent(item.franchise.id).toPalette()
    val parts = item.entry.date.split("-")
    val day = parts.getOrNull(2)?.toIntOrNull() ?: 0
    val month = Logic.monthLabel(item.entry.date).take(3)

    PressableRow(
        onClick = {
            vm.openFranchise(item.franchise.id)
            vm.openSheet(Sheet.EntrySheet(item.entry.id))
        },
        modifier = modifier.padding(horizontal = 8.dp),
        color = Color.Transparent,
    ) {
        ListItem(
            headlineContent = {
                Text(item.entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(
                    "${item.franchise.name} · ${item.entry.kindLabel}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent.deep,
                    modifier = Modifier.size(52.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            day.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            month,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent.accent,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
