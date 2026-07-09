package com.daklok.biblelockscreen

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * A verse the user has marked as a favorite.
 *
 * Identity is the combination of (text, ref, lang) — see
 * [FavoriteVersesManager.isFavorite]. This means the same verse imported
 * from two different databases (built-in vs. a custom one, or duplicated
 * across two custom databases) will not create duplicate favorite entries
 * as long as the text/ref/lang all match.
 */
data class FavoriteVerse(
    val text: String,
    val ref: String,
    val lang: String,
    val source: String,
    val addedAt: Long
)

/**
 * Stores the user's favorite verses in `filesDir/favorite_verses.json`.
 *
 * Follows the same on-disk-JSON pattern as [VerseJsonManager] (no Room, no
 * DataStore) but uses `org.json` instead of Gson, since this manager only
 * ever deals with a small flat list of simple string/long fields.
 */
object FavoriteVersesManager {

    private const val FILE_NAME = "favorite_verses.json"

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    /** Returns all favorites, most recently added first. */
    @Synchronized
    fun listFavorites(context: Context): List<FavoriteVerse> {
        val f = file(context)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val text = obj.optString("text", "")
                val ref = obj.optString("ref", "")
                val lang = obj.optString("lang", "")
                if (text.isEmpty() || ref.isEmpty()) return@mapNotNull null
                FavoriteVerse(
                    text = text,
                    ref = ref,
                    lang = lang,
                    source = obj.optString("source", LocalBibleProvider.SOURCE_BUILTIN.lowercase()),
                    addedAt = obj.optLong("addedAt", 0L)
                )
            }.sortedByDescending { it.addedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getFavoriteCount(context: Context): Int = listFavorites(context).size

    @Synchronized
    fun isFavorite(context: Context, text: String, ref: String, lang: String): Boolean {
        return listFavorites(context).any { it.text == text && it.ref == ref && it.lang == lang }
    }

    fun isFavorite(context: Context, verse: FavoriteVerse): Boolean =
        isFavorite(context, verse.text, verse.ref, verse.lang)

    /** Adds [verse] to the favorites, unless an identical (text, ref, lang) entry already exists. */
    @Synchronized
    fun addFavorite(context: Context, verse: FavoriteVerse): Boolean {
        val current = listFavorites(context).toMutableList()
        val alreadyExists = current.any {
            it.text == verse.text && it.ref == verse.ref && it.lang == verse.lang
        }
        if (alreadyExists) return false
        current.add(0, verse)
        return writeAll(context, current)
    }

    @Synchronized
    fun removeFavorite(context: Context, text: String, ref: String, lang: String): Boolean {
        val current = listFavorites(context).toMutableList()
        val removed = current.removeAll { it.text == text && it.ref == ref && it.lang == lang }
        if (!removed) return false
        return writeAll(context, current)
    }

    fun removeFavorite(context: Context, verse: FavoriteVerse): Boolean =
        removeFavorite(context, verse.text, verse.ref, verse.lang)

    private fun writeAll(context: Context, verses: List<FavoriteVerse>): Boolean {
        return try {
            val arr = JSONArray()
            verses.forEach { v ->
                val obj = JSONObject()
                obj.put("text", v.text)
                obj.put("ref", v.ref)
                obj.put("lang", v.lang)
                obj.put("source", v.source)
                obj.put("addedAt", v.addedAt)
                arr.put(obj)
            }
            file(context).writeText(arr.toString(2))
            true
        } catch (e: Exception) {
            false
        }
    }
}
