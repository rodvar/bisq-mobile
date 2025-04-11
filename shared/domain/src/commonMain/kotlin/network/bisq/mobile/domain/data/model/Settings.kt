package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
open class Settings : BaseModel() {
    open var bisqApiUrl: String = ""
    //todo better rely on the source data alone (has user profile, has bisqApiUrl set) as otherwise we can get out of sync
    open var firstLaunch: Boolean = true
    open var showChatRulesWarnBox: Boolean = true
}