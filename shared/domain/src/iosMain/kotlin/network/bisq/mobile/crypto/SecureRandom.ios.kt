package network.bisq.mobile.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
actual fun secureRandomBytes(length: Int): ByteArray {
    require(length >= 0) { "length must be >= 0" }
    if (length == 0) return ByteArray(0)
    val out = ByteArray(length)
    val status = out.usePinned { pinned ->
        SecRandomCopyBytes(kSecRandomDefault, length.toULong(), pinned.addressOf(0))
    }
    if (status != errSecSuccess) {
        throw IllegalStateException("SecRandomCopyBytes failed: status=$status")
    }
    return out
}
