package network.bisq.mobile.domain.data.replicated.user.profile

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.hexToByteArray

@Serializable
object UserProfileVOExtension {
    val UserProfileVO.id get() = networkId.pubKey.id

    val UserProfileVO.pubKeyHashAsByteArray: ByteArray get() = networkId.pubKey.hash.hexToByteArray()

    suspend fun UserProfileVO.getAvatarImage(userProfileServiceFacade: UserProfileServiceFacade): PlatformImage {
        return userProfileServiceFacade.getUserAvatar(this)
    }
}
