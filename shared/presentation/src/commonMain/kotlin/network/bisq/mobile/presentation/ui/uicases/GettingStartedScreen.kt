package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IGettingStarted : ViewPresenter {
    val title: String
    val bulletPoints: List<String>
    val offersOnline: StateFlow<Number>
    val publishedProfiles: StateFlow<Number>

    fun onStartTrading()
    fun navigateLearnMore()
}

@Composable
fun GettingStartedScreen() {
    val presenter: GettingStartedPresenter = koinInject()
    val tabPresenter: ITabContainerPresenter = koinInject()
    val offersOnline: Number = presenter.offersOnline.collectAsState().value
    val publishedProfiles: Number = presenter.publishedProfiles.collectAsState().value

    RememberPresenterLifecycle(presenter)

    BisqScrollLayout(
        padding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        isInteractive = presenter.isInteractive.collectAsState().value
    ) {

        Column {
            PriceProfileCard(
                price = presenter.formattedMarketPrice.collectAsState().value,
                priceText = "Market price"
            )
            BisqGap.V1()
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    PriceProfileCard(
                        price = offersOnline.toString(),
                        priceText = "Offers online"
                    )
                }
                BisqGap.H1()
                Box(modifier = Modifier.weight(1f)) {
                    PriceProfileCard(
                        price = publishedProfiles.toString(),
                        priceText = "Published profiles"
                    )
                }
            }
        }
        BisqButton("Trade guide", onClick = { presenter.navigateToGuide() })
        WelcomeCard(
            presenter = presenter,
            title = presenter.title,
            bulletPoints = presenter.bulletPoints,
            primaryButtonText = "Start Trading",
            footerLink = "action.learnMore".i18n()
        )
    }
}

@Composable
fun WelcomeCard(
    presenter: GettingStartedPresenter,
    title: String,
    bulletPoints: List<String>,
    primaryButtonText: String,
    footerLink: String
) {
    BisqCard(
        padding = BisqUIConstants.ScreenPadding2X,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
    ) {
        // Title
        BisqText.h4Regular(text = title)

        // Bullet Points
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalfQuarter)) {
            bulletPoints.forEach { point ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val whiteColor = BisqTheme.colors.white
                    Canvas(modifier = Modifier.size(BisqUIConstants.ScreenPaddingHalf)) {
                        drawCircle(color = whiteColor)
                    }
                    BisqGap.H1()
                    BisqText.smallMedium(text = point)
                }
            }
        }

        // Primary Button
        BisqButton(
            primaryButtonText,
            fullWidth = true,
            onClick = { presenter.onStartTrading() },
        )

        // Footer Link
        BisqButton(
            footerLink,
            color = BisqTheme.colors.primary,
            type = BisqButtonType.Clear,
            padding = PaddingValues(all = BisqUIConstants.Zero),
            onClick = { presenter.navigateLearnMore() }
        )
    }
}

@Composable
fun PriceProfileCard(price: String, priceText: String) {
    BisqCard(
        borderRadius = BisqUIConstants.ScreenPaddingQuarter,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BisqText.largeRegular(
            text = price,
            textAlign = TextAlign.Center,
        )

        BisqGap.V1()

        BisqText.smallRegularGrey(
            text = priceText,
            textAlign = TextAlign.Center,
        )
    }
}
