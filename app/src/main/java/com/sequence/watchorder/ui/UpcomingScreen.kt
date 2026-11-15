package com.sequence.watchorder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.UpcomingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val today = vm.today()
    val soon = Logic.upcomingList(vm.catalog.franchises, today)
    val recent = Logic.recentReleases(vm.catalog.franchises, today, 90)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Upcoming") },
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
            item {
                SectionHeader(
                    title = "Coming soon",
                    subtitle = plural(soon.size, "dated title", "dated titles"),
                )
            }
            if (soon.isEmpty()) {
                item { MutedLine("Nothing is dated in the future right now.") }
            } else {
                var lastMonth = ""
                soon.forEach { row ->
                    val month = Logic.monthLabel(row.entry.date)
                    if (month != lastMonth) {
                        lastMonth = month
                        item { MonthLabel(month) }
                    }
                    item(key = "soon-${row.franchise.id}-${row.entry.id}") {
                        UpcomingRow(vm, row, released = false)
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
                recent.forEach { row ->
                    item(key = "recent-${row.franchise.id}-${row.entry.id}") {
                        UpcomingRow(vm, row, released = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthLabel(label: String) {
    Text(
        label,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
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
private fun UpcomingRow(vm: MainViewModel, item: UpcomingItem, released: Boolean) {
    val accent = vm.catalog.accent(item.franchise.id).toPalette()
    val parts = item.entry.date.split("-")
    val day = parts.getOrNull(2)?.toIntOrNull() ?: 0
    val month = Logic.monthLabel(item.entry.date).take(3)
    val seen = item.entry.id in vm.progress.watched[item.franchise.id].orEmpty()

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
        trailingContent = if (released) {
            {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SeenToggle(
                        checked = seen,
                        size = 40.dp,
                        onToggle = { vm.toggleSeen(item.franchise.id, item.entry) },
                    )
                }
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}
