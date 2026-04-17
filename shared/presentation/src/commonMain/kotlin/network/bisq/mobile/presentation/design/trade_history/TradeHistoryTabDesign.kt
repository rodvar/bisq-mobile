package network.bisq.mobile.presentation.design.trade_history

/**
 * # Trade History Tab Integration Design
 *
 * Designs the modification to the existing "My Trades" tab (TabOpenTradeList) to include
 * both open trades and trade history using a segmented toggle at the top.
 *
 * ## Design rationale
 *
 * **Why not a 5th bottom tab?**
 * - 5 tabs cause label truncation in i18n (German, Portuguese labels are long)
 * - Breaks the established 4-tab rhythm; adds cognitive load
 * - Trade history is contextually related to open trades — same mental model
 *
 * **Why a segmented toggle (not top tabs)?**
 * - Compose Material `TabRow` has a distinctly different visual language (underline indicator)
 *   that clashes with the dark card-based Bisq theme
 * - A `ToggleTab` component already exists in the codebase (used in create offer amount screen)
 * - Two options = perfect fit for a binary toggle; tabs imply 3+ sections
 * - The toggle sits below the top bar and above the list, taking minimal vertical space
 *
 * ## Navigation structure
 *
 * ```
 * Bottom Tab: "My Trades" (TabOpenTradeList)
 *   ┌──────────────────────────────────────────┐
 *   │  [  Open trades  |  History  ]           │  ← ToggleTab (segmented control)
 *   ├──────────────────────────────────────────┤
 *   │                                          │
 *   │  (content switches based on toggle)      │
 *   │                                          │
 *   │  Open: existing OpenTradeListScreen      │
 *   │  History: TradeHistoryList from design    │
 *   │                                          │
 *   └──────────────────────────────────────────┘
 * ```
 *
 * ## Badge / notification indicator
 *
 * The bottom tab icon for "My Trades" currently shows a notification badge
 * for unread trade chat messages. This only applies to open trades.
 * When viewing the History tab, the badge should still reflect open trade
 * notifications (it's a tab-level indicator, not content-level).
 *
 * ## State preservation
 *
 * The selected toggle state (Open vs History) should survive back-stack navigation
 * (user navigates to trade detail and back). Use `rememberSaveable` for the toggle index.
 * When the user switches to another bottom tab and returns, the toggle resets to "Open"
 * (open trades are the primary view; history is secondary).
 *
 * ## I18N keys needed
 *
 * - `mobile.myTrades.tab.open` = "Open trades"
 * - `mobile.myTrades.tab.history` = "History"
 *
 * ## Desktop reference
 *
 * Bisq2 Desktop uses a horizontal tab bar with "Open Trades" and "Trade History"
 * as sibling tabs under the same navigation section. This design mirrors that
 * hierarchy but adapts to mobile with a compact toggle control.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ---------------------------------------------------------------------------
// Simulated data helpers (no domain types)
// ---------------------------------------------------------------------------

private enum class SimulatedTradeTab(
    val label: String,
) {
    OPEN("Open trades"),
    HISTORY("History"),
}

@Composable
private fun SimulatedOpenTradeCard(
    direction: String,
    peerName: String,
    date: String,
    fiatAmount: String,
    btcAmount: String,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        bottomStart = 0.dp,
                        topEnd = BisqUIConstants.BorderRadius,
                        bottomEnd = BisqUIConstants.BorderRadius,
                    ),
                ).background(BisqTheme.colors.dark_grey40)
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        // Yellow left border (notification indicator)
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .width(6.dp)
                    .height(60.dp)
                    .background(BisqTheme.colors.yellow),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                BisqText.BaseLightGrey(direction.uppercase())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(BisqTheme.colors.mid_grey30),
                        contentAlignment = Alignment.Center,
                    ) {
                        BisqText.SmallRegular(peerName.take(1).uppercase())
                    }
                    Spacer(Modifier.width(8.dp))
                    BisqText.BaseRegular(peerName)
                }
                BisqText.SmallLightGrey(date)
            }
            Column(horizontalAlignment = Alignment.End) {
                BisqText.LargeRegular(fiatAmount, color = BisqTheme.colors.primary)
                BisqGap.VQuarter()
                BisqText.SmallRegular(btcAmount)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main integrated screen design
// ---------------------------------------------------------------------------

/**
 * Design for the modified "My Trades" tab screen.
 *
 * The existing [OpenTradeListScreen] becomes the content for the "Open trades" toggle.
 * The [TradeHistoryListDesign] content becomes the "History" toggle.
 *
 * Implementation note: In production, this composable replaces the current
 * `OpenTradeListScreen` body. The presenter would expose:
 * ```kotlin
 * val selectedTab: StateFlow<Int>  // 0 = Open, 1 = History
 * fun onTabSelect(index: Int)
 * ```
 * Or, more simply, use `rememberSaveable { mutableIntStateOf(0) }` in the screen
 * since the toggle is purely a UI concern (both lists are independent).
 */
