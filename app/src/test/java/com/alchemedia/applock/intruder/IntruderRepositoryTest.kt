package com.alchemedia.applock.intruder

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.Proxy

class IntruderRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day

    /** Minimal Context double: the repository only asks for filesDir. */
    private fun repo(): Pair<IntruderRepository, File> {
        val files = tmp.newFolder("files")
        val context = Proxy.newProxyInstance(
            Context::class.java.classLoader,
            arrayOf(Context::class.java),
        ) { _, method, _ -> if (method.name == "getFilesDir") files else null } as Context
        return IntruderRepository(context) to File(files, "intruders")
    }

    private fun photo(dir: File, ageDays: Long, pkg: String = "com.bank") =
        File(dir, "${now - ageDays * day}-$pkg.jpg").also { it.writeText("x") }

    @Test
    fun purgeRemovesOnlyPhotosOlderThanCutoff() {
        val (repo, dir) = repo()
        val old = photo(dir, 40)
        val recent = photo(dir, 5)
        repo.refresh()
        assertEquals(1, repo.purgeOlderThan(30, now))
        assertFalse(old.exists())
        assertTrue(recent.exists())
        assertEquals(1, repo.shots.value.size)
    }

    @Test
    fun zeroDaysKeepsEverything() {
        val (repo, dir) = repo()
        photo(dir, 400)
        repo.refresh()
        assertEquals(0, repo.purgeOlderThan(0, now))
        assertEquals(1, repo.shots.value.size)
    }

    @Test
    fun metadataIsParsedFromTheFileName() {
        val (repo, dir) = repo()
        photo(dir, 2, "com.example.chat")
        File(dir, "${now}-applock.jpg").writeText("x")
        repo.refresh()
        val shots = repo.shots.value
        assertEquals(2, shots.size)
        assertEquals(now, shots.first().timestamp)
        assertEquals(null, shots.first().packageName)
        assertEquals("com.example.chat", shots.last().packageName)
    }
}
