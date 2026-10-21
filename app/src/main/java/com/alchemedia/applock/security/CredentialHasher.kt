package com.alchemedia.applock.security

import java.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 hashing for the PIN / pattern. The secret itself is never stored. */
object CredentialHasher {
    private const val ITERATIONS = 150_000
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(secret: String, saltBase64: String?, hashBase64: String?): Boolean {
        if (saltBase64 == null || hashBase64 == null) return false
        val expected = decode(hashBase64)
        val actual = hash(secret, decode(saltBase64))
        return MessageDigest.isEqual(expected, actual)
    }

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}
