package com.daklok.biblelockscreen

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object YouVersionFetcher {

    private const val TAG = "BibleLockScreenDebug"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun getVerseOfTheDay(language: String = "SK"): Pair<String, String>? {
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

                // 2. KROK: Načítame cieľovú stránku podľa jazyka
                val url = when (language.uppercase()) {
                    "EN" -> "https://www.bible.com/en-GB/bible/111/$verseId" // NIV
                    "CZ" -> "https://www.bible.com/cs/bible/509/$verseId" // CEP
                    "ES" -> "https://www.bible.com/es/bible/128/$verseId" // NVI (Spanish)
                    "IT" -> "https://www.bible.com/it/bible/122/$verseId" // NR2006 (Italian)
                    "FR" -> "https://www.bible.com/fr/bible/133/$verseId" // BDS (French)
                    "DE" -> "https://www.bible.com/de/bible/157/$verseId" // SCH2000 (German)
                    "HU" -> "https://www.bible.com/hu/bible/17/$verseId" // KAR (Hungarian)
                    "PL" -> "https://www.bible.com/pl/bible/138/$verseId" // UBG (Polish)
                    else -> "https://www.bible.com/sk/bible/163/$verseId" // SK SSV (Default)
                }
                
                val doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get()

                // 3. KROK: Získanie textu priamo z HTML <p> v sekcii <main>
                val verseText = doc
                    .select("main p")
                    .first()
                    ?.text()
                    ?.trim()
                    ?: ""

                Log.d(TAG, "TEXT VERŠA: $verseText")

                // 4. KROK: Získanie referencie z og:title
                val fullTitle = doc.select("meta[property=og:title]").attr("content")

                var reference = fullTitle.substringBefore(" -").trim()

                reference = reference
                    .replace(Regex("\\s*\\([^)]*\\)"), "")
                    .replace("SSV", "")
                    .replace("Katolícky preklad", "")
                    .replace("NIV", "")
                    .replace("CEP", "")
                    .replace("NVI", "")
                    .replace("NR2006", "")
                    .replace("BDS", "")
                    .replace("SCH2000", "")
                    .replace("KAR", "")
                    .replace("UBG", "")
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
