package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.CurrencyCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferCurrencySelectorScreen() {
    val presenter: CreateOfferMarketPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val searchText = presenter.searchText.collectAsState().value
    val filteredMarketList = presenter.marketListItemWithNumOffers.collectAsState().value

    MultiScreenWizardScaffold(
        "mobile.bisqEasy.tradeWizard.market.title".i18n(),
        stepIndex = 2,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        useStaticScaffold = true,
        horizontalAlignment = Alignment.Start,
        showUserAvatar = false,
        extraActions = {
            BisqIconButton(onClick = {
                presenter.onClose()
            }, modifier = Modifier.size(BisqUIConstants.topBarAvatarSize)){
                CloseIcon()
            }
        },
    ) {

        BisqText.h3Regular(presenter.headline)
        BisqGap.V1()

        BisqText.largeLightGrey("mobile.bisqEasy.tradeWizard.market.subTitle".i18n())

        BisqGap.V2()

        BisqSearchField(
            value = searchText,
            onValueChanged = { it, isValid -> presenter.setSearchText(it) },
        )

        BisqGap.V1()

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
        ) {
            items(filteredMarketList) { item ->
                CurrencyCard(
                    item,
                    isSelected = presenter.market == item.market,
                    onClick = { presenter.onSelectMarket(item) }
                )
            }
        }

        BisqGap.V1()
    }
}