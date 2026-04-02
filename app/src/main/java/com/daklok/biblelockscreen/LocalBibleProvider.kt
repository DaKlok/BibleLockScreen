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

    fun getVerse(context: Context, lang: String): Pair<String, String> {
        return try {

            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

            val jsonString = context.assets.open("verses_$lang.json").bufferedReader().use { it.readText() }

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
}