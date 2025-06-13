package network.bisq.mobile.domain.data.replicated.account

import kotlinx.serialization.Serializable

@Serializable
data class UserDefinedFiatAccountVO(
    val accountName: String,
    val accountPayload: UserDefinedFiatAccountPayloadVO,
    val currencyCodes: List<String> = emptyList() // Empty list means "Any Currency" for backward compatibility
)
//data class UserDefinedFiatAccountVO(
//    override val accountName: String,
//    override val paymentMethod: FiatPaymentMethodVO,
//    override val accountPayload: UserDefinedFiatAccountPayloadVO
//) : AccountVO<UserDefinedFiatAccountPayloadVO, FiatPaymentMethodVO>(accountName, paymentMethod, accountPayload)
