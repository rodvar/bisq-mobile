package network.bisq.mobile.crypto

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import network.bisq.mobile.ios.cfDictionaryOf
import network.bisq.mobile.ios.toByteArray
import platform.CoreCrypto.CCOptions
import network.bisq.mobile.ios.toNSData

import platform.CoreCrypto.CCOperation
import platform.CoreCrypto.CCStatus
import platform.CoreCrypto.CCCrypt
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCBlockSizeAES128
import platform.CoreCrypto.kCCDecrypt
import platform.CoreCrypto.kCCEncrypt
import platform.CoreCrypto.kCCKeySizeAES256
import platform.CoreCrypto.kCCOptionPKCS7Padding
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSMutableData
import platform.Foundation.dataWithLength
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrLabel
import platform.Security.kSecAttrService
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecRandomDefault
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecValueData

private val inMemoryKeys = mutableMapOf<String, ByteArray>()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private object LocalEncryption {
    private const val KEY_SIZE = 32 // 256 bits
    private const val IV_LENGTH_BYTES = 16 // CBC block size
    private const val TAG_LENGTH_BYTES = 32 // HMAC-SHA256 tag
    private const val SERVICE_NAME = "network.bisq.mobile"

    private fun generateAndStoreSymmetricKey(keyAlias: String) {
        val keyBytes = secureRandomBytes(KEY_SIZE)
        val keyData = keyBytes.toNSData()
        memScoped {
            val status = SecItemAdd(
                cfDictionaryOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrLabel to keyAlias,
                    kSecAttrAccount to "Account $keyAlias",
                    kSecAttrService to "Service $SERVICE_NAME",
                    kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlock,
                    kSecValueData to keyData
                ),
                null,
            )
            if (status != errSecSuccess && status != errSecDuplicateItem) {
                // Keychain not available (simulator tests or restricted context). Fallback to in-memory key for this process.
                inMemoryKeys[keyAlias] = keyBytes
                return
            }
        }
    }

    private fun retrieveSymmetricKey(keyAlias: String): ByteArray? {
        // In-memory fallback for simulator tests or when Keychain is unavailable
        inMemoryKeys[keyAlias]?.let { return it }
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val keyBytes = when (val status = SecItemCopyMatching(
                cfDictionaryOf(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrAccount to "Account $keyAlias",
                    kSecAttrService to "Service $SERVICE_NAME",
                    kSecReturnData to kCFBooleanTrue,
                    kSecMatchLimit to kSecMatchLimitOne
                ),
                result.ptr,
            )) {
                errSecSuccess -> (CFBridgingRelease(result.value) as? NSData)?.toByteArray()
                errSecItemNotFound -> null
                else -> null // Treat non-success as 'not found' to allow create-then-read flow
            }
            return keyBytes ?: inMemoryKeys[keyAlias]
        }
    }

    private fun getOrCreateKey(keyAlias: String): ByteArray {
        return retrieveSymmetricKey(keyAlias) ?: run {
            generateAndStoreSymmetricKey(keyAlias)
            retrieveSymmetricKey(keyAlias)
                ?: throw IllegalStateException("Failed to retrieve generated key")
        }
    }

    // Derive independent enc/mac subkeys via HMAC(key, label)
    private fun deriveKeys(master: ByteArray): Pair<ByteArray, ByteArray> {
        val enc = hmacSha256(master, "enc".encodeToByteArray())
        val mac = hmacSha256(master, "mac".encodeToByteArray())
        return enc to mac
    }

    fun encrypt(bytes: ByteArray, keyAlias: String): ByteArray {
        val keyData = getOrCreateKey(keyAlias)
        val (encKey, macKey) = deriveKeys(keyData)

        // Random 16-byte IV for CBC
        val iv = secureRandomBytes(IV_LENGTH_BYTES)

        // Encrypt using AES-256-CBC + PKCS7
        val out = ByteArray(bytes.size + kCCBlockSizeAES128.toInt())
        val outLen: Int
        val status: CCStatus = memScoped {
            val moved = alloc<kotlinx.cinterop.ULongVar>()
            val ccStatus = encKey.usePinned { keyPin ->
                iv.usePinned { ivPin ->
                    bytes.usePinned { inPin ->
                        out.usePinned { outPin ->
                            CCCrypt(
                                kCCEncrypt as CCOperation,
                                kCCAlgorithmAES,
                                kCCOptionPKCS7Padding as CCOptions,
                                keyPin.addressOf(0), kCCKeySizeAES256.toULong(),
                                ivPin.addressOf(0),
                                inPin.addressOf(0), bytes.size.toULong(),
                                outPin.addressOf(0), out.size.toULong(),
                                moved.ptr
                            )
                        }
                    }
                }
            }
            outLen = moved.value.toInt()
            ccStatus
        }
        if (status.toInt() != 0) error("CCCrypt encrypt failed: $status")
        val ciphertext = out.copyOf(outLen)

        val tag = hmacSha256(macKey, iv + ciphertext)
        return iv + ciphertext + tag
    }

    fun decrypt(bytes: ByteArray, keyAlias: String): ByteArray {
        // Accept legacy shape (IV + ciphertext) or new shape (IV + ciphertext + tag)
        if (bytes.size <= IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Invalid encrypted data: too short")
        }
        val keyData = getOrCreateKey(keyAlias)
        val (encKey, macKey) = deriveKeys(keyData)

        // If it cannot possibly contain a tag, treat as legacy CBC (no MAC)
        if (bytes.size < IV_LENGTH_BYTES + TAG_LENGTH_BYTES + 1) {
            val ivLegacy = bytes.copyOfRange(0, IV_LENGTH_BYTES)
            val ctLegacy = bytes.copyOfRange(IV_LENGTH_BYTES, bytes.size)
            return decryptLegacyCbc(keyData, ivLegacy, ctLegacy)
        }

        val iv = bytes.copyOfRange(0, IV_LENGTH_BYTES)
        val tag = bytes.copyOfRange(bytes.size - TAG_LENGTH_BYTES, bytes.size)
        val ciphertext = bytes.copyOfRange(IV_LENGTH_BYTES, bytes.size - TAG_LENGTH_BYTES)

        val expected = hmacSha256(macKey, iv + ciphertext)
        if (!expected.contentEquals(tag)) {
            // Fallback to legacy CBC if MAC fails (payload from old app)
            val ctLegacy = bytes.copyOfRange(IV_LENGTH_BYTES, bytes.size)
            return decryptLegacyCbc(keyData, iv, ctLegacy)
        }

        // Decrypt (new format)
        val out = ByteArray(ciphertext.size + kCCBlockSizeAES128.toInt())
        val outLen: Int
        val status: CCStatus = memScoped {
            val moved = alloc<kotlinx.cinterop.ULongVar>()
            val ccStatus = encKey.usePinned { keyPin ->
                iv.usePinned { ivPin ->
                    ciphertext.usePinned { inPin ->
                        out.usePinned { outPin ->
                            CCCrypt(
                                kCCDecrypt as CCOperation,
                                kCCAlgorithmAES,
                                kCCOptionPKCS7Padding as CCOptions,
                                keyPin.addressOf(0), kCCKeySizeAES256.toULong(),
                                ivPin.addressOf(0),
                                inPin.addressOf(0), ciphertext.size.toULong(),
                                outPin.addressOf(0), out.size.toULong(),
                                moved.ptr
                            )
                        }
                    }
                }
            }
            outLen = moved.value.toInt()
            ccStatus
        }
        if (status.toInt() != 0) error("CCCrypt decrypt failed: $status")
        return out.copyOf(outLen)
    }

        private fun decryptLegacyCbc(masterKey: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
            val out = ByteArray(ciphertext.size + kCCBlockSizeAES128.toInt())
            val outLen: Int
            val status: CCStatus = memScoped {
                val moved = alloc<kotlinx.cinterop.ULongVar>()
                val ccStatus = masterKey.usePinned { keyPin ->
                    iv.usePinned { ivPin ->
                        ciphertext.usePinned { inPin ->
                            out.usePinned { outPin ->
                                CCCrypt(
                                    kCCDecrypt as CCOperation,
                                    kCCAlgorithmAES,
                                    kCCOptionPKCS7Padding as CCOptions,
                                    keyPin.addressOf(0), kCCKeySizeAES256.toULong(),
                                    ivPin.addressOf(0),
                                    inPin.addressOf(0), ciphertext.size.toULong(),
                                    outPin.addressOf(0), out.size.toULong(),
                                    moved.ptr
                                )
                            }
                        }
                    }
                }
                outLen = moved.value.toInt()
                ccStatus
            }
            if (status.toInt() != 0) error("CCCrypt legacy decrypt failed: $status")
            return out.copyOf(outLen)
        }

        // Visible for tests: construct a legacy-format payload (IV + ciphertext) using the master key directly
        fun legacyCbcEncryptForTests(bytes: ByteArray, keyAlias: String): ByteArray {
            val masterKey = getOrCreateKey(keyAlias)
            val iv = secureRandomBytes(IV_LENGTH_BYTES)
            val out = ByteArray(bytes.size + kCCBlockSizeAES128.toInt())
            val outLen: Int
            val status: CCStatus = memScoped {
                val moved = alloc<kotlinx.cinterop.ULongVar>()
                val ccStatus = masterKey.usePinned { keyPin ->
                    iv.usePinned { ivPin ->
                        bytes.usePinned { inPin ->
                            out.usePinned { outPin ->
                                CCCrypt(
                                    kCCEncrypt as CCOperation,
                                    kCCAlgorithmAES,
                                    kCCOptionPKCS7Padding as CCOptions,
                                    keyPin.addressOf(0), kCCKeySizeAES256.toULong(),
                                    ivPin.addressOf(0),
                                    inPin.addressOf(0), bytes.size.toULong(),
                                    outPin.addressOf(0), out.size.toULong(),
                                    moved.ptr
                                )
                            }
                        }
                    }
                }
                outLen = moved.value.toInt()
                ccStatus
            }
            if (status.toInt() != 0) error("CCCrypt legacy encrypt failed: $status")
            val ciphertext = out.copyOf(outLen)
            return iv + ciphertext
        }

}

// Visible for tests (iosTest): construct legacy CBC payload with the default alias
internal fun legacyCbcEncrypt(data: ByteArray, keyAlias: String = "network.bisq.mobile"): ByteArray =
    LocalEncryption.legacyCbcEncryptForTests(data, keyAlias)


actual fun encrypt(data: ByteArray, keyAlias: String): ByteArray = LocalEncryption.encrypt(data, keyAlias)
actual fun decrypt(data: ByteArray, keyAlias: String): ByteArray = LocalEncryption.decrypt(data, keyAlias)
