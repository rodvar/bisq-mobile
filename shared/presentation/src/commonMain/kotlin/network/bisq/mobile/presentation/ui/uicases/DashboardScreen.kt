package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_chat
import bisqapps.shared.presentation.generated.resources.icon_markets
import bisqapps.shared.presentation.generated.resources.icon_payment
import bisqapps.shared.presentation.generated.resources.reputation
import bisqapps.shared.presentation.generated.resources.thumbs_up
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.components.molecules.AmountWithCurrency
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun DashboardScreen() {
    val presenter: DashboardPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val offersOnline: Number by presenter.offersOnline.collectAsState()
    val publishedProfiles: Number by presenter.publishedProfiles.collectAsState()
    val numConnections by presenter.numConnections.collectAsState()
    val isInteractive by presenter.isInteractive.collectAsState()
    val marketPrice by presenter.marketPrice.collectAsState()
    val tradeRulesConfirmed by presenter.tradeRulesConfirmed.collectAsState()

    DashboardContent(
        offersOnline = offersOnline,
        publishedProfiles = publishedProfiles,
        numConnections = numConnections,
        isInteractive = isInteractive,
        marketPrice = marketPrice,
        tradeRulesConfirmed = tradeRulesConfirmed,
        onNavigateToMarkets = presenter::onNavigateToMarkets,
        onOpenTradeGuide = presenter::onOpenTradeGuide
    )
}

@Composable
private fun DashboardContent(
    offersOnline: Number,
    publishedProfiles: Number,
    numConnections: Number,
    isInteractive: Boolean,
    marketPrice: String,
    tradeRulesConfirmed: Boolean,
    onNavigateToMarkets: () -> Unit,
    onOpenTradeGuide: () -> Unit
) {
    val padding = BisqUIConstants.ScreenPadding
    BisqScrollLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.spacedBy(padding),
        isInteractive = isInteractive,
    ) {

        Column {
            HomeInfoCard(
                price = marketPrice,
                text = "dashboard.marketPrice".i18n()
            )
            BisqGap.V1()

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(padding)
            ) {
                HomeInfoCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    price = offersOnline.toString(),
                    text = "dashboard.offersOnline".i18n()
                )
                HomeInfoCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    price = numConnections.toString(),
                    text = "dashboard.numConnections".i18n()
                )
            }
        }

        Spacer(modifier = Modifier.fillMaxHeight().weight(0.1f))
        if (tradeRulesConfirmed) {
            DashBoardCard(
                title = "mobile.dashboard.startTrading.headline".i18n(),
                bulletPoints = listOf(
                    Pair("mobile.dashboard.main.content1".i18n(), Res.drawable.icon_markets),
                    Pair("mobile.dashboard.main.content2".i18n(), Res.drawable.icon_chat),
                    Pair("mobile.dashboard.main.content3".i18n(), Res.drawable.reputation)
                ),
                buttonText = "mobile.dashboard.startTrading.button".i18n(),
                buttonHandler = { onNavigateToMarkets() }
            )
        } else {
            DashBoardCard(
                title = "mobile.dashboard.tradeGuide.headline".i18n(),
                bulletPoints = listOf(
                    Pair("mobile.dashboard.tradeGuide.bulletPoint1".i18n(), Res.drawable.thumbs_up),
                    Pair("bisqEasy.onboarding.top.content2".i18n(), Res.drawable.icon_payment),
                    Pair("bisqEasy.onboarding.top.content3".i18n(), Res.drawable.icon_chat)
                ),
                buttonText = "support.resources.guides.tradeGuide".i18n(),
                buttonHandler = { onOpenTradeGuide() }
            )
        }
        Spacer(modifier = Modifier.fillMaxHeight().weight(0.2f))
    }
}

@Composable
fun DashBoardCard(
    title: String,
    bulletPoints: List<Pair<String, DrawableResource>>,
    buttonText: String,
    buttonHandler: () -> Unit
) {
    BisqCard(
        padding = BisqUIConstants.ScreenPadding2X,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
    ) {
//        BisqText.h1Light(title)
        AutoResizeText(
            title, maxLines = 1,
            textStyle = BisqTheme.typography.h1Light,
            color = BisqTheme.colors.white,
            textAlign = TextAlign.Start,
//            lineHeight = lineHeight,
//            overflow = overflow,
//            modifier = modifier
        )

        Column {
            bulletPoints.forEach { (pointKey, icon) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = BisqUIConstants.ScreenPadding)
                ) {
                    Image(
                        painterResource(icon), "",
                        modifier = Modifier.size(30.dp)
                    )
                    BisqGap.H1()
                    BisqText.baseLight(pointKey)
                }
            }
        }

        BisqButton(
            buttonText,
            fullWidth = true,
            onClick = buttonHandler,
        )
    }
}

@Composable
fun HomeInfoCard(modifier: Modifier = Modifier, price: String, text: String) {
    BisqCard(
        modifier = modifier,
        borderRadius = BisqUIConstants.ScreenPaddingQuarter,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AmountWithCurrency(price) // TODO should be generic
        BisqGap.V1()

        BisqText.smallRegularGrey(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}