package network.bisq.mobile.client.service.user_profile

import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.replicated.user.profile.id
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.hexToByteArray
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ClientUserProfileServiceFacade(
    private val apiGateway: UserProfileApiGateway,
    private val clientCatHashService: ClientCatHashService<PlatformImage>
) : UserProfileServiceFacade, Logging {

    private var keyMaterialResponse: KeyMaterialResponse? = null

    // API
    override suspend fun hasUserProfile(): Boolean {
        return getUserIdentityIds().isNotEmpty()
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun generateKeyPair(result: (String, String, PlatformImage?) -> Unit) {
        val ts = Clock.System.now().toEpochMilliseconds()
        val apiResult = apiGateway.getKeyMaterial()
        if (apiResult.isFailure) {
            throw apiResult.exceptionOrNull()!!
        }

        val preparedData = apiResult.getOrThrow()
        createSimulatedDelay(Clock.System.now().toEpochMilliseconds() - ts)
        val pubKeyHash: ByteArray = preparedData.id.hexToByteArray()
        val powSolutionBase64 = preparedData.proofOfWork.solution
        val image: PlatformImage? = clientCatHashService.getImage(
            pubKeyHash,
            powSolutionBase64.decodeBase64Bytes(),
            0,
            120
        )

        result(preparedData.id, preparedData.nym, image)
        this.keyMaterialResponse = preparedData
    }

    override suspend fun createAndPublishNewUserProfile(nickName: String) {
        if (keyMaterialResponse == null) {
            return
        }
        val apiResult = apiGateway.createAndPublishNewUserProfile(nickName, keyMaterialResponse!!)
        if (apiResult.isFailure) {
            throw apiResult.exceptionOrNull()!!
        }

        val response: CreateUserIdentityResponse = apiResult.getOrThrow()
        this.keyMaterialResponse = null
        log.i { "Call to createAndPublishNewUserProfile successful. userProfileId = ${response.userProfileId}" }
    }

    override suspend fun getUserIdentityIds(): List<String> {
        val apiResult = apiGateway.getUserIdentityIds()
        if (apiResult.isFailure) {
            throw apiResult.exceptionOrNull()!!
        }

        return apiResult.getOrThrow()
    }

    override suspend fun applySelectedUserProfile(): Triple<String?, String?, String?> {
        val userProfile = getSelectedUserProfile()
        return Triple(userProfile.nickName, userProfile.nym, userProfile.id)
    }

    override suspend fun getSelectedUserProfile(): UserProfileVO {
        val apiResult = apiGateway.getSelectedUserProfile()
        if (apiResult.isFailure) {
            throw apiResult.exceptionOrNull()!!
        }
        return apiResult.getOrThrow()
    }

    // Private
    private suspend fun createSimulatedDelay(requestDuration: Long) {
        // Proof of work creation for difficulty 65536 takes about 50 ms to 100 ms on a 4 GHz Intel Core i7.
        // The API request is likely also quite fast
        // We design a delay of 200 - 1000 ms taking into account a random value and the requestDuration.
        // The delay should avoid a too fast flicker-effect in the UI when recreating the nym,
        // and should make the usage of the proof of work more visible.
        val random: Int = Random.nextInt(800)
        val delayDuration = min(1000.0, max(200.0, (200 + random - requestDuration).toDouble()))
            .toLong()
        delay(delayDuration)
    }
}