package network.bisq.mobile.presentation.design.peer_profile_offers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Design PoC — "Trade again": a peer's own offers surfaced on their [PeerProfileScreen], issue
 * #1631, Milestone 11 "Bisq community".
 *
 * DO NOT wire this into production. This file exists to be opened in Android Studio's Split /
 * Design view and reviewed via its `@Preview` functions. It builds on primitives only — no
 * `OfferItemPresentationModel`, no presenter, no Koin — so it renders safely in `@Preview`.
 *
 * ---------------------------------------------------------------------------------------------
 * ## 1. Option A vs. Option B — verdict: A (dedicated section on the profile screen)
 *
 * A dedicated "Their offers" section beats reusing the offerbook filtered-by-author, on both
 * personas:
 *
 * - **Novice**: the profile screen already establishes "who is this and can I trust them" — a
 *   filtered-offerbook detour (market list -> offer list -> exit) reintroduces the offerbook's own
 *   chrome (direction toggle, market picker affordances) that a beginner has to first confirm are
 *   *not* live/global before trusting what's on screen. A silently scoped list bolted onto a
 *   screen they're already reading requires no new mental model at all.
 * - **Experienced trader**: fewer taps to the thing they came for. "Trade again with this specific
 *   person" is a one-screen decision once reputation/history is visible; A gets there in the same
 *   scroll, B costs a market pick + a list screen + a deliberate exit back to where they started.
 * - **Degradation across 0 / 1 / many offers, many markets**: this is where B loses hardest. Zero
 *   offers means entering a "filtered offerbook" flow just to be told there's nothing there — a
 *   real screen transition for a null result. One offer means the market-list hop is pure
 *   overhead for a single row. Many offers across many markets is B's only edge (it reuses the
 *   real per-market list machinery), but A handles this fine too with lightweight in-place
 *   grouping — see §3 — without ever leaving the profile screen. B's "filtered mode" would also
 *   need its own unmistakable banner/chrome so a user mid-flow doesn't mistake it for the regular
 *   global offerbook (per the brief's own framing) — that's new state and new copy to design and
 *   maintain for a benefit A gets for free.
 *
 * Net: A is less to build, less to maintain, and reads faster for both personas. No hybrid needed.
 *
 * ## 2. Tap behavior — verdict: straight into take-offer, no interstitial detail step
 *
 * The rest of the app trains one interaction model: tapping an offer card starts taking it
 * (`OfferbookPresenter.onOfferSelected` -> `TakeOfferCoordinator`). Introducing a second model
 * (interstitial detail screen) *only* on offers reached from a profile would be the inconsistency,
 * not the safety net it might look like:
 * - The row already surfaces everything a detail screen would repeat first: direction, amount,
 *   price, payment methods (§3). There's no new fact to reveal before committing.
 * - Identity is *more* established here than in the offerbook, if anything, since the user just
 *   read this peer's reputation and chose to act from their profile — the exact context an
 *   interstitial "are you sure who you're trading with" step exists to provide elsewhere.
 * - The take-offer flow itself is not a single irrevocable tap — amount -> payment -> settlement
 *   -> review all have their own back button, and review is the actual confirmation gate. An
 *   interstitial here would duplicate that gate one screen too early instead of adding safety.
 *
 * One divergence from the offerbook card: the maker's avatar tap-target
 * (`OfferCard`'s `onPeerProfileClick`, see agent memory
 * `project_offercard_peer_profile_tap_target`) is dropped entirely in this row — see §3 — because
 * it would point back at the profile the user is already on, the same "no self-link" rule already
 * applied to `isMyOffer` in production.
 *
 * ## 3. Placement & shape
 *
 * **Placement**: directly below the reputation block, ABOVE "Send private message" / ignore /
 * report / contact. Rationale: the existing action stack is relationship management (message,
 * ignore, report, contact-tagging) — useful but secondary to why most people tap through from an
 * offer or a trade history row to a profile in the first place. A peer's live, takeable offers are
 * the highest-value, most differentiated content this screen can show (their reputation stats are
 * generic; these offers are not) and each row is itself a primary action (tap = take), so the
 * section earns the same "sits above the secondary action stack" precedent already used for the
 * "Send private message" button in [network.bisq.mobile.presentation.peer_profile.PeerProfileScreen].
 *
 * **Row shape — a deliberate divergence from `OfferCard`**: no avatar/stars/languages identity
 * column. `OfferCard` needs one per row because every card in the offerbook can belong to a
 * different maker; every row here belongs to the *same*, already-on-screen peer, so repeating
 * their identity per row would be pure noise. Dropping it also shrinks each row from `OfferCard`'s
 * fixed 150dp to a compact, wrap-content ~3-line card — which is what makes the "many offers
 * across many markets" case in §1 tractable inline: rows are cheap, so a dozen of them cost
 * little vertical space, especially compared to always paying a 150dp identity column per row for
 * information the user already has above the fold.
 *
 * What IS reused from `OfferCard`, verbatim, to keep the two surfaces visually of one family:
 * direction-colored label (buy=`primary`, sell=`danger`, same alpha), `BisqUIConstants` spacing
 * scale, the rounded-card + `dark_grey50` background language, and the shrink-then-ellipsis
 * (`AutoResizeText`) treatment for anything that can overflow.
 *
 * **Grouping for multi-market peers**: rows are grouped under a small market-code header (`EUR`,
 * `USD`, ...), ordered by **recency of activity, newest first** — a group's sort key is the
 * `max()` of its offers' creation timestamps (`PeerOfferRowData.offerDateMillis`, sourced from
 * `BisqEasyOffer.date` on `OfferItemPresentationModel` — already available per the issue brief,
 * no backend change needed). Rows within a group are sorted the same way, newest first. Rationale
 * (per rodvar, superseding this PoC's original alphabetical-by-market draft): a peer who just
 * posted a fresh EUR offer this morning and has a month-old, possibly-stale USD offer is more
 * usefully read EUR-first — recency is a better proxy for "what this peer is actively trading
 * right now" than an arbitrary currency-code sort. See [PeerOfferGroupHeader].
 *
 * **Empty state — hidden, not rendered**: when the peer genuinely has zero offers (sync complete),
 * the section renders NOTHING — no header, no "no active offers" caption. This mirrors the
 * existing `canSendPrivateMessage` convention on this exact screen ("absent rather than disabled
 * ... a permanently dead control reads worse than no control") applied one level up: a profile
 * page is not the offerbook, and most profile visits are for identity/reputation lookup, not offer
 * discovery — a dead "no offers" block would be clutter on every trader with no open offer right
 * now. See [PeerProfileOffersSectionState.Hidden] and [PeerProfileOffersSection_EmptyPreview] for
 * what that renders (nothing) versus what it doesn't.
 *
 * **Syncing state — must be honest, not silently empty**: the client flavour's all-markets offers
 * cache can lag on cold start (per the design brief). Silence would misreport "no offers" as a
 * false negative. While syncing, the section shows one small row with a spinner and a "Checking
 * for their offers..." caption instead of either hiding or showing a premature empty state. See
 * [PeerProfileOffersSectionState.Syncing].
 *
 * **Non-takeable offers**: reuses the offerbook's own eligibility gate and its existing muted
 * treatment (`OfferCard`'s `invalidOfferBackgroundColor`) rather than inventing a new visual
 * language — same reduced-opacity `dark_grey50` background. One addition beyond what `OfferCard`
 * does inline: a small caption line naming the reason ("Requires more reputation to take"). The
 * offerbook relies on a tap-to-reveal dialog because a dense list of many different makers has no
 * room to spell out reasons per card; here the user's whole reason for being on screen is this one
 * peer, so the extra line costs little and saves a tap for what would otherwise look like a
 * randomly greyed-out row. Tapping the row still opens the same reputation-requirement info the
 * offerbook already shows (reuse, not a new dialog) via [onNonTakeableOfferClick].
 *
 * **Section title — two variants** (per rodvar, superseding this PoC's original "Active offers
 * (N)" draft): "Trade again: {name}" once the user has actually traded with this peer before;
 * "Trade with {name}" — no colon — when the relationship is contact-only and no trade has
 * happened yet (§4). Both name the action the section exists for, right where the user decides to
 * take it, rather than describing the list's contents. The offer count didn't disappear — it's
 * demoted to a small caption under the title ("N active offers") rather than competing with the
 * peer's name for the headline. The title uses `AutoResizeText` (shrink-then-ellipsis, single
 * line) because either prefix plus a long display name can overflow narrower phones — same
 * treatment already used for usernames elsewhere in this file and in `OfferCard`.
 *
 * Quick copy judgment on "Trade with {name}": good as specified, kept as-is. It reads as a
 * natural, complete action phrase without implying a trade history that doesn't exist — unlike
 * reusing "again" here, which would be a small but real honesty slip for a contact you've never
 * actually traded with. Punctuation deliberately diverges from the traded variant: "Trade again:
 * {name}" reads as a label ("Category: item"), while "Trade with: {name}" would read like a typo
 * — inserting a colon directly after the preposition "with" breaks the sentence rather than
 * introducing a list, so the colon is dropped for this variant only.
 *
 * ## 4. Visibility gating — relationship-gated, absent otherwise
 *
 * The whole section — header, offers, everything — renders ONLY when the viewing user has a
 * relationship with this peer: either they've traded with them before, or the peer is in My
 * Contacts. For any other peer (someone whose profile you're looking at for the first time, with
 * no prior trade and no contact tag) the section is entirely absent, not shown-empty. This is the
 * same "absent rather than disabled ... a dead control reads worse than no control" convention
 * already applied to the zero-offers case in §3 and to `canSendPrivateMessage` on this exact
 * screen, extended one level up: a first-time profile view is not the moment to pitch "trade
 * again" copy at someone you've never dealt with, contact or otherwise.
 *
 * **Degradation notes for the implementer** — the eligibility check is not always a clean
 * boolean-or of two always-available facts:
 * - Contact state is itself feature-gated (only live while the CONTACTS hub segment is live, see
 *   agent memory `project_milestone11_community_ia` / `project_contacts_connect_plan`) — on a
 *   build/config where Contacts isn't live, this reduces to "hasTradedBefore only," matching how
 *   [network.bisq.mobile.presentation.peer_profile.PeerProfileUiState.showContactAction] already
 *   gates the Contacts action button on this same screen.
 * - "Has traded before" itself is not uniformly knowable: `TradesServiceFacade`'s closed-trades
 *   API is capability-gated (`BackendCapabilitiesService`, probe-based — see agent memory
 *   `reference_backend_capabilities_service`) and may be absent on an older paired trusted node on
 *   Bisq Connect. Where that's the case, "traded before" can only be computed from open trades
 *   still in memory, not full trade history — the rule degrades to "traded-in-open-trades or
 *   contact." This under-reports (a peer you closed a trade with long ago, on an old node, may
 *   read as never-traded) rather than over-reports, which is the safer failure direction for a
 *   copy line that claims a relationship exists.
 *
 * This PoC models the gate as two plain booleans (`hasTradedBefore`, `isContact`) on
 * [PeerProfileOffersSection] rather than a richer domain type — good enough to demonstrate the
 * gate and both header variants; a real implementation may want to fold the degradation notes
 * above into how the presenter computes `hasTradedBefore` rather than exposing them at this layer.
 *
 * ## i18n
 *
 * All strings are hardcoded English in this PoC (new keys land in `mobile.properties`
 * English-only per project convention; translations are propagated separately). Keys a real
 * implementation would add:
 * - `mobile.peerProfile.offers.sectionTitle` = "Trade again: {0}" (peer display name; shown when
 *   `hasTradedBefore`)
 * - `mobile.peerProfile.offers.sectionTitleContactOnly` = "Trade with {0}" (shown when the peer is
 *   a contact but `hasTradedBefore` is false — §4)
 * - `mobile.peerProfile.offers.countCaption` = "{0} active offers" — a real implementation needs
 *   this to go through the app's plural-aware i18n handling (singular "1 active offer" vs. plural)
 *   rather than this PoC's naive `if (count == 1)` branch; flagging since plural rules vary across
 *   the app's 14 languages and this PoC does not attempt to model that.
 * - `mobile.peerProfile.offers.syncing` = "Checking for their offers..."
 * - `mobile.peerProfile.offers.notTakeableReason` = "Requires more reputation to take"
 *
 * Direction labels ("Buy Bitcoin from ...", "Sell Bitcoin to ...") reuse the EXISTING
 * `mobile.bisqEasy.offerbook.offerCard.*` keys already used by `OfferCard` — no new key needed
 * there, only the label text is passed in precomputed, matching this PoC's sibling
 * `ContactOfferCard`'s convention of accepting already-localized strings.
 */
private const val NOT_TAKEABLE_REASON = "Requires more reputation to take"
private const val SYNCING_LABEL = "Checking for their offers..."

/**
 * One of the peer's offers, precomputed/localized by the (future) presenter — no domain types.
 *
 * @param offerDateMillis creation timestamp, sourced from `BisqEasyOffer.date` on the real
 *   `OfferItemPresentationModel` — drives both the within-group and group-level recency sort. See
 *   file KDoc §3 "Grouping for multi-market peers."
 */
internal data class PeerOfferRowData(
    val offerId: String,
    val directionLabel: String,
    val isBuyDirection: Boolean,
    val amountLabel: String,
    val priceLabel: String,
    val paymentIconCount: Int,
    val isTakeable: Boolean,
    val offerDateMillis: Long,
)

/** One market's worth of the peer's offers, e.g. all their EUR offers. */
internal data class MarketOffersGroup(
    val marketLabel: String,
    val offers: List<PeerOfferRowData>,
)

/** See §3 "Empty state" / "Syncing state" in the file KDoc for why these are distinct. */
internal sealed interface PeerProfileOffersSectionState {
    /** Sync complete, zero offers — renders nothing. See §3. */
    data object Hidden : PeerProfileOffersSectionState

    /** Cache still catching up (client flavour cold start) — renders a spinner row. */
    data object Syncing : PeerProfileOffersSectionState

    data class Loaded(
        val groups: List<MarketOffersGroup>,
    ) : PeerProfileOffersSectionState
}

/**
 * Entry point matching the slot this would occupy in `PeerProfileBody`, directly below the
 * reputation block and above the action stack (§3).
 *
 * @param hasTradedBefore true once the user has a past (or currently open — see §4's degradation
 *   note) trade with this peer. Drives the "Trade again" vs. "Trade with" header choice.
 * @param isContact true while this peer is in My Contacts. Combined with [hasTradedBefore] via OR
 *   to decide whether the section renders at all — see §4 "Visibility gating."
 */
@ExcludeFromCoverage
@Composable
internal fun PeerProfileOffersSection(
    peerDisplayName: String,
    hasTradedBefore: Boolean,
    isContact: Boolean,
    state: PeerProfileOffersSectionState,
    onOfferClick: (PeerOfferRowData) -> Unit,
    onNonTakeableOfferClick: (PeerOfferRowData) -> Unit,
) {
    // §4 "Visibility gating" — no relationship signal at all means the section is absent, full
    // stop, regardless of whether the peer happens to have offers.
    if (!hasTradedBefore && !isContact) return

    when (state) {
        is PeerProfileOffersSectionState.Hidden -> {
            // Deliberately empty — see file KDoc §3 "Empty state."
        }

        is PeerProfileOffersSectionState.Syncing -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                PeerOfferSyncingRow()
                BisqGap.V2()
            }
        }

        is PeerProfileOffersSectionState.Loaded -> {
            val totalCount = state.groups.sumOf { it.offers.size }
            // Newest-first by group: sort key is the max offer date within each group (§3).
            val orderedGroups =
                state.groups
                    .map { group -> group.copy(offers = group.offers.sortedByDescending { it.offerDateMillis }) }
                    .sortedByDescending { group -> group.offers.maxOf { it.offerDateMillis } }
            Column(modifier = Modifier.fillMaxWidth()) {
                PeerOfferSectionHeader(
                    peerDisplayName = peerDisplayName,
                    hasTradedBefore = hasTradedBefore,
                    totalCount = totalCount,
                )
                BisqGap.V1()
                orderedGroups.forEach { group ->
                    PeerOfferGroupHeader(marketLabel = group.marketLabel)
                    BisqGap.VHalf()
                    group.offers.forEach { offer ->
                        PeerOfferRow(
                            offer = offer,
                            onClick = {
                                if (offer.isTakeable) onOfferClick(offer) else onNonTakeableOfferClick(offer)
                            },
                        )
                        BisqGap.VHalf()
                    }
                    BisqGap.VHalf()
                }
                BisqGap.V1()
            }
        }
    }
}

