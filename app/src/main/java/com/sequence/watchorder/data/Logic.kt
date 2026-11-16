package com.sequence.watchorder.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pure port of the ordering and feed logic in the original TypeScript modules
 * (src/lib/orders.ts and src/lib/feed.ts). Keeping it free of Android types makes
 * it easy to reason about and test.
 */
object Logic {

    private val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
    private val MONTHS_SHORT = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    fun today(): String = LocalDate.now().toString()

    fun isUpcoming(entry: Entry, today: String = today()): Boolean = entry.date > today

    fun isReleased(entry: Entry, today: String = today()): Boolean = !isUpcoming(entry, today)

    /** Sort a franchise's entries by release date or by in-universe story order. */
    fun orderEntries(franchise: Franchise, mode: OrderMode): List<Entry> {
        val entries = franchise.entries.toMutableList()
        if (mode == OrderMode.RELEASE) {
            entries.sortWith(compareBy<Entry> { it.date }.thenBy { it.title })
        } else {
            val eraRank = { era: String? ->
                val i = franchise.eraOrder.indexOf(era)
                if (i == -1) franchise.eraOrder.size else i
            }
            entries.sortWith(
                compareBy<Entry> { eraRank(it.era) }
                    .thenBy { it.story }
                    .thenBy { it.date },
            )
        }
        return entries
    }

    /** Sort works by first publication, or keep the suggested reading order. */
    fun orderWorks(works: List<Work>, mode: OrderMode): List<Work> {
        if (mode == OrderMode.RELEASE) {
            return works.withIndex()
                .sortedWith(compareBy({ it.value.year }, { it.index }))
                .map { it.value }
        }
        return works
    }

    fun withPositions(ordered: List<Entry>): List<PositionedEntry> =
        ordered.mapIndexed { index, entry -> PositionedEntry(entry, index + 1) }

    fun withWorkPositions(ordered: List<Work>): List<PositionedWork> =
        ordered.mapIndexed { index, work -> PositionedWork(work, index + 1) }

    fun matchesKind(entry: Entry, kind: String): Boolean = when (kind) {
        "film" -> entry.isFilm
        "series" -> entry.kind == Entry.KIND_SERIES || entry.kind == Entry.KIND_SPECIAL
        "game" -> entry.isGame
        else -> true
    }

    /** The filter groups a mixed franchise can offer: films, series/specials and games. */
    private fun kindGroup(entry: Entry): String = when (entry.kind) {
        Entry.KIND_FILM -> "film"
        Entry.KIND_GAME -> "game"
        else -> "series"
    }

    fun hasKind(franchise: Franchise, kind: String): Boolean =
        franchise.entries.any { matchesKind(it, kind) }

    fun hasMixedKinds(franchise: Franchise): Boolean =
        franchise.entries.map { kindGroup(it) }.distinct().size > 1

    /** "played" for a game-only franchise, "watched" for everything else. */
    fun watchVerb(franchise: Franchise): String =
        if (franchise.entries.isNotEmpty() && franchise.entries.all { it.isGame }) "played" else "watched"

    fun hasOptional(franchise: Franchise): Boolean = franchise.entries.any { it.optional }

    fun hasAnimation(franchise: Franchise): Boolean = franchise.entries.any { it.animated }

    fun hasOptionalWorks(works: List<Work>): Boolean = works.any { it.optional }

    fun visibleItems(
        items: List<PositionedEntry>,
        watched: Set<String>,
        filter: WatchFilter,
    ): List<PositionedEntry> = items.filter { item ->
        val entry = item.entry
        matchesKind(entry, filter.kind) &&
            !(filter.essentials && entry.optional) &&
            !(filter.hideSeen && entry.id in watched)
    }

