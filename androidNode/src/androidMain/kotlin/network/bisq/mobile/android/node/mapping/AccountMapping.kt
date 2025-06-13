package network.bisq.mobile.android.node.mapping

import bisq.account.accounts.UserDefinedFiatAccount
import bisq.account.accounts.UserDefinedFiatAccountPayload
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO

object UserDefinedFiatAccountMapping {
    fun toBisq2Model(value: UserDefinedFiatAccountVO): UserDefinedFiatAccount {
        return UserDefinedFiatAccount(
            value.accountName,
            value.accountPayload.accountData
        )
        // Note: currencyCodes are handled at the mobile app level and not passed to Bisq2 backend
    }

    fun fromBisq2Model(value: UserDefinedFiatAccount): UserDefinedFiatAccountVO {
        return UserDefinedFiatAccountVO(
            value.accountName,
            UserDefinedFiatAccountPayloadMapping.fromBisq2Model(value.accountPayload),
            emptyList() // Default to "Any Currency" for accounts from backend
        )
    }
}

object UserDefinedFiatAccountPayloadMapping {
    fun toBisq2Model(value: UserDefinedFiatAccountPayloadVO): UserDefinedFiatAccountPayload {
        return UserDefinedFiatAccountPayload(
            "",
            "",
            value.accountData
        )
    }

    fun fromBisq2Model(value: UserDefinedFiatAccountPayload): UserDefinedFiatAccountPayloadVO {
        return UserDefinedFiatAccountPayloadVO(
            value.accountData
        )
    }
}