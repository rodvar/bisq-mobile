package network.bisq.mobile.crypto

import java.security.SecureRandom

actual fun secureRandomBytes(length: Int): ByteArray {
    require(length >= 0) { "length must be >= 0" }
    val bytes = ByteArray(length)
    SecureRandom().nextBytes(bytes)
    return bytes
}

