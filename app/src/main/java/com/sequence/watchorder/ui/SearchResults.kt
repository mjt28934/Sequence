package com.sequence.watchorder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.MainViewModel
import com.sequence.watchorder.Sheet
import com.sequence.watchorder.data.Franchise
import com.sequence.watchorder.data.Logic

/**
 * The search field that grows under Home's app bar.
 *
 * It is a plain field rather than a new screen: Home keeps its scroll position underneath and the
 * close button on the app bar puts everything back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(28.dp)
    // Focus is worth showing on an inline field: the border picks up the accent instead of
    // moving anything or replacing the field.
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = seqTween(SeqMotion.SHORT_MS),
        label = "searchFocus",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp)
            .border(1.5.dp, borderColor, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            interactionSource = interaction,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text("Franchises, titles, books") },
            leadingIcon = { Icon(SeqIcons.Search, contentDescription = null) },
            trailingIcon = {
                // The clear button appears only when there is something to clear.
                if (!Motion.enabled) {
                    if (value.isNotEmpty()) {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(SeqIcons.Close, contentDescription = "Clear search")
                        }
                    }
                } else {
                    AnimatedVisibility(
                        visible = value.isNotEmpty(),
                        enter = fadeIn(tween(SeqMotion.SHORT_MS)) +
                            scaleIn(initialScale = 0.75f),
                        exit = fadeOut(tween(SeqMotion.PRESS_MS)) +
                            scaleOut(targetScale = 0.75f),
                    ) {
                        IconButton(onClick = { onValueChange("") }) {
                            Icon(SeqIcons.Close, contentDescription = "Clear search")
                        }
                    }
                }
            },
            singleLine = true,
            shape = shape,
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

/**
 * What Home shows while search is open: the full franchise index until the user types, then the
 * matches. The two swap with the same vertical move the rest of the app uses.
 */
@Composable
fun HomeSearchResults(vm: MainViewModel, modifier: Modifier = Modifier) {
    val q = vm.query.trim()
    SectionSwitcher(
        target = q.isEmpty(),
        modifier = modifier.fillMaxSize(),
        axis = SectionAxis.VERTICAL,
        direction = { from, _ -> if (from) 1 else -1 },
    ) { indexing ->
        if (indexing) {
            FranchiseIndex(vm)
        } else {
            SearchResultsList(vm, q)
        }
    }
}

/** Every franchise, A to Z, grouped under its first letter: the browse state before typing. */
@Composable
private fun FranchiseIndex(vm: MainViewModel) {
    val sorted = vm.catalog.franchises.sortedWith(
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.name },
    )
    val groups = sorted.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            SectionHeader(
                title = "All franchises",
                subtitle = "${plural(sorted.size, "franchise", "franchises")} · start typing to narrow it down",
            )
        }
        groups.forEach { (letter, list) ->
            item(key = "letter-$letter") { LetterHeader(letter) }
            items(list, key = { "f-${it.id}" }) { franchise ->
                FranchiseRow(franchise, onClick = { vm.openFranchise(franchise.id) })
            }
        }
    }
}

@Composable
private fun LetterHeader(letter: Char) {
    Text(
        letter.toString(),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SearchResultsList(vm: MainViewModel, q: String) {
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
            item {
                SectionHeader(
                    title = "Franchises",
                    subtitle = plural(results.franchises.size, "match", "matches"),
                )
            }
            items(results.franchises, key = { "f-${it.id}" }) { f ->
                FranchiseRow(f, onClick = { vm.openFranchise(f.id) })
            }
        }
        if (results.titles.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Films and series",
                    subtitle = plural(results.titles.size, "match", "matches"),
                )
            }
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
            item {
                SectionHeader(
                    title = "Books and comics",
                    subtitle = plural(results.works.size, "match", "matches"),
                )
            }
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
    PressableRow(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
    ) {
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
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultRow(
    thumb: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    PressableRow(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
    ) {
        ListItem(
            headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingContent = thumb,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