    fun groupItems(items: List<PositionedEntry>, mode: OrderMode): List<EntryGroup> {
        if (mode == OrderMode.RELEASE) {
            return if (items.isEmpty()) emptyList() else listOf(EntryGroup(null, items))
        }
        val groups = mutableListOf<EntryGroup>()
        for (item in items) {
            val last = groups.lastOrNull()
            if (last != null && last.label == item.entry.era) {
                groups[groups.lastIndex] = last.copy(items = last.items + item)
            } else {
                groups.add(EntryGroup(item.entry.era, listOf(item)))
            }
        }
        return groups
    }

    fun visibleWorks(
        items: List<PositionedWork>,
        read: Set<String>,
        filter: BookFilter,
    ): List<PositionedWork> = items.filter { item ->
        val work = item.work
        val formatOk = filter.format == "all" || work.format == filter.format
        formatOk &&
            !(filter.essentials && work.optional) &&
            !(filter.hideRead && work.id in read)
    }

    fun groupWorks(items: List<PositionedWork>, mode: OrderMode): List<WorkGroup> {
        if (mode == OrderMode.RELEASE) {
            return if (items.isEmpty()) emptyList() else listOf(WorkGroup(null, items))
        }
        val groups = mutableListOf<WorkGroup>()
        for (item in items) {
            val last = groups.lastOrNull()
            if (last != null && last.label == item.work.path) {
                groups[groups.lastIndex] = last.copy(items = last.items + item)
            } else {
                groups.add(WorkGroup(item.work.path, listOf(item)))
            }
        }
        return groups
    }

    fun progressOf(entries: List<Entry>, watched: Set<String>, today: String = today()): Progress {
        val counted = entries.filter { isReleased(it, today) }
        val done = counted.count { it.id in watched }
        return Progress(done, counted.size)
    }

    fun franchiseProgress(franchise: Franchise, watched: Set<String>, today: String = today()): Progress =
        progressOf(franchise.entries, watched, today)

    fun statusOf(progress: Progress): String = when {
        progress.total > 0 && progress.done >= progress.total -> "completed"
        progress.done > 0 -> "in-progress"
        else -> "not-started"
    }

    /** The first released, unseen entry in the given order. */
    fun nextUp(
        ordered: List<Entry>,
        watched: Set<String>,
        today: String = today(),
        filter: WatchFilter = WatchFilter(),
    ): Entry? = ordered.firstOrNull { entry ->
        isReleased(entry, today) &&
            entry.id !in watched &&
            (!filter.essentials || !entry.optional) &&
            matchesKind(entry, filter.kind)
    }

    /** Ids of every released entry up to and including [entryId], in the given order. */
    fun idsUpTo(items: List<Entry>, entryId: String, today: String = today()): List<String> {
        val index = items.indexOfFirst { it.id == entryId }
        if (index == -1) return emptyList()
        return items.take(index + 1).filter { isReleased(it, today) }.map { it.id }
    }

    fun formatCounts(works: List<Work>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (work in works) counts[work.format] = (counts[work.format] ?: 0) + 1
        return counts
    }

    fun readProgress(works: List<Work>, read: Set<String>): Progress {
        val done = works.count { it.id in read }
        return Progress(done, works.size)
    }

    fun nextToRead(
        ordered: List<Work>,
        read: Set<String>,
        filter: BookFilter = BookFilter(),
    ): Work? = ordered.firstOrNull { work ->
        work.id !in read &&
            (!filter.essentials || !work.optional) &&
            (filter.format == "all" || work.format == filter.format)
    }

    fun workIdsUpTo(items: List<Work>, workId: String): List<String> {
        val index = items.indexOfFirst { it.id == workId }
        if (index == -1) return emptyList()
        return items.take(index + 1).map { it.id }
    }

    fun overallProgress(franchises: List<Franchise>, watchedFor: (String) -> List<String>, today: String = today()): Progress {
        var done = 0
        var total = 0
        for (f in franchises) {
            val p = franchiseProgress(f, watchedFor(f.id).toSet(), today)
            done += p.done
            total += p.total
        }
        return Progress(done, total)
    }

