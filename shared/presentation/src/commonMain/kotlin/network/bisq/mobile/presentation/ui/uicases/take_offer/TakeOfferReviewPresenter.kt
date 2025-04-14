package network.bisq.mobile.presentation.ui.uicases.take_offer

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PercentageFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.utils.PriceUtil
import network.bisq.mobile.domain.utils.StringUtils.truncate
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod
import network.bisq.mobile.presentation.ui.navigation.Routes

class TakeOfferReviewPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val takeOfferPresenter: TakeOfferPresenter,
) : BasePresenter(mainPresenter) {

    lateinit var headLine: String
    lateinit var quoteSidePaymentMethodDisplayString: String
    lateinit var baseSidePaymentMethodDisplayString: String
    lateinit var amountToPay: String
    lateinit var amountToReceive: String
    lateinit var fee: String
    lateinit var feeDetails: String
    lateinit var price: String
    lateinit var marketCodes: String
    lateinit var priceDetails: String
    lateinit var takersDirection: DirectionEnum

    private lateinit var takeOfferModel: TakeOfferPresenter.TakeOfferModel

    // We pass that to the domain, which updates the state while take offer is in progress, so that we can show the status
    // or error to the user
    private val takeOfferStatus: MutableStateFlow<TakeOfferStatus?> = MutableStateFlow(null)
    private val takeOfferErrorMessage: MutableStateFlow<String?> = MutableStateFlow(null)

    private val _showTakeOfferProgressDialog = MutableStateFlow(false)
    val showTakeOfferProgressDialog: StateFlow<Boolean> get() = _showTakeOfferProgressDialog
    private fun setShowTakeOfferProgressDialog(value: Boolean) {
        _showTakeOfferProgressDialog.value = value
    }

    private val _showTakeOfferSuccessDialog = MutableStateFlow(false)
    val showTakeOfferSuccessDialog: StateFlow<Boolean> get() = _showTakeOfferSuccessDialog
    private fun setShowTakeOfferSuccessDialog(value: Boolean) {
        _showTakeOfferSuccessDialog.value = value
    }

    private var jobs: MutableSet<Job> = mutableSetOf()

    override fun onViewAttached() {
        presenterScope.launch {
            takeOfferStatus.collect { value ->
                log.i { "takeOfferStatus: $value" }
                //todo show state
            }
        }
        presenterScope.launch {
            takeOfferErrorMessage
                .drop(1) // To ignore the first init message
                .collect { message ->
                    log.e { "takeOfferErrorMessage: $message" }
                    showSnackbar(message ?: "Unexpected error occurred, please try again", true)
                }
        }

        takeOfferModel = takeOfferPresenter.takeOfferModel
        val offerListItem = takeOfferModel.offerItemPresentationVO
        takersDirection = offerListItem.bisqEasyOffer.direction.mirror

        quoteSidePaymentMethodDisplayString = i18NPaymentMethod(takeOfferModel.quoteSidePaymentMethod)
        baseSidePaymentMethodDisplayString = i18NPaymentMethod(takeOfferModel.baseSidePaymentMethod)

        val formattedQuoteAmount = AmountFormatter.formatAmount(takeOfferModel.quoteAmount, true, true)
        val formattedBaseAmount = AmountFormatter.formatAmount(takeOfferModel.baseAmount, false, false)

        headLine = "${takersDirection.name.uppercase()} Bitcoin"

        if (takersDirection.isBuy) {
            amountToPay = formattedQuoteAmount
            amountToReceive = formattedBaseAmount
            fee = "bisqEasy.tradeWizard.review.noTradeFees".i18n()
            feeDetails = "bisqEasy.tradeWizard.review.sellerPaysMinerFeeLong".i18n()
        } else {
            amountToPay = formattedBaseAmount
            amountToReceive = formattedQuoteAmount
            fee = "bisqEasy.tradeWizard.review.sellerPaysMinerFee".i18n()
            feeDetails = "bisqEasy.tradeWizard.review.noTradeFeesLong".i18n()
        }

        marketCodes = offerListItem.bisqEasyOffer.market.marketCodes
        price = offerListItem.formattedPrice.value //todo we need updated price not static offer price
        applyPriceDetails()
    }

    override fun onViewUnattaching() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    fun onBack() {
        navigateBack()
    }

    fun onTakeOffer() {
        setShowTakeOfferProgressDialog(true)
        disableInteractive()

        jobs.forEach { it.cancel() }
        jobs.clear()
        jobs.add(presenterScope.launch {
            try {
                // takeOffer use withContext(IODispatcher) for calling the service
                val (statusFlow, errorFlow) = takeOfferPresenter.takeOffer()

                // The stateFlow objects are set in the ioScope in the service. Thus we need to map them to the presenterScope.
                jobs.add(launch {
                    statusFlow.collect { takeOfferStatus.value = it }
                })
                jobs.add(launch {
                    errorFlow.collect { takeOfferErrorMessage.value = it }
                })
                setShowTakeOfferSuccessDialog(true)
            } catch (e: Exception) {
                log.e("Take offer failed", e)
                takeOfferErrorMessage.value = e.message ?: ("Take offer failed with exception: " + e.toString().truncate(50))
            } finally {
                setShowTakeOfferProgressDialog(false)
                enableInteractive()
            }
        })
    }

    fun onGoToOpenTrades() {
        setShowTakeOfferSuccessDialog(false)
        closeWorkflow()
        // ensure we go to the my trade tab
        navigateToTab(Routes.TabOpenTradeList)
    }

    private fun closeWorkflow() {
        // Navigate back to TabContainer, which is part of RootNavigator's nav stack.
        // Rather than navigating back to a specific Tab, which is part of TabNavController
        navigateBackTo(Routes.TabContainer)
    }

    private fun applyPriceDetails() {
        val priceSpec = takeOfferModel.offerItemPresentationVO.bisqEasyOffer.priceSpec
        val percent =
            PriceUtil.findPercentFromMarketPrice(
                marketPriceServiceFacade,
                priceSpec,
                takeOfferModel.offerItemPresentationVO.bisqEasyOffer.market
            )
        if ((priceSpec is FloatPriceSpecVO || priceSpec is MarketPriceSpecVO) && percent == 0.0) {
            priceDetails = "bisqEasy.tradeWizard.review.priceDetails".i18n()
        } else {
            val priceWithCode = PriceQuoteFormatter.format(takeOfferModel.priceQuote, true, true)
            val percentagePrice = PercentageFormatter.format(percent, true)
            val aboveOrBelow: String = if (percent > 0) "above" else "below" //todo
            priceDetails = if (priceSpec is FloatPriceSpecVO) {
                "bisqEasy.tradeWizard.review.priceDetails.float".i18n(percentagePrice, aboveOrBelow, priceWithCode)
            } else {
                if (percent == 0.0) {
                    "bisqEasy.tradeWizard.review.priceDetails.fix.atMarket".i18n(priceWithCode)
                } else {
                    "bisqEasy.tradeWizard.review.priceDetails.fix".i18n(percentagePrice, aboveOrBelow, priceWithCode)
                }
            }
        }
    }

}
