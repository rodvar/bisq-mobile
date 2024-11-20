package network.bisq.mobile.domain.data.model

open class UserProfileModel: BaseModel() {
    var nickName: String = ""
    var nym: String = ""
    var generateKeyPairInProgress = false
    var createAndPublishInProgress = false
    var pubKeyHash: ByteArray? = null
    override fun toString(): String {
        return "UserProfileModel(_nickName=${nickName}, " +
                "_id=${id}, " +
                "_nym=${nym}, " +
                "_generateKeyPairInProgress=${generateKeyPairInProgress}, " +
                "_createAndPublishInProgress=${createAndPublishInProgress}, " +
                "pubKeyHash=${pubKeyHash.contentToString()})"
    }
}