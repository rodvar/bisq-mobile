package network.bisq.mobile.client.service.accounts

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.service.market.AccountsApiGateway
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientAccountsServiceFacade(
    private val apiGateway: AccountsApiGateway,
    private val json: Json
) : AccountsServiceFacade, Logging {

    private val _accounts = MutableStateFlow<List<UserDefinedFiatAccountVO>>(emptyList())
    override val accounts: StateFlow<List<UserDefinedFiatAccountVO>> get() = _accounts

    private val _selectedAccount = MutableStateFlow<UserDefinedFiatAccountVO?>(null)
    override val selectedAccount: StateFlow<UserDefinedFiatAccountVO?> get() = _selectedAccount

    override suspend fun getAccounts(): List<UserDefinedFiatAccountVO> {
        val result = apiGateway.getPaymentAccounts()
        if (result.isSuccess) {
            result.getOrThrow().let {
                _accounts.value = it.sortedBy { it.accountName }
            }
        }
        return _accounts.value
    }

    override suspend fun addAccount(account: UserDefinedFiatAccountVO) {
        apiGateway.addAccount(account.accountName, account.accountPayload.accountData)
        getAccounts()
        setSelectedAccount(account)
    }

    override suspend fun saveAccount(account: UserDefinedFiatAccountVO) {
        removeAccount(selectedAccount.value!!, false)
        apiGateway.addAccount(account.accountName, account.accountPayload.accountData)
        getAccounts()
        setSelectedAccount(account)
    }

    override suspend fun removeAccount(account: UserDefinedFiatAccountVO, updateSelectedAccount: Boolean) {
        apiGateway.deleteAccount(account.accountName)
        getAccounts()
        if (updateSelectedAccount) {
            val nextAccount = accounts.value.firstOrNull()
            if (nextAccount != null) {
                setSelectedAccount(nextAccount)
            }
        }
    }

    override suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO) {
        _selectedAccount.value = account
    }

}