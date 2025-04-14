package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.formatters.PercentageFormatter
import network.bisq.mobile.domain.formatters.PriceQuoteFormatter
import network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class CreateOfferReviewPresenter(
    mainPresenter: MainPresenter,
    private val createOfferPresenter: CreateOfferPresenter
) : BasePresenter(mainPresenter) {

    lateinit var headLine: String
    lateinit var quoteSidePaymentMethodDisplayString: String
    lateinit var baseSidePaymentMethodDisplayString: String
    lateinit var amountToPay: String
    lateinit var amountToReceive: String
    lateinit var fee: String
    lateinit var feeDetails: String
    lateinit var formattedPrice: String
    lateinit var marketCodes: String
    lateinit var priceDetails: String
    lateinit var direction: DirectionEnum
    var formattedBaseRangeMinAmount: String = ""
    var formattedBaseRangeMaxAmount: String = ""
    var isRangeOffer: Boolean = false

    private lateinit var createOfferModel: CreateOfferPresenter.CreateOfferModel


    override fun onViewAttached() {
        createOfferModel = createOfferPresenter.createOfferModel
        direction = createOfferModel.direction

        quoteSidePaymentMethodDisplayString =
            createOfferModel.selectedQuoteSidePaymentMethods.joinToString(", ") { i18NPaymentMethod(it) }
        baseSidePaymentMethodDisplayString =
            createOfferModel.selectedBaseSidePaymentMethods.joinToString(", ") { i18NPaymentMethod(it) }

        val formattedQuoteAmount: String
        val formattedBaseAmount: String
        if (createOfferModel.amountType == CreateOfferPresenter.AmountType.FIXED_AMOUNT) {
            formattedQuoteAmount = AmountFormatter.formatAmount(createOfferModel.quoteSideFixedAmount!!, true, true)
            formattedBaseAmount = AmountFormatter.formatAmount(createOfferModel.baseSideFixedAmount!!, false, false)
        } else {
            formattedQuoteAmount = AmountFormatter.formatRangeAmount(
                createOfferModel.quoteSideMinRangeAmount!!,
                createOfferModel.quoteSideMaxRangeAmount!!,
                true
            )
            formattedBaseAmount = AmountFormatter.formatRangeAmount(
                createOfferModel.baseSideMinRangeAmount!!,
                createOfferModel.baseSideMaxRangeAmount!!,
                false
            )
            formattedBaseRangeMinAmount = AmountFormatter.formatAmount(createOfferModel.baseSideMinRangeAmount!!, false, false)
            formattedBaseRangeMaxAmount = AmountFormatter.formatAmount(createOfferModel.baseSideMaxRangeAmount!!, false, false)
        }
        headLine = "${direction.name.uppercase()} Bitcoin"

        if (direction.isBuy) {
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

        marketCodes = createOfferModel.market!!.marketCodes
        formattedPrice = PriceQuoteFormatter.format(createOfferModel.priceQuote, true, false)
        isRangeOffer = createOfferModel.amountType == CreateOfferPresenter.AmountType.RANGE_AMOUNT

        applyPriceDetails()
    }

    private fun applyPriceDetails() {
        val percentagePriceValue = createOfferModel.percentagePriceValue
        if (percentagePriceValue == 0.0) {
            priceDetails = "bisqEasy.tradeWizard.review.priceDetails".i18n()
        } else {
            val priceWithCode = PriceQuoteFormatter.format(createOfferModel.priceQuote, true, true)
            val percentagePrice = PercentageFormatter.format(percentagePriceValue, true)
            val aboveOrBelow: String = if (percentagePriceValue > 0) "above" else "below" //todo
            priceDetails = if (createOfferModel.priceType == CreateOfferPresenter.PriceType.PERCENTAGE) {
                "bisqEasy.tradeWizard.review.priceDetails.float".i18n(percentagePrice, aboveOrBelow, priceWithCode)
            } else {
                if (percentagePriceValue == 0.0) {
                    "bisqEasy.tradeWizard.review.priceDetails.fix.atMarket".i18n(priceWithCode)
                } else {
                    "bisqEasy.tradeWizard.review.priceDetails.fix".i18n(percentagePrice, aboveOrBelow, priceWithCode)
                }
            }
        }
    }

    fun onBack() {
        navigateBack()
    }

    fun onCreateOffer() {
        ioScope.launch {
            enableInteractive(false)
            // TODO deactivate buttons, show waiting state
            createOfferPresenter.createOffer()
            // TODO hide waiting state, show successfully published state, show button to open offer book, clear navigation backstack
            onGoToOfferList()
            enableInteractive()
        }
    }

    private fun onGoToOfferList() {
        presenterScope.launch {
            // FIXME without clearing the backstack it does not work
            val rootNavController = getRootNavController()
            var currentBackStack = rootNavController.currentBackStack.value
            while (currentBackStack.size > 2) {
                rootNavController.popBackStack()
                currentBackStack = rootNavController.currentBackStack.value
            }
            navigateToTab(Routes.TabOfferbook)
        }
    }
}
