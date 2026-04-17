/**
 * TradeHistoryListDesign.kt — Design PoC
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Full-screen trade history list — the mobile adaptation of Bisq2's desktop
 * BisqEasyHistoryView table (sortable columns with 11 fields).
 *
 * Mobile replaces the table with:
 *   - A search bar at the top (filter by peer name or market, e.g. "BTC/USD")
 *   - A LazyColumn of TradeHistoryCard items, newest-first
 *   - Section header showing result count
 *   - Loading skeleton state (shimmer-placeholder cards)
 *   - Empty state with call-to-action to the offerbook
 *
 * ======================================================================================
 * SEARCH / FILTER DESIGN
 * ======================================================================================
 * WHY SEARCH IS NEEDED:
 * Unlike open trades (typically 1-5 at a time), trade history accumulates indefinitely.
 * Active traders can reach 50-100+ closed trades within months. The primary use case
 * is "find that specific trade" — e.g., for dispute resolution, tax records, or to
 * verify a past transaction with a specific peer. Without search, users must scroll
 * through a reverse-chronological list, which becomes impractical at scale.
 * Desktop includes search in its history table for this reason.
 *
 * NOTE: Search can be deferred to a post-V1 release if needed. Early users will have
 * few closed trades, so the list is usable without it. Search makes it efficient at
 * scale — it's an optimization, not a blocker.
 *
 * IMPLEMENTATION:
 * The search bar is intentionally simple: a single text field filtering by peer name
 * OR market code (e.g., typing "USD" surfaces all USD trades; typing "SatoshiFan" finds
 * trades with that peer). This covers the most common "find a specific trade" use case
 * without requiring a separate filter sheet.
 *
 * A sort button (SortIcon in the rightSuffix slot of BisqSearchField) opens a bottom
 * sheet with sort options: Newest first (default), Oldest first, Amount (high-to-low),
 * Amount (low-to-high). The current sort selection is shown as a chip below the search
 * bar only when a non-default sort is active.
 *
 * ======================================================================================
 * SECTION HEADER
 * ======================================================================================
 * "N trades" subtitle renders below the search bar. When search is active it reads
 * "N of M trades". This gives the user a fast count without a loading indicator.
 * The header uses SmallLightGrey — low visual weight, purely informational.
 *
 * ======================================================================================
 * LOADING STATE
 * ======================================================================================
 * Shows 3 placeholder cards: same structure as TradeHistoryCard but with shimmer
 * placeholders instead of real data. Using a uniform shimmer height per section
 * avoids layout jump when real data loads.
 *
 * In this POC the shimmer is simulated with a semi-transparent grey surface.
 * During implementation, replace with a proper Shimmer library call or AnimatedShimmer
 * composable (already used elsewhere in the project).
 *
 * ======================================================================================
 * EMPTY STATE
 * ======================================================================================
 * Two variants:
 *   a) No trades at all — illustration + "No completed trades yet" + "Browse offers"
 *      CTA button. This is the first-run experience.
 *   b) No search results — "No trades match your search" + "Clear search" text button.
 *      Inline, no illustration needed since the user just typed something wrong.
 *
 * ======================================================================================
 * SCROLL BEHAVIOR
 * ======================================================================================
 * The search bar is pinned (not part of LazyColumn). The section count header scrolls
 * away with the list content as item 0 of the LazyColumn. This keeps the search bar
 * always accessible without covering trade cards.
 *
 * ======================================================================================
 * NAVIGATION
 * ======================================================================================
 * Tapping a card navigates to a trade detail screen (not designed in this POC).
 * The NavRoute for production implementation will likely be:
 *   data class TradeHistory(val tradeId: String) : NavRoute
 *
 * ======================================================================================
 * IMPLEMENTATION NOTES
 * ======================================================================================
 * - Presenter exposes: closedTrades: StateFlow<List<...>>, isLoading: StateFlow<Boolean>,
 *   searchQuery: StateFlow<String>, onSearchQueryChange(String), onSelectTrade(tradeId)
 * - The presenter filters client-side (no backend search endpoint needed) since the
 *   number of closed trades is bounded and lives in memory
 * - The domain source is the same TradesServiceFacade.closedTrades flow used by desktop's
 *   BisqEasyHistoryController — no new API endpoints required
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * mobile.tradeHistory.title                = "Trade History"
 * mobile.tradeHistory.count.one            = "1 trade"
 * mobile.tradeHistory.count.many           = "{0} trades"
 * mobile.tradeHistory.count.filtered       = "{0} of {1} trades"
 * mobile.tradeHistory.empty.noTrades       = "No completed trades yet"
 * mobile.tradeHistory.empty.noTrades.sub   = "Completed trades will appear here after BTC is confirmed or a trade is cancelled."
 * mobile.tradeHistory.empty.noResults      = "No trades match your search"
 * mobile.tradeHistory.search.placeholder   = "Search by peer or market"
 * mobile.tradeHistory.sort.label           = "Sort"
 * mobile.tradeHistory.sort.newestFirst     = "Newest first"
 * mobile.tradeHistory.sort.oldestFirst     = "Oldest first"
 * mobile.tradeHistory.sort.amountHighLow   = "Amount: high to low"
 * mobile.tradeHistory.sort.amountLowHigh   = "Amount: low to high"
 */
