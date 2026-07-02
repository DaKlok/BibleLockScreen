package com.daklok.biblelockscreen

import android.content.Context
import android.os.PowerManager
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

enum class LogLevel(val label: String) {
    DEBUG("DEBUG"), INFO("INFO"), WARN("WARN"), ERROR("ERROR")
}

data class LogEntry(
    val timestampRaw: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val raw: String
)

/**
 * Lightweight, file-backed logger used to power the in-app "Developer Logs"
 * screen. Every line is flushed to disk immediately (not buffered in
 * memory), so the log survives the app being force-closed or killed by the
 * system — which is the whole point: it lets us see, on the *next* launch,
 * that the process died unexpectedly and a scheduled wallpaper update was
 * likely skipped as a result.
 */
object AppLogger {
    private const val LOG_FILE_NAME = "app_logs.txt"
    private const val STATE_PREFS_NAME = "app_logger_state"
    private const val MAX_LOG_LINES = 1500
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    @Volatile
    private var crashHandlerInstalled = false

    // ───────────────────────── Core write / read API ─────────────────────────

    private fun write(context: Context, level: LogLevel, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] [${level.label}] [$tag] $message\n"
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            FileWriter(file, true).use { writer ->
                writer.append(logEntry)
            }
            trimLogsIfNeeded(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Verbose / trace-level detail. */
    fun d(context: Context, tag: String, message: String) = write(context, LogLevel.DEBUG, tag, message)

    /** Normal, expected app behavior. */
    fun i(context: Context, tag: String, message: String) = write(context, LogLevel.INFO, tag, message)

    /** Something unexpected but non-fatal — e.g. battery optimization is on. */
    fun w(context: Context, tag: String, message: String) = write(context, LogLevel.WARN, tag, message)

    /** A failure — e.g. wallpaper couldn't be applied. */
    fun e(context: Context, tag: String, message: String) = write(context, LogLevel.ERROR, tag, message)

    fun getLogs(context: Context): List<String> {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                file.readLines().reversed()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /** Same as [getLogs] but parsed into structured entries (newest first). */
    fun getLogEntries(context: Context): List<LogEntry> = getLogs(context).map(::parseLine)

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun trimLogsIfNeeded(context: Context) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                val lines = file.readLines()
                if (lines.size > MAX_LOG_LINES) {
                    val trimmedLines = lines.drop(lines.size - MAX_LOG_LINES)
                    file.writeText(trimmedLines.joinToString("\n") + "\n")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Matches the current "[timestamp] [LEVEL] [tag] message" format.
    private val lineRegex = Regex("""^\[(.*?)]\s*\[(DEBUG|INFO|WARN|ERROR)]\s*\[(.*?)]\s*(.*)$""")

    // Falls back to the older "[timestamp] [tag] message" format so any
    // logs written before this format existed still display correctly.
    private val legacyRegex = Regex("""^\[(.*?)]\s*\[(.*?)]\s*(.*)$""")

    private fun parseLine(raw: String): LogEntry {
        lineRegex.find(raw)?.let { m ->
            val (ts, lvl, tag, msg) = m.destructured
            return LogEntry(ts, LogLevel.valueOf(lvl), tag, msg, raw)
        }
        legacyRegex.find(raw)?.let { m ->
            val (ts, tag, msg) = m.destructured
            val guessedLevel = when {
                msg.contains("error", true) -> LogLevel.ERROR
                msg.contains("ignored", true) -> LogLevel.WARN
                else -> LogLevel.INFO
            }
            return LogEntry(ts, guessedLevel, tag, msg, raw)
        }
        return LogEntry("", LogLevel.DEBUG, "?", raw, raw)
    }

    // ───────────────────── Process-death / crash detection ─────────────────────
    //
    // Strategy: we persist a "session_open" flag that is set to true as soon
    // as the app starts, and only cleared again when the app shuts down
    // *cleanly* (Activity.onDestroy() with isFinishing == true). If the
    // process is killed by the system in the background — or crashes — that
    // clearing step never runs, so the flag is still `true` the next time
    // the app starts. That's our signal that the previous session ended
    // abnormally.

    private fun statePrefs(context: Context) =
        context.getSharedPreferences(STATE_PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Call once from Activity.onCreate(). Detects whether the previous
     * session ended without a clean shutdown and logs a warning explaining
     * that scheduled wallpaper updates may have been missed as a result.
     * Also logs current battery-optimization status, since that's the most
     * common reason Android kills this kind of background work.
     */
    fun onAppStart(context: Context) {
        val prefs = statePrefs(context)
        val wasOpen = prefs.getBoolean("session_open", false)
        val crashed = prefs.getBoolean("session_crashed", false)
        val lastHeartbeat = prefs.getLong("last_heartbeat", 0L)

        if (wasOpen) {
            val elapsedStr = if (lastHeartbeat > 0L) {
                formatDuration(System.currentTimeMillis() - lastHeartbeat)
            } else {
                "an unknown amount of time"
            }
            if (crashed) {
                w(
                    context, "App",
                    "Previous session ended in a CRASH (see the error above from that session). " +
                            "Any wallpaper update scheduled during that time was likely skipped."
                )
            } else {
                w(
                    context, "App",
                    "⚠ App process was KILLED by the system while backgrounded — no clean shutdown occurred. " +
                            "Last seen active $elapsedStr ago. This is the most likely reason a scheduled wallpaper update didn't run."
                )
            }
        }

        prefs.edit()
            .putBoolean("session_open", true)
            .putBoolean("session_crashed", false)
            .putLong("last_heartbeat", System.currentTimeMillis())
            .apply()

        i(context, "App", "onCreate() called")
        logBatteryOptimizationStatus(context)
    }

    /** Call periodically while backgrounded (e.g. Activity.onStop()) so we know roughly how long ago the app was last alive. */
    fun heartbeat(context: Context) {
        statePrefs(context).edit().putLong("last_heartbeat", System.currentTimeMillis()).apply()
    }

    /** Call from Activity.onDestroy() when isFinishing == true, i.e. a real, user-initiated close. */
    fun onAppCleanExit(context: Context) {
        statePrefs(context).edit().putBoolean("session_open", false).apply()
        i(context, "App", "onDestroy() called (clean exit)")
    }

    /** Call from Activity.onDestroy() when isFinishing == false — a config-change recreation, not a real close. */
    fun onAppConfigChangeDestroy(context: Context) {
        d(context, "App", "onDestroy() called (recreating — config change)")
    }

    fun logBatteryOptimizationStatus(context: Context) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val exempt = pm.isIgnoringBatteryOptimizations(context.packageName)
            if (exempt) {
                i(context, "System", "Battery optimization: app is exempt (unrestricted) ✓")
            } else {
                w(
                    context, "System",
                    "Battery optimization is ENABLED for this app. Android is allowed to kill it in the " +
                            "background, which can silently stop scheduled wallpaper updates."
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Installs a global uncaught-exception handler that logs the crash
     * (so it's visible in the Developer Logs on next launch) before handing
     * off to whatever handler was previously installed, so normal Android
     * crash behavior (the "app has stopped" dialog, etc.) is unaffected.
     */
    fun installCrashHandler(context: Context) {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true

        val appContext = context.applicationContext
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                e(appContext, "Crash", "Uncaught exception on '${thread.name}': ${throwable.javaClass.simpleName}: ${throwable.message}")
                e(appContext, "Crash", throwable.stackTraceToString().take(2000))
                statePrefs(appContext).edit().putBoolean("session_crashed", true).apply()
            } catch (_: Exception) {
                // Never let logging itself interfere with normal crash handling.
            } finally {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }
}