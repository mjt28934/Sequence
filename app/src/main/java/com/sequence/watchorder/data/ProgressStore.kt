package com.sequence.watchorder.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists progress as the same JSON shape the web build used in localStorage. */
class ProgressStore(context: Context, private val franchiseIds: Set<String>) {

    private val prefs = context.getSharedPreferences("sequence", Context.MODE_PRIVATE)

    fun load(): ProgressState {
        val raw = prefs.getString(KEY, null) ?: return ProgressState()
        return decode(raw) ?: ProgressState()
    }

    /** The exact JSON written to prefs, also used for exported backups. */
    fun encode(state: ProgressState): String {
        val root = JSONObject()
        root.put("state", stateJson(state))
        root.put("version", 0)
        return root.toString()
    }

    /**
     * Parses saved text or an exported backup. Both the wrapped `{"state": ...}` shape and a bare
     * state object are accepted; null means the text is not a Sequence backup.
     */
    fun decode(raw: String): ProgressState? {
        return try {
            val root = JSONObject(raw)
            val state = root.optJSONObject("state") ?: root
            // A wrapped backup is always ours; a bare object has to carry at least one known mark,
            // so importing an unrelated JSON file cannot silently wipe progress.
            val isBackup = root.has("state") ||
                state.has("watched") || state.has("read") || state.has("mode")
            if (!isBackup) return null
            val mode = if (state.optString("mode") == "story") OrderMode.STORY else OrderMode.RELEASE
            val sort = when (state.optString("libSort")) {
                "az" -> LibSort.AZ
                "unseen" -> LibSort.UNSEEN
                "progress" -> LibSort.PROGRESS
                else -> LibSort.RECENT
            }
            val view = if (state.optString("libView") == "list") LibView.LIST else LibView.GRID
            val theme = when (state.optString("theme")) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            val startTab = when (state.optString("startTab")) {
                "library" -> StartTab.LIBRARY
                "upcoming" -> StartTab.UPCOMING
                "tags" -> StartTab.TAGS
                else -> StartTab.HOME
            }
            ProgressState(
                watched = readMap(state.optJSONObject("watched")),
                read = readMap(state.optJSONObject("read")),
                mode = mode,
                saved = readList(state.optJSONArray("saved")),
                recent = readList(state.optJSONArray("recent")),
                libSort = sort,
                libView = view,
                theme = theme,
                haptics = state.optBoolean("haptics", true),
                reduceMotion = state.optBoolean("reduceMotion", false),
                startTab = startTab,
                hideFinished = state.optBoolean("hideFinished", false),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun save(state: ProgressState) {
        prefs.edit().putString(KEY, encode(state)).apply()
    }

    private fun stateJson(state: ProgressState): JSONObject = JSONObject()
        .put("watched", writeMap(state.watched))
        .put("read", writeMap(state.read))
        .put("mode", if (state.mode == OrderMode.STORY) "story" else "release")
        .put("saved", JSONArray(state.saved))
        .put("recent", JSONArray(state.recent))
        .put("libSort", state.libSort.name.lowercase())
        .put("libView", if (state.libView == LibView.LIST) "list" else "grid")
        .put("theme", state.theme.name.lowercase())
        .put("haptics", state.haptics)
        .put("reduceMotion", state.reduceMotion)
        .put("startTab", state.startTab.name.lowercase())
        .put("hideFinished", state.hideFinished)

    private fun readMap(o: JSONObject?): Map<String, List<String>> {
        if (o == null) return emptyMap()
        val out = mutableMapOf<String, List<String>>()
        for (key in o.keys()) {
            val arr = o.optJSONArray(key) ?: continue
            val list = buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i, "")
                    if (value.isNotEmpty()) add(value)
                }
            }
            out[key] = list
        }
        return out
    }

    private fun readList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val value = arr.optString(i, "")
                if (value.isNotEmpty() && value in franchiseIds) add(value)
            }
        }
    }

    private fun writeMap(map: Map<String, List<String>>): JSONObject {
        val o = JSONObject()
        for ((key, value) in map) o.put(key, JSONArray(value))
        return o
    }

    private companion object {
        const val KEY = "sequence-progress"
    }
}