/**
 * "Trade again: {name}" (traded before) or "Trade with {name}" (contact-only, never traded)
 * headline, with the offer count demoted to a small caption underneath (§3 "Section title") —
 * names the action, not the list contents, while still surfacing the count.
 */
@Composable
private fun PeerOfferSectionHeader(
    peerDisplayName: String,
    hasTradedBefore: Boolean,
    totalCount: Int,
) {
    val title = if (hasTradedBefore) "Trade again: $peerDisplayName" else "Trade with $peerDisplayName"
    Column {
        AutoResizeText(
            text = title,
            textStyle = BisqTheme.typography.baseMedium,
            color = BisqTheme.colors.white,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            minimumFontSize = 11.sp,
        )
        BisqGap.VQuarter()
        BisqText.XSmallRegularGrey(
            text = if (totalCount == 1) "1 active offer" else "$totalCount active offers",
        )
    }
}

/** Small caps-style market label, same visual weight as `contactDetails.title` on this screen. */
@Composable
private fun PeerOfferGroupHeader(marketLabel: String) {
    BisqText.XSmallRegularGrey(text = marketLabel.uppercase())
}

@Composable
private fun PeerOfferSyncingRow() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey50.copy(alpha = 0.6f))
                .padding(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = BisqTheme.colors.mid_grey20,
        )
        BisqText.SmallRegularGrey(text = SYNCING_LABEL)
    }
}