@Composable
internal fun MyTradesScreenWithHistory(
    selectedTab: Int = 0,
    onTabSelect: (Int) -> Unit = {},
    // Open trades content
    openTradesContent: @Composable () -> Unit = {},
    // History content
    historyContent: @Composable () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = BisqUIConstants.ScreenPadding),
    ) {
        BisqGap.V1()

        // Segmented toggle — reuses existing ToggleTab component
        ToggleTab(
            options = SimulatedTradeTab.entries.toList(),
            selectedOption = if (selectedTab == 0) SimulatedTradeTab.OPEN else SimulatedTradeTab.HISTORY,
            onOptionSelect = { tab -> onTabSelect(tab.ordinal) },
            getDisplayString = { it.label },
        )

        BisqGap.V1()

        // Content area
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> openTradesContent()
                1 -> historyContent()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun MyTradesScreen_OpenTab_WithTrades_Preview() {
    BisqTheme.Preview {
        var tab by remember { mutableIntStateOf(0) }
        MyTradesScreenWithHistory(
            selectedTab = tab,
            onTabSelect = { tab = it },
            openTradesContent = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(
                        listOf(
                            Triple("Buying from", "SatoshiFan", "150.00 EUR"),
                            Triple("Selling to", "BitcoinBob", "200.00 USD"),
                        ),
                    ) { (direction, peer, amount) ->
                        SimulatedOpenTradeCard(
                            direction = direction,
                            peerName = peer,
                            date = "2026-04-15 14:30",
                            fiatAmount = amount,
                            btcAmount = "0.00180000 BTC",
                        )
                    }
                }
            },
            historyContent = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun MyTradesScreen_HistoryTab_WithTrades_Preview() {
    BisqTheme.Preview {
        var tab by remember { mutableIntStateOf(1) }
        MyTradesScreenWithHistory(
            selectedTab = tab,
            onTabSelect = { tab = it },
            openTradesContent = {},
            historyContent = {
                // Reuse the history card designs
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(
                        listOf(
                            SimulatedClosedTrade("Bought from", "SatoshiFan", "2026-04-10", "150.00 EUR", "0.00180000 BTC", "Completed", true),
                            SimulatedClosedTrade("Sold to", "LightningLucy", "2026-04-08", "0.50 XMR", "0.00050000 BTC", "Completed", true),
                            SimulatedClosedTrade("Buying from", "TorTrader", "2026-04-05", "300.00 USD", "0.00350000 BTC", "Cancelled", false),
                        ),
                    ) { trade ->
                        SimulatedHistoryCard(trade)
                    }
                }
            },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun MyTradesScreen_HistoryTab_Empty_Preview() {
    BisqTheme.Preview {
        var tab by remember { mutableIntStateOf(1) }
        MyTradesScreenWithHistory(
            selectedTab = tab,
            onTabSelect = { tab = it },
            openTradesContent = {},
            historyContent = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BisqText.H4LightGrey(
                        text = "No trade history yet",
                        textAlign = TextAlign.Center,
                    )
                    BisqGap.V2()
                    BisqText.BaseLightGrey(
                        text = "Completed and closed trades will appear here.",
                        textAlign = TextAlign.Center,
                    )
                    BisqGap.V3()
                    BisqButton(
                        text = "Browse offers",
                        onClick = {},
                    )
                }
            },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun MyTradesScreen_OpenTab_Empty_Preview() {
    BisqTheme.Preview {
        var tab by remember { mutableIntStateOf(0) }
        MyTradesScreenWithHistory(
            selectedTab = tab,
            onTabSelect = { tab = it },
            openTradesContent = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BisqText.H4LightGrey(
                        text = "You don't have any open trades",
                        textAlign = TextAlign.Center,
                    )
                    BisqGap.V3()
                    BisqButton(
                        text = "Browse offers",
                        onClick = {},
                    )
                }
            },
            historyContent = {},
        )
    }
}

// ---------------------------------------------------------------------------
// Helper for history previews within this file
// ---------------------------------------------------------------------------

private data class SimulatedClosedTrade(
    val direction: String,
    val peerName: String,
    val date: String,
    val fiatAmount: String,
    val btcAmount: String,
    val outcome: String,
    val isCompleted: Boolean,
)

@Composable
private fun SimulatedHistoryCard(trade: SimulatedClosedTrade) {
    val borderColor = if (trade.isCompleted) BisqTheme.colors.primary else BisqTheme.colors.danger
    val amountColor = if (trade.isCompleted) BisqTheme.colors.primary else BisqTheme.colors.mid_grey30

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        bottomStart = 0.dp,
                        topEnd = BisqUIConstants.BorderRadius,
                        bottomEnd = BisqUIConstants.BorderRadius,
                    ),
                ).background(BisqTheme.colors.dark_grey30)
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        // Outcome left border
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(60.dp)
                    .background(borderColor),
        )

        Column(modifier = Modifier.padding(start = 8.dp)) {
            // Outcome badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(borderColor.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    BisqText.XSmallRegular(trade.outcome, color = borderColor)
                }
                BisqText.XSmallRegularGrey("Buyer / Taker")
            }

            BisqGap.VHalf()

            // Main content row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    BisqText.BaseLightGrey(trade.direction.uppercase())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(BisqTheme.colors.mid_grey30),
                            contentAlignment = Alignment.Center,
                        ) {
                            BisqText.XSmallRegular(trade.peerName.take(1).uppercase())
                        }
                        Spacer(Modifier.width(6.dp))
                        BisqText.SmallRegular(trade.peerName)
                    }
                    BisqText.SmallLightGrey(trade.date)
                }
                Column(horizontalAlignment = Alignment.End) {
                    BisqText.LargeRegular(trade.fiatAmount, color = amountColor)
                    BisqGap.VQuarter()
                    BisqText.SmallRegular(trade.btcAmount)
                }
            }
        }
    }
}
