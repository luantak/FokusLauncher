package com.lu4p.fokuslauncher.utils

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Builds a plain-text diagnostic file (device / app metadata + this process's logcat buffer)
 * for user-initiated export from Settings.
 */
object AppDiagnosticLogExporter {

    private const val LOG_LINES = "12000"

    fun writeExportFile(context: Context): File {
        val dir = File(context.cacheDir, "log_export").apply { mkdirs() }
        val outFile = File(dir, "fokus-launcher-diagnostic.txt")
        val header = buildHeader(context)
        val logs = captureLogcatForCurrentProcess()
        outFile.writeText(header + logs)
        return outFile
    }

    private fun buildHeader(context: Context): String {
        val pm = context.packageManager
        val pkg = context.packageName
        val pInfo = pm.getPackageInfo(pkg, 0)
        val versionName = pInfo.versionName ?: "?"
        val versionCode = PackageInfoCompat.getLongVersionCode(pInfo)
        return buildString {
            appendLine("Fokus Launcher — diagnostic export")
            appendLine("Generated: ${OffsetDateTime.now(ZoneId.systemDefault())}")
            appendLine("Package: $pkg")
            appendLine("Version: $versionName ($versionCode)")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("SDK: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Process ID: ${android.os.Process.myPid()}")
            appendLine("=".repeat(60))
            appendLine()
        }
    }

    private fun captureLogcatForCurrentProcess(): String {
        val pid = android.os.Process.myPid().toString()
        return runCatching {
            val process =
                    ProcessBuilder(
                                    "logcat",
                                    "-d",
                                    "-v",
                                    "threadtime",
                                    "--pid",
                                    pid,
                                    "-t",
                                    LOG_LINES
                            )
                            .redirectErrorStream(true)
                            .start()
            val text =
                    process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val exit = process.waitFor()
            if (exit != 0 && text.isBlank()) {
                "(logcat exited with code $exit and no output)\n"
            } else {
                text.ifBlank { "(No log lines returned for this process.)\n" }
            }
        }
                .getOrElse { e ->
                    "(Log capture failed: ${e.javaClass.simpleName}: ${e.message})\n"
                }
    }
}
