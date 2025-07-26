package network.bisq.mobile.android.node.service.trades

import bisq.account.payment_method.BitcoinPaymentRail
import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.ChatChannelDomain
import bisq.chat.ChatChannelSelectionService
import bisq.chat.ChatService
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannelService
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.chat.priv.LeavePrivateChatManager
import bisq.common.monetary.Monetary
import bisq.common.observable.Pin
import bisq.common.observable.collection.CollectionObserver
import bisq.contract.bisq_easy.BisqEasyContract
import bisq.i18n.Res
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.offer.payment_method.BitcoinPaymentMethodSpec
import bisq.offer.payment_method.FiatPaymentMethodSpec
import bisq.offer.payment_method.PaymentMethodSpecUtil
import bisq.support.mediation.MediationRequestService
import bisq.trade.bisq_easy.BisqEasyTrade
import bisq.trade.bisq_easy.BisqEasyTradeService
import bisq.trade.bisq_easy.protocol.BisqEasyProtocol
import bisq.trade.bisq_easy.protocol.BisqEasyTradeState
import bisq.user.banned.BannedUserService
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfile
import bisq.user.profile.UserProfileService
import bisq.user.reputation.ReputationService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.android.node.mapping.TradeItemPresentationDtoFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.service.trades.TradeSynchronizationHelper
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Node implementation of TradesServiceFacade with enhanced trade state synchronization.
 *
 * **Trade Notification Bug Fix**: This class includes a comprehensive solution to address
 * the issue where trade completion notifications are missed when the mobile app is killed
 * and restarted.
 *
 * **Key Features**:
 * - Automatic trade state synchronization on app restart
 * - Intelligent detection of stale trades that may have missed updates
 * - Non-intrusive sync requests via existing chat infrastructure
 * - Proactive notifications for trades requiring attention
 *
 * **How It Works**:
 * 1. On service activation, waits 2 seconds then runs synchronization
 * 2. Identifies trades that might have missed state updates based on age and state
 * 3. Sends sync requests via chat messages to trigger peer message processing
 * 4. Trade states update automatically without requiring manual user interaction
 *
 * **Benefits**:
 * - Eliminates the need for users to manually send chat messages to "unstick" trades
 * - Provides automatic recovery from missed trade completion messages
 * - Maintains backward compatibility with existing trade flow
 */
