package network.bisq.mobile.client.httpclient

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import network.bisq.mobile.crypto.hmacSha256
import network.bisq.mobile.crypto.getSha256
import network.bisq.mobile.domain.utils.toHex

class AuthUtilsTest {

    @Test
    fun normalized_path_and_query_trims_trailing_slash_except_root() {
        assertEquals("/api/v1/orders", AuthUtils.getNormalizedPathAndQuery(Url("https://host/api/v1/orders/")))
        assertEquals("/", AuthUtils.getNormalizedPathAndQuery(Url("https://host/")))
        assertEquals("/api?x=1&y=2", AuthUtils.getNormalizedPathAndQuery(Url("https://host/api?x=1&y=2")))
    }

    @Test
    fun generate_auth_hash_matches_manual_hmac_over_canonical() {
        val password = "secret"
        val nonce = "00112233445566778899aabbccddeeff"
        val timestamp = "1700000000000"
        val method = "post"
        val normalizedPath = "/v1/order"
        val body = "hello".encodeToByteArray()
        val bodySha256Hex = getSha256(body).toHex()

        val canonical = "$nonce\n${timestamp}\n${method.uppercase()}\n$normalizedPath\n$bodySha256Hex"
        val expected = hmacSha256(password.encodeToByteArray(), canonical.encodeToByteArray()).toHex()

        assertEquals(expected, AuthUtils.generateAuthHash(password, nonce, timestamp, method, normalizedPath, bodySha256Hex))
    }

    @Test
    fun generate_nonce_length_and_hex_format() {
        val nonce = AuthUtils.generateNonce(16)
        assertEquals(32, nonce.length) // hex of 16 bytes
    }
}

