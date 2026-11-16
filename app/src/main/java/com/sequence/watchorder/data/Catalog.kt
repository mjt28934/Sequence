package com.sequence.watchorder.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** The static catalogue bundled in assets/sequence.json. */
class Catalog private constructor(
    val franchises: List<Franchise>,
    val works: Map<String, List<Work>>,
    val genres: List<String>,
    val dataAsOf: String,
    private val accents: Map<String, Accent>,
) {
    private val byId = franchises.associateBy { it.id }

    fun franchise(id: String): Franchise? = byId[id]

    fun worksFor(id: String): List<Work> = works[id].orEmpty()

    fun accent(id: String): Accent = accents[id] ?: DEFAULT_ACCENT

    fun totalEntries(): Int = franchises.sumOf { it.entries.size }

    companion object {
        val DEFAULT_ACCENT = Accent("#aac7ff", "#0a305f", "#1d2a44", "#1a2030")

        fun load(context: Context): Catalog {
            val text = context.assets.open("sequence.json").bufferedReader().use { it.readText() }
            return parse(text)
        }

        fun parse(text: String): Catalog {
            val root = JSONObject(text)
            val genres = root.getJSONArray("genres").toStringList()
            val franchises = root.getJSONArray("franchises").let { arr ->
                buildList { for (i in 0 until arr.length()) add(parseFranchise(arr.getJSONObject(i))) }
            }
            val worksObj = root.getJSONObject("works")
            val works = buildMap {
                for (fid in worksObj.keys()) {
                    val arr = worksObj.getJSONArray(fid)
                    put(fid, buildList { for (i in 0 until arr.length()) add(parseWork(arr.getJSONObject(i))) })
                }
            }
            val accentsObj = root.getJSONObject("accents")
            val accents = buildMap {
                for (fid in accentsObj.keys()) {
                    val a = accentsObj.getJSONObject(fid)
                    put(
                        fid,
                        Accent(
                            accentHex = a.getString("accent"),
                            inkHex = a.getString("ink"),
                            deepHex = a.getString("deep"),
                            softHex = a.getString("soft"),
                        ),
                    )
                }
            }
            return Catalog(franchises, works, genres, root.optString("dataAsOf", ""), accents)
        }

        private fun parseFranchise(o: JSONObject): Franchise {
            val entriesArr = o.getJSONArray("entries")
            val entries = buildList {
                for (i in 0 until entriesArr.length()) add(parseEntry(entriesArr.getJSONObject(i)))
            }
            return Franchise(
                id = o.getString("id"),
                name = o.getString("name"),
                years = o.optString("years", ""),
                hook = o.optString("hook", ""),
                blurb = o.optString("blurb", ""),
                storyHint = o.optString("storyHint", ""),
                genre = o.optString("genre", ""),
                accentHex = o.optString("accent", "#aac7ff"),
                eraOrder = o.optJSONArray("eraOrder")?.toStringList().orEmpty(),
                entries = entries,
            )
        }

        private fun parseEntry(o: JSONObject): Entry = Entry(
            id = o.getString("id"),
            title = o.getString("title"),
            date = o.getString("date"),
            year = o.optInt("year", o.getString("date").take(4).toIntOrNull() ?: 0),
            kind = o.optString("kind", "film").ifBlank { "film" },
            era = o.stringOrNull("era"),
            story = o.optInt("story", 0),
            note = o.stringOrNull("note"),
            optional = o.optBoolean("optional", false),
            animated = o.optBoolean("animated", false),
        )

        private fun parseWork(o: JSONObject): Work = Work(
            id = o.getString("id"),
            title = o.getString("title"),
            year = o.getInt("year"),
            format = o.getString("format"),
            by = o.getString("by"),
            path = o.getString("path"),
            note = o.stringOrNull("note"),
            optional = o.optBoolean("optional", false),
            cover = o.stringOrNull("cover"),
        )

        private fun JSONObject.stringOrNull(name: String): String? =
            if (has(name) && !isNull(name)) getString(name) else null

        private fun JSONArray.toStringList(): List<String> =
            buildList { for (i in 0 until length()) add(getString(i)) }
    }
}
