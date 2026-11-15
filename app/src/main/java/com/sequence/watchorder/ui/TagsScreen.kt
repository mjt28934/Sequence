package com.sequence.watchorder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.data.Logic

/**
 * Tags: the categories Home no longer shows.
 *
 * The first state is every tag as a tile. Picking one slides in that tag's franchises, and the back
 * arrow (or the system back gesture) returns to the tag list without leaving the tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Tags") },
                navigationIcon = {
                    if (vm.genre != "all") {
                        BarIconButton(SeqIcons.Back, "All tags") { vm.selectGenre("all") }
                    }
                },
                actions = {
                    BarIconButton(SeqIcons.Settings, "Settings") { vm.openSettings() }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        SectionSwitcher(
            target = vm.genre,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            axis = SectionAxis.HORIZONTAL,
            direction = { _, to -> if (to == "all") -1 else 1 },
        ) { genre ->
            if (genre == "all") {
                TagGrid(vm)
            } else {
                GenreFranchises(vm, genre)
            }
        }
    }
}

@Composable
private fun TagGrid(vm: MainViewModel) {
    val shelves = Logic.genreShelves(vm.catalog.franchises, vm.catalog.genres)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "Every tag",
                subtitle = "${vm.catalog.genres.size} tags · ${vm.catalog.franchises.size} franchises",
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }
        items(shelves, key = { it.first }) { shelf ->
            val first = shelf.second.firstOrNull() ?: return@items
            GenreTile(
                genre = shelf.first,
                franchise = first,
                accent = vm.catalog.accent(first.id).toPalette(),
                onClick = { vm.selectGenre(shelf.first) },
                modifier = Modifier.fillMaxWidth(),
                count = shelf.second.size,
            )
        }
    }
}

@Composable
private fun GenreFranchises(vm: MainViewModel, genre: String) {
    val state = vm.progress
    val today = vm.today()
    val list = vm.catalog.franchises
        .filter { it.genre == genre }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            SectionHeader(
                title = genre,
                subtitle = plural(list.size, "franchise", "franchises"),
            )
        }
        items(list.chunked(2), key = { row -> row.first().id }) { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { f ->
                    PosterTile(
                        franchise = f,
                        subtitle = countLine(f.entries.size, vm.catalog.worksFor(f.id).size),
                        progress = Logic.franchiseProgress(
                            f,
                            state.watched[f.id].orEmpty().toSet(),
                            today,
                        ),
                        accent = vm.catalog.accent(f.id).toPalette(),
                        onClick = { vm.openFranchise(f.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
