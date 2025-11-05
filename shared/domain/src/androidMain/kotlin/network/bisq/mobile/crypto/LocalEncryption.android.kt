package network.bisq.mobile.crypto

import android.os.Build

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.InvalidKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec

private object LocalEncryption {
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128

    // Backward-compat (pre‑GCM): AES/CBC/PKCS7
    private const val CBC_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val CBC_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val CBC_IV_LENGTH_BYTES = 16

    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODE/$PADDING"
    private const val CBC_TRANSFORMATION = "$ALGORITHM/$CBC_BLOCK_MODE/$CBC_PADDING"

    private fun newCipher(): Cipher = Cipher.getInstance(TRANSFORMATION)
    private fun newCipherCbc(): Cipher = Cipher.getInstance(CBC_TRANSFORMATION)
    private val keyStore = KeyStore
        .getInstance("AndroidKeyStore")
        .apply {
            load(null)
        }

    private fun getKey(keyAlias: String): SecretKey {
        val existingKey = keyStore
            .getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey(keyAlias)
    }

    private fun createKey(keyAlias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(BLOCK_MODE)
            .setEncryptionPaddings(PADDING)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                builder.setIsStrongBoxBacked(true)
            } catch (_: Throwable) {
                // ignore if not supported
            }
        }
        val parameterSpec = builder.build()

        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    fun encrypt(bytes: ByteArray, keyAlias: String): ByteArray {
        val key = getKey(keyAlias)
        return try {
            val cipher = newCipher()
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(bytes)
            iv + encrypted
        } catch (e: InvalidKeyException) {
            // Backward-compat: if existing keystore key only allows CBC
            if (e.message?.contains("Incompatible block mode", ignoreCase = true) == true) {
                val cipher = newCipherCbc()
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val iv = cipher.iv // 16 bytes for CBC
                val encrypted = cipher.doFinal(bytes)
                iv + encrypted
            } else {
                throw e
            }
        }
    }

    fun decrypt(bytes: ByteArray, keyAlias: String): ByteArray {
        require(bytes.isNotEmpty()) { "Invalid encrypted data: empty" }
        val key = getKey(keyAlias)
        // Try GCM first
        runCatching {
            require(bytes.size > IV_LENGTH_BYTES) { "Invalid encrypted data: too short for GCM" }
            val iv = bytes.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertextAndTag = bytes.copyOfRange(IV_LENGTH_BYTES, bytes.size)
            val cipher = newCipher()
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            return cipher.doFinal(ciphertextAndTag)
        }.onFailure { e ->
            // If the existing key does not allow GCM, fall back to legacy CBC
            val incompatible = (e as? InvalidKeyException)?.message?.contains("Incompatible block mode", ignoreCase = true) == true
            if (!incompatible) {
                // Not an incompatible mode error; continue to try CBC only if the shape fits (16B IV)
                if (bytes.size <= CBC_IV_LENGTH_BYTES) throw e
            }
        }
        // Legacy CBC fallback
        require(bytes.size > CBC_IV_LENGTH_BYTES) { "Invalid encrypted data: too short for CBC" }
        val cbcIv = bytes.copyOfRange(0, CBC_IV_LENGTH_BYTES)
        val cbcCiphertext = bytes.copyOfRange(CBC_IV_LENGTH_BYTES, bytes.size)
        val cbc = newCipherCbc()
        cbc.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(cbcIv))
        return cbc.doFinal(cbcCiphertext)
    }
}

actual fun encrypt(data: ByteArray, keyAlias: String): ByteArray {
    return LocalEncryption.encrypt(data, keyAlias)
}

actual fun decrypt(data: ByteArray, keyAlias: String): ByteArray {
    return LocalEncryption.decrypt(data, keyAlias)
}