/**
 * Compact row for one of the peer's offers — no identity column (§3). Whole row is the tap
 * target, matching the offerbook's own no-added-chrome convention (no chevron/underline).
 */
@Composable
private fun PeerOfferRow(
    offer: PeerOfferRowData,
    onClick: () -> Unit,
) {
    val directionColor =
        if (offer.isBuyDirection) {
            BisqTheme.colors.primary.copy(alpha = 0.8f)
        } else {
            BisqTheme.colors.danger.copy(alpha = 0.8f)
        }
    val backgroundColor =
        if (offer.isTakeable) {
            BisqTheme.colors.dark_grey50.copy(alpha = 0.9f)
        } else {
            BisqTheme.colors.dark_grey50.copy(alpha = 0.4f)
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(backgroundColor)
                .debouncedClickable(onClick = onClick)
                .padding(BisqUIConstants.ScreenPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.BaseRegular(
                text = offer.directionLabel,
                color = directionColor,
                modifier = Modifier.weight(1f, fill = false),
            )
            BisqGap.HHalf()
            AutoResizeText(
                text = "@ ${offer.priceLabel}",
                textStyle = BisqTheme.typography.smallLight,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                minimumFontSize = 9.sp,
            )
        }
        BisqGap.VHalf()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BisqText.BaseLight(text = offer.amountLabel)
            PeerOfferPaymentIcons(count = offer.paymentIconCount)
        }
        if (!offer.isTakeable) {
            BisqGap.VHalf()
            BisqText.XSmallRegularGrey(text = NOT_TAKEABLE_REASON)
        }
    }
}

