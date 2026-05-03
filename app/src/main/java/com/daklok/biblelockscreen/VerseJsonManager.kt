package com.daklok.biblelockscreen

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

object VerseJsonManager {

    private const val DB_DIR = "verse_databases"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private fun dbDir(context: Context): File =
        File(context.filesDir, DB_DIR).also { it.mkdirs() }

    fun listCustomDatabases(context: Context): List<CustomVerseDb> {
        return dbDir(context)
            .listFiles { f -> f.name.startsWith("verses_") && f.name.endsWith(".json") }
            ?.mapNotNull { file ->
                val lang = file.name.removePrefix("verses_").removeSuffix(".json")
                val verses = try {
                    gson.fromJson(file.readText(), Array<Verse>::class.java)?.toList() ?: emptyList()
                } catch (e: Exception) { emptyList() }
                if (verses.isNotEmpty()) CustomVerseDb(lang, file.name, verses.size, file)
                else null
            } ?: emptyList()
    }

    fun importFromUri(context: Context, uri: Uri, lang: String): Result<Int> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                ?: return Result.failure(Exception("Could not read file"))
            val verses = gson.fromJson(json, Array<Verse>::class.java)?.toList()
                ?: return Result.failure(Exception("Invalid JSON format"))
            if (verses.isEmpty()) return Result.failure(Exception("File contains no verses"))
            File(dbDir(context), "verses_${lang.uppercase()}.json").writeText(json)
            Result.success(verses.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveCustomDatabase(context: Context, lang: String, verses: List<Verse>): Result<Unit> {
        return try {
            val tagged = verses.map { it.copy(lang = lang.uppercase()) }
            File(dbDir(context), "verses_${lang.uppercase()}.json").writeText(gson.toJson(tagged))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportAsJson(context: Context, lang: String): String? {
        val customFile = File(dbDir(context), "verses_${lang.uppercase()}.json")
        if (customFile.exists()) return customFile.readText()
        return try {
            context.assets.open("verses_$lang.json").bufferedReader().readText()
        } catch (e: Exception) { null }
    }

    fun exportToDownloads(context: Context, lang: String): String? {
        val json = exportAsJson(context, lang) ?: return null
        return try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, "verses_$lang.json")
            file.writeText(json)
            file.absolutePath
        } catch (e: Exception) { null }
    }

    fun deleteCustomDatabase(context: Context, lang: String) {
        File(dbDir(context), "verses_${lang.uppercase()}.json").delete()
    }

    fun loadCustomVerses(context: Context, lang: String): List<Verse>? {
        val file = File(dbDir(context), "verses_${lang.uppercase()}.json")
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), Array<Verse>::class.java)?.toList()
        } catch (e: Exception) { null }
    }
}

data class CustomVerseDb(
    val lang: String,
    val fileName: String,
    val verseCount: Int,
    val file: File
)