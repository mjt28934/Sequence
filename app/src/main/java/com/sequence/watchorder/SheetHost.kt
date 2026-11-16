package com.sequence.watchorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sequence.watchorder.data.BookFilter
import com.sequence.watchorder.data.Franchise
import com.sequence.watchorder.data.LibSort
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.OrderMode
import com.sequence.watchorder.data.WatchFilter
import com.sequence.watchorder.data.Work
import com.sequence.watchorder.ui.AccentPalette
import com.sequence.watchorder.ui.ArtImage
import com.sequence.watchorder.ui.ArtKind
import com.sequence.watchorder.ui.DefaultAccent
import com.sequence.watchorder.ui.RowThumb
import com.sequence.watchorder.ui.SeqIcons
import com.sequence.watchorder.ui.SheetActionRow
import com.sequence.watchorder.ui.SheetButton
import com.sequence.watchorder.ui.SheetHeader
import com.sequence.watchorder.ui.SheetNote
import com.sequence.watchorder.ui.SheetOptionRow
import com.sequence.watchorder.ui.SheetToggleRow
import com.sequence.watchorder.ui.toPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHost(vm: MainViewModel) {
    val sheet = vm.sheet ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val franchise = vm.franchise
    val accent = franchise?.let { vm.catalog.accent(it.id).toPalette() } ?: DefaultAccent

    ModalBottomSheet(
        onDismissRequest = { vm.closeSheet() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            when (sheet) {
                Sheet.Order -> OrderSheet(vm, franchise)
                Sheet.Filter -> FilterSheet(vm, franchise)
                Sheet.More -> MoreSheet(vm, franchise)
                Sheet.Sort -> SortSheet(vm)
                Sheet.ConfirmClearAll -> ConfirmClearAllSheet(vm)
                is Sheet.EntrySheet -> franchise?.let {
                    EntryDetailSheet(vm, it, sheet.entryId, accent)
                }
                is Sheet.WorkSheet -> franchise?.let {
                    WorkDetailSheet(vm, it, sheet.workId, accent)
                }
            }
        }
    }
}

