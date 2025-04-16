package network.bisq.mobile.presentation.ui.uicases.take_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.ui.components.organisms.offer.TakeOfferProgressDialog
import network.bisq.mobile.presentation.ui.components.organisms.offer.TakeOfferSuccessDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun TakeOfferReviewTradeScreen() {
    val presenter: TakeOfferReviewPresenter = koinInject()
    val showProgressDialog = presenter.showTakeOfferProgressDialog.collectAsState().value
    val showSuccessDialog = presenter.showTakeOfferSuccessDialog.collectAsState().value

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.review".i18n(),
        stepIndex = 3,
        stepsLength = 3,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "bisqEasy.takeOffer.review.takeOffer".i18n(),
        nextOnClick = { presenter.onTakeOffer() },
        snackbarHostState = presenter.getSnackState(),
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        BisqText.h3Regular("bisqEasy.takeOffer.progress.review".i18n())
        BisqGap.V2()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
        ) {
            InfoBox(
                label = "bisqEasy.tradeState.header.direction".i18n().uppercase(),
                value = presenter.headLine,
            )
            if (presenter.takersDirection.isBuy) {
                if (presenter.isSmallScreen()) {
                    InfoBox(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = presenter.amountToPay,
                    )
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = presenter.amountToReceive,
                        rightAlign = true
                    )
                } else {
                    InfoRowContainer {
                        InfoBox(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = presenter.amountToPay,
                        )
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = presenter.amountToReceive,
                            rightAlign = true
                        )
                    }
                }
            } else {
                if (presenter.isSmallScreen()) {
                    InfoBoxSats(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = presenter.amountToPay,
                    )
                    InfoBox(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = presenter.amountToReceive,
                        rightAlign = true
                    )
                } else {
                    InfoRowContainer {
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = presenter.amountToPay,
                        )
                        InfoBox(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = presenter.amountToReceive,
                            rightAlign = true
                        )
                    }
                }
            }
        }

        BisqHDivider()
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            InfoBox(
                label = "bisqEasy.tradeWizard.review.priceDescription.taker".i18n(),
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Regular(presenter.price)
                            BisqText.baseRegularGrey(presenter.marketCodes)
                        }
                        BisqText.smallRegularGrey(presenter.priceDetails)
                    }
                }
            )

            if (presenter.quoteSidePaymentMethodDisplayString.length + presenter.baseSidePaymentMethodDisplayString.length < 30) {
                InfoRow(
                    label1 = "bisqEasy.takeOffer.review.method.fiat".i18n(),
                    value1 = presenter.quoteSidePaymentMethodDisplayString,
                    label2 = "bisqEasy.takeOffer.review.method.bitcoin".i18n(),
                    value2 = presenter.baseSidePaymentMethodDisplayString,
                )
            } else {
                InfoBox(
                    label = "bisqEasy.takeOffer.review.method.fiat".i18n(),
                    value = presenter.quoteSidePaymentMethodDisplayString,
                )
                InfoBox(
                    label = "bisqEasy.takeOffer.review.method.bitcoin".i18n(),
                    value = presenter.baseSidePaymentMethodDisplayString,
                )
            }

            InfoBox(
                label = "bisqEasy.tradeWizard.review.feeDescription".i18n(),
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Regular(presenter.fee)
                        }
                        BisqText.smallRegularGrey(presenter.feeDetails)
                    }
                }
            )
        }

        if (showProgressDialog) {
            TakeOfferProgressDialog()
        }

        if (showSuccessDialog) {
            TakeOfferSuccessDialog(
                onShowTrades = { presenter.onGoToOpenTrades() }
            )
        }

    }
}