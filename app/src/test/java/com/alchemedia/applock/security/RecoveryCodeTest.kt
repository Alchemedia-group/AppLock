package com.alchemedia.applock.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeTest {
    @Test
    fun codeHasThreeGroupsOfFour() {
        val code = RecoveryCode.generate()
        assertTrue(code, Regex("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}").matches(code))
    }

    @Test
    fun codeAvoidsLookAlikeCharacters() {
        repeat(200) {
            val code = RecoveryCode.generate()
            assertTrue(code, code.none { it in "01OI" })
        }
    }

    @Test
    fun codesDiffer() {
        assertNotEquals(RecoveryCode.generate(), RecoveryCode.generate())
    }

    @Test
    fun normalizeIgnoresCaseSpacesAndDashes() {
        assertEquals("ABCD2345WXYZ", RecoveryCode.normalize("abcd-2345 wxyz"))
        assertEquals(RecoveryCode.normalize("ABCD-2345-WXYZ"), RecoveryCode.normalize(" abcd 2345 wxyz "))
    }

    @Test
    fun generatedCodeSurvivesHashRoundTripThroughNormalize() {
        val code = RecoveryCode.generate()
        val salt = CredentialHasher.newSalt()
        val hash = CredentialHasher.encode(CredentialHasher.hash(RecoveryCode.normalize(code), salt))
        val typedByHand = code.lowercase().replace("-", " ")
        assertTrue(CredentialHasher.verify(RecoveryCode.normalize(typedByHand), CredentialHasher.encode(salt), hash))
    }
}
