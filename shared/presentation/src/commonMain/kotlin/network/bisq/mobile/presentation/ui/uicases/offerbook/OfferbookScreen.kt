package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqFABAddButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WebLinkConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

const val MAX_LOADING_TIME_MS = 2500L

@Composable
fun OfferbookScreen() {
    val presenter: OfferbookPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val sortedFilteredOffers by presenter.sortedFilteredOffers.collectAsState()
    val selectedDirection by presenter.selectedDirection.collectAsState()
    val showDeleteConfirmation by presenter.showDeleteConfirmation.collectAsState()
    val showNotEnoughReputationDialog by presenter.showNotEnoughReputationDialog.collectAsState()
    val isInteractive by presenter.isInteractive.collectAsState()
    val selectedMarket by presenter.selectedMarket.collectAsState()

    // Show a loading overlay while offers are being fetched (per market, direction)
    var showLoading by remember { mutableStateOf(true) }

    // Reset and start a short grace period every time market or tab (direction) changes; hide early if data arrives.
    LaunchedEffect(selectedMarket, selectedDirection) {
        showLoading = true
        // Allow data to arrive; if still empty after the delay, fall back to real empty state.
        delay(MAX_LOADING_TIME_MS)
        if (sortedFilteredOffers.isEmpty()) showLoading = false
    }
    // If offers arrive sooner, immediately hide the loading overlay.
    LaunchedEffect(sortedFilteredOffers) {
        if (sortedFilteredOffers.isNotEmpty()) showLoading = false
    }

    BisqStaticScaffold(
        topBar = {
            val quoteCode = selectedMarket?.market?.quoteCurrencyCode
                ?.takeIf { it.isNotBlank() }
                ?.uppercase()
            TopBar(title = "mobile.offerbook.title".i18n(quoteCode ?: "—"))
        },
        floatingButton = {
            BisqFABAddButton(
                onClick = { presenter.createOffer() },
                enabled = !presenter.isDemo()
            )
        },
        isInteractive = isInteractive,
        shouldBlurBg = showDeleteConfirmation || showNotEnoughReputationDialog
    ) {
        DirectionToggle(
            selectedDirection,
            onStateChange = { direction -> presenter.onSelectDirection(direction) }
        )

        val filterUi by presenter.filterUiState.collectAsState()

        // Track bottom sheet expansion at the screen level to avoid auto-closing when we temporarily hide.
        var filterExpanded by remember { mutableStateOf(false) }

        // Hide the filter controller when there are no offers and no filters are active,
        // but keep it visible if the bottom sheet is currently expanded.
        val shouldShowFilter = filterUi.hasActiveFilters || sortedFilteredOffers.isNotEmpty() || filterExpanded
        if (shouldShowFilter) {
            BisqGap.V1()
            OfferbookFilterController(
                state = filterUi,
                onTogglePayment = presenter::togglePaymentMethod,
                onToggleSettlement = presenter::toggleSettlementMethod,
                onOnlyMyOffersChange = presenter::setOnlyMyOffers,
                onClearAll = presenter::clearAllFilters,
                onSetPaymentSelection = presenter::setPaymentSelection,
                onSetSettlementSelection = presenter::setSettlementSelection,
                isExpanded = filterExpanded,
                onExpandedChange = { filterExpanded = it },
            )
        }


        if (sortedFilteredOffers.isEmpty() && !showLoading) {
            NoOffersSection(presenter)
            return@BisqStaticScaffold
        }

        BisqGap.V1()

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(items = sortedFilteredOffers, key = { it.offerId }) { item ->
                OfferCard(
                    item,
                    onSelectOffer = {
                        presenter.onOfferSelected(item)
                    },
                    userProfileIconProvider = presenter.userProfileIconProvider
                )
            }
        }
        // Loading overlay for initial fetches on slow connections (e.g., Tor - specially on Connect)
        if (showLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BisqTheme.colors.primary)
            }
        }
    }

    if (showDeleteConfirmation) {
        ConfirmationDialog(
            headline = if (presenter.isDemo()) "Delete disabled on demo mode" else "bisqEasy.offerbook.chatMessage.deleteOffer.confirmation".i18n(),
            onConfirm = { presenter.onConfirmedDeleteOffer() },
            onDismiss = { presenter.onDismissDeleteOffer() }
        )
    }

    if (showNotEnoughReputationDialog) {
        if (presenter.isReputationWarningForSellerAsTaker) {
            ConfirmationDialog(
                headline = presenter.notEnoughReputationHeadline,
                headlineLeftIcon = { WarningIcon() },
                headlineColor = BisqTheme.colors.warning,
                message = presenter.notEnoughReputationMessage,
                confirmButtonText = "confirmation.yes".i18n(),
                dismissButtonText = "action.cancel".i18n(),
                onConfirm = { presenter.onNavigateToReputation() },
                onDismiss = { presenter.onDismissNotEnoughReputationDialog() }
            )
        } else {
            WebLinkConfirmationDialog(
                link = BisqLinks.REPUTATION_WIKI_URL,
                headline = presenter.notEnoughReputationHeadline,
                headlineLeftIcon = { WarningIcon() },
                headlineColor = BisqTheme.colors.warning,
                message = presenter.notEnoughReputationMessage,
                confirmButtonText = "confirmation.yes".i18n(),
                dismissButtonText = "hyperlinks.openInBrowser.no".i18n(),
                onConfirm = { presenter.onOpenReputationWiki() },
                onDismiss = { presenter.onDismissNotEnoughReputationDialog() }
            )
        }
    }
}

@Composable
fun NoOffersSection(presenter: OfferbookPresenter) {
    Column(
        modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding4X).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BisqText.h4LightGrey(
            text = "mobile.offerBookScreen.noOffersSection.thereAreNoOffers".i18n(), // There are no offers
            textAlign = TextAlign.Center
        )
        BisqGap.V4()
        BisqButton(
            text = "offer.create".i18n(),
            onClick = presenter::createOffer
        )
    }
}