package com.sequence.watchorder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.AppTab
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.Sheet
import com.sequence.watchorder.data.Franchise
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.Work

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(vm.searchFocusRequest, vm.searchFocusHandled) {
        if (vm.searchFocusRequest > vm.searchFocusHandled) {
            try {
                focusRequester.requestFocus()
                keyboard?.show()
            } catch (_: IllegalStateException) {
                // Field is not attached yet; a later request retries.
            }
            vm.markSearchFocusHandled(vm.searchFocusRequest)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(top = 8.dp),
            ) {
                Text(
                    "Search",
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    TextField(
                        value = vm.query,
                        onValueChange = { vm.updateQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Franchises, titles, books") },
                        leadingIcon = { Icon(SeqIcons.Search, contentDescription = null) },
                        trailingIcon = {
                            if (vm.query.isNotEmpty()) {
                                IconButton(onClick = { vm.updateQuery("") }) {
                                    Icon(SeqIcons.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            val q = vm.query.trim()
            if (q.isEmpty()) {
                BrowseGenres(vm)
            } else {
                SearchResults(vm, q)
            }
        }
    }
}

@Composable
private fun BrowseGenres(vm: MainViewModel) {
    val shelves = Logic.genreShelves(vm.catalog.franchises, vm.catalog.genres)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            SectionHeader(
                title = "Browse all",
                subtitle = "${vm.catalog.genres.size} genres · ${vm.catalog.franchises.size} franchises",
                modifier = Modifier.padding(horizontal = 0.dp),
            )
        }
        items(shelves, key = { it.first }) { shelf ->
            val first = shelf.second.firstOrNull() ?: return@items
            GenreTile(
                genre = shelf.first,
                franchise = first,
                accent = vm.catalog.accent(first.id).toPalette(),
                onClick = {
                    vm.selectGenre(shelf.first)
                    vm.selectTab(AppTab.HOME)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResults(vm: MainViewModel, q: String) {
    val results = Logic.searchAll(vm.catalog, q)
    val empty = results.franchises.isEmpty() && results.titles.isEmpty() && results.works.isEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        if (empty) {
            item {
                EmptyState(
                    icon = SeqIcons.Search,
                    title = "No results",
                    body = "Nothing matches \u201C$q\u201D. Try a franchise, a film, an author or a title.",
                )
            }
        }
        if (results.franchises.isNotEmpty()) {
            item { SectionHeader(title = "Franchises") }
            items(results.franchises, key = { "f-${it.id}" }) { f ->
                FranchiseRow(f, onClick = { vm.openFranchise(f.id) })
            }
        }
        if (results.titles.isNotEmpty()) {
            item { SectionHeader(title = "Films and series") }
            items(results.titles, key = { "t-${it.franchise.id}-${it.entry.id}" }) { item ->
                ResultRow(
                    thumb = { RowThumb(item.franchise, width = 64.dp) },
                    title = item.entry.title,
                    subtitle = "${item.franchise.name} · ${item.entry.year}",
                    onClick = { vm.openFranchise(item.franchise.id) },
                )
            }
        }
        if (results.works.isNotEmpty()) {
            item { SectionHeader(title = "Books and comics") }
            items(results.works, key = { "w-${it.second.id}" }) { pair ->
                ResultRow(
                    thumb = { CoverThumb(pair.second) },
                    title = pair.second.title,
                    subtitle = "${pair.second.by} · ${pair.first.name}",
                    onClick = {
                        vm.openFranchise(pair.first.id, books = true)
                        vm.openSheet(Sheet.WorkSheet(pair.second.id))
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FranchiseRow(franchise: Franchise, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(franchise.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                "${franchise.genre} · ${franchise.years}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { RowThumb(franchise, width = 64.dp) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultRow(
    thumb: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = thumb,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    )
}
