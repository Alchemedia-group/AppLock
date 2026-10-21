package com.alchemedia.applock.security

import com.alchemedia.applock.data.RelockMode
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UnlockSessionTest {
    private var now = 1_000L

    @Before
    fun setUp() {
        UnlockSession.clear()
        UnlockSession.clock = { now }
    }

    @After
    fun tearDown() = UnlockSession.clear()

    @Test
    fun nothingIsUnlockedInitially() {
        assertFalse(UnlockSession.isUnlocked("a", RelockMode.IMMEDIATE))
    }

    @Test
    fun immediateModeStaysUnlockedUntilPruned() {
        UnlockSession.markUnlocked("a")
        now += 10 * 60_000
        assertTrue(UnlockSession.isUnlocked("a", RelockMode.IMMEDIATE))
        UnlockSession.retainOnly("b")
        assertFalse(UnlockSession.isUnlocked("a", RelockMode.IMMEDIATE))
    }

    @Test
    fun retainOnlyKeepsTheForegroundApp() {
        UnlockSession.markUnlocked("a")
        UnlockSession.markUnlocked("b")
        UnlockSession.retainOnly("a")
        assertTrue(UnlockSession.isUnlocked("a", RelockMode.IMMEDIATE))
        assertFalse(UnlockSession.isUnlocked("b", RelockMode.IMMEDIATE))
    }

    @Test
    fun oneMinuteGraceExpires() {
        UnlockSession.markUnlocked("a")
        now += 59_999
        assertTrue(UnlockSession.isUnlocked("a", RelockMode.ONE_MINUTE))
        now += 2
        assertFalse(UnlockSession.isUnlocked("a", RelockMode.ONE_MINUTE))
    }

    @Test
    fun fiveMinuteGraceExpires() {
        UnlockSession.markUnlocked("a")
        now += 299_999
        assertTrue(UnlockSession.isUnlocked("a", RelockMode.FIVE_MINUTES))
        now += 2
        assertFalse(UnlockSession.isUnlocked("a", RelockMode.FIVE_MINUTES))
    }

    @Test
    fun screenOffModeIgnoresTimeButClearRelocks() {
        UnlockSession.markUnlocked("a")
        now += 3_600_000
        assertTrue(UnlockSession.isUnlocked("a", RelockMode.SCREEN_OFF))
        UnlockSession.clear()
        assertFalse(UnlockSession.isUnlocked("a", RelockMode.SCREEN_OFF))
    }
}
