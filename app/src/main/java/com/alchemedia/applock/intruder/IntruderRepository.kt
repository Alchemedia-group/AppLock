package com.alchemedia.applock.intruder

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

data class IntruderShot(val file: File, val timestamp: Long, val packageName: String?)

/**
 * Intruder photos live in app-private storage. The file name carries the metadata
 * (`<epochMillis>-<package>.jpg`) so no separate index can get out of sync.
 */
class IntruderRepository(private val dir: File) {
    constructor(context: Context) : this(File(context.filesDir, "intruders"))

    init {
        dir.mkdirs()
    }

    private val _shots = MutableStateFlow(readShots())
    val shots: StateFlow<List<IntruderShot>> = _shots

    fun newFile(packageName: String?): File =
        File(dir, "${System.currentTimeMillis()}-${packageName ?: NO_PACKAGE}.jpg")

    fun refresh() {
        _shots.value = readShots()
    }

    fun delete(shot: IntruderShot) {
        shot.file.delete()
        refresh()
    }

    /** Deletes photos older than [days] days (no-op for 0). Returns how many were removed. */
    fun purgeOlderThan(days: Int, now: Long = System.currentTimeMillis()): Int {
        if (days <= 0) return 0
        val cutoff = now - days * DAY_MS
        val old = readShots().filter { it.timestamp < cutoff }
        old.forEach { it.file.delete() }
        if (old.isNotEmpty()) refresh()
        return old.size
    }

    fun deleteAll() {
        dir.listFiles()?.forEach { it.delete() }
        refresh()
    }

    private fun readShots(): List<IntruderShot> =
        dir.listFiles { f -> f.extension == "jpg" }
            .orEmpty()
            .mapNotNull { file ->
                val name = file.nameWithoutExtension
                val timestamp = name.substringBefore('-').toLongOrNull() ?: return@mapNotNull null
                val pkg = name.substringAfter('-', NO_PACKAGE).takeUnless { it == NO_PACKAGE }
                IntruderShot(file, timestamp, pkg)
            }
            .sortedByDescending { it.timestamp }

    private companion object {
        const val NO_PACKAGE = "applock"
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
