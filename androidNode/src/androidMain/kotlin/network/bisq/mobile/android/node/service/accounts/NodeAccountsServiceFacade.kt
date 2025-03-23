package network.bisq.mobile.android.node.service.accounts

import bisq.account.AccountService
import bisq.account.accounts.UserDefinedFiatAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.UserDefinedFiatAccountMapping
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeAccountsServiceFacade(applicationService: AndroidApplicationService.Provider) : AccountsServiceFacade, Logging {
    private val accountService: AccountService by lazy { applicationService.accountService.get() }

    private val _accounts = MutableStateFlow<List<UserDefinedFiatAccountVO>>(emptyList())
    override val accounts: StateFlow<List<UserDefinedFiatAccountVO>> get() = _accounts

    private val _selectedAccount = MutableStateFlow<UserDefinedFiatAccountVO?>(null)
    override val selectedAccount: StateFlow<UserDefinedFiatAccountVO?> get() = _selectedAccount

    private val backgroundScope = CoroutineScope(BackgroundDispatcher)

    override suspend fun getAccounts(): List<UserDefinedFiatAccountVO> {
        log.e { "NodeAccountServiceFacade :: getAccounts()" }
        return accountService
            .getAccountByNameMap()
            .values
            .map { UserDefinedFiatAccountMapping.fromBisq2Model(it as UserDefinedFiatAccount) }
            .sortedBy { it.accountName }
            .also { _accounts.value = it }
    }

    override suspend fun addAccount(account: UserDefinedFiatAccountVO) {
        val bisq2Account = UserDefinedFiatAccountMapping.toBisq2Model(account)
        accountService.addPaymentAccount(bisq2Account)
        getAccounts()
        setSelectedAccount(account)
    }

    override suspend fun saveAccount(account: UserDefinedFiatAccountVO) {
        removeAccount(selectedAccount.value!!, false)
        accountService.addPaymentAccount(UserDefinedFiatAccountMapping.toBisq2Model(account))
        getAccounts()
        setSelectedAccount(account)
    }

    override suspend fun removeAccount(account: UserDefinedFiatAccountVO, updateSelectedAccount: Boolean) {
        accountService.removePaymentAccount(UserDefinedFiatAccountMapping.toBisq2Model(account))
        getAccounts()
        if (updateSelectedAccount) {
            val nextAccount = accounts.value.firstOrNull()
            if (nextAccount != null) {
                setSelectedAccount(nextAccount)
            }
        }
    }

    override suspend fun setSelectedAccount(account: UserDefinedFiatAccountVO) {
        accountService.setSelectedAccount(UserDefinedFiatAccountMapping.toBisq2Model(account))
       _selectedAccount.value = account
    }

    override fun activate() {
        log.i("Activating NodeAccountsServiceFacade")
        backgroundScope.launch {
            getAccounts()
        }
    }

    override fun deactivate() {
        log.i("Deactivating NodeAccountsServiceFacade")
    }
}