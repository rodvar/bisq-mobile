package network.bisq.mobile.android.node.service.offerbook.market

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage
import bisq.common.observable.Pin
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.LifeCycleAware
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.domain.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.utils.Logging

class NodeMarketListItemService(private val applicationService: AndroidApplicationService.Provider) :
    LifeCycleAware, Logging {

    // Dependencies
    private val marketPriceService: MarketPriceService by lazy {
        applicationService.bondedRolesService.get().marketPriceService
    }
    private val bisqEasyOfferbookChannelService: BisqEasyOfferbookChannelService by lazy {
        applicationService.chatService.get().bisqEasyOfferbookChannelService
    }

    // Properties
    private val _marketListItems: List<MarketListItem> by lazy { fillMarketListItems() }
    val marketListItems: List<MarketListItem> get() = _marketListItems

    // Misc
    private var numOffersObservers: MutableList<NumOffersObserver> = mutableListOf()

    // Life cycle
    override fun activate() {
        numOffersObservers.forEach { it.resume() }
    }

    override fun deactivate() {
        numOffersObservers.forEach { it.dispose() }
    }

    // Private
    private fun fillMarketListItems(): MutableList<MarketListItem> {
        val offerbookMarketItems: MutableList<MarketListItem> = mutableListOf()
        bisqEasyOfferbookChannelService.channels
            .forEach { channel ->
                val marketVO = MarketVO(
                    channel.market.baseCurrencyCode,
                    channel.market.quoteCurrencyCode,
                    channel.market.baseCurrencyName,
                    channel.market.quoteCurrencyName,
                )

                // We convert channel.market to our replicated Market model
                val offerbookMarketItem = MarketListItem(marketVO)

                val market = Mappings.MarketMapping.toPojo(marketVO)
                if (marketPriceService.marketPriceByCurrencyMap.isEmpty() ||
                    marketPriceService.marketPriceByCurrencyMap.containsKey(market)
                ) {
                    offerbookMarketItems.add(offerbookMarketItem)
                    val numOffersObserver = NumOffersObserver(channel, offerbookMarketItem::setNumOffers)
                    numOffersObservers.add(numOffersObserver)
                }
            }
        return offerbookMarketItems
    }

    // Inner class
    inner class NumOffersObserver(
        private val channel: BisqEasyOfferbookChannel,
        val setNumOffers: (Int) -> Unit
    ) {
        private var channelPin: Pin? = null

        init {
            channelPin = channel.chatMessages.addObserver { this.updateNumOffers() }
        }

        fun resume() {
            dispose()
            channelPin = channel.chatMessages.addObserver { this.updateNumOffers() }
        }

        fun dispose() {
            channelPin?.unbind()
            channelPin = null
        }

        private fun updateNumOffers() {
            val numOffers = channel.chatMessages.stream()
                .filter { obj: BisqEasyOfferbookMessage -> obj.hasBisqEasyOffer() }
                .count().toInt()
            setNumOffers(numOffers)
        }
    }
}