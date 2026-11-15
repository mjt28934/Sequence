package com.sequence.watchorder

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.sequence.watchorder.data.BookFilter
import com.sequence.watchorder.data.Catalog
import com.sequence.watchorder.data.Entry
import com.sequence.watchorder.data.LibSort
import com.sequence.watchorder.data.LibView
import com.sequence.watchorder.data.Logic
import com.sequence.watchorder.data.OrderMode
import com.sequence.watchorder.data.ProgressState
import com.sequence.watchorder.data.ProgressStore
import com.sequence.watchorder.data.StartTab
import com.sequence.watchorder.data.ThemeMode
import com.sequence.watchorder.data.WatchFilter
import com.sequence.watchorder.data.Work

enum class AppTab(val label: String) {
    HOME("Home"),
    LIBRARY("Library"),
    UPCOMING("Upcoming"),
    TAGS("Tags"),
}

enum class DetailTab { WATCH, BOOKS }

sealed interface Sheet {
    data object Order : Sheet
    data object Filter : Sheet
    data object More : Sheet
    data object Sort : Sheet
    data object ConfirmClearAll : Sheet
    data class EntrySheet(val entryId: String) : Sheet
    data class WorkSheet(val workId: String) : Sheet
}

data class Snack(val id: Long, val text: String, val undo: (() -> Unit)? = null)

class MainViewModel : AndroidViewModel {

    val catalog: Catalog
    private val store: ProgressStore?

    var progress by mutableStateOf(ProgressState())
        private set

    constructor(app: Application) : super(app) {
        catalog = Catalog.load(app)
        store = ProgressStore(app, catalog.franchises.map { it.id }.toSet())
        progress = store.load()
        tab = progress.startTab.toTab()
    }

    /**
     * State-only instance used by screenshot tests: no assets, no disk persistence.
     * Everything else behaves identically.
     */
    internal constructor(catalog: Catalog, initial: ProgressState) : super(Application()) {
        this.catalog = catalog
        this.store = null
        this.progress = initial
        tab = initial.startTab.toTab()
    }

    var tab by mutableStateOf(AppTab.HOME)
        private set
    var genre by mutableStateOf("all")
        private set
    var libraryFilter by mutableStateOf("all")
        private set
    var query by mutableStateOf("")
        private set

    var detailId by mutableStateOf<String?>(null)
        private set
    var detailTab by mutableStateOf(DetailTab.WATCH)
        private set
    var watchFilter by mutableStateOf(WatchFilter())
        private set
    var bookFilter by mutableStateOf(BookFilter())
        private set
    var blurbOpen by mutableStateOf(false)
        private set
    var settingsOpen by mutableStateOf(false)
        private set

    var sheet by mutableStateOf<Sheet?>(null)
        private set

    var snack by mutableStateOf<Snack?>(null)
        private set
    var searchOpen by mutableStateOf(false)
        private set
    var searchFocusRequest by mutableIntStateOf(0)
        private set

    private var snackId = 0L

    val franchise get() = detailId?.let { catalog.franchise(it) }

    fun worksFor(franchise: com.sequence.watchorder.data.Franchise): List<Work> =
        catalog.worksFor(franchise.id)

    fun watchedFor(id: String): List<String> = progress.watched[id].orEmpty()

    fun readFor(id: String): List<String> = progress.read[id].orEmpty()

    fun selectTab(value: AppTab) {
        tab = value
    }

    /** Opens the search field on Home and asks it to take focus. */
    fun openSearch() {
        searchOpen = true
        searchFocusRequest += 1
    }

    /** Closes search and clears the query so Home is not left filtered. */
    fun closeSearch() {
        searchOpen = false
        query = ""
    }

    fun selectGenre(value: String) {
        genre = value
    }

    fun selectLibraryFilter(value: String) {
        libraryFilter = value
    }

    fun updateQuery(value: String) {
        query = value
    }

