package com.daklok.biblelockscreen

import android.content.Context
import java.util.Calendar
import com.google.gson.Gson
import java.util.Locale


data class Verse(
    val text: String,
    val ref: String,
    val lang: String
)


object LocalBibleProvider {

    fun getDefaultLanguage(): String {
        return when (val sysLang = Locale.getDefault().language.uppercase()) {
            "CS" -> "CZ"
            "SK", "EN", "CZ", "ES", "IT", "FR", "DE", "HU", "PL" -> sysLang
            else -> "EN"
        }
    }

    /**
     * Loads JSON for a language — checks custom databases first, falls back to bundled assets.
     */
    private fun loadJson(context: Context, lang: String): String? {
        VerseJsonManager.loadCustomVerses(context, lang)?.let { verses ->
            if (verses.isNotEmpty()) return com.google.gson.Gson().toJson(verses)
        }
        return try {
            context.assets.open("verses_$lang.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) { null }
    }

    fun getVerse(context: Context, lang: String): Pair<String, String> {
        return try {

            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

            val jsonString = loadJson(context, lang) ?: return Pair("Error loading verse", "")

            val verses = Gson().fromJson(jsonString, Array<Verse>::class.java)

            if (verses.isNullOrEmpty()) {
                Pair("No verses found", "")
            } else {
                val selected = verses[dayOfYear % verses.size]
                Pair(selected.text, selected.ref)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("Error loading verse", "")
        }
    }

    /**
     * Returns a verse synced across all devices by dividing the UTC epoch hours
     * by the interval. Every device in the same UTC slot gets the same verse index.
     * For 24h intervals this is equivalent to the daily verse (same slot all day).
     */
    fun getVerseForInterval(context: Context, lang: String, intervalHours: Int): Pair<String, String> {
        return try {
            val jsonString = loadJson(context, lang) ?: return Pair("Error loading verse", "")

            val verses = Gson().fromJson(jsonString, Array<Verse>::class.java)

            if (verses.isNullOrEmpty()) {
                return Pair("No verses found", "")
            }

            val index = if (intervalHours >= 24) {
                // 📅 DAY-BASED (stable per calendar day)
                val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                (dayOfYear - 1) % verses.size
            } else {
                // ⏱️ HOUR-BASED (stable time slots)
                val epochHours = System.currentTimeMillis() / (1000L * 60 * 60)
                val slot = epochHours / intervalHours
                (slot % verses.size).toInt()
            }

            val selected = verses[index]
            Pair(selected.text, selected.ref)

        } catch (e: Exception) {
            e.printStackTrace()
            Pair("Error loading verse", "")
        }
    }

    /**
     * Returns the verse for screen-off mode using a simple incrementing counter
     * stored in SharedPreferences. Each screen-off event advances the counter by 1,
     * so every lock shows a new verse regardless of time elapsed.
     */
    fun getVerseForScreenOff(context: Context, lang: String): Pair<String, String> {
        return try {
            val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
            val index = prefs.getInt("screen_off_verse_index", 0)

            val jsonString = loadJson(context, lang) ?: return Pair("Error loading verse", "")
            val verses = Gson().fromJson(jsonString, Array<Verse>::class.java)

            if (verses.isNullOrEmpty()) {
                Pair("No verses found", "")
            } else {
                val selected = verses[index % verses.size]
                Pair(selected.text, selected.ref)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("Error loading verse", "")
        }
    }
}