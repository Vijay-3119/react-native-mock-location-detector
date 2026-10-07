package com.mocklocationdetector

import android.content.Context
import android.os.Build
import java.io.File

object RootChecker {

    private val SU_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/data/adb/su"
    )

    private val KERNEL_SU_PATHS = arrayOf(
        "/data/adb/ksu",
        "/data/adb/ksud"
    )

    private val APATCH_PATHS = arrayOf(
        "/data/adb/apatch",
        "/data/adb/apatchd"
    )

    fun isRooted(context: Context): Boolean {
        return checkBuildTags() || checkSuFiles() || checkKernelSU() || checkAPatch() || checkWhichSu()
    }

    private fun checkBuildTags(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkSuFiles(): Boolean {
        for (path in SU_PATHS) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    private fun checkKernelSU(): Boolean {
        for (path in KERNEL_SU_PATHS) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    private fun checkAPatch(): Boolean {
        for (path in APATCH_PATHS) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    private fun checkWhichSu(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val reader = process.inputStream.bufferedReader()
            reader.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }
}