    fun openFranchise(id: String, books: Boolean = false) {
        sheet = null
        detailId = id
        detailTab = if (books) DetailTab.BOOKS else DetailTab.WATCH
        watchFilter = WatchFilter()
        bookFilter = BookFilter()
        blurbOpen = false
        settingsOpen = false
        mutate({ it.touched(id) }, "", undoable = false, showSnack = false)
    }

    fun openSettings() {
        sheet = null
        detailId = null
        settingsOpen = true
    }

    fun closeDetail() {
        sheet = null
        detailId = null
        blurbOpen = false
    }

    fun closeSettings() {
        settingsOpen = false
    }

    /** Returns true when it handled the back press. */
    fun onBack(): Boolean {
        if (sheet != null) {
            sheet = null
            return true
        }
        if (detailId != null) {
            closeDetail()
            return true
        }
        if (settingsOpen) {
            closeSettings()
            return true
        }
        if (searchOpen) {
            closeSearch()
            return true
        }
        if (tab == AppTab.TAGS && genre != "all") {
            genre = "all"
            return true
        }
        if (tab != AppTab.HOME) {
            tab = AppTab.HOME
            return true
        }
        return false
    }

    fun openSheet(value: Sheet) {
        sheet = value
    }

    fun closeSheet() {
        sheet = null
    }

    fun selectDetailTab(value: DetailTab) {
        detailTab = value
    }

    fun toggleBlurb() {
        blurbOpen = !blurbOpen
    }

    fun updateWatchFilter(value: WatchFilter) {
        watchFilter = value
    }

    fun updateBookFilter(value: BookFilter) {
        bookFilter = value
    }

    fun setMode(value: OrderMode) {
        mutate({ it.copy(mode = value) }, "", undoable = false, showSnack = false)
        sheet = null
    }

    fun setLibSort(value: LibSort) {
        mutate({ it.copy(libSort = value) }, "", undoable = false, showSnack = false)
        sheet = null
    }

    fun toggleLibView() {
        val next = if (progress.libView == LibView.GRID) LibView.LIST else LibView.GRID
        mutate({ it.copy(libView = next) }, "", undoable = false, showSnack = false)
    }

    fun setLibView(value: LibView) {
        mutate({ it.copy(libView = value) }, "", undoable = false, showSnack = false)
    }

    fun setTheme(value: ThemeMode) {
        mutate({ it.copy(theme = value) }, "", undoable = false, showSnack = false)
    }

    fun setHaptics(on: Boolean) {
        mutate({ it.copy(haptics = on) }, "", undoable = false, showSnack = false)
    }

    fun setReduceMotion(on: Boolean) {
        mutate(
            { it.copy(reduceMotion = on) },
            if (on) "Animations reduced" else "Animations on",
            undoable = false,
        )
    }

    fun setStartTab(value: StartTab) {
        mutate({ it.copy(startTab = value) }, "", undoable = false, showSnack = false)
    }

    fun setHideFinished(on: Boolean) {
        mutate(
            { it.copy(hideFinished = on) },
            if (on) "Finished franchises hidden on Home" else "Finished franchises shown on Home",
            undoable = false,
        )
    }

    /** Back to the shipped defaults; progress, saved titles and every mark stay untouched. */
    fun resetSettings() {
        mutate(
            {
                it.copy(
                    mode = OrderMode.RELEASE,
                    libSort = LibSort.RECENT,
                    libView = LibView.GRID,
                    theme = ThemeMode.SYSTEM,
                    haptics = true,
                    reduceMotion = false,
                    startTab = StartTab.HOME,
                    hideFinished = false,
                )
            },
            "Settings reset to defaults",
            undoable = false,
        )
        tab = AppTab.HOME
    }

    /** The saved state as a JSON backup, or null when nothing is persisted (screenshot tests). */
    fun exportProgress(): String? = store?.encode(progress)

    /** Replaces everything with a backup; false when the text is not a Sequence backup. */
    fun importProgress(text: String): Boolean {
        val loaded = store?.decode(text) ?: return false
        progress = loaded
        store.save(loaded)
        tab = loaded.startTab.toTab()
        snack = Snack(++snackId, "Progress and settings imported")
        return true
    }

