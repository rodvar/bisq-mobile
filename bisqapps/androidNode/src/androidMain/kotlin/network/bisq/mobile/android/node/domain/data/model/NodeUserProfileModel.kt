package network.bisq.mobile.android.node.domain.data.model

import bisq.security.pow.ProofOfWork
import network.bisq.mobile.domain.data.model.UserProfileModel
import java.security.KeyPair

// TODO proof of work is not Pojo
class NodeUserProfileModel : UserProfileModel() {
    lateinit var keyPair: KeyPair
    lateinit var proofOfWork: ProofOfWork
}