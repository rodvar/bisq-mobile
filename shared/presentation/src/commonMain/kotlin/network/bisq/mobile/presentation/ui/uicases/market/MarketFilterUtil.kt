package network.bisq.mobile.presentation.ui.uicases.market

import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVOExtensions.marketCodes
import network.bisq.mobile.domain.utils.Logging

/**
 * Shared utility for filtering markets across different screens
 */
object MarketFilterUtil : Logging {

    /**
     * Filters markets to only include those with available price data
     * @param markets List of markets to filter
     * @param marketPriceServiceFacade Service to check price data availability
     * @return Filtered list containing only markets with price data
     */
    fun filterMarketsWithPriceData(
        markets: List<MarketListItem>,
        marketPriceServiceFacade: MarketPriceServiceFacade
    ): List<MarketListItem> {
        return markets.filter { marketListItem ->
            val hasPriceData = marketPriceServiceFacade.findMarketPriceItem(marketListItem.market) != null
            if (!hasPriceData) {
                log.d { "Filtering out market ${marketListItem.market.marketCodes} - no price data available" }
            }
            hasPriceData
        }
    }

    /**
     * Applies search filtering to markets
     * @param markets List of markets to filter
     * @param searchText Search query
     * @return Filtered list matching search criteria
     */
    fun filterMarketsBySearch(
        markets: List<MarketListItem>,
        searchText: String
    ): List<MarketListItem> {
        return if (searchText.isBlank()) {
            markets
        } else {
            markets.filter { marketListItem ->
                marketListItem.market.quoteCurrencyCode.contains(searchText, ignoreCase = true) ||
                        marketListItem.market.quoteCurrencyName.contains(searchText, ignoreCase = true)
            }
        }
    }

    /**
     * Complete market filtering pipeline for Create Offer flow
     * Filters by price data availability, applies search, and sorts
     */
    fun filterAndSortMarketsForCreateOffer(
        markets: List<MarketListItem>,
        searchText: String,
        marketPriceServiceFacade: MarketPriceServiceFacade
    ): List<MarketListItem> {
        return markets
            .let { filterMarketsWithPriceData(it, marketPriceServiceFacade) }
            .let { sortMarketsStandard(it) }
            .let { filterMarketsBySearch(it, searchText) }
    }

    /**
     * Sorts markets using the standard Bisq sorting logic
     * @param markets List of markets to sort
     * @return Sorted list with main currencies first, then by number of offers, then alphabetically
     */
    private fun sortMarketsStandard(markets: List<MarketListItem>): List<MarketListItem> {
        return markets.sortedWith(
            compareByDescending<MarketListItem> { it.numOffers }
                .thenByDescending { OffersServiceFacade.mainCurrencies.contains(it.market.quoteCurrencyCode.lowercase()) }
                .thenBy { item ->
                    if (!OffersServiceFacade.mainCurrencies.contains(item.market.quoteCurrencyCode.lowercase())) {
                        item.market.quoteCurrencyName
                    } else null
                }
        )
    }
}
