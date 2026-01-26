package com.daklok.biblelockscreen

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object YouVersionFetcher {

    private const val TAG = "BibleLockScreenDebug"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun getVerseOfTheDay(): Pair<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "--- ŠTART SŤAHOVANIA ---")

                // 1. KROK: Načítame anglickú stránku pre získanie ID
                val engDoc = Jsoup.connect("https://www.bible.com/en-GB/verse-of-the-day")
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get()

                val jsonData = engDoc.select("script#__NEXT_DATA__").first()?.data()
                if (jsonData == null) return@withContext null

                val regex = """\"usfm\":\"([^\"]+)\"""".toRegex()
                val verseId = regex.find(jsonData)?.groups?.get(1)?.value ?: return@withContext null

                Log.d(TAG, "Získané ID verša: $verseId")

                // 2. KROK: Načítame slovenskú stránku (Preklad 163)
                val slovakUrl = "https://www.bible.com/sk/bible/163/$verseId"
                val svkDoc = Jsoup.connect(slovakUrl)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get()

                // 3. KROK: Získanie textu priamo z HTML <p> v sekcii <main>
                val verseText = svkDoc
                    .select("main p")
                    .first()
                    ?.text()
                    ?.trim()
                    ?: ""

                Log.d(TAG, "--------------------------------------------------")
                Log.d(TAG, "TEXT VERŠA (Dĺžka: ${verseText.length} znakov):")
                Log.d(TAG, verseText)
                Log.d(TAG, "--------------------------------------------------")

                // 4. KROK: Získanie referencie z og:title
                val fullTitle = svkDoc.select("meta[property=og:title]").attr("content")

                var reference = fullTitle.substringBefore(" -").trim()

                reference = reference
                    .replace(Regex("\\s*\\([^)]*\\)"), "")
                    .replace("SSV", "")
                    .replace("Katolícky preklad", "")
                    .trim()

                Log.d(TAG, "Vyčistená referencia: $reference")

                if (verseText.isNotEmpty() && reference.isNotEmpty()) {
                    return@withContext Pair(verseText, reference)
                }

                return@withContext null

            } catch (e: Exception) {
                Log.e(TAG, "Kritická chyba: ${e.message}")
                return@withContext null
            }
        }
    }
}
