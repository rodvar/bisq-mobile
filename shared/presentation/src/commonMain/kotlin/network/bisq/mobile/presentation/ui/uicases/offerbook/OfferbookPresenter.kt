package network.bisq.mobile.presentation.ui.uicases.offerbook

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.domain.utils.OfferUtils.getFixedOrMaxAmount
import network.bisq.mobile.domain.utils.OfferUtils.getFixedOrMinAmount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter


class OfferbookPresenter(
    mainPresenter: MainPresenter,
    private val offersServiceFacade: OffersServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter,
    private val createOfferPresenter: CreateOfferPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade
) : BasePresenter(mainPresenter) {
    private val _selectedDirection = MutableStateFlow(DirectionEnum.SELL)
    val selectedDirection: StateFlow<DirectionEnum> = _selectedDirection
    private val includeOfferPredicate: MutableStateFlow<(OfferItemPresentationModel) -> Boolean> =
        MutableStateFlow { _: OfferItemPresentationModel -> true }

    val sortedFilteredOffers: StateFlow<List<OfferItemPresentationModel>> =
        combine(offersServiceFacade.offerbookListItems, selectedDirection, includeOfferPredicate) { offers, direction, includeOffer ->
            offers.filter { it.bisqEasyOffer.direction.mirror == direction }
                .filter(includeOffer)
                .sortedWith(compareByDescending<OfferItemPresentationModel> { it.bisqEasyOffer.date }
                    .thenBy { it.bisqEasyOffer.id })
        }.stateIn(
            scope = presenterScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _showDeleteConfirmation = MutableStateFlow(false)
    val showDeleteConfirmation: StateFlow<Boolean> = _showDeleteConfirmation

    private val _showNotEnoughReputationDialog = MutableStateFlow(false)
    val showNotEnoughReputationDialog: StateFlow<Boolean> = _showNotEnoughReputationDialog

    var notEnoughReputationHeadline: String = ""
    var notEnoughReputationMessage: String = ""

    private var selectedOffer: OfferItemPresentationModel? = null

    override fun onViewAttached() {
        super.onViewAttached()

        selectedOffer = null
        includeOfferPredicate.value = { _ -> true } // Reset to trigger update at screen change

        presenterScope.launch {
            combine(
                offersServiceFacade.offerbookListItems,
                selectedDirection
            ) { offers, direction ->
                offers to direction // create a new object to enforce to always emits
            }.collect { (_, _) ->
                updateIncludeOfferPredicate()
            }
        }

        updateIncludeOfferPredicate()
    }

    fun onOfferSelected(item: OfferItemPresentationModel) {
        selectedOffer = item
        if (item.isMyOffer) {
            _showDeleteConfirmation.value = true
        } else {
            takeOffer()
        }
    }

    fun onConfirmedDeleteOffer() {
        runCatching {
            selectedOffer?.let { item ->
                require(item.isMyOffer)
                presenterScope.launch {
                    withContext(IODispatcher) {
                        offersServiceFacade.deleteOffer(item.offerId)
                    }
                    deselectOffer()
                }
            }
        }.onFailure {
            log.e(it) { "Failed to delete offer ${selectedOffer?.offerId}" }
            showSnackbar(
                "Unable to delete offer ${selectedOffer?.offerId}",
                true
            )
            deselectOffer()
        }
    }

    fun onDismissDeleteOffer() {
        _showDeleteConfirmation.value = false
        deselectOffer()
    }

    private fun updateIncludeOfferPredicate() {
        presenterScope.launch {
            val invalidSellOfferIds = sortedFilteredOffers.value
                .filter {
                    it.bisqEasyOffer.direction == DirectionEnum.SELL &&
                            withContext(IODispatcher) {
                                BisqEasyTradeAmountLimits.isSellOfferInvalid(
                                    it,
                                    true,
                                    marketPriceServiceFacade,
                                    reputationServiceFacade
                                )
                            }
                }
                .map { it.bisqEasyOffer.id }
                .toSet()

            includeOfferPredicate.value = { item ->
                item.bisqEasyOffer.id !in invalidSellOfferIds
            }
        }
    }

    private fun takeOffer() {
        runCatching {
            selectedOffer?.let { item ->
                require(!item.isMyOffer)
                presenterScope.launch {
                    if (canTakeOffer(item)) {
                        takeOfferPresenter.selectOfferToTake(item)
                        if (takeOfferPresenter.showAmountScreen()) {
                            navigateTo(Routes.TakeOfferTradeAmount)
                        } else if (takeOfferPresenter.showPaymentMethodsScreen()) {
                            navigateTo(Routes.TakeOfferPaymentMethod)
                        } else {
                            navigateTo(Routes.TakeOfferReviewTrade)
                        }
                    } else {
                        _showNotEnoughReputationDialog.value = true
                    }
                }
            }
        }.onFailure {
            log.e(it) { "Failed to take offer ${selectedOffer?.offerId}" }
            showSnackbar(
                "Unable to take offer ${selectedOffer?.offerId}",
                true
            )
            deselectOffer()
        }
    }

    fun onLearnHowToBuildReputation() {
        _showNotEnoughReputationDialog.value = false
    }

    fun onDismissNotEnoughReputationDialog() {
        _showNotEnoughReputationDialog.value = false
    }

    private suspend fun canTakeOffer(item: OfferItemPresentationModel): Boolean {
        val bisqEasyOffer = item.bisqEasyOffer
        val selectedUserProfile = userProfileServiceFacade.getSelectedUserProfile()
            ?: run {
                log.w { "SelectedUserProfile is null" }
                return false
            }
        val requiredReputationScoreForMaxOrFixed =
            BisqEasyTradeAmountLimits.findRequiredReputationScoreForMaxOrFixedAmount(marketPriceServiceFacade, bisqEasyOffer)
                ?: run {
                    log.w { "requiredReputationScoreForMaxOrFixedAmount is null" }
                    return false
                }
        val requiredReputationScoreForMinOrFixed =
            BisqEasyTradeAmountLimits.findRequiredReputationScoreForMinOrFixedAmount(marketPriceServiceFacade, bisqEasyOffer)
                ?: run {
                    log.w { "requiredReputationScoreForMinAmount is null" }
                    return false
                }

        val market = bisqEasyOffer.market
        val quoteCurrencyCode = market.quoteCurrencyCode
        val minFiatAmount: String = AmountFormatter.formatAmount(
            FiatVOFactory.from(getFixedOrMinAmount(bisqEasyOffer), quoteCurrencyCode),
            useLowPrecision = true,
            withCode = true
        )
        val maxFiatAmount: String = AmountFormatter.formatAmount(
            FiatVOFactory.from(getFixedOrMaxAmount(bisqEasyOffer), quoteCurrencyCode),
            useLowPrecision = true,
            withCode = true
        )

        val userProfileId = if (bisqEasyOffer.direction == DirectionEnum.SELL)
            bisqEasyOffer.makerNetworkId.pubKey.id // Offer maker is seller
        else
            selectedUserProfile.id // I am seller

        val sellersScore: Long = run {
            val reputationScoreResult: Result<ReputationScoreVO> = withContext(IODispatcher) {
                reputationServiceFacade.getReputation(userProfileId)
            }
            reputationScoreResult.exceptionOrNull()?.let { exception ->
                log.w("Exception at reputationServiceFacade.getReputation", exception)
            }
            reputationScoreResult.getOrNull()?.totalScore ?: 0
        }

        val isAmountRangeOffer = bisqEasyOffer.amountSpec is RangeAmountSpecVO

        val canBuyerTakeOffer = sellersScore >= requiredReputationScoreForMinOrFixed
        if (!canBuyerTakeOffer) {
            val link = "hyperlinks.openInBrowser.attention".i18n("https://bisq.wiki/Reputation#How_to_build_reputation")
            if (bisqEasyOffer.direction == DirectionEnum.SELL) {
                // I am as taker the buyer. We check if seller has the required reputation
                val learnMore = "mobile.reputation.learnMore".i18n()
                notEnoughReputationHeadline = "chat.message.takeOffer.buyer.invalidOffer.headline".i18n()
                val warningKey = if (isAmountRangeOffer) "chat.message.takeOffer.buyer.invalidOffer.rangeAmount.text"
                else "chat.message.takeOffer.buyer.invalidOffer.fixedAmount.text"
                notEnoughReputationMessage = warningKey.i18n(
                    sellersScore,
                    if (isAmountRangeOffer) requiredReputationScoreForMinOrFixed else requiredReputationScoreForMaxOrFixed,
                    if (isAmountRangeOffer) minFiatAmount else maxFiatAmount
                ) + "\n\n" + learnMore + "\n\n\n" + link
            } else {
                //  I am as taker the seller. We check if my reputation permits to take the offer
                val learnMore = "mobile.reputation.buildReputation".i18n()
                notEnoughReputationHeadline = "chat.message.takeOffer.seller.insufficientScore.headline".i18n()
                val warningKey = if (isAmountRangeOffer) "chat.message.takeOffer.seller.insufficientScore.rangeAmount.warning"
                else "chat.message.takeOffer.seller.insufficientScore.fixedAmount.warning"
                notEnoughReputationMessage = warningKey.i18n(
                    sellersScore,
                    if (isAmountRangeOffer) requiredReputationScoreForMinOrFixed else requiredReputationScoreForMaxOrFixed,
                    if (isAmountRangeOffer) minFiatAmount else maxFiatAmount
                ) + "\n\n" + learnMore + "\n\n\n" + link
            }
        }

        return canBuyerTakeOffer
    }

    private fun deselectOffer() {
        selectedOffer = null
    }

    fun onSelectDirection(direction: DirectionEnum) {
        _selectedDirection.value = direction
    }

    fun createOffer() {
        try {
            val market = offersServiceFacade.selectedOfferbookMarket.value.market
            createOfferPresenter.onStartCreateOffer()
            createOfferPresenter.commitMarket(market)
            navigateTo(Routes.CreateOfferDirection)
        } catch (e: Exception) {
            log.e(e) { "Failed to create offer" }
            showSnackbar(
                if (isDemo()) "Create offer is disabled in demo mode" else "Cannot create offer at this time, please try again later"
            )
        }
    }
}
