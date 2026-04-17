/**
 * TradeHistoryCardDesign.kt — Design PoC
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Reusable card component for a single closed trade in the trade history list.
 *
 * Closed trades differ from open trades in two important ways:
 *   1. The outcome is final — the card must communicate success/failure at a glance.
 *   2. The card is archival — interaction is reduced (no chat badge, no action urgency).
 *
 * The card uses a colored left-border stripe to signal outcome (same border mechanism as
 * OpenTradeCard) but outcome-driven rather than notification-driven:
 *   - Green  (primary)  : COMPLETED — trade finished, BTC confirmed
 *   - Red    (danger)   : CANCELLED or REJECTED — peer or self cancelled/rejected
 *   - Orange (warning)  : FAILED / FAILED_AT_PEER — protocol error, no BTC movement
 *
 * Layout mirrors OpenTradeListItem's two-column approach (left: peer/metadata, right:
 * amounts) but adds an outcome badge row above the columns. The background uses a
 * slightly darker shade than open trades (dark_grey30 vs dark_grey40) to create a
 * visual "archived" feel without sacrificing readability.
 *
 * ======================================================================================
 * OUTCOME BADGE DESIGN
 * ======================================================================================
 * The outcome badge sits at the top of the card, full-width, using a tinted pill strip.
 * It uses the same tinted-surface pattern as ChargebackRiskBadge in PaymentAccountCard
 * (color.copy(alpha=0.12f) background + color text + colored 3dp left accent line).
 * This is immediately scannable without being visually aggressive.
 *
 * A small "My role" label (Buyer/Seller · Maker/Taker) sits to the right of the outcome
 * badge inside the same row. This answers the user's first mental question:
 * "did I complete this trade, and what was my role in it?"
 *
 * ======================================================================================
 * AMOUNT DISPLAY STRATEGY
 * ======================================================================================
 * - Fiat amount is in primary green for completed trades, white for cancelled/failed.
 *   Reason: green on completed reinforces the positive outcome; neutral on cancelled
 *   avoids implying money changed hands.
 * - BTC amount uses BtcSatsText for the sig-digit dim-leading-zeros display.
 * - Price shows the @ prefix in grey, value in white — same style as OpenTradeListItem.
 *
 * ======================================================================================
 * PEER ROW
 * ======================================================================================
 * Uses a simulated peer row (username + StarRating inline) rather than UserProfileRow
 * because UserProfileRow requires domain VO types. The simulated version uses a
 * placeholder circle avatar (colored Box) + username text + star rating text.
 * During implementation, replace SimulatedPeerRow with the real UserProfileRow.
 *
 * ======================================================================================
 * IMPLEMENTATION NOTES
 * ======================================================================================
 * - Data model: maps from BisqEasyTradeDto + derived fields from presenter
 * - The SimulatedTradeHistoryItem struct mirrors what a real presenter would expose:
 *   all formatting already applied (no domain types in view)
 * - Trade outcome is an enum (not a raw BisqEasyTradeStateDto string) — the presenter
 *   should map the 40+ FSM states down to these 4 terminal buckets
 * - "My role" string: presenter derives from TradeRoleDto
 *   (BUYER_AS_TAKER → "Buyer · Taker", SELLER_AS_MAKER → "Seller · Maker", etc.)
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * mobile.tradeHistory.outcome.completed   = "Completed"
 * mobile.tradeHistory.outcome.cancelled   = "Cancelled"
 * mobile.tradeHistory.outcome.rejected    = "Rejected"
 * mobile.tradeHistory.outcome.failed      = "Failed"
 * mobile.tradeHistory.role.buyerAsTaker   = "Buyer · Taker"
 * mobile.tradeHistory.role.buyerAsMaker   = "Buyer · Maker"
 * mobile.tradeHistory.role.sellerAsTaker  = "Seller · Taker"
 * mobile.tradeHistory.role.sellerAsMaker  = "Seller · Maker"
 * mobile.tradeHistory.card.tradeId        = "Trade #{0}"   (reuse open trades pattern)
 */
