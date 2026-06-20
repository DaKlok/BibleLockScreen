package com.daklok.biblelockscreen

import android.content.Context
import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Full-app backup & restore. Bundles all user data into a single ZIP file:
 *
 *   bible_lockscreen_backup.zip
 *   ├── prefs.json          — every SharedPreferences key/value (with type info)
 *   ├── wallpaper.jpg       — the internal user_wallpaper.jpg (if one exists)
 *   └── databases/
 *       ├── verses_KJV.json
 *       ├── verses_NIV.json
 *       └── ...
 *
 * Restore writes everything back and the caller should recreate() the Activity
 * so all `remember { prefs... }` state re-initializes from the freshly-written
 * SharedPreferences.
 */
object SettingsBackupManager {

    private const val PREFS_NAME = "bible_app_prefs"
    private const val WALLPAPER_FILENAME = "user_wallpaper.jpg"
    private const val DB_DIR = "verse_databases"

    private const val PREFS_ENTRY = "prefs.json"
    private const val WALLPAPER_ENTRY = "wallpaper.jpg"
    private const val DB_PREFIX = "databases/"

    data class BackupSummary(
        val prefsCount: Int,
        val hasWallpaper: Boolean,
        val databaseCount: Int
    )

    // ─────────────────────────────────────────────────────────────────────
    // Export
    // ─────────────────────────────────────────────────────────────────────

    fun export(context: Context, outputUri: Uri): Result<BackupSummary> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val prefsJson = JsonObject()
            var prefsCount = 0

            val allPrefs: Map<String, Any?> = prefs.all
            for ((key, value) in allPrefs) {
                val entry = JsonObject()
                when (value) {
                    is Boolean -> {
                        entry.addProperty("type", "boolean")
                        entry.addProperty("value", value)
                    }
                    is Int -> {
                        entry.addProperty("type", "int")
                        entry.addProperty("value", value)
                    }
                    is Float -> {
                        entry.addProperty("type", "float")
                        entry.addProperty("value", value)
                    }
                    is Long -> {
                        entry.addProperty("type", "long")
                        entry.addProperty("value", value)
                    }
                    is String -> {
                        entry.addProperty("type", "string")
                        entry.addProperty("value", value)
                    }
                    is Set<*> -> {
                        entry.addProperty("type", "set")
                        val arr = JsonArray()
                        value.forEach { v -> arr.add(v?.toString() ?: "") }
                        entry.add("value", arr)
                    }
                    else -> continue // skip unknown types
                }
                prefsJson.add(key, entry)
                prefsCount++
            }

            val wallpaperFile = File(context.filesDir, WALLPAPER_FILENAME)
            val hasWallpaper = wallpaperFile.exists()

            val dbDir = File(context.filesDir, DB_DIR)
            val dbFiles: List<File> = if (dbDir.exists()) {
                dbDir.listFiles { f -> f.name.endsWith(".json") }?.toList() ?: emptyList()
            } else emptyList()

            context.contentResolver.openOutputStream(outputUri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    // prefs.json
                    zos.putNextEntry(ZipEntry(PREFS_ENTRY))
                    val prettyJson = GsonBuilder().setPrettyPrinting().create().toJson(prefsJson)
                    zos.write(prettyJson.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    // wallpaper.jpg (if the user has set one)
                    if (hasWallpaper) {
                        zos.putNextEntry(ZipEntry(WALLPAPER_ENTRY))
                        wallpaperFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }

                    // databases/*.json
                    for (dbFile in dbFiles) {
                        zos.putNextEntry(ZipEntry("$DB_PREFIX${dbFile.name}"))
                        dbFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            } ?: return Result.failure(Exception("Could not open output stream"))

            Result.success(BackupSummary(prefsCount, hasWallpaper, dbFiles.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Import
    // ─────────────────────────────────────────────────────────────────────

    fun import(context: Context, inputUri: Uri): Result<BackupSummary> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var prefsCount = 0
            var hasWallpaper = false
            var databaseCount = 0

            context.contentResolver.openInputStream(inputUri)?.use { istream ->
                ZipInputStream(istream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == PREFS_ENTRY -> {
                                val content = zis.readBytes().toString(Charsets.UTF_8)
                                val prefsJson = JsonParser.parseString(content).asJsonObject
                                val editor = prefs.edit()

                                editor.clear()
                                for ((key, valueEntry) in prefsJson.entrySet()) {
                                    val obj = valueEntry.asJsonObject
                                    val type = obj.get("type").asString
                                    val valueEl = obj.get("value")
                                    when (type) {
                                        "boolean" -> editor.putBoolean(key, valueEl.asBoolean)
                                        "int" -> editor.putInt(key, valueEl.asInt)
                                        "float" -> editor.putFloat(key, valueEl.asFloat)
                                        "long" -> editor.putLong(key, valueEl.asLong)
                                        "string" -> editor.putString(key, valueEl.asString)
                                        "set" -> {
                                            val set = valueEl.asJsonArray.map { it.asString }.toSet()
                                            editor.putStringSet(key, set)
                                        }
                                    }
                                    prefsCount++
                                }
                                editor.apply()
                            }

                            entry.name == WALLPAPER_ENTRY -> {
                                val wallpaperFile = File(context.filesDir, WALLPAPER_FILENAME)
                                wallpaperFile.outputStream().use { out -> zis.copyTo(out) }
                                hasWallpaper = true
                            }

                            entry.name.startsWith(DB_PREFIX) -> {
                                val dbDir = File(context.filesDir, DB_DIR).also { it.mkdirs() }
                                val filename = entry.name.removePrefix(DB_PREFIX)
                                if (filename.isNotBlank() && filename.endsWith(".json")) {
                                    val dbFile = File(dbDir, filename)
                                    dbFile.outputStream().use { out -> zis.copyTo(out) }
                                    databaseCount++
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: return Result.failure(Exception("Could not open input stream"))

            Result.success(BackupSummary(prefsCount, hasWallpaper, databaseCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