@Composable
private fun OrderSheet(vm: MainViewModel, franchise: Franchise?) {
    val screenTab = vm.detailTab == DetailTab.WATCH ||
        (franchise != null && vm.catalog.worksFor(franchise.id).isEmpty())
    val releaseTitle = if (screenTab) "Release date" else "Published"
    val releaseBody = if (screenTab) "The order they premiered" else "By the year each one first came out"
    val storyTitle = if (screenTab) "Story order" else "Suggested"
    val storyBody = if (screenTab) "The order the story happens" else "Grouped into reading paths"

    SheetHeader("Order")
    SheetOptionRow(
        title = releaseTitle,
        subtitle = releaseBody,
        selected = vm.progress.mode == OrderMode.RELEASE,
        onClick = { vm.setMode(OrderMode.RELEASE) },
    )
    SheetOptionRow(
        title = storyTitle,
        subtitle = storyBody,
        selected = vm.progress.mode == OrderMode.STORY,
        onClick = { vm.setMode(OrderMode.STORY) },
    )
    SheetNote(
        "Changing this only re-sorts the list. Nothing is lost.",
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun FilterSheet(vm: MainViewModel, franchise: Franchise?) {
    val works = franchise?.let { vm.catalog.worksFor(it.id) }.orEmpty()
    val screenTab = vm.detailTab == DetailTab.WATCH || works.isEmpty()
    val watchFilter = vm.watchFilter
    val bookFilter = vm.bookFilter
    val mixedKinds = franchise?.let { Logic.hasMixedKinds(it) } == true
    val hasFilms = franchise?.let { Logic.hasKind(it, "film") } == true
    val hasSeries = franchise?.let { Logic.hasKind(it, "series") } == true
    val hasGames = franchise?.let { Logic.hasKind(it, "game") } == true
    val hasAnimation = franchise?.let { Logic.hasAnimation(it) } == true
    val optionalEntries = franchise?.let { Logic.hasOptional(it) } == true
    val optionalWorks = Logic.hasOptionalWorks(works)

    SheetHeader("Filter", if (screenTab) "Narrow the watch order" else "Narrow the reading list")

    if (screenTab) {
        if (mixedKinds) {
            SheetOptionRow(
                title = "Everything",
                subtitle = if (hasGames) "Films, series, games and specials" else "Films, series and specials",
                selected = watchFilter.kind == "all",
                onClick = { vm.updateWatchFilter(watchFilter.copy(kind = "all")) },
            )
            if (hasFilms) {
                SheetOptionRow(
                    title = "Films only",
                    subtitle = if (hasGames) "Skip the series, games and specials" else "Skip the series and specials",
                    selected = watchFilter.kind == "film",
                    onClick = { vm.updateWatchFilter(watchFilter.copy(kind = "film")) },
                )
            }
            if (hasSeries) {
                SheetOptionRow(
                    title = "Series and specials only",
                    subtitle = if (hasGames) "Skip the films and games" else "Skip the films",
                    selected = watchFilter.kind == "series",
                    onClick = { vm.updateWatchFilter(watchFilter.copy(kind = "series")) },
                )
            }
            if (hasGames) {
                SheetOptionRow(
                    title = "Games only",
                    subtitle = "Skip the films, series and specials",
                    selected = watchFilter.kind == "game",
                    onClick = { vm.updateWatchFilter(watchFilter.copy(kind = "game")) },
                )
            }
        }
        if (hasAnimation) {
            SheetToggleRow(
                title = "Animation only",
                subtitle = "Cartoons, animated series and animated films",
                checked = watchFilter.animatedOnly,
                onCheckedChange = { vm.updateWatchFilter(watchFilter.copy(animatedOnly = it)) },
            )
        }
        if (optionalEntries) {
            SheetToggleRow(
                title = "Essentials only",
                subtitle = "Hide optional viewing",
                checked = watchFilter.essentials,
                onCheckedChange = { vm.updateWatchFilter(watchFilter.copy(essentials = it)) },
            )
        }
        SheetToggleRow(
            title = "Hide seen",
            subtitle = "Only show what is left",
            checked = watchFilter.hideSeen,
            onCheckedChange = { vm.updateWatchFilter(watchFilter.copy(hideSeen = it)) },
        )
    } else {
        if (optionalWorks) {
            SheetToggleRow(
                title = "Essentials only",
                subtitle = "Hide optional reading",
                checked = bookFilter.essentials,
                onCheckedChange = { vm.updateBookFilter(bookFilter.copy(essentials = it)) },
            )
        }
        SheetToggleRow(
            title = "Hide read",
            subtitle = "Only show what is left",
            checked = bookFilter.hideRead,
            onCheckedChange = { vm.updateBookFilter(bookFilter.copy(hideRead = it)) },
        )
    }
    SheetNote("Filters stay on until you change them.")
}

@Composable
private fun MoreSheet(vm: MainViewModel, franchise: Franchise?) {
    if (franchise == null) return
    val hasSeen = vm.watchedFor(franchise.id).isNotEmpty()
    val hasRead = vm.readFor(franchise.id).isNotEmpty()
    val hasBooks = vm.catalog.worksFor(franchise.id).isNotEmpty()

    SheetHeader(franchise.name, "Progress for this franchise")
    SheetActionRow(
        icon = SeqIcons.Trash,
        title = "Clear everything seen",
        enabled = hasSeen,
        danger = true,
        onClick = {
            vm.closeSheet()
            vm.clearSeen(franchise.id)
        },
    )
    if (hasBooks) {
        SheetActionRow(
            icon = SeqIcons.Trash,
            title = "Clear everything read",
            enabled = hasRead,
            danger = true,
            onClick = {
                vm.closeSheet()
                vm.clearRead(franchise.id)
            },
        )
    }
}

@Composable
private fun SortSheet(vm: MainViewModel) {
    SheetHeader("Sort library", "How franchises are ordered")
    for (sort in LibSort.entries) {
        SheetOptionRow(
            title = sort.label,
            subtitle = sortDescription(sort),
            selected = vm.progress.libSort == sort,
            onClick = { vm.setLibSort(sort) },
        )
    }
}

private fun sortDescription(sort: LibSort): String? = when (sort) {
    LibSort.RECENT -> "Most recently opened first"
    LibSort.AZ -> "Alphabetical by name"
    LibSort.UNSEEN -> "Most left to watch first"
    LibSort.PROGRESS -> "Closest to finished first"
}

@Composable
private fun ConfirmClearAllSheet(vm: MainViewModel) {
    SheetHeader("Clear all progress?", "This cannot be undone")
    SheetNote(
        "This removes everything you have marked seen or read, and your saved list, " +
            "on this phone.",
    )
    Column(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SheetButton("Clear everything", onClick = { vm.clearAll() }, danger = true)
        SheetButton("Cancel", onClick = { vm.closeSheet() }, tonal = true)
    }
}

@Composable
private fun EntryDetailSheet(
    vm: MainViewModel,
    franchise: Franchise,
    entryId: String,
    accent: AccentPalette,
) {
    val entry = franchise.entries.firstOrNull { it.id == entryId } ?: return
    val state = vm.progress
    val today = vm.today()
    val watched = vm.watchedFor(franchise.id).toSet()
    val seen = entry.id in watched
    val upcoming = Logic.isUpcoming(entry, today)
    val mixed = Logic.hasMixedKinds(franchise)
    val filter = WatchFilter(
        kind = if (mixed) vm.watchFilter.kind else "all",
        hideSeen = vm.watchFilter.hideSeen,
        essentials = vm.watchFilter.essentials,
        animatedOnly = vm.watchFilter.animatedOnly,
    )
    val ordered = Logic.orderEntries(franchise, state.mode)
    val shown = Logic.visibleItems(Logic.withPositions(ordered), watched, filter)
    val ids = Logic.idsUpTo(shown.map { it.entry }, entry.id)
    val canUpTo = ids.any { it !in watched }
    val position = ordered.indexOfFirst { it.id == entry.id } + 1

    val meta = buildList {
        add(Logic.formatDate(entry.date))
        add(entry.kindLabel)
        if (!entry.era.isNullOrEmpty() && franchise.eraOrder.size > 1) add(entry.era)
        if (entry.optional) add("Optional")
    }.joinToString(" · ")

    Row(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RowThumb(franchise, width = 104.dp)
        Column(Modifier.weight(1f)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                meta,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "#$position in " + (if (state.mode == OrderMode.RELEASE) "release order" else "story order") +
                    if (upcoming) " · Upcoming" else "",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = accent.accent,
            )
        }
    }
    if (!entry.note.isNullOrEmpty()) {
        SheetNote(entry.note, modifier = Modifier.padding(top = 8.dp))
    }
    Column(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!upcoming) {
            SheetButton(
                text = if (seen) "Mark as unseen" else "Mark as seen",
                onClick = {
                    vm.closeSheet()
                    vm.toggleSeen(franchise.id, entry)
                },
            )
        }
        if (canUpTo && !upcoming) {
            SheetButton(
                text = "Mark everything up to here as seen",
                tonal = true,
                onClick = {
                    vm.closeSheet()
                    vm.markSeenUpTo(franchise.id, ids)
                },
            )
        }
    }
}

