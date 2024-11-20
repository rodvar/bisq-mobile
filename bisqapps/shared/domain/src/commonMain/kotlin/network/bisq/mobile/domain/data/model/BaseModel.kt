package network.bisq.mobile.domain.data.model

import kotlinx.serialization.Serializable

/**
 * BisqApps Base Domain Model definition
 */
@Serializable
abstract class BaseModel {
    var id: String? = null
    // Add here any common properties of models (id?, timestamps?)
//    abstract val id: String
}