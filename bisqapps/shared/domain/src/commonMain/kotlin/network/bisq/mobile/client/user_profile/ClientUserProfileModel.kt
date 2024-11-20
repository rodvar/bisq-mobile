package network.bisq.mobile.client.user_profile

import network.bisq.mobile.client.replicated_model.security.keys.KeyPair
import network.bisq.mobile.client.replicated_model.security.pow.ProofOfWork
import network.bisq.mobile.domain.data.model.UserProfileModel

class ClientUserProfileModel : UserProfileModel() {
    lateinit var preparedDataAsJson: String
    lateinit var keyPair: KeyPair
    lateinit var proofOfWork: ProofOfWork
}