/** Stand-in for `PaymentMethods` — plain colored squares, no real payment icon assets (PoC). */
@Composable
private fun PeerOfferPaymentIcons(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
        repeat(count) {
            Box(
                modifier =
                    Modifier
                        .size(BisqUIConstants.ScreenPadding)
                        .clip(RoundedCornerShape(BisqUIConstants.ScreenPadding1))
                        .background(BisqTheme.colors.mid_grey10),
            )
        }
    }
}

/** Stand-in for the profile header (avatar/name/stars) so in-context previews show placement. */
@Composable
private fun SimulatedProfileHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier =
                Modifier
                    .size(BisqUIConstants.ScreenPadding7X)
                    .clip(CircleShape)
                    .background(BisqTheme.colors.mid_grey10),
        )
        BisqGap.V1()
        BisqText.H5Regular(text = "SatoshiFan")
        BisqGap.VHalf()
        StarRating(rating = 4.5)
        BisqGap.VHalf()
        BisqText.BaseLightGrey(text = "Reputation: 12,400")
    }
}

// ============================================================================================
// Previews
// ============================================================================================

private const val PREVIEW_PEER_NAME = "SatoshiFan"

/** @param offerDateMillis toy timestamp for sort demos — not real epoch millis, just relative order. */
private fun mockOffer(
    offerId: String,
    isBuy: Boolean,
    amountLabel: String,
    priceLabel: String,
    offerDateMillis: Long,
    isTakeable: Boolean = true,
    paymentIconCount: Int = 3,
) = PeerOfferRowData(
    offerId = offerId,
    directionLabel = if (isBuy) "Buy Bitcoin from SatoshiFan" else "Sell Bitcoin to SatoshiFan",
    isBuyDirection = isBuy,
    amountLabel = amountLabel,
    priceLabel = priceLabel,
    paymentIconCount = paymentIconCount,
    isTakeable = isTakeable,
    offerDateMillis = offerDateMillis,
)

