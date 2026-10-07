package com.mocklocationdetector

import android.content.Context
import android.content.pm.PackageManager
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

object MagiskChecker {

    private val MAGISK_FILES = arrayOf(
        "/data/adb/magisk",
        "/data/adb/magisk.db",
        "/data/adb/magisk_simple"
    )

    private val LSPOSED_FILES = arrayOf(
        "/data/adb/lspd",
        "/data/adb/modules/lsposed",
        "/data/adb/modules/riru_lsposed"
    )

    private val KNOWN_PACKAGES = arrayOf(
        "com.topjohnwu.magisk",
        "de.robv.android.xposed.installer",
        "org.lsposed.manager"
    )

    fun isMagiskPresent(context: Context): Boolean {
        return checkMagiskFiles() || checkMounts() || checkPackages(context)
    }

    fun isXposedOrLSPosedPresent(context: Context): Boolean {
        return checkLSPosedFiles() || checkXposedStackTrace()
    }

    private fun checkMagiskFiles(): Boolean {
        for (path in MAGISK_FILES) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    private fun checkLSPosedFiles(): Boolean {
        for (path in LSPOSED_FILES) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    private fun checkMounts(): Boolean {
        val mountsFile = File("/proc/self/mounts")
        if (!mountsFile.exists() || !mountsFile.canRead()) return false

        try {
            BufferedReader(FileReader(mountsFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    if (currentLine.contains("magisk") ||
                        currentLine.contains("core/mirror") ||
                        currentLine.contains("core/img")
                    ) {
                        return true
                    }
                }
            }
        } catch (ignored: Throwable) {
        }
        return false
    }

    private fun checkPackages(context: Context): Boolean {
        val pm = context.packageManager
        for (pkg in KNOWN_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Package not present
            }
        }
        return false
    }

    private fun checkXposedStackTrace(): Boolean {
        try {
            throw Exception("XposedCheck")
        } catch (e: Exception) {
            for (element in e.stackTrace) {
                val className = element.className
                if (className.contains("de.robv.android.xposed") ||
                    className.contains("org.lsposed") ||
                    className.contains("EdXposed")
                ) {
                    return true
                }
            }
        }
        return false
    }
}
