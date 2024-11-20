package network.bisq.mobile.android.node.domain.data.repository

import bisq.security.pow.ProofOfWork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.domain.data.model.NodeUserProfileModel
import network.bisq.mobile.domain.data.model.UserProfileModel
import network.bisq.mobile.domain.data.repository.UserProfileRepository
import java.security.KeyPair

class NodeUserProfileRepository: UserProfileRepository<NodeUserProfileModel>() {
    // StateFlow for generateKeyPairInProgress, derived from the repository data
    val _keyPair = MutableStateFlow(false)
    val keyPair: StateFlow<Boolean> = _keyPair

    // StateFlow for createAndPublishInProgress, derived from the repository data
    val _proofOfWork: StateFlow<Boolean> = MutableStateFlow(false)
    val proofOfWork: StateFlow<Boolean> = _proofOfWork

    override fun newModel(): UserProfileModel {
        return NodeUserProfileModel()
    }

    // Example setters to update the base repository data
    fun updateKeyPair(value: KeyPair) {
        updateData(currentData()?.apply { keyPair = value })
    }

    fun updateProofOfWork(value: ProofOfWork) {
        updateData(currentData()?.apply { proofOfWork = value })
    }
}