class NodeTradesServiceFacade(applicationService: AndroidApplicationService.Provider) :
    ServiceFacade(), TradesServiceFacade {

    // Dependencies
    private val marketPriceService: MarketPriceService by lazy { applicationService.bondedRolesService.get().marketPriceService }
    private val bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService by lazy {
        applicationService.chatService.get().bisqEasyOfferbookChannelService
    }
    private val bannedUserService: BannedUserService by lazy { applicationService.userService.get().bannedUserService }
    private val chatService: ChatService by lazy { applicationService.chatService.get() }
    private val bisqEasyOpenTradeChannelService: BisqEasyOpenTradeChannelService by lazy { applicationService.chatService.get().bisqEasyOpenTradeChannelService }
    private val leavePrivateChatManager: LeavePrivateChatManager by lazy { applicationService.chatService.get().leavePrivateChatManager }
    private val bisqEasyTradeService: BisqEasyTradeService by lazy { applicationService.tradeService.get().bisqEasyTradeService }
    private val mediationRequestService: MediationRequestService by lazy { applicationService.supportService.get().mediationRequestService }
    private val userIdentityService: UserIdentityService by lazy { applicationService.userService.get().userIdentityService }
    private val userProfileService: UserProfileService by lazy { applicationService.userService.get().userProfileService }
    private val reputationService: ReputationService by lazy { applicationService.userService.get().reputationService }

    // Properties
    private val _openTradeItems = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
    override val openTradeItems: StateFlow<List<TradeItemPresentationModel>> get() = _openTradeItems

    private val _selectedTrade = MutableStateFlow<TradeItemPresentationModel?>(null)
    override val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = _selectedTrade

    // Misc
    private var tradesPin: Pin? = null
    private var channelsPin: Pin? = null
    private val pinsByTradeId: MutableMap<String, MutableSet<Pin>> = mutableMapOf()

    override fun activate() {
        super<ServiceFacade>.activate()

        tradesPin = bisqEasyTradeService.trades.addObserver(object : CollectionObserver<BisqEasyTrade?> {
            override fun add(trade: BisqEasyTrade?) {
                if (trade != null) {
                    handleTradeAdded(trade)
                }
            }

            override fun remove(element: Any) {
                if (element is BisqEasyTrade) {
                    handleTradeRemoved(element)
                }
            }

            override fun clear() {
                handleTradesCleared()
            }
        })

        // Trigger trade state synchronization after activation
        serviceScope.launch {
            delay(2000) // Wait for network to be ready
            synchronizeTradeStates()
        }

        channelsPin = bisqEasyOpenTradeChannelService.channels.addObserver(object : CollectionObserver<BisqEasyOpenTradeChannel?> {
            override fun add(channel: BisqEasyOpenTradeChannel?) {
                if (channel != null) {
                    handleChannelAdded(channel)
                }
            }

            override fun remove(element: Any) {
                if (element is BisqEasyOpenTradeChannel) {
                    handleChannelRemoved(element)
                }
            }

            override fun clear() {
                handleChannelsCleared()
            }
        })
    }

    override fun deactivate() {
        channelsPin?.unbind()
        tradesPin?.unbind()

        unbindAllPinsByTradeId()

        super<ServiceFacade>.deactivate()
    }

    // API

    override suspend fun takeOffer(
        bisqEasyOffer: BisqEasyOfferVO,
        takersBaseSideAmount: MonetaryVO,
        takersQuoteSideAmount: MonetaryVO,
        bitcoinPaymentMethod: String,
        fiatPaymentMethod: String,
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>
    ): Result<String> {
        try {
            val tradeId = doTakeOffer(
                Mappings.BisqEasyOfferMapping.toBisq2Model(bisqEasyOffer),
                Mappings.MonetaryMapping.toBisq2Model(takersBaseSideAmount),
                Mappings.MonetaryMapping.toBisq2Model(takersQuoteSideAmount),
                BitcoinPaymentMethodSpec(PaymentMethodSpecUtil.getBitcoinPaymentMethod(bitcoinPaymentMethod)),
                FiatPaymentMethodSpec(PaymentMethodSpecUtil.getFiatPaymentMethod(fiatPaymentMethod)),
                takeOfferStatus,
                takeOfferErrorMessage
            )
            return Result.success(tradeId)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override fun selectOpenTrade(tradeId: String) {
        _selectedTrade.value = openTradeItems.value
            .firstOrNull { it.tradeId == tradeId }
    }

    override suspend fun rejectTrade(): Result<Unit> {
        try {
            val (channel, trade, userName) = getTradeChannelUserNameTriple()
            val encoded: String = Res.encode("bisqEasy.openTrades.tradeLogMessage.rejected", userName)
            bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel).join()
            bisqEasyTradeService.rejectTrade(trade)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun cancelTrade(): Result<Unit> {
        try {
            val (channel, trade, userName) = getTradeChannelUserNameTriple()
            val encoded: String = Res.encode("bisqEasy.openTrades.tradeLogMessage.cancelled", userName)
            bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel).join()
            bisqEasyTradeService.cancelTrade(trade)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun closeTrade(): Result<Unit> {
        try {
            val (channel, trade, userName) = getTradeChannelUserNameTriple()
            bisqEasyTradeService.removeTrade(trade)
            leavePrivateChatManager.leaveChannel(channel)
            _selectedTrade.value = null
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // Phase 1
    override suspend fun sellerSendsPaymentAccount(paymentAccountData: String): Result<Unit> {
        try {
            val (channel, trade, userName) = getTradeChannelUserNameTriple()
            val encoded = Res.encode(
                "bisqEasy.tradeState.info.seller.phase1.tradeLogMessage",
                channel.myUserIdentity.userName,
                paymentAccountData
            )
            bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel)
            bisqEasyTradeService.sellerSendsPaymentAccount(trade, paymentAccountData)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun buyerSendBitcoinPaymentData(bitcoinPaymentData: String): Result<Unit> {
        try {
            val (channel, trade, userName) = getTradeChannelUserNameTriple()
            val paymentMethod = selectedTrade.value!!.bisqEasyTradeModel.contract.baseSidePaymentMethodSpec.paymentMethod
            val key = "bisqEasy.tradeState.info.buyer.phase1a.tradeLogMessage.$paymentMethod"
            val encoded = Res.encode(
                key,
                userName,
                bitcoinPaymentData
            )
            bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel)
            bisqEasyTradeService.buyerSendBitcoinPaymentData(trade, bitcoinPaymentData)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // Phase 2
    override suspend fun sellerConfirmFiatReceipt(): Result<Unit> {
        try {
            val (channel, trade, userName) = getTradeChannelUserNameTriple()
            val encoded = Res.encode(
                "bisqEasy.tradeState.info.seller.phase2b.tradeLogMessage",
                userName,
                selectedTrade.value!!.formattedQuoteAmount
            )
            bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel)
            bisqEasyTradeService.sellerConfirmFiatReceipt(trade)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun buyerConfirmFiatSent(): Result<Unit> {
        try {
            val (channel, trade, userName) = getTradeChannelUserNameTriple()
            val encoded = Res.encode(
                "bisqEasy.tradeState.info.buyer.phase2a.tradeLogMessage",
                userName,
                selectedTrade.value!!.quoteCurrencyCode
            )
            bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel)
            bisqEasyTradeService.buyerConfirmFiatSent(trade)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // Phase 3
    override suspend fun sellerConfirmBtcSent(paymentProof: String?): Result<Unit> {
        try {
            val (channel, trade, userName) = getTradeChannelUserNameTriple()
            val encoded: String
            val paymentMethod = trade.contract.baseSidePaymentMethodSpec.paymentMethod
            val paymentRailName = paymentMethod.paymentRail.name
            val proofType = Res.get("bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.paymentProof.$paymentRailName")
            encoded = if (paymentProof == null) {
                Res.encode(
                    "bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage.noProofProvided",
                    userName
                )
            } else {
                Res.encode(
                    "bisqEasy.tradeState.info.seller.phase3a.tradeLogMessage",
                    userName,
                    proofType,
                    paymentProof
                )
            }

            bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel)
            bisqEasyTradeService.sellerConfirmBtcSent(trade, Optional.ofNullable(paymentProof))
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun btcConfirmed(): Result<Unit> {
        try {
            val (channel, trade, userName) = getTradeChannelUserNameTriple()
            val paymentRail = trade.contract.baseSidePaymentMethodSpec.paymentMethod.paymentRail
            if (paymentRail == BitcoinPaymentRail.LN && trade.isBuyer) {
                val encoded = Res.encode(
                    "bisqEasy.tradeState.info.buyer.phase3b.tradeLogMessage.ln",
                    userName
                )
                bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel)
            }
            bisqEasyTradeService.btcConfirmed(trade)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun exportTradeDate(): Result<Unit> {
        //todo
        return Result.success(Unit)
    }

    override fun resetSelectedTradeToNull() {
        _selectedTrade.value = null
    }

    // Private
    private suspend fun doTakeOffer(
        bisqEasyOffer: BisqEasyOffer,
        takersBaseSideAmount: Monetary,
        takersQuoteSideAmount: Monetary,
        bitcoinPaymentMethodSpec: BitcoinPaymentMethodSpec,
        fiatPaymentMethodSpec: FiatPaymentMethodSpec,
        takeOfferStatus: MutableStateFlow<TakeOfferStatus?>,
        takeOfferErrorMessage: MutableStateFlow<String?>
    ): String {
        var errorMessagePin: Pin? = null
        var peersErrorMessagePin: Pin? = null
        try {
//            FIXME api doesn't exist anymore with latest bisq2 jars API changes
//            check(!bannedUserService.isNetworkIdBanned(bisqEasyOffer.makerNetworkId)) { "You cannot take the offer because the maker is banned" }
            val takerIdentity = userIdentityService.selectedUserIdentity
            check(!bannedUserService.isUserProfileBanned(takerIdentity.userProfile)) // if taker is banned we don't give him hints.
            val mediator = mediationRequestService.selectMediator(bisqEasyOffer.makersUserProfileId, takerIdentity.id, bisqEasyOffer.id)
            val priceSpec = bisqEasyOffer.priceSpec
            val marketPrice: Long = marketPriceService.findMarketPrice(bisqEasyOffer.market).map { it.priceQuote.value }.orElse(0)
            val bisqEasyProtocol: BisqEasyProtocol = bisqEasyTradeService.createBisqEasyProtocol(
                takerIdentity.identity,
                bisqEasyOffer,
                takersBaseSideAmount,
                takersQuoteSideAmount,
                bitcoinPaymentMethodSpec,
                fiatPaymentMethodSpec,
                mediator,
                priceSpec,
                marketPrice
            )
            val bisqEasyTrade: BisqEasyTrade = bisqEasyProtocol.model
            log.i { "Selected mediator for trade ${bisqEasyTrade.shortId}: ${mediator.map(UserProfile::getUserName).orElse("N/A")}" }

            val tradeId = bisqEasyTrade.id

            errorMessagePin = bisqEasyTrade.errorMessageObservable().addObserver { message: String? ->
                if (message != null) {
                    takeOfferErrorMessage.value = Res.get(
                        "bisqEasy.openTrades.failed.popup",
                        message,
                        bisqEasyTrade.errorStackTrace.take(500)
                    )
                }
            }
            peersErrorMessagePin = bisqEasyTrade.peersErrorMessageObservable().addObserver { peersErrorMessage: String? ->
                if (peersErrorMessage != null) {
                    takeOfferErrorMessage.value = Res.get(
                        "bisqEasy.openTrades.failedAtPeer.popup",
                        peersErrorMessage,
                        bisqEasyTrade.peersErrorStackTrace.take(500)
                    )
                }
            }

            bisqEasyTradeService.takeOffer(bisqEasyTrade)
            takeOfferStatus.value = TakeOfferStatus.SENT
            val contract: BisqEasyContract = bisqEasyTrade.contract

            // We have 120 seconds socket timeout, so we should never get triggered here, as the message will be sent as mailbox message
            withTimeout(150.seconds) {
                this@NodeTradesServiceFacade.bisqEasyOpenTradeChannelService.sendTakeOfferMessage(tradeId, bisqEasyOffer, contract.mediator)
                    .thenAccept { result ->
                        // In case the user has switched to another market we want to select that market in the offer book
                        val chatChannelSelectionService: ChatChannelSelectionService =
                            chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK)
                        bisqEasyOfferbookChannelService.findChannel(contract.offer.market)
                            .ifPresent { chatChannel: BisqEasyOfferbookChannel? -> chatChannelSelectionService.selectChannel(chatChannel) }
                        takeOfferStatus.value = TakeOfferStatus.SUCCESS
                        this@NodeTradesServiceFacade.bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId)
                            .ifPresent { channel ->
                                val taker = userIdentityService.selectedUserIdentity.userProfile.userName
                                val maker: String = channel.peer.userName
                                val encoded = Res.encode("bisqEasy.takeOffer.tradeLogMessage", taker, maker)
                                this@NodeTradesServiceFacade.bisqEasyOpenTradeChannelService.sendTradeLogMessage(encoded, channel)
                            }
                    }
                    .get()
            }
            return tradeId
        } catch (e: Exception) {
            log.e { "doTakeOffer failed $e" }
            throw e
        } finally {
            errorMessagePin?.unbind()
            peersErrorMessagePin?.unbind()
        }
    }

    // Trade
    private fun handleTradeAdded(trade: BisqEasyTrade) {
        val tradeId = trade.id
        val findChannelByTradeId: Optional<BisqEasyOpenTradeChannel> = bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId)
        if (findChannelByTradeId.isPresent) {
            handleTradeAndChannelAdded(trade, findChannelByTradeId.get())
        } else {
            log.w { "Trade with id $tradeId was added but associated channel is not found." }
        }
    }

    private fun handleTradeRemoved(trade: BisqEasyTrade) {
        val tradeId = trade.id
        val findChannelByTradeId = bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId)
        if (findChannelByTradeId.isPresent) {
            handleTradeAndChannelRemoved(trade)
        } else {
            if (!findListItem(trade).isPresent) {
                log.w {
                    "Trade with id $tradeId was removed but associated channel and listItem is not found. " +
                            "We ignore that call."
                }
            } else {
                log.w {
                    "Trade with id $tradeId was removed but associated channel is not found but a listItem with that trade is still present." +
                            "We call handleTradeAndChannelRemoved."
                }
                handleTradeAndChannelRemoved(trade)
            }
        }
    }

    private fun handleTradesCleared() {
        handleClearTradesAndChannels()
    }

    // Channel
    private fun handleChannelAdded(channel: BisqEasyOpenTradeChannel) {
        val tradeId = channel.tradeId
        val optionalTrade = bisqEasyTradeService.findTrade(tradeId)
        if (optionalTrade.isPresent) {
            handleTradeAndChannelAdded(optionalTrade.get(), channel)
        } else {
            log.w { "Channel with tradeId $tradeId was added but associated trade is not found." }
        }
    }

    private fun handleChannelRemoved(channel: BisqEasyOpenTradeChannel) {
        val tradeId = channel.tradeId
        val optionalTrade = bisqEasyTradeService.findTrade(tradeId)
        if (optionalTrade.isPresent) {
            handleTradeAndChannelRemoved(optionalTrade.get())
        } else {
            val trade = bisqEasyTradeService.findTrade(tradeId)
            if (!trade.isPresent) {
                log.d {
                    "Channel with tradeId $tradeId was removed but associated trade and the listItem is not found. " +
                            "This is expected as we first remove the trade and then the channel."
                }
            } else {
                log.w {
                    "Channel with tradeId $tradeId was removed but associated trade is not found but we still have the listItem with that trade. " +
                            "We call handleTradeAndChannelRemoved."
                }
                handleTradeAndChannelRemoved(trade.get())
            }
        }
    }

    private fun handleChannelsCleared() {
        handleClearTradesAndChannels()
    }

    // TradeAndChannel
    private fun handleTradeAndChannelAdded(trade: BisqEasyTrade, channel: BisqEasyOpenTradeChannel) {
        if (findListItem(trade).isPresent) {
            log.d {
                "We got called handleTradeAndChannelAdded but we have that trade list item already. " +
                        "This is expected as we get called both when a trade is added and the associated channel."
            }
            return
        }

        val tradeItemPresentationVO = TradeItemPresentationDtoFactory.create(trade, channel, userProfileService, reputationService)
        val openTradeItem = TradeItemPresentationModel(tradeItemPresentationVO)
        _openTradeItems.update { it + openTradeItem }

        val tradeId = trade.id
        pinsByTradeId[tradeId]?.forEach { it.unbind() }
        val pins = mutableSetOf<Pin>()
        pinsByTradeId[tradeId] = pins

        //openTradeItems
        pins += trade.tradeStateObservable().addObserver { tradeState ->
            openTradeItem.bisqEasyTradeModel.tradeState.value = Mappings.BisqEasyTradeStateMapping.fromBisq2Model(tradeState)
        }
        pins += trade.interruptTradeInitiator.addObserver { interruptTradeInitiator ->
            if (interruptTradeInitiator != null) {
                openTradeItem.bisqEasyTradeModel.interruptTradeInitiator.value =
                    Mappings.RoleMapping.fromBisq2Model(interruptTradeInitiator)
            }
        }
        pins += trade.paymentAccountData.addObserver { value ->
            if (value != null) {
                openTradeItem.bisqEasyTradeModel.paymentAccountData.value = value
            }
        }
        pins += trade.bitcoinPaymentData.addObserver { value ->
            if (value != null) {
                openTradeItem.bisqEasyTradeModel.bitcoinPaymentData.value = value
            }
        }
        pins += trade.paymentProof.addObserver { value ->
            if (value != null) {
                openTradeItem.bisqEasyTradeModel.paymentProof.value = value
            }
        }
        pins += trade.errorMessageObservable().addObserver { value ->
            if (value != null) {
                openTradeItem.bisqEasyTradeModel.errorMessage.value = value
            }
        }
        pins += trade.errorStackTraceObservable().addObserver { value ->
            if (value != null) {
                openTradeItem.bisqEasyTradeModel.errorStackTrace.value = value
            }
        }
        pins += trade.peersErrorMessageObservable().addObserver { value ->
            if (value != null) {
                openTradeItem.bisqEasyTradeModel.peersErrorMessage.value = value
            }
        }
        pins += trade.peersErrorStackTraceObservable().addObserver { value ->
            if (value != null) {
                openTradeItem.bisqEasyTradeModel.peersErrorStackTrace.value = value
            }
        }

        pins += channel.isInMediationObservable().addObserver { isInMediation ->
            if (isInMediation != null) {
                openTradeItem.bisqEasyOpenTradeChannelModel.setIsMediator(isInMediation)
            }
        }
    }

    private fun handleTradeAndChannelRemoved(trade: BisqEasyTrade) {
        val tradeId = trade.id
        if (!findListItem(trade).isPresent) {
            log.w { "We got called handleTradeAndChannelRemoved but we have not found any trade list item with tradeId $tradeId" }
            return
        }

        val item = findListItem(trade).get()
        _openTradeItems.update { it - item }

        unbindPinByTradeId(tradeId)
    }

    private fun handleClearTradesAndChannels() {
        _openTradeItems.value = emptyList()
        _selectedTrade.value = null
        unbindAllPinsByTradeId()
    }

    // Misc
    private fun findListItem(trade: BisqEasyTrade): Optional<TradeItemPresentationModel> {
        return findListItem(trade.id)
    }

    private fun findListItem(tradeId: String): Optional<TradeItemPresentationModel> {
        return openTradeItems.value.stream()
            .filter { it.bisqEasyTradeModel.id == tradeId }
            .findAny()
    }

    private fun unbindPinByTradeId(tradeId: String) {
        if (pinsByTradeId.containsKey(tradeId)) {
            pinsByTradeId[tradeId]?.forEach { it.unbind() }
            pinsByTradeId.remove(tradeId)
        }
    }

    private fun unbindAllPinsByTradeId() {
        pinsByTradeId.values.forEach { pins -> pins.forEach { it.unbind() } }
        pinsByTradeId.clear()
    }

    private fun getTradeChannelUserNameTriple(): Triple<BisqEasyOpenTradeChannel, BisqEasyTrade, String> {
        val tradeId = requireNotNull(selectedTrade.value) { "Selected trade must not be null" }.tradeId
        val channel =
            requireNotNull(bisqEasyOpenTradeChannelService.findChannelByTradeId(tradeId).getOrNull()) { "Channel must not be null" }
        val trade = requireNotNull(bisqEasyTradeService.findTrade(tradeId).getOrNull()) { "Trade must not be null" }
        val userName = channel.myUserIdentity.userName
        return Triple(channel, trade, userName)
    }

    /**
     * Synchronizes trade states after app restart to ensure we haven't missed any state changes
     * while the app was killed.
     *
     * **Problem**: When the mobile app is killed while a trade is in progress, and the peer
     * completes the trade from desktop, the mobile app doesn't receive notifications about
     * the trade completion when it restarts. The trade remains stuck in an intermediate state
     * until the user manually sends a chat message.
     *
     * **Root Cause**: Trade protocol messages and chat messages are separate systems. When the
     * app is killed, trade completion messages may not be processed properly on restart. Chat
     * activity triggers message processing, which is why sending a chat message "fixes" the issue.
     *
     * **Solution**: This method automatically identifies trades that might have missed state
     * updates and sends sync requests via the chat system to trigger message processing.
     *
     * **Timing**: Uses shared TradeSynchronizationHelper logic optimized for ongoing trades:
     * - Quick-progress states: sync after 30 seconds
     * - All ongoing trades: sync after 60 seconds
     * - Long-running trades: sync after 5 minutes
     */
    private suspend fun synchronizeTradeStates() {
        try {
            log.i { "KMP: Starting node trade state synchronization after app restart" }

            // Convert Bisq trades to presentation models for shared logic
            val currentTrades = _openTradeItems.value
            val tradesNeedingSync = TradeSynchronizationHelper.getTradesNeedingSync(currentTrades, isAppRestart = true)

            TradeSynchronizationHelper.logSynchronizationActivity(currentTrades, tradesNeedingSync)

            if (tradesNeedingSync.isEmpty()) {
                log.d { "KMP: No trades need synchronization" }
                return
            }

            // Send sync requests for trades that need it
            tradesNeedingSync.forEach { trade ->
                try {
                    // Find the corresponding Bisq trade
                    val bisqTrade = bisqEasyTradeService.trades.find { it.id == trade.tradeId }
                    if (bisqTrade != null) {
                        requestTradeStateSync(bisqTrade)
                    } else {
                        log.w { "KMP: Could not find Bisq trade for ${trade.tradeId}" }
                    }
                } catch (e: Exception) {
                    log.e(e) { "KMP: Error requesting sync for trade ${trade.tradeId}" }
                }
            }

            log.i { "KMP: Node trade state synchronization completed" }
        } catch (e: Exception) {
            log.e(e) { "KMP: Error during node trade state synchronization" }
        }
    }



    /**
     * Gets the last activity timestamp for a trade.
     * Currently uses the trade creation time as a baseline.
     *
     * @param trade The trade to get activity timestamp for
     * @return The timestamp of the last known activity for this trade
     */
    private fun getLastTradeActivity(trade: BisqEasyTrade): Long {
        // Use the trade's creation time as fallback
        return trade.contract.takeOfferDate
    }

    /**
     * Requests a trade state synchronization by sending a chat message.
     *
     * **How it works**: Sends a system log message via the existing chat infrastructure.
     * This triggers the peer to process any pending trade protocol messages, effectively
     * synchronizing the trade state without requiring manual user interaction.
     *
     * **Non-intrusive**: Uses the existing message processing pipeline and doesn't
     * disrupt normal chat functionality.
     *
     * @param trade The trade to request synchronization for
     */
    private suspend fun requestTradeStateSync(trade: BisqEasyTrade) {
        try {
            val tradeId = trade.id
            val channel = bisqEasyOpenTradeChannelService.findChannel(tradeId)

            if (channel.isPresent) {
                log.d { "KMP: Sending sync request for trade $tradeId" }

                // Send a system message to trigger message processing
                // This will cause any pending trade protocol messages to be processed
                val syncMessage = "Synchronizing trade state..."
                bisqEasyOpenTradeChannelService.sendTradeLogMessage(syncMessage, channel.get())

                log.d { "KMP: Sync request sent for trade $tradeId" }
            } else {
                log.w { "KMP: No channel found for trade $tradeId, cannot request sync" }
            }
        } catch (e: Exception) {
            log.e(e) { "KMP: Error requesting trade state sync for trade ${trade.id}" }
        }
    }
}