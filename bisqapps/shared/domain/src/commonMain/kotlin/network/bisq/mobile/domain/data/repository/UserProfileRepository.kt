package network.bisq.mobile.domain.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.client.user_profile.ClientUserProfileModel
import network.bisq.mobile.domain.data.model.UserProfileModel

open class UserProfileRepository<out T : UserProfileModel>: SingleObjectRepository<T>() {
     // StateFlow for nickName, derived from the repository data
    private val _nickName = MutableStateFlow("")
    val nickName: StateFlow<String> = _nickName

    private val _id = MutableStateFlow("")
    // StateFlow for id, derived from the repository data
    val id: StateFlow<String> = _id

    private val _nym = MutableStateFlow("")
    val nym: StateFlow<String> = _nym

    private val _generateKeyPairInProgress = MutableStateFlow(false)
    val generateKeyPairInProgress: StateFlow<Boolean> = _generateKeyPairInProgress

    private val _createAndPublishInProgress = MutableStateFlow(false)
    val createAndPublishInProgress: StateFlow<Boolean> = _createAndPublishInProgress

    open fun newModel() = ClientUserProfileModel()

    // Example setters to update the base repository data
    fun updateNickName(value: String) {
        _nickName.value = value
        currentData()?.let {
            it.nickName = value
            updateData(it)
        }
    }

    fun updateId(value: String) {
        _id.value = value
        currentData()?.let {
            it.id = value
            updateData(it)
        }
        updateData(currentData()?.apply { id = value })
    }

    fun updateNym(value: String) {
        _nym.value = value
        currentData()?.let {
            it.nym = value
            updateData(it)
        }
        updateData(currentData()?.apply { nym = value })
    }

    fun setGenerateKeyPairInProgress(value: Boolean) {
        _generateKeyPairInProgress.value = value
        currentData()?.let {
            it.generateKeyPairInProgress = value
            updateData(it)
        }
    }

    fun setCreateAndPublishInProgress(value: Boolean) {
        _createAndPublishInProgress.value = value
        currentData()?.let {
            it.createAndPublishInProgress = value
            updateData(it)
        }
    }
}