package network.bisq.mobile.presentation.design.trade_history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Screen-level composable
// -------------------------------------------------------------------------------------

/**
 * Trade history list screen.
 *
 * Renders a pinned search bar above a LazyColumn of closed trade cards.
 * Handles three states: loading, empty (two variants), and populated list.
 *
 * @param trades Full list of closed trades (pre-sorted by presenter, newest first)
 * @param isLoading True while the presenter is loading trades from the service
 * @param searchQuery Current text in the search field
 * @param onSearchQueryChange Called when user edits the search field
 * @param onSelectTrade Called with tradeId when user taps a card
 * @param onBrowseOffers Called when user taps the CTA on the no-trades empty state
 */
@Composable
internal fun TradeHistoryListScreen(
    trades: List<SimulatedTradeHistoryItem>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectTrade: (String) -> Unit,
    onBrowseOffers: () -> Unit,
) {
    // Client-side filtering: match peer name or any part of the formatted price (market)
    val filteredTrades =
        remember(trades, searchQuery) {
            if (searchQuery.isBlank()) {
                trades
            } else {
                val query = searchQuery.trim().lowercase()
                trades.filter { item ->
                    item.peerName.lowercase().contains(query) ||
                        item.formattedPrice.lowercase().contains(query) ||
                        item.fiatPaymentMethod.lowercase().contains(query) ||
                        item.quoteAmountWithCode.lowercase().contains(query)
                }
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BisqTheme.colors.backgroundColor),
    ) {
        // Pinned search bar
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                    ),
        ) {
            BisqSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Search by peer or market",
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Content area
        when {
            isLoading -> TradeHistoryLoadingState()

            filteredTrades.isEmpty() && searchQuery.isBlank() ->
                TradeHistoryEmptyState(onBrowseOffers = onBrowseOffers)

            filteredTrades.isEmpty() ->
                TradeHistoryNoResultsState(onClearSearch = { onSearchQueryChange("") })

            else ->
                TradeHistoryList(
                    trades = filteredTrades,
                    totalCount = trades.size,
                    searchQuery = searchQuery,
                    onSelectTrade = onSelectTrade,
                )
        }
    }
}

// -------------------------------------------------------------------------------------
// Populated list state
// -------------------------------------------------------------------------------------

@Composable
private fun TradeHistoryList(
    trades: List<SimulatedTradeHistoryItem>,
    totalCount: Int,
    searchQuery: String,
    onSelectTrade: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = BisqUIConstants.ScreenPadding,
                end = BisqUIConstants.ScreenPadding,
                top = BisqUIConstants.ScreenPaddingHalf,
                bottom = BisqUIConstants.ScreenPadding2X,
            ),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        // Section count header — scrolls with the list
        item(key = "header") {
            val countLabel =
                if (searchQuery.isBlank()) {
                    if (trades.size == 1) "1 trade" else "${trades.size} trades"
                } else {
                    "${trades.size} of $totalCount trades"
                }
            BisqText.SmallLightGrey(
                text = countLabel,
                modifier =
                    Modifier.padding(
                        start = BisqUIConstants.ScreenPaddingQuarter,
                        bottom = BisqUIConstants.ScreenPaddingQuarter,
                    ),
            )
        }

        itemsIndexed(trades, key = { _, item -> item.tradeId }) { _, item ->
            TradeHistoryCard(
                item = item,
                onClick = { onSelectTrade(item.tradeId) },
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// Loading state — shimmer placeholder cards
// -------------------------------------------------------------------------------------

@Composable
private fun TradeHistoryLoadingState() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPaddingHalf,
            ),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        userScrollEnabled = false,
    ) {
        items(3) {
            ShimmerTradeCard()
        }
    }
}

/**
 * Placeholder card shown during loading.
 * Uses a semi-transparent surface to approximate card dimensions without real data.
 * Replace the inner boxes with an AnimatedShimmer composable during implementation.
 */
@Composable
private fun ShimmerTradeCard() {
    val shimmerColor = BisqTheme.colors.dark_grey40
    val placeholderColor = BisqTheme.colors.dark_grey50

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(shimmerColor)
                .padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        // Outcome badge placeholder
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                    .background(placeholderColor),
        )
        BisqGap.VQuarter()
        // Peer name placeholder
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor),
        )
        // Star rating placeholder
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.30f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor),
        )
        // Date placeholder
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor),
        )
        BisqGap.VQuarter()
        // Amount placeholder (right-aligned simulation)
        Box(
            modifier =
                Modifier
                    .align(Alignment.End)
                    .fillMaxWidth(0.40f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor),
        )
    }
}