package network.bisq.mobile.presentation.design.trade_history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Outcome enum — maps the 40+ FSM terminal states down to 4 visual buckets
// -------------------------------------------------------------------------------------

internal enum class SimulatedTradeOutcome {
    COMPLETED,
    CANCELLED,
    REJECTED,
    FAILED,
}

// -------------------------------------------------------------------------------------
// Simulated domain model — primitives only, no presenter dependency
// -------------------------------------------------------------------------------------

internal data class SimulatedTradeHistoryItem(
    val tradeId: String,
    val shortTradeId: String,
    // Buyer / Seller direction label, e.g. "Bought from" / "Sold to"
    val directionalTitle: String,
    val peerName: String,
    // 0.0 – 5.0
    val peerReputationScore: Double,
    // e.g. "Apr 8, 2026"
    val formattedDate: String,
    // e.g. "14:32"
    val formattedTime: String,
    // e.g. "100.00 USD"
    val quoteAmountWithCode: String,
    // e.g. "0.00150000" — BTC format for BtcSatsText
    val formattedBaseAmount: String,
    // e.g. "68,420 USD/BTC"
    val formattedPrice: String,
    val fiatPaymentMethod: String,
    val bitcoinSettlementMethod: String,
    // e.g. "Buyer · Maker"
    val myRole: String,
    val outcome: SimulatedTradeOutcome,
)

// -------------------------------------------------------------------------------------
// Color helpers
// -------------------------------------------------------------------------------------

@Composable
private fun outcomeColor(outcome: SimulatedTradeOutcome): Color =
    when (outcome) {
        SimulatedTradeOutcome.COMPLETED -> BisqTheme.colors.primary
        SimulatedTradeOutcome.CANCELLED -> BisqTheme.colors.danger
        SimulatedTradeOutcome.REJECTED -> BisqTheme.colors.danger
        SimulatedTradeOutcome.FAILED -> BisqTheme.colors.warning
    }

private fun outcomeLabel(outcome: SimulatedTradeOutcome): String =
    when (outcome) {
        SimulatedTradeOutcome.COMPLETED -> "Completed"
        SimulatedTradeOutcome.CANCELLED -> "Cancelled"
        SimulatedTradeOutcome.REJECTED -> "Rejected"
        SimulatedTradeOutcome.FAILED -> "Failed"
    }

// -------------------------------------------------------------------------------------
// Outcome badge — tinted pill strip with colored left accent
// -------------------------------------------------------------------------------------

@Composable
private fun OutcomeBadgeRow(
    outcome: SimulatedTradeOutcome,
    myRole: String,
) {
    val color = outcomeColor(outcome)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadiusSmall),
        color = color.copy(alpha = 0.10f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingQuarter,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                // 3dp colored left accent line
                Surface(
                    modifier = Modifier.size(width = 3.dp, height = 14.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = color,
                ) {}
                BisqText.SmallRegular(
                    text = outcomeLabel(outcome),
                    color = color,
                )
            }
            BisqText.XSmallLight(
                text = myRole,
                color = BisqTheme.colors.mid_grey20,
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// Simulated peer row — placeholder until real UserProfileRow can be used
// -------------------------------------------------------------------------------------

@Composable
private fun SimulatedPeerRow(
    peerName: String,
    reputationScore: Double,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        // Placeholder avatar circle — replace with UserProfileIcon in production
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BisqTheme.colors.dark_grey50),
            contentAlignment = Alignment.Center,
        ) {
            BisqText.XSmallLight(
                text = peerName.take(1).uppercase(),
                color = BisqTheme.colors.mid_grey30,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BisqText.SmallRegular(
                text = peerName,
                color = BisqTheme.colors.white,
            )
            StarRating(rating = reputationScore)
        }
    }
}

// -------------------------------------------------------------------------------------
// Public card composable
// -------------------------------------------------------------------------------------