@Composable
private fun WorkDetailSheet(
    vm: MainViewModel,
    franchise: Franchise,
    workId: String,
    accent: AccentPalette,
) {
    val work = vm.catalog.worksFor(franchise.id).firstOrNull { it.id == workId } ?: return
    val state = vm.progress
    val read = vm.readFor(franchise.id).toSet()
    val isRead = work.id in read
    val works = vm.catalog.worksFor(franchise.id)
    val formats = Logic.formatCounts(works)
    val activeFormats = Work.FORMATS.filter { (formats[it] ?: 0) > 0 }
    val filter = BookFilter(
        format = if (activeFormats.size > 1) vm.bookFilter.format else "all",
        hideRead = vm.bookFilter.hideRead,
        essentials = vm.bookFilter.essentials,
    )
    val ordered = Logic.orderWorks(works, state.mode)
    val shown = Logic.visibleWorks(Logic.withWorkPositions(ordered), read, filter)
    val ids = Logic.workIdsUpTo(shown.map { it.work }, work.id)
    val canUpTo = ids.any { it !in read }
    val position = ordered.indexOfFirst { it.id == work.id } + 1

    Row(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .width(96.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            ArtImage(ArtKind.COVER, work.id, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f)) {
            Text(
                work.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                work.by,
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                (Work.FORMAT_SINGULAR[work.format] ?: work.format) + " · " + work.year +
                    if (work.optional) " · Optional" else "",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "#$position · ${work.path}",
                modifier = Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = accent.accent,
            )
        }
    }
    if (!work.note.isNullOrEmpty()) {
        SheetNote(work.note, modifier = Modifier.padding(top = 8.dp))
    }
    Column(
        Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SheetButton(
            text = if (isRead) "Mark as unread" else "Mark as read",
            onClick = {
                vm.closeSheet()
                vm.toggleRead(franchise.id, work)
            },
        )
        if (canUpTo) {
            SheetButton(
                text = "Mark everything up to here as read",
                tonal = true,
                onClick = {
                    vm.closeSheet()
                    vm.markReadUpTo(franchise.id, ids)
                },
            )
        }
    }
}
