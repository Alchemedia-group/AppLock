package com.alchemedia.applock.intruder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class IntruderRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val day = 24L * 60 * 60 * 1000
    private val now = 1000 * day

    private fun repo(): Pair<IntruderRepository, File> {
        val dir = File(tmp.newFolder("files"), "intruders")
        return IntruderRepository(dir) to dir
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
