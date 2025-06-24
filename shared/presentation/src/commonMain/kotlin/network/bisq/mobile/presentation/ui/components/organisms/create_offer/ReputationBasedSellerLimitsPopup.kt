package network.bisq.mobile.presentation.ui.components.organisms.create_offer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqConfig
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.GreyCloseButton
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.dialog.BisqDialog
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun ReputationBasedSellerLimitsPopup(
    onDismiss: () -> Unit,
    onBuildRepLinkClick: () -> Unit,
    reputationScore: String,
    maxSellAmount: String
) {

    BisqDialog(
        horizontalAlignment = Alignment.Start,
        padding = BisqUIConstants.ScreenPadding,
        onDismissRequest = onDismiss,
    ) {

        BisqText.h6Medium("bisqEasy.tradeWizard.amount.limitInfo.overlay.headline".i18n())

        BisqGap.V1()

        BisqText.baseRegular("bisqEasy.tradeWizard.amount.seller.limitInfo.overlay".i18n(reputationScore, maxSellAmount))

        BisqGap.V3()

        LinkButton(
            text = "bisqEasy.tradeWizard.amount.limitInfo.overlay.learnHowToBuildReputation".i18n(),
            link = BisqConfig.REPUTATION_BUILD_WIKI_URL,
            type = BisqButtonType.Outline,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
            fullWidth = true,
            onClick = onBuildRepLinkClick
        )

        BisqGap.V1()

        GreyCloseButton(onClick = onDismiss)
    }
}