// -------------------------------------------------------------------------------------
// Empty state — no trades at all
// -------------------------------------------------------------------------------------

@Composable
private fun TradeHistoryEmptyState(onBrowseOffers: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding2X,
                    vertical = BisqUIConstants.ScreenPadding4X,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Decorative archive icon placeholder — replace with a dedicated Res.drawable.trade_history
        // icon or reuse trade_completed once available
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40),
            contentAlignment = Alignment.Center,
        ) {
            BisqText.H4LightGrey("?")
        }

        BisqGap.V2()

        BisqText.H5Light(
            text = "No completed trades yet",
            textAlign = TextAlign.Center,
        )

        BisqGap.V1()

        BisqText.SmallLightGrey(
            text = "Completed trades will appear here after BTC is confirmed or a trade is cancelled.",
            textAlign = TextAlign.Center,
        )

        BisqGap.V2()

        BisqButton(
            text = "Browse offers",
            onClick = onBrowseOffers,
        )
    }
}

// -------------------------------------------------------------------------------------
// No-results state — search returned nothing
// -------------------------------------------------------------------------------------

@Composable
private fun TradeHistoryNoResultsState(onClearSearch: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding2X,
                    vertical = BisqUIConstants.ScreenPadding3X,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        BisqGap.V2()

        BisqText.BaseLight(
            text = "No trades match your search",
            textAlign = TextAlign.Center,
        )

        BisqGap.V1()

        BisqButton(
            text = "Clear search",
            type = BisqButtonType.Grey,
            onClick = onClearSearch,
        )
    }
}

// -------------------------------------------------------------------------------------
// Preview data helpers
// -------------------------------------------------------------------------------------

private val allSampleTrades =
    listOf(
        sampleCompletedBuyerTrade,
        sampleCompletedSellerTrade,
        sampleCancelledTrade,
        sampleRejectedTrade,
        sampleFailedTrade,
    )

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/**
 * Preview: full list with mixed outcomes — the typical populated state.
 * Shows 5 trades: 2 completed, 1 cancelled, 1 rejected, 1 failed.
 */
@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 900)
@Composable
private fun List_MixedOutcomes_Preview() {
    BisqTheme.Preview {
        var searchQuery by remember { mutableStateOf("") }
        TradeHistoryListScreen(
            trades = allSampleTrades,
            isLoading = false,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onSelectTrade = {},
            onBrowseOffers = {},
        )
    }
}

/**
 * Preview: search active — only completed trades remain after filtering "SEPA".
 * Demonstrates the "N of M trades" counter and narrowed list.
 */
@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 900)
@Composable
private fun List_SearchActive_Preview() {
    BisqTheme.Preview {
        TradeHistoryListScreen(
            trades = allSampleTrades,
            isLoading = false,
            searchQuery = "SEPA",
            onSearchQueryChange = {},
            onSelectTrade = {},
            onBrowseOffers = {},
        )
    }
}

/**
 * Preview: loading state — shimmer placeholder cards while data is fetched.
 */
@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun List_Loading_Preview() {
    BisqTheme.Preview {
        TradeHistoryListScreen(
            trades = emptyList(),
            isLoading = true,
            searchQuery = "",
            onSearchQueryChange = {},
            onSelectTrade = {},
            onBrowseOffers = {},
        )
    }
}

/**
 * Preview: empty state — first-run, no closed trades at all.
 */
@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 700)
@Composable
private fun List_EmptyState_Preview() {
    BisqTheme.Preview {
        TradeHistoryListScreen(
            trades = emptyList(),
            isLoading = false,
            searchQuery = "",
            onSearchQueryChange = {},
            onSelectTrade = {},
            onBrowseOffers = {},
        )
    }
}

/**
 * Preview: no-results state — search has no matches.
 */
@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 500)
@Composable
private fun List_NoResults_Preview() {
    BisqTheme.Preview {
        TradeHistoryListScreen(
            trades = allSampleTrades,
            isLoading = false,
            searchQuery = "xyznotfound",
            onSearchQueryChange = {},
            onSelectTrade = {},
            onBrowseOffers = {},
        )
    }
}

/**
 * Preview: single trade — edge case for list with one entry.
 */
@ExcludeFromCoverage
@Preview(showBackground = true, heightDp = 400)
@Composable
private fun List_SingleTrade_Preview() {
    BisqTheme.Preview {
        TradeHistoryListScreen(
            trades = listOf(sampleCompletedBuyerTrade),
            isLoading = false,
            searchQuery = "",
            onSearchQueryChange = {},
            onSelectTrade = {},
            onBrowseOffers = {},
        )
    }
}
