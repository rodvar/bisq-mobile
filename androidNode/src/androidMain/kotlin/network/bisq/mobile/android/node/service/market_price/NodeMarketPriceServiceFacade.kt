package network.bisq.mobile.android.node.service.market_price

import bisq.bonded_roles.market_price.MarketPriceService
import bisq.common.observable.Pin
import bisq.presentation.formatters.PriceFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.android.node.mapping.Mappings.MarketMapping
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade

class NodeMarketPriceServiceFacade(private val applicationService: AndroidApplicationService.Provider) :
    ServiceFacade(), MarketPriceServiceFacade {

    // Dependencies
    private val marketPriceService: MarketPriceService by lazy { applicationService.bondedRolesService.get().marketPriceService }

    // Properties
    private val _selectedMarketPriceItem: MutableStateFlow<MarketPriceItem?> = MutableStateFlow(null)
    override val selectedMarketPriceItem: StateFlow<MarketPriceItem?> get() = _selectedMarketPriceItem

    private val _selectedFormattedMarketPrice = MutableStateFlow("N/A")
    override val selectedFormattedMarketPrice: StateFlow<String> = _selectedFormattedMarketPrice

    // Misc
    private var selectedMarketPin: Pin? = null
    private var marketPricePin: Pin? = null

    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()

        observeSelectedMarket()
        observeMarketPrice()
    }

    override fun deactivate() {
        selectedMarketPin?.unbind()
        selectedMarketPin = null
        marketPricePin?.unbind()
        marketPricePin = null

        super<ServiceFacade>.deactivate()
    }

    // API
    override fun selectMarket(marketListItem: MarketListItem) {
        val market = MarketMapping.toBisq2Model(marketListItem.market)
        marketPriceService.setSelectedMarket(market)
    }

    override fun findMarketPriceItem(marketVO: MarketVO): MarketPriceItem? {
        val market = MarketMapping.toBisq2Model(marketVO)
        return marketPriceService.findMarketPrice(market)
            .map { it.priceQuote }
            .map { Mappings.PriceQuoteMapping.fromBisq2Model(it) }
            .map { priceQuoteVO ->
                val formattedPrice = MarketPriceFormatter.format(priceQuoteVO.value, marketVO)
                MarketPriceItem(marketVO, priceQuoteVO, formattedPrice)
            }
            .orElse(null)
    }

    override fun findUSDMarketPriceItem(): MarketPriceItem? {
        return findMarketPriceItem(MarketVOFactory.USD)
    }

    override fun refreshSelectedFormattedMarketPrice() {
        updateMarketPriceItem()
    }

    // Private
    private fun observeMarketPrice() {
        marketPricePin = marketPriceService.marketPriceByCurrencyMap.addObserver(Runnable { updateMarketPriceItem() })
    }

    private fun observeSelectedMarket() {
        selectedMarketPin?.unbind()
        selectedMarketPin = marketPriceService.selectedMarket.addObserver { market ->
            try {
                updateMarketPriceItem()
            } catch (e: Exception) {
                log.e("Failed to update market item", e)
            }
        }
    }

    private fun updateMarketPriceItem() {
        val market = marketPriceService.selectedMarket.get()
        if (market != null) {
            marketPriceService.findMarketPriceQuote(market)
                .ifPresent { priceQuote ->
                    val marketVO = MarketMapping.fromBisq2Model(market)
                    val formattedPrice = PriceFormatter.formatWithCode(priceQuote)
                    _selectedFormattedMarketPrice.value = formattedPrice
                    val priceQuoteVO = Mappings.PriceQuoteMapping.fromBisq2Model(priceQuote)
                    _selectedMarketPriceItem.value = MarketPriceItem(marketVO, priceQuoteVO, formattedPrice)
                }
        }
    }
}