    /** Shows a one-off message, used by the backup actions. */
    fun notify(text: String) {
        snack = Snack(++snackId, text)
    }

    fun toggleSeen(franchiseId: String, entry: Entry) {
        val before = watchedFor(franchiseId)
        val on = entry.id !in before
        val after = if (on) before + entry.id else before - entry.id
        mutate(
            { it.copy(watched = it.watched + (franchiseId to after)).touched(franchiseId) },
            (if (on) "Marked seen: " else "Marked unseen: ") + entry.title,
        )
    }

    fun toggleRead(franchiseId: String, work: Work) {
        val before = readFor(franchiseId)
        val on = work.id !in before
        val after = if (on) before + work.id else before - work.id
        mutate(
            { it.copy(read = it.read + (franchiseId to after)).touched(franchiseId) },
            (if (on) "Marked read: " else "Marked unread: ") + work.title,
        )
    }

    fun markSeenUpTo(franchiseId: String, ids: List<String>) {
        if (ids.isEmpty()) return
        val before = watchedFor(franchiseId)
        val merged = (before + ids).distinct()
        val added = merged.size - before.size
        if (added == 0) return
        mutate(
            { it.copy(watched = it.watched + (franchiseId to merged)).touched(franchiseId) },
            "Marked $added ${if (added == 1) "title" else "titles"} as seen",
        )
    }

    fun markReadUpTo(franchiseId: String, ids: List<String>) {
        if (ids.isEmpty()) return
        val before = readFor(franchiseId)
        val merged = (before + ids).distinct()
        val added = merged.size - before.size
        if (added == 0) return
        mutate(
            { it.copy(read = it.read + (franchiseId to merged)).touched(franchiseId) },
            "Marked $added ${if (added == 1) "book" else "books"} as read",
        )
    }

    fun clearSeen(franchiseId: String) {
        mutate(
            { it.copy(watched = it.watched + (franchiseId to emptyList())) },
            "Cleared everything seen",
        )
    }

    fun clearRead(franchiseId: String) {
        mutate(
            { it.copy(read = it.read + (franchiseId to emptyList())) },
            "Cleared everything read",
        )
    }

    fun toggleSaved(franchiseId: String) {
        val on = franchiseId !in progress.saved
        mutate(
            {
                it.copy(
                    saved = if (on) it.saved + franchiseId else it.saved.filter { id -> id != franchiseId },
                )
            },
            if (on) "Saved to your library" else "Removed from your library",
        )
    }

    fun clearAll() {
        mutate(
            {
                it.copy(
                    watched = emptyMap(),
                    read = emptyMap(),
                    saved = emptyList(),
                    recent = emptyList(),
                )
            },
            "All progress cleared",
            undoable = false,
        )
        sheet = null
    }

    fun dismissSnack() {
        snack = null
    }

    fun runUndo(snackToRun: Snack) {
        snackToRun.undo?.invoke()
        snack = null
    }

    private fun mutate(
        block: (ProgressState) -> ProgressState,
        message: String,
        undoable: Boolean = true,
        showSnack: Boolean = true,
    ) {
        val before = progress
        val after = block(before)
        if (after == before) return
        progress = after
        store?.save(after)
        if (showSnack) {
            snack = Snack(
                id = ++snackId,
                text = message,
                undo = if (undoable) {
                    {
                        progress = before
                        store?.save(before)
                    }
                } else {
                    null
                },
            )
        }
    }

    private fun ProgressState.touched(id: String): ProgressState =
        copy(recent = (listOf(id) + recent.filter { it != id }).take(12))

    fun today(): String = Logic.today()
}

private fun StartTab.toTab(): AppTab = when (this) {
    StartTab.HOME -> AppTab.HOME
    StartTab.LIBRARY -> AppTab.LIBRARY
    StartTab.UPCOMING -> AppTab.UPCOMING
    StartTab.TAGS -> AppTab.TAGS
}
