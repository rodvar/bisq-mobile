package network.bisq.mobile.presentation.ui.uicases.take_offer

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun TakeOfferPaymentMethodScreen() {
    val presenter: TakeOfferPaymentMethodPresenter = koinInject()

    val baseSidePaymentMethod: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val quoteSidePaymentMethod: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    // Use filtered payment methods
    val filteredQuoteSidePaymentMethods by presenter.filteredQuoteSidePaymentMethods.collectAsState()

    LaunchedEffect(Unit) {
        presenter.baseSidePaymentMethod.collect { value ->
            baseSidePaymentMethod.value = value?.let { setOf(it) } ?: emptySet()
        }
    }

    LaunchedEffect(Unit) {
        presenter.quoteSidePaymentMethod.collect { value ->
            quoteSidePaymentMethod.value = value?.let { setOf(it) } ?: emptySet()
        }
    }

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.method".i18n(),
        stepIndex = 2,
        stepsLength = 3,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        snackbarHostState = presenter.getSnackState()
    ) {

        BisqText.h3Regular("bisqEasy.takeOffer.paymentMethods.headline.fiatAndBitcoin".i18n())

        // Show payment methods if there are multiple options OR if filtering resulted in fewer options
        if (presenter.hasMultipleQuoteSidePaymentMethods || filteredQuoteSidePaymentMethods.size != presenter.quoteSidePaymentMethods.size) {
            BisqGap.V2()
            BisqGap.V2()

            if (filteredQuoteSidePaymentMethods.isEmpty()) {
                // Show message when no compatible payment methods are available
                BisqText.baseRegular(
                    "No payment methods available for ${presenter.quoteCurrencyCode}. Please create a payment account that supports this currency.",
                    color = network.bisq.mobile.presentation.ui.theme.BisqTheme.colors.warning
                )
            } else {
                PaymentMethodCard(
                    title = "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.buyer".i18n(presenter.quoteCurrencyCode),
                    imagePaths = presenter.getQuoteSidePaymentMethodsImagePaths(),
                    availablePaymentMethods = filteredQuoteSidePaymentMethods,
                    selectedPaymentMethods = quoteSidePaymentMethod,
                    onToggle = { selected -> presenter.onQuoteSidePaymentMethodSelected(selected) },
                )
            }
        }

        if (presenter.hasMultipleBaseSidePaymentMethods) {
            BisqGap.V2()
            BisqGap.V2()

            PaymentMethodCard(
                title = "bisqEasy.takeOffer.paymentMethods.subtitle.bitcoin.seller".i18n(presenter.quoteCurrencyCode),
                imagePaths = presenter.getBaseSidePaymentMethodsImagePaths(),
                availablePaymentMethods = presenter.baseSidePaymentMethods,
                selectedPaymentMethods = baseSidePaymentMethod,
                onToggle = { selected -> presenter.onBaseSidePaymentMethodSelected(selected) },
            )
        }
    }

}