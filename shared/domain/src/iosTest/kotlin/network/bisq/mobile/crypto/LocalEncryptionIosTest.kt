package network.bisq.mobile.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class LocalEncryptionIosTest {

    @Test
    fun round_trip_encrypt_decrypt_default_alias() {
        val plaintext = "KMP iOS AES-GCM".encodeToByteArray()
        val ciphertext = encrypt(plaintext)
        val decrypted = decrypt(ciphertext)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun tamper_detection_fails_on_modified_bytes() {
        val plaintext = "tamper".encodeToByteArray()
        val ciphertext = encrypt(plaintext)
        // flip last byte (likely within tag)
        val tampered = ciphertext.copyOf()
        tampered[tampered.lastIndex] = (tampered.last() xor 0x01)
        assertFailsWith<Throwable> { decrypt(tampered) }
    }

    @Test
    fun invalid_input_too_short() {
        assertFailsWith<IllegalArgumentException> { decrypt(ByteArray(8)) }
    }


    @Test
    fun legacy_format_fallback_decrypts() {
        val plaintext = "legacy path".encodeToByteArray()
        val legacy = legacyCbcEncrypt(plaintext)
        val decrypted = decrypt(legacy)
        assertContentEquals(plaintext, decrypted)
    }

    private infix fun Byte.xor(other: Int): Byte = (this.toInt() xor other).toByte()
}