@ExcludeFromCoverage
@Preview(name = "Typical — 3 offers across 2 markets, newest-first group order")
@Composable
private fun PeerProfileOffersSection_TypicalPreview() {
    BisqTheme.Preview {
        // EUR's newest offer (o2, date 1800) is older than USD's only offer (o3, date 5000), so
        // USD sorts ABOVE EUR despite being added second in this list — demonstrates §3's
        // recency-over-alphabetical group ordering. Within EUR, o2 (1800) also sorts above o1 (1200).
        PeerProfileOffersSection(
            peerDisplayName = PREVIEW_PEER_NAME,
            hasTradedBefore = true,
            isContact = false,
            state =
                PeerProfileOffersSectionState.Loaded(
                    groups =
                        listOf(
                            MarketOffersGroup(
                                marketLabel = "EUR",
                                offers =
                                    listOf(
                                        mockOffer(
                                            "o1",
                                            isBuy = true,
                                            amountLabel = "100.00 - 500.00 EUR",
                                            priceLabel = "50,000",
                                            offerDateMillis = 1_200L,
                                        ),
                                        mockOffer(
                                            "o2",
                                            isBuy = false,
                                            amountLabel = "200.00 EUR",
                                            priceLabel = "51,200",
                                            offerDateMillis = 1_800L,
                                        ),
                                    ),
                            ),
                            MarketOffersGroup(
                                marketLabel = "USD",
                                offers =
                                    listOf(
                                        mockOffer(
                                            "o3",
                                            isBuy = true,
                                            amountLabel = "50.00 - 300.00 USD",
                                            priceLabel = "52,800",
                                            offerDateMillis = 5_000L,
                                        ),
                                    ),
                            ),
                        ),
                ),
            onOfferClick = {},
            onNonTakeableOfferClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Single offer")
@Composable
private fun PeerProfileOffersSection_SingleOfferPreview() {
    BisqTheme.Preview {
        PeerProfileOffersSection(
            peerDisplayName = PREVIEW_PEER_NAME,
            hasTradedBefore = true,
            isContact = false,
            state =
                PeerProfileOffersSectionState.Loaded(
                    groups =
                        listOf(
                            MarketOffersGroup(
                                marketLabel = "EUR",
                                offers =
                                    listOf(
                                        mockOffer(
                                            "o1",
                                            isBuy = true,
                                            amountLabel = "500.00 EUR",
                                            priceLabel = "50,000",
                                            offerDateMillis = 1_000L,
                                        ),
                                    ),
                            ),
                        ),
                ),
            onOfferClick = {},
            onNonTakeableOfferClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Empty — section fully hidden (design decision, see file KDoc §3)")
@Composable
private fun PeerProfileOffersSection_EmptyPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding)) {
            SimulatedProfileHeader()
            BisqGap.V2()
            // Nothing renders here for a peer with zero offers — this comment marks where the
            // section would sit. The action stack (message/ignore/report) follows immediately.
            PeerProfileOffersSection(
                peerDisplayName = PREVIEW_PEER_NAME,
                hasTradedBefore = true,
                isContact = false,
                state = PeerProfileOffersSectionState.Hidden,
                onOfferClick = {},
                onNonTakeableOfferClick = {},
            )
            BisqText.SmallRegularGrey(text = "↓ action stack starts immediately here, no gap")
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Gated — never traded, not a contact (section absent, see file KDoc §4)")
@Composable
private fun PeerProfileOffersSection_GatedAbsentPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding)) {
            SimulatedProfileHeader()
            BisqGap.V2()
            // hasTradedBefore = false, isContact = false — the section returns early and renders
            // nothing at all, even though this peer (unlike the Empty preview above) DOES have
            // offers in the Loaded state passed in below. The gate short-circuits before that
            // state is ever inspected — proving visibility is relationship-gated, not offer-gated.
            PeerProfileOffersSection(
                peerDisplayName = PREVIEW_PEER_NAME,
                hasTradedBefore = false,
                isContact = false,
                state =
                    PeerProfileOffersSectionState.Loaded(
                        groups =
                            listOf(
                                MarketOffersGroup(
                                    marketLabel = "EUR",
                                    offers =
                                        listOf(
                                            mockOffer(
                                                "o1",
                                                isBuy = true,
                                                amountLabel = "500.00 EUR",
                                                priceLabel = "50,000",
                                                offerDateMillis = 1_000L,
                                            ),
                                        ),
                                ),
                            ),
                    ),
                onOfferClick = {},
                onNonTakeableOfferClick = {},
            )
            BisqText.SmallRegularGrey(text = "↓ action stack starts immediately here, no gap")
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "Contact, never traded — \"Trade with\" header variant (§4)")
@Composable
private fun PeerProfileOffersSection_ContactOnlyHeaderPreview() {
    BisqTheme.Preview {
        PeerProfileOffersSection(
            peerDisplayName = PREVIEW_PEER_NAME,
            hasTradedBefore = false,
            isContact = true,
            state =
                PeerProfileOffersSectionState.Loaded(
                    groups =
                        listOf(
                            MarketOffersGroup(
                                marketLabel = "EUR",
                                offers =
                                    listOf(
                                        mockOffer(
                                            "o1",
                                            isBuy = true,
                                            amountLabel = "100.00 - 500.00 EUR",
                                            priceLabel = "50,000",
                                            offerDateMillis = 2_000L,
                                        ),
                                        mockOffer(
                                            "o2",
                                            isBuy = false,
                                            amountLabel = "200.00 EUR",
                                            priceLabel = "51,200",
                                            offerDateMillis = 1_000L,
                                        ),
                                    ),
                            ),
                        ),
                ),
            onOfferClick = {},
            onNonTakeableOfferClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Syncing — client cold-start cache lag")
@Composable
private fun PeerProfileOffersSection_SyncingPreview() {
    BisqTheme.Preview {
        PeerProfileOffersSection(
            peerDisplayName = PREVIEW_PEER_NAME,
            hasTradedBefore = true,
            isContact = false,
            state = PeerProfileOffersSectionState.Syncing,
            onOfferClick = {},
            onNonTakeableOfferClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "One non-takeable offer (insufficient reputation)")
@Composable
private fun PeerProfileOffersSection_NonTakeablePreview() {
    BisqTheme.Preview {
        PeerProfileOffersSection(
            peerDisplayName = PREVIEW_PEER_NAME,
            hasTradedBefore = true,
            isContact = false,
            state =
                PeerProfileOffersSectionState.Loaded(
                    groups =
                        listOf(
                            MarketOffersGroup(
                                marketLabel = "EUR",
                                offers =
                                    listOf(
                                        mockOffer(
                                            "o1",
                                            isBuy = true,
                                            amountLabel = "500.00 EUR",
                                            priceLabel = "50,000",
                                            offerDateMillis = 2_000L,
                                        ),
                                        mockOffer(
                                            "o2",
                                            isBuy = false,
                                            amountLabel = "5,000.00 EUR",
                                            priceLabel = "51,000",
                                            offerDateMillis = 1_000L,
                                            isTakeable = false,
                                        ),
                                    ),
                            ),
                        ),
                ),
            onOfferClick = {},
            onNonTakeableOfferClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Stress — many offers, many markets, newest-first group order")
@Composable
private fun PeerProfileOffersSection_ManyMarketsPreview() {
    BisqTheme.Preview {
        // List order below is EUR, USD, GBP (alphabetical); actual dates make GBP the most recent
        // market, then EUR, then USD oldest — rendered order should be GBP, EUR, USD, matching
        // neither the list order nor the alphabetical order, to make the sort visibly obvious.
        PeerProfileOffersSection(
            peerDisplayName = PREVIEW_PEER_NAME,
            hasTradedBefore = true,
            isContact = false,
            state =
                PeerProfileOffersSectionState.Loaded(
                    groups =
                        listOf(
                            MarketOffersGroup(
                                marketLabel = "EUR",
                                offers =
                                    listOf(
                                        mockOffer(
                                            "o1",
                                            isBuy = true,
                                            amountLabel = "100.00 - 500.00 EUR",
                                            priceLabel = "50,000",
                                            offerDateMillis = 3_000L,
                                        ),
                                        mockOffer(
                                            "o2",
                                            isBuy = false,
                                            amountLabel = "200.00 EUR",
                                            priceLabel = "51,200",
                                            offerDateMillis = 2_500L,
                                        ),
                                        mockOffer(
                                            "o3",
                                            isBuy = true,
                                            amountLabel = "50.00 - 150.00 EUR",
                                            priceLabel = "49,800",
                                            offerDateMillis = 2_000L,
                                        ),
                                    ),
                            ),
                            MarketOffersGroup(
                                marketLabel = "USD",
                                offers =
                                    listOf(
                                        mockOffer(
                                            "o4",
                                            isBuy = true,
                                            amountLabel = "50.00 - 300.00 USD",
                                            priceLabel = "52,800",
                                            offerDateMillis = 1_500L,
                                        ),
                                        mockOffer(
                                            "o5",
                                            isBuy = false,
                                            amountLabel = "1,000.00 USD",
                                            priceLabel = "53,100",
                                            offerDateMillis = 1_000L,
                                            isTakeable = false,
                                        ),
                                    ),
                            ),
                            MarketOffersGroup(
                                marketLabel = "GBP",
                                offers =
                                    listOf(
                                        mockOffer(
                                            "o6",
                                            isBuy = true,
                                            amountLabel = "80.00 - 240.00 GBP",
                                            priceLabel = "44,300",
                                            offerDateMillis = 4_000L,
                                        ),
                                    ),
                            ),
                        ),
                ),
            onOfferClick = {},
            onNonTakeableOfferClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "In context — below reputation, above action stack")
@Composable
private fun PeerProfileOffersSection_InContextPreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding)) {
            SimulatedProfileHeader()
            BisqGap.V2()
            PeerProfileOffersSection(
                peerDisplayName = PREVIEW_PEER_NAME,
                hasTradedBefore = true,
                isContact = false,
                state =
                    PeerProfileOffersSectionState.Loaded(
                        groups =
                            listOf(
                                MarketOffersGroup(
                                    marketLabel = "EUR",
                                    offers =
                                        listOf(
                                            mockOffer(
                                                "o1",
                                                isBuy = true,
                                                amountLabel = "500.00 EUR",
                                                priceLabel = "50,000",
                                                offerDateMillis = 1_000L,
                                            ),
                                        ),
                                ),
                            ),
                    ),
                onOfferClick = {},
                onNonTakeableOfferClick = {},
            )
            BisqGap.V2()
            BisqText.SmallRegularGrey(text = "[ Send private message ]  (action stack follows)")
        }
    }
}
