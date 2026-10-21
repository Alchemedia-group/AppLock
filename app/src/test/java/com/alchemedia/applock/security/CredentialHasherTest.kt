package com.alchemedia.applock.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialHasherTest {
    private val salt = CredentialHasher.newSalt()
    private val saltB64 = CredentialHasher.encode(salt)

    @Test
    fun correctSecretVerifies() {
        val hash = CredentialHasher.encode(CredentialHasher.hash("1234", salt))
        assertTrue(CredentialHasher.verify("1234", saltB64, hash))
    }

    @Test
    fun wrongSecretFails() {
        val hash = CredentialHasher.encode(CredentialHasher.hash("1234", salt))
        assertFalse(CredentialHasher.verify("1235", saltB64, hash))
        assertFalse(CredentialHasher.verify("", saltB64, hash))
    }

    @Test
    fun missingCredentialNeverVerifies() {
        assertFalse(CredentialHasher.verify("1234", null, null))
        assertFalse(CredentialHasher.verify("1234", saltB64, null))
    }

    @Test
    fun saltsAreRandomAndChangeTheHash() {
        assertNotEquals(saltB64, CredentialHasher.encode(CredentialHasher.newSalt()))
        val other = CredentialHasher.newSalt()
        assertNotEquals(
            CredentialHasher.encode(CredentialHasher.hash("1234", salt)),
            CredentialHasher.encode(CredentialHasher.hash("1234", other)),
        )
    }

    @Test
    fun hashIs256Bits() {
        assertEquals(32, CredentialHasher.hash("0258", salt).size)
    }
}
