package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.withTolerance
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter.AmountType
import kotlin.math.roundToLong


class CreateOfferAmountPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val createOfferPresenter: CreateOfferPresenter,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val offersServiceFacade: OffersServiceFacade,
) : BasePresenter(mainPresenter) {

    val offerbookListItems: StateFlow<List<OfferItemPresentationModel>> = offersServiceFacade.offerbookListItems

    lateinit var headline: String
    lateinit var quoteCurrencyCode: String
    lateinit var formattedMinAmount: String

    val _amountType: MutableStateFlow<AmountType> = MutableStateFlow(AmountType.FIXED_AMOUNT)
    val amountType: StateFlow<AmountType> = _amountType

    // FIXED_AMOUNT
    var fixedAmountSliderPosition: Float = 0.5f

    private val _reputationBasedMaxValue: MutableStateFlow<Float?> = MutableStateFlow(null)
    val reputationBasedMaxSliderValue: StateFlow<Float?> = _reputationBasedMaxValue

    private val _rightMarkerValue: MutableStateFlow<Float?> = MutableStateFlow(null)
    val rightMarkerSliderValue: StateFlow<Float?> = _rightMarkerValue

    var formattedMinAmountWithCode: String = ""
    var formattedMaxAmountWithCode: String = ""
    private val _formattedQuoteSideFixedAmount = MutableStateFlow("")
    val formattedQuoteSideFixedAmount: StateFlow<String> = _formattedQuoteSideFixedAmount
    private val _formattedBaseSideFixedAmount = MutableStateFlow("")
    val formattedBaseSideFixedAmount: StateFlow<String> = _formattedBaseSideFixedAmount

    // RANGE_AMOUNT
    var minRangeInitialSliderValue: Float = 0.1f
    var maxRangeInitialSliderValue: Float = 0.9f
    private var rangeSliderPosition: ClosedFloatingPointRange<Float> = 0.0f..1.0f
    private val _formattedQuoteSideMinRangeAmount = MutableStateFlow("")
    val formattedQuoteSideMinRangeAmount: StateFlow<String> = _formattedQuoteSideMinRangeAmount
    private val _formattedBaseSideMinRangeAmount = MutableStateFlow("")
    val formattedBaseSideMinRangeAmount: StateFlow<String> = _formattedBaseSideMinRangeAmount

    private val _formattedQuoteSideMaxRangeAmount = MutableStateFlow("")
    val formattedQuoteSideMaxRangeAmount: StateFlow<String> = _formattedQuoteSideMaxRangeAmount
    private val _formattedBaseSideMaxRangeAmount = MutableStateFlow("")
    val formattedBaseSideMaxRangeAmount: StateFlow<String> = _formattedBaseSideMaxRangeAmount
    private val _requiredReputation = MutableStateFlow<Long>(0L)
    val requiredReputation: StateFlow<Long> = _requiredReputation

    private val _amountLimitInfo = MutableStateFlow("")
    val amountLimitInfo: StateFlow<String> = _amountLimitInfo

    private val _amountLimitInfoOverlayInfo = MutableStateFlow("")
    val amountLimitInfoOverlayInfo: StateFlow<String> = _amountLimitInfoOverlayInfo

    private val _shouldShowWarningIcon = MutableStateFlow(false)
    val shouldShowWarningIcon: StateFlow<Boolean> = _shouldShowWarningIcon

    private lateinit var createOfferModel: CreateOfferPresenter.CreateOfferModel
    private var minAmount: Long = DEFAULT_MIN_USD_TRADE_AMOUNT.value
    private var maxAmount: Long = MAX_USD_TRADE_AMOUNT.value
    private lateinit var priceQuote: PriceQuoteVO
    private lateinit var quoteSideFixedAmount: FiatVO
    private lateinit var baseSideFixedAmount: CoinVO
    private lateinit var quoteSideMinRangeAmount: FiatVO
    private lateinit var baseSideMinRangeAmount: CoinVO
    private lateinit var quoteSideMaxRangeAmount: FiatVO
    private lateinit var baseSideMaxRangeAmount: CoinVO
    private var _isBuy: MutableStateFlow<Boolean> = MutableStateFlow(true)
    var isBuy: StateFlow<Boolean> = _isBuy

    private var _formattedReputationBasedMaxAmount: MutableStateFlow<String> = MutableStateFlow("")
    val formattedReputationBasedMaxAmount: StateFlow<String> = _formattedReputationBasedMaxAmount

    private var _showLimitPopup: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLimitPopup: StateFlow<Boolean> = _showLimitPopup
    fun setShowLimitPopup(newValue: Boolean) {
        _showLimitPopup.value = newValue
    }

    var _fixedSliderPosition: MutableStateFlow<Float> = MutableStateFlow(0f)
    val fixedSliderPosition: StateFlow<Float> = _fixedSliderPosition

    override fun onViewAttached() {
        super.onViewAttached()
        createOfferModel = createOfferPresenter.createOfferModel
        quoteCurrencyCode = createOfferModel.market!!.quoteCurrencyCode
        _amountType.value = createOfferModel.amountType

        headline = if (createOfferModel.direction.isBuy)
            "bisqEasy.tradeWizard.amount.headline.buyer".i18n()
        else
            "bisqEasy.tradeWizard.amount.headline.seller".i18n()

        minAmount = BisqEasyTradeAmountLimits.getMinAmountValue(marketPriceServiceFacade, quoteCurrencyCode)
        maxAmount = BisqEasyTradeAmountLimits.getMaxAmountValue(marketPriceServiceFacade, quoteCurrencyCode)

        formattedMinAmount = AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode))
        formattedMinAmountWithCode =
            AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode), true, true)
        formattedMaxAmountWithCode =
            AmountFormatter.formatAmount(FiatVOFactory.from(maxAmount, quoteCurrencyCode), true, true)

        fixedAmountSliderPosition = createOfferModel.fixedAmountSliderPosition
        applyFixedAmountSliderValue(fixedAmountSliderPosition)

        rangeSliderPosition = createOfferModel.rangeSliderPosition
        applyRangeAmountSliderValue(rangeSliderPosition)

        _isBuy.value = createOfferModel.direction.isBuy

        updateAmountLimitInfo()
    }

    fun onSelectAmountType(value: AmountType) {
        _amountType.value = value
        updateAmountLimitInfo()
    }

    fun onFixedAmountTextValueChange(textInput: String) {
        val _value = textInput.toDoubleOrNullLocaleAware()
        if (_value != null) {
            applyFixedAmountSliderValue(getFractionForFiat(_value))
            updateAmountLimitInfo()
        }
    }

    fun onMinAmountTextValueChange(textInput: String) {
        //todo parse input string and apply it to model
    }

    fun onMaxAmountTextValueChange(textInput: String) {
        //todo parse input string and apply it to model
    }

    fun onFixedAmountSliderValueChange(value: Float) {
        applyFixedAmountSliderValue(value)
        updateAmountLimitInfo()
    }

    fun onMinRangeSliderValueChange(value: Float) {
        applyMinRangeAmountSliderValue(value)
        updateAmountLimitInfo()
    }

    fun onMaxRangeSliderValueChange(value: Float) {
        applyMaxRangeAmountSliderValue(value)
        updateAmountLimitInfo()
    }

    private fun updateAmountLimitInfo() {
        if (isBuy.value) {
            updateBuyersAmountLimitInfo()
        } else {
            updateSellerAmountLimitInfo()
        }
    }

    fun onRangeAmountSliderChanged(value: ClosedFloatingPointRange<Float>) {
        applyRangeAmountSliderValue(value)
        updateAmountLimitInfo()
    }

    fun getFractionForFiat(value: Double): Float {
        val range = maxAmount - minAmount
        val inFraction = ((value * 10000) - minAmount) / range
        return inFraction.toFloat()
    }

    private fun updateBuyersAmountLimitInfo() {
        if (!isBuy.value) {
            return
        }

        _reputationBasedMaxValue.value = null
        val market = createOfferModel.market ?: return

        val fixedOrMinAmount: FiatVO = if (amountType.value == AmountType.FIXED_AMOUNT) {
            quoteSideFixedAmount
        } else {
            quoteSideMinRangeAmount
        }

        val requiredReputation: Long = findRequiredReputationScoreByFiatAmount(
            marketPriceServiceFacade,
            market,
            fixedOrMinAmount
        ) ?: 0L
        _requiredReputation.value = requiredReputation

        presenterScope.launch {
            val peersScoreByUserProfileId = withContext(IODispatcher) {
                getPeersScoreByUserProfileId()
            }
            val numPotentialTakers =
                peersScoreByUserProfileId.filter { (_, value) -> withTolerance(value) >= requiredReputation }.count()
            _shouldShowWarningIcon.value = numPotentialTakers == 0

            val numSellers = "bisqEasy.tradeWizard.amount.buyer.numSellers".i18nPlural(numPotentialTakers)
            _amountLimitInfo.value = "bisqEasy.tradeWizard.amount.buyer.limitInfo".i18n(numSellers)

            val highestScore = peersScoreByUserProfileId.maxOfOrNull { it.value } ?: 0L
            val highestPossibleAmountFromSellers =
                getReputationBasedQuoteSideAmount(marketPriceServiceFacade, market, highestScore)?.value ?: 0
            val highestPossibleAmountWithTolerance: Float = withTolerance(highestPossibleAmountFromSellers).toFloat()
            val range = maxAmount - minAmount
            _rightMarkerValue.value = (highestPossibleAmountWithTolerance - minAmount) / range

            val formattedFixedOrMinAmount =
                AmountFormatter.formatAmount(fixedOrMinAmount, useLowPrecision = true, withCode = true)
            val firstPart: String =
                "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.firstPart".i18n(
                    formattedFixedOrMinAmount,
                    requiredReputation
                )
            val secondPart = if (numPotentialTakers == 0) {
                "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.secondPart.noSellers".i18n()
            } else {
                if (numPotentialTakers == 1)
                    "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.secondPart.singular".i18n(numSellers)
                else
                    "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.secondPart.plural".i18n(numSellers)
            }
            _amountLimitInfoOverlayInfo.value = firstPart + "\n\n" + secondPart + "\n\n"
        }
    }

    private fun updateSellerAmountLimitInfo() {
        val range = maxAmount - minAmount
        presenterScope.launch {
            val userProfile: UserProfileVO? = withContext(IODispatcher) {
                userProfileServiceFacade.getSelectedUserProfile()
            }

            userProfile?.let { profile ->
                val reputationScore: ReputationScoreVO? = withContext(IODispatcher) {
                    reputationServiceFacade.getReputation(profile.id).getOrNull()
                }
                reputationScore?.let { reputation ->
                    _requiredReputation.value = reputation.totalScore
                    val market = createOfferModel.market ?: return@launch
                    getReputationBasedQuoteSideAmount(
                        marketPriceServiceFacade,
                        market,
                        _requiredReputation.value
                    )?.let { amount ->
                        val reputationBasedMaxValue = (amount.value.toFloat() - minAmount) / range
                        _reputationBasedMaxValue.value = reputationBasedMaxValue
                        _rightMarkerValue.value = reputationBasedMaxValue

                        _formattedReputationBasedMaxAmount.value = AmountFormatter.formatAmount(
                            amount,
                            useLowPrecision = true,
                            withCode = true
                        )
                        _amountLimitInfo.value =
                            "bisqEasy.tradeWizard.amount.seller.limitInfo".i18n(_formattedReputationBasedMaxAmount.value)
                    }
                }
            }
        }
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onNext() {
        commitToModel()
        navigateTo(Routes.CreateOfferPrice)
    }

    fun navigateToReputation() {
        disableInteractive()
        navigateToUrl("https://bisq.wiki/Reputation")
        enableInteractive()
    }

    fun navigateToBuildReputation() {
        disableInteractive()
        navigateToUrl("https://bisq.wiki/Reputation#How_to_build_reputation")
        enableInteractive()
    }

    fun validateFiatField(value: String): String? {
        val _value = value.toDoubleOrNullLocaleAware()

        when {
            _value == null -> return "Invalid number" // TODO: i18n
            _value * 10000 < minAmount -> return "Should be greater than ${minAmount / 10000.0}" // TODO: i18n
            _value * 10000 > maxAmount -> return "Should be less than ${maxAmount / 10000.0}" // TODO: i18n
            else -> return null
        }

    }

    private suspend fun getNumPotentialTakers(requiredReputationScore: Long): Int {
        return getPeersScoreByUserProfileId().filter { (_, value) -> withTolerance(value) >= requiredReputationScore }
            .count()
    }

    private suspend fun getPeersScoreByUserProfileId(): Map<String, Long> {
        val myProfileIds = userProfileServiceFacade.getUserIdentityIds()
        val scoreByUserProfileId = reputationServiceFacade.getScoreByUserProfileId().getOrNull() ?: emptyMap()
        return scoreByUserProfileId.filter { (key, _) -> !myProfileIds.contains(key) }

    }

    private fun applyRangeAmountSliderValue(rangeSliderPosition: ClosedFloatingPointRange<Float>) {
        this.rangeSliderPosition = rangeSliderPosition

        val range = maxAmount - minAmount
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)

        val min = rangeSliderPosition.start;
        val minValue: Float = minAmount + (min * range)
        val roundedMinQuoteValue: Long = (minValue / 10000f).roundToLong() * 10000

        quoteSideMinRangeAmount = FiatVOFactory.from(roundedMinQuoteValue, quoteCurrencyCode)
        _formattedQuoteSideMinRangeAmount.value = AmountFormatter.formatAmount(quoteSideMinRangeAmount)

        baseSideMinRangeAmount =
            priceQuote.toBaseSideMonetary(quoteSideMinRangeAmount) as CoinVO
        _formattedBaseSideMinRangeAmount.value = AmountFormatter.formatAmount(baseSideMinRangeAmount, false)

        val max = rangeSliderPosition.endInclusive
        val maxValue: Float = minAmount + (max * range)
        val roundedMaxQuoteValue: Long = (maxValue / 10000f).roundToLong() * 10000

        quoteSideMaxRangeAmount = FiatVOFactory.from(roundedMaxQuoteValue, quoteCurrencyCode)
        _formattedQuoteSideMaxRangeAmount.value = AmountFormatter.formatAmount(quoteSideMaxRangeAmount)

        baseSideMaxRangeAmount =
            priceQuote.toBaseSideMonetary(quoteSideMaxRangeAmount) as CoinVO
        _formattedBaseSideMaxRangeAmount.value = AmountFormatter.formatAmount(baseSideMaxRangeAmount, false)
    }


    private fun applyMinRangeAmountSliderValue(amount: Float) {
        minRangeInitialSliderValue = amount
        quoteSideMinRangeAmount = FiatVOFactory.from(sliderValueToAmount(minRangeInitialSliderValue), quoteCurrencyCode)
        _formattedQuoteSideMinRangeAmount.value = AmountFormatter.formatAmount(quoteSideMinRangeAmount)
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
        baseSideMinRangeAmount = priceQuote.toBaseSideMonetary(quoteSideMinRangeAmount) as CoinVO
        _formattedBaseSideMinRangeAmount.value = AmountFormatter.formatAmount(baseSideMinRangeAmount, false)
    }

    private fun applyMaxRangeAmountSliderValue(amount: Float) {
        maxRangeInitialSliderValue = amount
        quoteSideMaxRangeAmount = FiatVOFactory.from(sliderValueToAmount(maxRangeInitialSliderValue), quoteCurrencyCode)
        _formattedQuoteSideMaxRangeAmount.value = AmountFormatter.formatAmount(quoteSideMaxRangeAmount)
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
        baseSideMaxRangeAmount = priceQuote.toBaseSideMonetary(quoteSideMaxRangeAmount) as CoinVO
        _formattedBaseSideMaxRangeAmount.value = AmountFormatter.formatAmount(baseSideMaxRangeAmount, false)
    }

    private fun applyFixedAmountSliderValue(amount: Float) {
        fixedAmountSliderPosition = amount
        _fixedSliderPosition.value = amount
        quoteSideFixedAmount = FiatVOFactory.from(sliderValueToAmount(fixedAmountSliderPosition), quoteCurrencyCode)
        _formattedQuoteSideFixedAmount.value = AmountFormatter.formatAmount(quoteSideFixedAmount)
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
        baseSideFixedAmount = priceQuote.toBaseSideMonetary(quoteSideFixedAmount) as CoinVO
        _formattedBaseSideFixedAmount.value = AmountFormatter.formatAmount(baseSideFixedAmount, false)
    }

    private fun sliderValueToAmount(amount: Float): Long {
        val range = maxAmount - minAmount
        val value: Float = minAmount + (amount * range)
        return (value / 10000f).roundToLong() * 10000
    }

    private fun commitToModel() {
        createOfferPresenter.commitAmount(
            amountType.value,
            quoteSideFixedAmount,
            baseSideFixedAmount,
            quoteSideMinRangeAmount,
            baseSideMinRangeAmount,
            quoteSideMaxRangeAmount,
            baseSideMaxRangeAmount
        )
    }
}
