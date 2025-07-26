package network.bisq.mobile.client.service.trades

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.subscription.ModificationType
import network.bisq.mobile.client.websocket.subscription.Subscription
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.trades.TakeOfferStatus
import network.bisq.mobile.domain.service.trades.TradeSynchronizationHelper
import network.bisq.mobile.domain.service.trades.TradesServiceFacade

/**
 * Client implementation of TradesServiceFacade with enhanced trade state synchronization.
 *
 * **Trade Notification Bug Fix**: This class includes a comprehensive solution to address
 * the issue where trade completion notifications are missed when the mobile app is killed
 * and restarted.
 *
 * **Key Features**:
 * - Automatic trade state synchronization on app restart
 * - Uses existing chat API to trigger server-side message processing
 * - Intelligent timing optimized for ongoing trades (30-60 seconds)
 * - Shared synchronization logic with node implementation
 *
 * **How It Works**:
 * 1. On service activation, waits 2 seconds then runs synchronization
 * 2. Uses TradeSynchronizationHelper to identify trades needing sync
 * 3. Sends chat messages via existing API to trigger peer message processing
 * 4. Monitors TRADE_PROPERTIES subscription for automatic state updates
 */
class ClientTradesServiceFacade(
    private val apiGateway: TradesApiGateway,
    webSocketClientProvider: WebSocketClientProvider,
    json: Json
) : ServiceFacade(), TradesServiceFacade {

    // Cache for trade properties received before trades list is populated
    private val pendingTradeProperties = mutableMapOf<String, TradePropertiesDto>()

    // Properties
    private val _openTradeItems = MutableStateFlow<List<TradeItemPresentationModel>>(emptyList())
    override val openTradeItems: StateFlow<List<TradeItemPresentationModel>> get() = _openTradeItems

    private val _selectedTrade = MutableStateFlow<TradeItemPresentationModel?>(null)
    override val selectedTrade: StateFlow<TradeItemPresentationModel?> get() = _selectedTrade

    // Misc
    private val tradeId get() = selectedTrade.value?.tradeId
    private val openTradesSubscription: Subscription<TradeItemPresentationDto> =
        Subscription(webSocketClientProvider, json, Topic.TRADES, this::handleTradeItemPresentationChange)

    private val tradePropertiesSubscription: Subscription<Map<String, TradePropertiesDto>> =
        Subscription(webSocketClientProvider, json, Topic.TRADE_PROPERTIES, this::handleTradePropertiesChange)

    override fun activate() {
        super<ServiceFacade>.activate()

        openTradesSubscription.subscribe()
        tradePropertiesSubscription.subscribe()

        launchTradeStateSync()
    }

    override fun deactivate() {
        openTradesSubscription.dispose()
        tradePropertiesSubscription.dispose()

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
        val apiResult = apiGateway.takeOffer(
            bisqEasyOffer.id,
            takersBaseSideAmount.value,
            takersQuoteSideAmount.value,
            bitcoinPaymentMethod,
            fiatPaymentMethod,
        )
        if (apiResult.isSuccess) {
            takeOfferStatus.value = TakeOfferStatus.SUCCESS
            return Result.success(apiResult.getOrThrow().tradeId)
        } else {
            return Result.failure(apiResult.exceptionOrNull()!!)
        }
    }

    override fun selectOpenTrade(tradeId: String) {
        _selectedTrade.value = findOpenTradeItemModel(tradeId)
    }

    override suspend fun rejectTrade(): Result<Unit> {
        return apiGateway.rejectTrade(requireNotNull(tradeId))
    }

    override suspend fun cancelTrade(): Result<Unit> {
        return apiGateway.cancelTrade(requireNotNull(tradeId))
    }

    override suspend fun closeTrade(): Result<Unit> {
        val result = apiGateway.closeTrade(requireNotNull(tradeId))
        if (result.isSuccess) {
            _selectedTrade.value = null
        }
        return result
    }

    override suspend fun sellerSendsPaymentAccount(paymentAccountData: String): Result<Unit> {
        return apiGateway.sellerSendsPaymentAccount(requireNotNull(tradeId), paymentAccountData)
    }

    override suspend fun buyerSendBitcoinPaymentData(bitcoinPaymentData: String): Result<Unit> {
        return apiGateway.buyerSendBitcoinPaymentData(requireNotNull(tradeId), bitcoinPaymentData)
    }

    override suspend fun sellerConfirmFiatReceipt(): Result<Unit> {
        return apiGateway.sellerConfirmFiatReceipt(requireNotNull(tradeId))
    }

    override suspend fun buyerConfirmFiatSent(): Result<Unit> {
        return apiGateway.buyerConfirmFiatSent(requireNotNull(tradeId))
    }

    override suspend fun sellerConfirmBtcSent(paymentProof: String?): Result<Unit> {
        return apiGateway.sellerConfirmBtcSent(requireNotNull(tradeId), paymentProof)
    }

    override suspend fun btcConfirmed(): Result<Unit> {
        return apiGateway.btcConfirmed(requireNotNull(tradeId))
    }

    override suspend fun exportTradeDate(): Result<Unit> {
        //todo
        return Result.success(Unit)
    }

    override fun resetSelectedTradeToNull() {
        _selectedTrade.value = null
    }

    // Private
    private fun handleTradeItemPresentationChange(payload: List<TradeItemPresentationDto>, modificationType: ModificationType) {
        if (modificationType == ModificationType.REPLACE ||
            modificationType == ModificationType.ADDED
        ) {
            payload.forEach { item ->
                val tradeModel = TradeItemPresentationModel(item)
                _openTradeItems.update { it + tradeModel }

                // Apply any pending trade properties for this trade
                applyPendingTradeProperties(tradeModel)
            }
        } else if (modificationType == ModificationType.REMOVED) {
            payload.forEach { item ->
                val toRemove: TradeItemPresentationModel? = findOpenTradeItemModel(item.trade.id)
                if (toRemove != null) {
                    _openTradeItems.update { it - toRemove }
                }
            }
        }
    }

    /**
     * Applies any pending trade properties for a newly loaded trade.
     */
    private fun applyPendingTradeProperties(trade: TradeItemPresentationModel) {
        val tradeId = trade.tradeId
        val shortTradeId = trade.shortTradeId

        // Check for pending properties using both full and short trade IDs
        val pendingData = pendingTradeProperties[tradeId]
            ?: pendingTradeProperties[shortTradeId]
            ?: pendingTradeProperties.entries.find { it.key.take(8) == shortTradeId }?.value

        if (pendingData != null) {
            log.i { "Applying pending trade properties for $tradeId" }
            applyTradeProperties(trade, tradeId, pendingData)

            // Remove from pending cache
            pendingTradeProperties.remove(tradeId)
            pendingTradeProperties.remove(shortTradeId)
            pendingTradeProperties.entries.removeAll { it.key.take(8) == shortTradeId }
        }
    }

    private fun handleTradePropertiesChange(payload: List<Map<String, TradePropertiesDto>>, modificationType: ModificationType) {
        log.i { "handleTradePropertiesChange called with ${payload.size} items, modificationType: $modificationType" }

        payload.flatMap { it.entries }
            .forEach { (tradeId, data) ->
                log.i { "Processing trade properties for $tradeId - state: ${data.tradeState}" }

                val trade = findOpenTradeItemModel(tradeId)
                if (trade != null) {
                    // Trade found - apply properties immediately
                    applyTradeProperties(trade, tradeId, data)
                } else {
                    // Trade not found - cache properties for later application
                    log.i { "Trade not found, caching properties for $tradeId" }
                    pendingTradeProperties[tradeId] = data
                }
            }
    }

    /**
     * Applies trade properties to a trade model.
     */
    private fun applyTradeProperties(trade: TradeItemPresentationModel, tradeId: String, data: TradePropertiesDto) {
        log.i { "Apply mutable data to trade with ID $tradeId - new state: ${data.tradeState}" }
        data.tradeState?.let {
            log.i { "Updating trade $tradeId state from ${trade.bisqEasyTradeModel.tradeState.value} to $it" }
            trade.bisqEasyTradeModel.tradeState.value = it
        }
        data.paymentAccountData?.let { trade.bisqEasyTradeModel.paymentAccountData.value = it }
        data.bitcoinPaymentData?.let { trade.bisqEasyTradeModel.bitcoinPaymentData.value = it }
        data.paymentProof?.let { trade.bisqEasyTradeModel.paymentProof.value = it }
        data.interruptTradeInitiator?.let { trade.bisqEasyTradeModel.interruptTradeInitiator.value = it }
    }

    private fun findOpenTradeItemModel(tradeId: String): TradeItemPresentationModel? {
        // First try exact match
        var result = _openTradeItems.value.find { it.tradeId == tradeId }

        // If not found, try matching by short ID (for TRADE_PROPERTIES compatibility)
        if (result == null) {
            result = _openTradeItems.value.find { it.shortTradeId == tradeId.take(8) }
            if (result != null) {
                log.d { "Found trade by short ID match: ${result.tradeId} for lookup $tradeId" }
            }
        }

        if (result == null) {
            log.w { "Could not find trade for ID: $tradeId. Available trades: ${_openTradeItems.value.map { "${it.shortTradeId}(${it.tradeId})" }}" }
        }

        return result
    }

    /**
     * Synchronizes trade states after app restart to ensure we haven't missed any state changes
     * while the app was killed.
     *
     * **Client Implementation**: Uses WebSocket re-subscription to force server to send fresh data.
     * Also logs detailed trade state information for debugging.
     *
     * **Timing**: Uses shared TradeSynchronizationHelper logic optimized for ongoing trades:
     * - Quick-progress states: sync after 30 seconds
     * - All ongoing trades: sync after 60 seconds
     * - Long-running trades: sync after 5 minutes
     */
    override suspend fun synchronizeTradeStates() {
        try {
            log.i { "Starting client trade state synchronization after app restart" }

            val currentTrades = _openTradeItems.value

            // Log detailed trade information for debugging
            currentTrades.forEach { trade ->
                val state = trade.bisqEasyTradeModel.tradeState.value
                val age = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - trade.bisqEasyTradeModel.takeOfferDate
                log.i { "Current trade ${trade.shortTradeId} - state: $state, age: ${age / 1000}s" }
            }

            val tradesNeedingSync = TradeSynchronizationHelper.getTradesNeedingSync(currentTrades, isAppRestart = true)

            TradeSynchronizationHelper.logSynchronizationActivity(currentTrades, tradesNeedingSync)

            if (tradesNeedingSync.isEmpty()) {
                log.d { "No trades need synchronization, but refreshing WebSocket once" }
                refreshWebSocketSubscriptions()
                return
            }

            // Refresh WebSocket subscriptions once to get fresh data
            log.i { "Refreshing WebSocket subscriptions for ${tradesNeedingSync.size} trades needing sync" }
            refreshWebSocketSubscriptions()

            log.i { "Client trade state synchronization completed" }
        } catch (e: Exception) {
            log.e(e) { "Error during client trade state synchronization" }
        }
    }

    /**
     * Requests a trade state synchronization by re-subscribing to WebSocket topics.
     *
     * **How it works**: Re-subscribes to TRADES and TRADE_PROPERTIES topics to force
     * the server to send fresh data. This is more reliable than chat messages because
     * it directly requests current state from the server.
     *
     * **Why this works**: When a client re-subscribes, the server calls getJsonPayload()
     * which returns the current state of all trades, ensuring we get the latest data.
     *
     * @param trade The trade to request synchronization for (currently syncs all trades)
     */
    private suspend fun requestTradeStateSync(trade: TradeItemPresentationModel) {
        try {
            val tradeId = trade.tradeId
            log.d { "Requesting client sync via WebSocket re-subscription for trade $tradeId" }

            // Re-subscribe to force server to send fresh data
            refreshWebSocketSubscriptions()

            log.d { "Client sync request completed for trade $tradeId" }
        } catch (e: Exception) {
            log.e(e) { "Error requesting client trade state sync for trade ${trade.tradeId}" }
        }
    }

    /**
     * Refreshes WebSocket subscriptions to get fresh data from the server.
     * This forces the server to send current state via getJsonPayload().
     *
     * **Important**: Only call this during app restart, not during normal operation,
     * as it can disrupt real-time updates.
     */
    private suspend fun refreshWebSocketSubscriptions() {
        try {
            log.i { "Refreshing WebSocket subscriptions to get fresh trade data" }

            // Dispose current subscriptions
            openTradesSubscription.dispose()
            tradePropertiesSubscription.dispose()

            // Wait a moment for cleanup
            delay(500) // Increased delay to ensure proper cleanup

            // Re-subscribe to get fresh data
            openTradesSubscription.subscribe()
            tradePropertiesSubscription.subscribe()

            // Wait for subscriptions to be established
            delay(1000) // Wait for WebSocket connection to be re-established

            log.i { "WebSocket subscriptions refreshed successfully" }
        } catch (e: Exception) {
            log.e(e) { "Error refreshing WebSocket subscriptions" }
        }
    }

}