    fun overallReadProgress(
        franchises: List<Franchise>,
        worksFor: (String) -> List<Work>,
        readFor: (String) -> List<String>,
    ): Progress {
        var done = 0
        var total = 0
        for (f in franchises) {
            val p = readProgress(worksFor(f.id), readFor(f.id).toSet())
            done += p.done
            total += p.total
        }
        return Progress(done, total)
    }

    fun describeCounts(franchise: Franchise): String {
        val allGames = franchise.entries.isNotEmpty() && franchise.entries.all { it.isGame }
        val other = franchise.entries.count { kindGroup(it) == "series" }
        val parts = mutableListOf("${franchise.entries.size} ${if (allGames) "games" else "titles"}")
        if (other > 0) parts.add("$other series & specials")
        return parts.joinToString(" · ")
    }

    fun describeWorkCounts(works: List<Work>): String = when (works.size) {
        0 -> "No books or comics"
        1 -> "1 book or comic"
        else -> "${works.size} books and comics"
    }

    fun monthLabel(iso: String): String {
        val parts = iso.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        return "${MONTHS[(m - 1).coerceIn(0, 11)]} $y"
    }

    fun formatDate(iso: String): String = try {
        val date = LocalDate.parse(iso)
        "${MONTHS_SHORT[date.monthValue - 1]} ${date.dayOfMonth}, ${date.year}"
    } catch (_: Exception) {
        iso
    }

    fun addDays(iso: String, days: Long): String =
        LocalDate.parse(iso).plusDays(days).format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun byRecent(list: List<Franchise>, recent: List<String>): List<Franchise> {
        val rank = { id: String ->
            val i = recent.indexOf(id)
            if (i == -1) Int.MAX_VALUE else i
        }
        return list.withIndex()
            .sortedWith(compareBy({ rank(it.value.id) }, { it.index }))
            .map { it.value }
    }

    fun screenActive(seen: Progress): Boolean =
        seen.total > 0 && seen.done > 0 && seen.done < seen.total

    fun booksActive(books: Progress): Boolean =
        books.total > 0 && books.done > 0 && books.done < books.total

    fun continueList(
        catalog: Catalog,
        state: ProgressState,
        today: String = today(),
        limit: Int = 12,
    ): List<ContinueCard> {
        val out = mutableListOf<ContinueCard>()
        for (f in byRecent(catalog.franchises, state.recent)) {
            val watched = state.watched[f.id].orEmpty()
            val read = state.read[f.id].orEmpty()
            val seen = franchiseProgress(f, watched.toSet(), today)
            val books = readProgress(catalog.worksFor(f.id), read.toSet())
            if (!screenActive(seen) && !booksActive(books)) continue

            val ordered = orderEntries(f, state.mode)
            val next = nextUp(ordered, watched.toSet(), today)
            val works = orderWorks(catalog.worksFor(f.id), state.mode)
            val nextBook = nextToRead(works, read.toSet())
            out.add(
                ContinueCard(
                    franchise = f,
                    next = next,
                    position = if (next == null) 0 else ordered.indexOfFirst { it.id == next.id } + 1,
                    nextBook = nextBook,
                    bookPosition = if (nextBook == null) 0 else works.indexOfFirst { it.id == nextBook.id } + 1,
                    seen = seen,
                    books = books,
                ),
            )
            if (out.size >= limit) break
        }
        return out
    }

    fun upNextList(cards: List<ContinueCard>, limit: Int = 8): List<UpNextItem> =
        cards.filter { screenActive(it.seen) && it.next != null }
            .take(limit)
            .map { UpNextItem(it.franchise, it.next!!, it.position) }

    fun readNextList(cards: List<ContinueCard>, limit: Int = 8): List<ReadNextItem> =
        cards.filter { booksActive(it.books) && it.nextBook != null }
            .take(limit)
            .map { ReadNextItem(it.franchise, it.nextBook!!, it.bookPosition) }

