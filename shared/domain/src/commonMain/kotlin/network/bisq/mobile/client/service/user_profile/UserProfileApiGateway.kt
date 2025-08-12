package network.bisq.mobile.client.service.user_profile

import io.ktor.http.encodeURLPath
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

class UserProfileApiGateway(
    private val webSocketApiClient: WebSocketApiClient
) {
    private val basePath = "user-identities"
    private val profileBasePath = "user-profiles"

    suspend fun ping(): Result<KeyMaterialResponse> {
        return webSocketApiClient.get("$basePath/ping")
    }

    suspend fun getKeyMaterial(): Result<KeyMaterialResponse> {
        return webSocketApiClient.get("$basePath/key-material")
    }

    suspend fun createAndPublishNewUserProfile(
        nickName: String, keyMaterialResponse: KeyMaterialResponse
    ): Result<CreateUserIdentityResponse> {
        val createUserIdentityRequest = CreateUserIdentityRequest(
            nickName, "", "", keyMaterialResponse
        )
        return webSocketApiClient.post(basePath, createUserIdentityRequest)
    }

    suspend fun updateUserProfile(statement: String, terms: String): Result<CreateUserIdentityResponse> {
        val request = UpdateUserIdentityRequest(statement, terms)
        return webSocketApiClient.patch(basePath, request)
    }

    suspend fun getUserIdentityIds(): Result<List<String>> {
        return webSocketApiClient.get("$basePath/ids")
    }

    suspend fun getSelectedUserProfile(): Result<UserProfileVO> {
        return webSocketApiClient.get("$basePath/selected/user-profile")
    }

    suspend fun findUserProfiles(ids: List<String>): Result<List<UserProfileVO>> {
        if (ids.isEmpty()) {
            return Result.success(emptyList())
        }
        return webSocketApiClient.get("$profileBasePath?ids=${ids.joinToString(",")}")
    }

    suspend fun getIgnoredUserIds(): Result<List<String>> {
        return webSocketApiClient.get("$profileBasePath/ignored")
    }

    suspend fun ignoreUser(userId: String): Result<Unit> {
        val request = IgnoreUserProfileRequest(userId)
        return webSocketApiClient.post("$profileBasePath/ignore", request)
    }

    suspend fun undoIgnoreUser(userId: String): Result<Unit> {
        return webSocketApiClient.delete("$profileBasePath/ignore/${userId.encodeURLPath()}")
    }
}