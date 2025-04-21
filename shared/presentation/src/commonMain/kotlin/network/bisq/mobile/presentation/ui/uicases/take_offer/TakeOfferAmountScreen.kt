package network.bisq.mobile.presentation.ui.uicases.take_offer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun TakeOfferTradeAmountScreen() {
    val presenter: TakeOfferAmountPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.amount".i18n(),
        stepIndex = 1,
        stepsLength = 3,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() }
    ) {
        BisqText.h3Regular("bisqEasy.takeOffer.amount.headline.buyer".i18n())
        BisqGap.V1()
        BisqText.largeLightGrey(
            // We get currency code appended but for formattedMinAmount we want to omit it in the string
            text = "bisqEasy.takeOffer.amount.description".i18n(
                presenter.formattedMinAmount,
                presenter.formattedMaxAmountWithCode
            ),
        )

        Spacer(modifier = Modifier.height(128.dp))

        BisqAmountSelector(
            presenter.quoteCurrencyCode,
            presenter.formattedMinAmountWithCode,
            presenter.formattedMaxAmountWithCode,
            presenter.sliderPosition,
            MutableStateFlow(0f), //todo
            MutableStateFlow(0f),
            presenter.formattedQuoteAmount,
            presenter.formattedBaseAmount,
            { sliderValue -> presenter.onSliderValueChanged(sliderValue) },
            { textInput -> presenter.onTextValueChanged(textInput) }
        )
    }
}