    fun upcomingList(franchises: List<Franchise>, today: String = today()): List<UpcomingItem> {
        val out = mutableListOf<UpcomingItem>()
        for (f in franchises) {
            for (entry in f.entries) {
                if (entry.date > today) out.add(UpcomingItem(f, entry))
            }
        }
        return out.withIndex()
            .sortedWith(compareBy({ it.value.entry.date }, { it.index }))
            .map { it.value }
    }

    fun recentReleases(franchises: List<Franchise>, today: String = today(), days: Long = 90): List<UpcomingItem> {
        val from = addDays(today, -days)
        val out = mutableListOf<UpcomingItem>()
        for (f in franchises) {
            for (entry in f.entries) {
                if (entry.date <= today && entry.date >= from) out.add(UpcomingItem(f, entry))
            }
        }
        return out.withIndex()
            .sortedWith(compareByDescending<IndexedValue<UpcomingItem>> { it.value.entry.date }.thenBy { it.index })
            .map { it.value }
    }

    fun genreShelves(franchises: List<Franchise>, genres: List<String>): List<Pair<String, List<Franchise>>> =
        genres.map { genre -> genre to franchises.filter { it.genre == genre } }
            .filter { it.second.isNotEmpty() }

    fun inLibrary(saved: Boolean, seen: Progress, books: Progress): Boolean =
        saved || seen.done > 0 || books.done > 0

    fun libraryList(
        catalog: Catalog,
        state: ProgressState,
        filter: String,
        today: String = today(),
    ): List<LibraryItem> {
        val items = mutableListOf<LibraryItem>()
        for (f in catalog.franchises) {
            val seen = franchiseProgress(f, state.watched[f.id].orEmpty().toSet(), today)
            val books = readProgress(catalog.worksFor(f.id), state.read[f.id].orEmpty().toSet())
            val isSaved = f.id in state.saved
            if (!inLibrary(isSaved, seen, books)) continue
            if (filter != "all" && statusOf(seen) != filter) continue
            items.add(LibraryItem(f, seen, books, seen.total - seen.done, isSaved))
        }
        val byName = compareBy(String.CASE_INSENSITIVE_ORDER) { item: LibraryItem -> item.franchise.name }
        return when (state.libSort) {
            LibSort.AZ -> items.sortedWith(byName)
            LibSort.UNSEEN -> items.sortedWith(compareByDescending<LibraryItem> { it.unseen }.then(byName))
            LibSort.PROGRESS -> items.sortedWith(compareByDescending<LibraryItem> { it.seen.pct }.then(byName))
            LibSort.RECENT -> {
                val rank = { id: String ->
                    val i = state.recent.indexOf(id)
                    if (i == -1) Int.MAX_VALUE else i
                }
                items.withIndex()
                    .sortedWith(compareBy({ rank(it.value.franchise.id) }, { it.index }))
                    .map { it.value }
            }
        }
    }

    fun searchAll(catalog: Catalog, query: String, limit: Int = 25): SearchResults {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isEmpty()) return SearchResults(emptyList(), emptyList(), emptyList())
        val franchises = mutableListOf<Franchise>()
        val titles = mutableListOf<UpcomingItem>()
        val works = mutableListOf<Pair<Franchise, Work>>()
        for (f in catalog.franchises) {
            if ("${f.name} ${f.hook} ${f.genre}".lowercase(Locale.ROOT).contains(needle)) franchises.add(f)
            for (entry in f.entries) {
                if (titles.size < limit && entry.title.lowercase(Locale.ROOT).contains(needle)) {
                    titles.add(UpcomingItem(f, entry))
                }
            }
            for (work in catalog.worksFor(f.id)) {
                val hay = "${work.title} ${work.by} ${work.path}".lowercase(Locale.ROOT)
                if (works.size < limit && hay.contains(needle)) works.add(f to work)
            }
        }
        return SearchResults(franchises, titles, works)
    }

    fun appToday(): String = today()
}
