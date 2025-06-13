package network.bisq.mobile.presentation.ui.uicases.take_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TakeOfferPaymentMethodPresenter(
    mainPresenter: MainPresenter,
    private val takeOfferPresenter: TakeOfferPresenter,
    private val accountsServiceFacade: AccountsServiceFacade
) : BasePresenter(mainPresenter) {

    var hasMultipleQuoteSidePaymentMethods: Boolean = false
    var hasMultipleBaseSidePaymentMethods: Boolean = false
    lateinit var quoteSidePaymentMethods: List<String>
    lateinit var baseSidePaymentMethods: List<String>
    val quoteSidePaymentMethod: MutableStateFlow<String?> = MutableStateFlow(null)
    val baseSidePaymentMethod: MutableStateFlow<String?> = MutableStateFlow(null)
    lateinit var quoteCurrencyCode: String

    private lateinit var takeOfferModel: TakeOfferPresenter.TakeOfferModel

    // Filtered payment methods based on user's payment accounts and currency compatibility
    val filteredQuoteSidePaymentMethods: StateFlow<List<String>> by lazy {
        accountsServiceFacade.accounts.map { accounts ->
            filterPaymentMethodsByCurrency(quoteSidePaymentMethods, quoteCurrencyCode, accounts)
        }.stateIn(
            scope = presenterScope,
            started = SharingStarted.Eagerly,
            initialValue = quoteSidePaymentMethods
        )
    }

    init {
        takeOfferModel = takeOfferPresenter.takeOfferModel
        hasMultipleQuoteSidePaymentMethods = takeOfferModel.hasMultipleQuoteSidePaymentMethods
        hasMultipleBaseSidePaymentMethods = takeOfferModel.hasMultipleBaseSidePaymentMethods

        val offerListItem = takeOfferModel.offerItemPresentationVO
        quoteSidePaymentMethods = offerListItem.quoteSidePaymentMethods
        if (takeOfferModel.quoteSidePaymentMethod.isNotEmpty()) {
            quoteSidePaymentMethod.value = takeOfferModel.quoteSidePaymentMethod
        } else {
            if (quoteSidePaymentMethods.size == 1) {
                quoteSidePaymentMethod.value = quoteSidePaymentMethods[0]
            }
        }

        baseSidePaymentMethods = offerListItem.baseSidePaymentMethods
        if (takeOfferModel.baseSidePaymentMethod.isNotEmpty()) {
            baseSidePaymentMethod.value = takeOfferModel.baseSidePaymentMethod
        } else {
            if (offerListItem.baseSidePaymentMethods.size == 1) {
                baseSidePaymentMethod.value = offerListItem.baseSidePaymentMethods[0]
            }
        }
        quoteCurrencyCode = offerListItem.bisqEasyOffer.market.quoteCurrencyCode
    }

    override fun onViewAttached() {
        super.onViewAttached()
        launchIO {
            accountsServiceFacade.getAccounts()
        }
    }

    override fun onViewUnattaching() {
        dismissSnackbar()
        super.onViewUnattaching()
    }

    fun onQuoteSidePaymentMethodSelected(paymentMethod: String) {
        quoteSidePaymentMethod.value = paymentMethod
    }

    fun onBaseSidePaymentMethodSelected(paymentMethod: String) {
        baseSidePaymentMethod.value = paymentMethod
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onNext() {
        if (isValid()) {
            commitToModel()
            navigateTo(Routes.TakeOfferReviewTrade)
        } else {
            if (quoteSidePaymentMethod.value == null) {
                showSnackbar("bisqEasy.tradeWizard.review.paymentMethodDescriptions.fiat.taker".i18n())
            } else if (baseSidePaymentMethod.value == null) {
                showSnackbar("bisqEasy.tradeWizard.review.paymentMethodDescriptions.btc.taker".i18n())
            }
            // Note the data is set at the service layer, so if there is only one payment method we
            // have it set at the service. We do not need to check here if we have the multiple options.
        }
    }

    private fun commitToModel() {
        if (isValid()) {
            takeOfferPresenter.commitPaymentMethod(quoteSidePaymentMethod.value!!, baseSidePaymentMethod.value!!)
        }
    }

    private fun isValid() = quoteSidePaymentMethod.value != null && baseSidePaymentMethod.value != null

    fun getQuoteSidePaymentMethodsImagePaths(): List<String> {
        return getPaymentMethodsImagePaths(filteredQuoteSidePaymentMethods.value, "fiat")
    }

    fun getBaseSidePaymentMethodsImagePaths(): List<String> {
        return getPaymentMethodsImagePaths(baseSidePaymentMethods, "bitcoin")
    }

    private fun getPaymentMethodsImagePaths(list: List<String>, directory: String) = list
        .map { paymentMethod ->
            val fileName = paymentMethod.lowercase().replace("-", "_")
            "drawable/payment/$directory/$fileName.png"
        }

    /**
     * Filters payment methods based on currency compatibility with user's payment accounts.
     * Includes payment methods that:
     * 1. Have user accounts with matching currency
     * 2. Have user accounts with no currency set (Any Currency)
     */
    private fun filterPaymentMethodsByCurrency(
        availablePaymentMethods: List<String>,
        tradeCurrency: String,
        userAccounts: List<UserDefinedFiatAccountVO>
    ): List<String> {
        // If no user accounts, show all available payment methods
        if (userAccounts.isEmpty()) {
            return availablePaymentMethods
        }

        // Find accounts that support the trade currency or have no currency restriction
        val compatibleAccounts = userAccounts.filter { account ->
            account.currencyCodes.isEmpty() || // No currency set = "Any Currency"
            account.currencyCodes.contains(tradeCurrency) // Exact currency match
        }

        // If no compatible accounts, return empty list (user needs to create compatible payment accounts)
        if (compatibleAccounts.isEmpty()) {
            return emptyList()
        }

        // For now, we return all available payment methods if user has compatible accounts
        // In the future, this could be enhanced to filter by specific payment method types
        // that match the user's account configurations
        return availablePaymentMethods
    }
}