/**
 * Trade history card for a single closed trade.
 *
 * Visual structure:
 * ┌─────────────────────────────────────────┐  ← dark_grey30 background
 * │ [Completed badge ▌]        [Buyer·Taker]│  ← outcome badge row
 * │ Bought from:                            │
 * │ [avatar] SatoshiFan42  ★★★★☆   100.00 USD  ← peer left / fiat right
 * │ Apr 8, 2026 14:32      @ 68,420 USD/BTC
 * │ Trade #abc123d         0.00150000 BTC
 * │ [sepa] ⇄ [btc]
 * └─────────────────────────────────────────┘
 *
 * The left border stripe (4dp) mirrors OpenTradeCard but uses outcome color.
 * The card background is dark_grey30 (one step darker than open trades' dark_grey40)
 * to give a subtle "archived" tone.
 *
 * @param item The closed trade data (all fields pre-formatted by presenter)
 * @param onClick Optional tap handler — navigates to trade detail
 */
@Composable
internal fun TradeHistoryCard(
    item: SimulatedTradeHistoryItem,
    onClick: () -> Unit = {},
) {
    val borderColor = outcomeColor(item.outcome)
    val borderWidth = 4.dp
    val borderRadius = BisqUIConstants.BorderRadius
    val cardBackground = BisqTheme.colors.dark_grey30

    // Card shape: flat left edge (for the border stripe) + rounded right corners
    val shape =
        RoundedCornerShape(
            topStart = 0.dp,
            bottomStart = 0.dp,
            topEnd = borderRadius,
            bottomEnd = borderRadius,
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cardBackground)
                .drawBehind {
                    val strokeWidthPx = borderWidth.toPx()
                    drawLine(
                        color = borderColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = strokeWidthPx,
                    )
                }.padding(BisqUIConstants.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        // Row 1: Outcome badge + my role
        OutcomeBadgeRow(
            outcome = item.outcome,
            myRole = item.myRole,
        )

        BisqGap.VQuarter()

        // Row 2: Main content — peer/metadata left, amounts right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left column: direction, peer, date, trade ID
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                BisqText.SmallLightGrey(
                    text = item.directionalTitle.uppercase(),
                )
                SimulatedPeerRow(
                    peerName = item.peerName,
                    reputationScore = item.peerReputationScore,
                )
                BisqText.SmallLightGrey("${item.formattedDate}  ${item.formattedTime}")
                BisqText.SmallLightGrey("Trade #${item.shortTradeId}")
            }

            // Right column: fiat amount, price, BTC amount, payment methods
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(top = 2.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                // Fiat amount: green if completed, white if cancelled/failed
                val fiatColor =
                    if (item.outcome == SimulatedTradeOutcome.COMPLETED) {
                        BisqTheme.colors.primary
                    } else {
                        BisqTheme.colors.mid_grey30
                    }
                BisqText.LargeRegular(
                    text = item.quoteAmountWithCode,
                    color = fiatColor,
                )

                // Price with @ prefix
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BisqText.SmallRegularGrey("@ ")
                    if (item.formattedPrice.length > 16) {
                        BisqText.XSmallRegular(item.formattedPrice)
                    } else {
                        BisqText.SmallRegular(item.formattedPrice)
                    }
                }

                // BTC amount with sig-digit styling
                BtcSatsText(item.formattedBaseAmount)

                // Payment method icons
                PaymentMethods(
                    baseSidePaymentMethods = listOf(item.bitcoinSettlementMethod),
                    quoteSidePaymentMethods = listOf(item.fiatPaymentMethod),
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Preview data
// -------------------------------------------------------------------------------------

internal val sampleCompletedBuyerTrade =
    SimulatedTradeHistoryItem(
        tradeId = "t-abc123def456ghi789",
        shortTradeId = "abc123d",
        directionalTitle = "Bought from",
        peerName = "SatoshiFan42",
        peerReputationScore = 4.5,
        formattedDate = "Apr 8, 2026",
        formattedTime = "14:32",
        quoteAmountWithCode = "342.10 USD",
        formattedBaseAmount = "0.00500000",
        formattedPrice = "68,420 USD/BTC",
        fiatPaymentMethod = "SEPA",
        bitcoinSettlementMethod = "MAINCHAIN",
        myRole = "Buyer · Taker",
        outcome = SimulatedTradeOutcome.COMPLETED,
    )

internal val sampleCompletedSellerTrade =
    SimulatedTradeHistoryItem(
        tradeId = "t-xyz789abc123def456",
        shortTradeId = "xyz789a",
        directionalTitle = "Sold to",
        peerName = "LightningLover99",
        peerReputationScore = 3.0,
        formattedDate = "Apr 6, 2026",
        formattedTime = "09:15",
        quoteAmountWithCode = "820.00 EUR",
        formattedBaseAmount = "0.01200000",
        formattedPrice = "68,333 EUR/BTC",
        fiatPaymentMethod = "REVOLUT",
        bitcoinSettlementMethod = "LN",
        myRole = "Seller · Maker",
        outcome = SimulatedTradeOutcome.COMPLETED,
    )

internal val sampleCancelledTrade =
    SimulatedTradeHistoryItem(
        tradeId = "t-can000def456ghi789",
        shortTradeId = "can000d",
        directionalTitle = "Buying from",
        peerName = "CryptoNovice7",
        peerReputationScore = 1.5,
        formattedDate = "Apr 5, 2026",
        formattedTime = "11:00",
        quoteAmountWithCode = "200.00 USD",
        formattedBaseAmount = "0.00290000",
        formattedPrice = "68,965 USD/BTC",
        fiatPaymentMethod = "ZELLE",
        bitcoinSettlementMethod = "MAINCHAIN",
        myRole = "Buyer · Taker",
        outcome = SimulatedTradeOutcome.CANCELLED,
    )

internal val sampleRejectedTrade =
    SimulatedTradeHistoryItem(
        tradeId = "t-rej001abc123def456",
        shortTradeId = "rej001a",
        directionalTitle = "Selling to",
        peerName = "FreshTrader",
        peerReputationScore = 0.0,
        formattedDate = "Apr 3, 2026",
        formattedTime = "16:45",
        quoteAmountWithCode = "500.00 EUR",
        formattedBaseAmount = "0.00730000",
        formattedPrice = "68,493 EUR/BTC",
        fiatPaymentMethod = "SEPA_INSTANT",
        bitcoinSettlementMethod = "LN",
        myRole = "Seller · Taker",
        outcome = SimulatedTradeOutcome.REJECTED,
    )

internal val sampleFailedTrade =
    SimulatedTradeHistoryItem(
        tradeId = "t-fail002ghi789abc12",
        shortTradeId = "fail002",
        directionalTitle = "Buying from",
        peerName = "NodeOp_Berlin",
        peerReputationScore = 4.0,
        formattedDate = "Apr 1, 2026",
        formattedTime = "08:20",
        quoteAmountWithCode = "150.00 CHF",
        formattedBaseAmount = "0.00220000",
        formattedPrice = "68,181 CHF/BTC",
        fiatPaymentMethod = "TWINT",
        bitcoinSettlementMethod = "MAINCHAIN",
        myRole = "Buyer · Maker",
        outcome = SimulatedTradeOutcome.FAILED,
    )

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Completed_Buyer_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            TradeHistoryCard(item = sampleCompletedBuyerTrade)
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Completed_Seller_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            TradeHistoryCard(item = sampleCompletedSellerTrade)
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Cancelled_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            TradeHistoryCard(item = sampleCancelledTrade)
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Rejected_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            TradeHistoryCard(item = sampleRejectedTrade)
        }
    }
}

@ExcludeFromCoverage
@Preview(showBackground = true)
@Composable
private fun Card_Failed_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            TradeHistoryCard(item = sampleFailedTrade)
        }
    }
}
