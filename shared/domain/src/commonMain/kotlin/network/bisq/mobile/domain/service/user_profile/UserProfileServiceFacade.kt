package network.bisq.mobile.domain.service.user_profile

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.identity.UserIdentityVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

interface UserProfileServiceFacade : LifeCycleAware {

    val selectedUserProfile: StateFlow<UserProfileVO?>

    /**
     * Returns true if there is a user identity already created.
     * This should be used to detect a first time user who has no identity created yet and where
     * we display the create user profile screen.
     */
    suspend fun hasUserProfile(): Boolean

    /**
     * Generates a key pair and derived data as well as proof of work which is used to strengthen
     * the security of the nym (Bot ID) and the profile image (CatHash) against brute force attacks.
     * The user can generate unlimited key pairs until they get a nym and profile image they like.
     * We keep always the last generated one, to be used for creating the user identity, once the user
     * has provided a nick name and clicked the `create` button.
     * As the duration for creating the proof of work is very short and most of the time not
     * noticeable, we add an artificial delay to communicate that there is some proof of work
     * happening in the background.
     * The profile ID is the hash of the public key. The Nym is generated based on that hash and
     * the proof of work solution.
     * The CatHash image is also created based on that hash and the proof of work solution.
     */
    suspend fun generateKeyPair(result: (String, String, PlatformImage?) -> Unit)

    /**
     * Once the user clicks the `create` button we create a user identity and publish the
     * user profile to the network.
     * The user identity contains the key pair and is private data. The UserProfile is public data
     * and shared with the network.
     */
    suspend fun createAndPublishNewUserProfile(nickName: String)

    /**
     * Updates the user profile and publishes it to the network.
     * The user identity contains the key pair and is private data. The UserProfile is public data
     * and shared with the network.
     */
    suspend fun updateAndPublishUserProfile(statement: String?, terms: String?): Result<UserProfileVO>

    /**
     * Create UserProfileModels from the userIdentities.
     */
    suspend fun getUserIdentityIds(): List<String>

    /**
     * Applies the selected user identity to the user profile model
     * @return Triple containing nickname, nym and id
     */
    suspend fun applySelectedUserProfile(): Triple<String?, String?, String?>

    /**
     * @return UserProfile if existent, null otherwise
     */
    suspend fun getSelectedUserProfile(): UserProfileVO?

    /**
     * @return UserProfile if exists, null otherwise
     */
    suspend fun findUserProfile(id: String): UserProfileVO?

    /**
     * @return List of UserProfiles for the given IDs (empty if none found)
     */
    suspend fun findUserProfiles(ids: List<String>): List<UserProfileVO>

    /**
     * @return Get avatar of the user
     * This function may perform CPU-intensive work such as Base64 decoding and image generation.
     * It is recommended to call this from a background (non-main) dispatcher.
     */
    suspend fun getUserAvatar(userProfile: UserProfileVO): PlatformImage?

    suspend fun ignoreUserProfile(id: String)

    suspend fun undoIgnoreUserProfile(id: String)

    suspend fun isUserIgnored(profileId: String): Boolean

    suspend fun getIgnoredUserProfileIds(): List<String>
}