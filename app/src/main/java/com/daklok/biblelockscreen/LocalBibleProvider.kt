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

    const val SOURCE_BUILTIN = "BUILTIN"
    const val SOURCE_CUSTOM = "CUSTOM"

    fun getDefaultLanguage(): String {
        return when (val sysLang = Locale.getDefault().language.uppercase()) {
            "CS" -> "CZ"
            "SK", "EN", "CZ", "ES", "IT", "FR", "DE", "HU", "PL" -> sysLang
            else -> "EN"
        }
    }

    /**
     * Loads JSON for a language, respecting the source.
     *
     * - SOURCE_CUSTOM → only loads from the user's custom databases.
     * - SOURCE_BUILTIN (default) → only loads from bundled assets.
     *
     * This separation is what lets a user have a custom DB with the same
     * code as a built-in (e.g. a custom "EN") and explicitly choose which
     * one to use.
     */
    private fun loadJson(context: Context, lang: String, source: String = SOURCE_BUILTIN): String? {
        return when (source) {
            SOURCE_CUSTOM -> {
                VerseJsonManager.loadCustomVerses(context, lang)?.let { verses ->
                    if (verses.isNotEmpty()) Gson().toJson(verses) else null
                }
            }
            else -> {
                try {
                    context.assets.open("verses_$lang.json").bufferedReader().use { it.readText() }
                } catch (e: Exception) { null }
            }
        }
    }

    fun getVerse(context: Context, lang: String, source: String = SOURCE_BUILTIN): Pair<String, String> {
        return try {

            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

            val jsonString = loadJson(context, lang, source) ?: return Pair("Error loading verse", "")

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
     * Returns a verse for the current interval-based time slot.
     *
     * The slot index is derived from LOCAL wall-clock time — matching how
     * the wallpaper-change alarms themselves are now scheduled (see
     * MainActivity.computeSlotInitialDelayMs / computeDailyCycleInitialDelayMs).
     * Previously this used raw UTC epoch hours, which could disagree with
     * the local-time-based alarm schedule: two runs close together in local
     * time (e.g. changing the anchor hour from 22:00 to 23:00) could still
     * land in the same UTC slot and show the same verse, since UTC slot
     * boundaries don't line up with local ones except at a UTC offset of 0.
     */
    fun getVerseForInterval(context: Context, lang: String, intervalHours: Int, source: String = SOURCE_BUILTIN): Pair<String, String> {
        return try {
            val jsonString = loadJson(context, lang, source) ?: return Pair("Error loading verse", "")

            val verses = Gson().fromJson(jsonString, Array<Verse>::class.java)

            if (verses.isNullOrEmpty()) {
                return Pair("No verses found", "")
            }

            val index = if (intervalHours >= 24) {
                // 📅 DAY-BASED (stable per calendar day, already local)
                val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                (dayOfYear - 1) % verses.size
            } else {
                // ⏱️ HOUR-BASED (stable local time slots)
                val cal = Calendar.getInstance()
                val localHourOfDay = cal.get(Calendar.HOUR_OF_DAY)
                // A simple monotonic local day counter — only needs to be
                // consistent day-to-day, not calendar-exact, since it's just
                // feeding a modulo further below.
                val localDayCount = cal.get(Calendar.YEAR) * 366L + cal.get(Calendar.DAY_OF_YEAR)
                val totalLocalHours = localDayCount * 24 + localHourOfDay
                val slot = totalLocalHours / intervalHours
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
    fun getVerseForScreenOff(context: Context, lang: String, source: String = SOURCE_BUILTIN): Pair<String, String> {
        return try {
            val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
            val index = prefs.getInt("screen_off_verse_index", 0)

            val jsonString = loadJson(context, lang, source) ?: return Pair("Error loading verse", "")
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