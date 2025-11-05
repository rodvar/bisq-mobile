package network.bisq.mobile.crypto

/**
 * Returns cryptographically secure random bytes using the platform CSPRNG.
 */
expect fun secureRandomBytes(length: Int): ByteArray

