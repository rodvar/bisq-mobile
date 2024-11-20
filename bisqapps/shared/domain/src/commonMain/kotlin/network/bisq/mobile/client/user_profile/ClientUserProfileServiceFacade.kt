package network.bisq.mobile.client.user_profile

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import network.bisq.mobile.client.replicated_model.user.profile.UserProfile
import network.bisq.mobile.domain.client.main.user_profile.UserProfileApiGateway
import network.bisq.mobile.domain.data.model.UserProfileModel
import network.bisq.mobile.domain.data.repository.UserProfileRepository
import network.bisq.mobile.domain.service.UserProfileServiceFacade
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ClientUserProfileServiceFacade(
    override val repository: UserProfileRepository<ClientUserProfileModel>,
    private val apiGateway: UserProfileApiGateway
) :
    UserProfileServiceFacade<ClientUserProfileModel, UserProfileRepository<ClientUserProfileModel>> {
    private val log = Logger.withTag(this::class.simpleName ?: "UserProfileServiceFacade")


    override suspend fun hasUserProfile(): Boolean {
        return getUserIdentityIds().isNotEmpty()
    }

    override suspend fun generateKeyPair() {
        var model = repository.data.value
        if (model == null) {
            model = ClientUserProfileModel()
            repository.create(model)
        }
        try {
            model.generateKeyPairInProgress = true
            // TODO this flag should be UI related not in the domain model - forcing a new update here
            repository.update(model)
            val ts = Clock.System.now().toEpochMilliseconds()
            val response = apiGateway.requestPreparedData()
            model.preparedDataAsJson = response.first
            val preparedData = response.second

            createSimulatedDelay(Clock.System.now().toEpochMilliseconds() - ts)

            model.keyPair = preparedData.keyPair
            model.proofOfWork = preparedData.proofOfWork
            model.nym = preparedData.nym
            model.id = preparedData.id
            repository.update(model)
        } catch (e: Exception) {
            log.e { e.toString() }
        } finally {
            model.generateKeyPairInProgress = false
            repository.update(model)
        }
    }

    override suspend fun createAndPublishNewUserProfile() {
        var model = repository.data.value
        if (model == null) {
            model = ClientUserProfileModel()
            repository.create(model)
        }
        try {
            model.generateKeyPairInProgress = true
            // TODO this flag should be UI related not in the domain model - forcing a new update here
            repository.update(model)

            val response: UserProfileResponse =
                apiGateway.createAndPublishNewUserProfile(
                    model.nickName,
                    model.preparedDataAsJson
                )
            require(model.id == response.userProfileId)
            { "userProfileId from model does not match userProfileId from response" }

            repository.update(model)

        } catch (e: Exception) {
            log.e { e.toString() }
        } finally {
            model.createAndPublishInProgress = false
            repository.update(model)
        }
    }


    override suspend fun getUserIdentityIds(): List<String> {
        return try {
            apiGateway.getUserIdentityIds()
        } catch (e: Exception) {
            log.e { e.toString() }
            emptyList()
        }
    }

    override suspend fun applySelectedUserProfile() {
        try {
            val model = repository.data.value
            val userProfile = getSelectedUserProfile()
            if (model != null) {
                model.nickName = userProfile.nickName
                model.nym = userProfile.nym
                model.id = userProfile.id
                repository.update(model)
            } else {
                log.w { "model in repository is null, cannot update model" }
            }
        } catch (e: Exception) {
            log.e { e.toString() }
        }
    }

    private suspend fun getSelectedUserProfile(): UserProfile {
        return apiGateway.getSelectedUserProfile()
    }

    private suspend fun findUserProfile(id: String): UserProfileModel? {
        TODO("Not yet implemented")
    }

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