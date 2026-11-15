package com.sequence.watchorder.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists progress as the same JSON shape the web build used in localStorage. */
class ProgressStore(context: Context, private val franchiseIds: Set<String>) {

    private val prefs = context.getSharedPreferences("sequence", Context.MODE_PRIVATE)

    fun load(): ProgressState {
        val raw = prefs.getString(KEY, null) ?: return ProgressState()
        return try {
            val state = JSONObject(raw).optJSONObject("state") ?: return ProgressState()
            val mode = if (state.optString("mode") == "story") OrderMode.STORY else OrderMode.RELEASE
            val sort = when (state.optString("libSort")) {
                "az" -> LibSort.AZ
                "unseen" -> LibSort.UNSEEN
                "progress" -> LibSort.PROGRESS
                else -> LibSort.RECENT
            }
            val view = if (state.optString("libView") == "list") LibView.LIST else LibView.GRID
            ProgressState(
                watched = readMap(state.optJSONObject("watched")),
                read = readMap(state.optJSONObject("read")),
                mode = mode,
                saved = readList(state.optJSONArray("saved")),
                recent = readList(state.optJSONArray("recent")),
                libSort = sort,
                libView = view,
            )
        } catch (_: Exception) {
            ProgressState()
        }
    }

    fun save(state: ProgressState) {
        val root = JSONObject()
        val obj = JSONObject()
            .put("watched", writeMap(state.watched))
            .put("read", writeMap(state.read))
            .put("mode", if (state.mode == OrderMode.STORY) "story" else "release")
            .put("saved", JSONArray(state.saved))
            .put("recent", JSONArray(state.recent))
            .put("libSort", state.libSort.name.lowercase())
            .put("libView", if (state.libView == LibView.LIST) "list" else "grid")
        root.put("state", obj)
        root.put("version", 0)
        prefs.edit().putString(KEY, root.toString()).apply()
    }

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
