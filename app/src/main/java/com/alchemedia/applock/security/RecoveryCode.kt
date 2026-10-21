package com.alchemedia.applock.security

import java.security.SecureRandom

/** One-time code, shown once at setup, that can reset a forgotten PIN or pattern. */
object RecoveryCode {
    // No 0/O/1/I so the code survives being copied by hand.
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val LENGTH = 12
    private const val GROUP = 4

    fun generate(random: SecureRandom = SecureRandom()): String =
        buildString {
            repeat(LENGTH) { i ->
                if (i > 0 && i % GROUP == 0) append('-')
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }

    /** Upper-cases and strips everything except letters and digits, so "abcd efgh" matches "ABCD-EFGH". */
    fun normalize(input: String): String = input.uppercase().filter { it.isLetterOrDigit() }
}
