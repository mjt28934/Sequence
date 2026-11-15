package com.sequence.watchorder.data

/** A film, series or special in a franchise's watch order. */
data class Entry(
    val id: String,
    val title: String,
    val date: String,
    val year: Int,
    val kind: String = KIND_FILM,
    val era: String? = null,
    val story: Int = 0,
    val note: String? = null,
    val optional: Boolean = false,
) {
    val isFilm: Boolean get() = kind == KIND_FILM
    val kindLabel: String
        get() = when (kind) {
            KIND_SERIES -> "Series"
            KIND_SPECIAL -> "Special"
            else -> "Film"
        }

    companion object {
        const val KIND_FILM = "film"
        const val KIND_SERIES = "series"
        const val KIND_SPECIAL = "special"
    }
}

/** A movie franchise, with its entries and presentation details. */
data class Franchise(
    val id: String,
    val name: String,
    val years: String,
    val hook: String,
    val blurb: String,
    val storyHint: String,
    val genre: String,
    val accentHex: String,
    val eraOrder: List<String>,
    val entries: List<Entry>,
)

/** A book, comic or other print tie-in. */
data class Work(
    val id: String,
    val title: String,
    val year: Int,
    val format: String,
    val by: String,
    val path: String,
    val note: String? = null,
    val optional: Boolean = false,
    val cover: String? = null,
) {
    companion object {
        val FORMATS = listOf("novel", "novelization", "comic", "graphic-novel", "short-stories", "companion")
        val FORMAT_LABELS = mapOf(
            "novel" to "Novels",
            "novelization" to "Novelizations",
            "comic" to "Comics",
            "graphic-novel" to "Graphic novels",
            "short-stories" to "Short stories",
            "companion" to "Companions",
        )
        val FORMAT_SINGULAR = mapOf(
            "novel" to "Novel",
            "novelization" to "Novelization",
            "comic" to "Comic",
            "graphic-novel" to "Graphic novel",
            "short-stories" to "Short stories",
            "companion" to "Companion",
        )
    }
}

/** The four colour tokens derived from a franchise's accent. */
data class Accent(
    val accentHex: String,
    val inkHex: String,
    val deepHex: String,
    val softHex: String,
)

data class Progress(
    val done: Int,
    val total: Int,
) {
    val pct: Int get() = if (total == 0) 0 else Math.round((done / total.toFloat()) * 100f)
}

data class PositionedEntry(val entry: Entry, val position: Int)

data class PositionedWork(val work: Work, val position: Int)

data class EntryGroup(val label: String?, val items: List<PositionedEntry>)

data class WorkGroup(val label: String?, val items: List<PositionedWork>)

enum class OrderMode { RELEASE, STORY }

enum class LibSort(val label: String) {
    RECENT("Recently used"),
    AZ("A to Z"),
    UNSEEN("Most left to watch"),
    PROGRESS("Furthest along"),
}

enum class LibView { GRID, LIST }

data class ProgressState(
    val watched: Map<String, List<String>> = emptyMap(),
    val read: Map<String, List<String>> = emptyMap(),
    val mode: OrderMode = OrderMode.RELEASE,
    val saved: List<String> = emptyList(),
    val recent: List<String> = emptyList(),
    val libSort: LibSort = LibSort.RECENT,
    val libView: LibView = LibView.GRID,
)

data class WatchFilter(
    val kind: String = "all",
    val hideSeen: Boolean = false,
    val essentials: Boolean = false,
)

data class BookFilter(
    val format: String = "all",
    val hideRead: Boolean = false,
    val essentials: Boolean = false,
)

data class ContinueCard(
    val franchise: Franchise,
    val next: Entry?,
    val position: Int,
    val nextBook: Work?,
    val bookPosition: Int,
    val seen: Progress,
    val books: Progress,
)

data class UpNextItem(val franchise: Franchise, val entry: Entry, val position: Int)

data class ReadNextItem(val franchise: Franchise, val work: Work, val position: Int)

data class UpcomingItem(val franchise: Franchise, val entry: Entry)

data class LibraryItem(
    val franchise: Franchise,
    val seen: Progress,
    val books: Progress,
    val unseen: Int,
    val saved: Boolean,
)

data class SearchResults(
    val franchises: List<Franchise>,
    val titles: List<UpcomingItem>,
    val works: List<Pair<Franchise, Work>>,
)
