package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferPaymentMethodScreen() {
    val presenter: CreateOfferPaymentMethodPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val selectedQuoteSidePaymentMethods: MutableStateFlow<Set<String>> = remember { presenter.selectedQuoteSidePaymentMethods }

    MultiScreenWizardScaffold(
        "mobile.bisqEasy.createOffer.progress.quoteSidePaymentMethod".i18n(),
        stepIndex = 5,
        stepsLength = 7,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onQuoteSideNext() },
        snackbarHostState = presenter.getSnackState(),
        showUserAvatar = false,
        extraActions = {
            BisqIconButton(onClick = {
                presenter.onClose()
            }, size = BisqUIConstants.topBarAvatarSize) {
                CloseIcon()
            }
        },
    ) {
        BisqText.h3Regular("mobile.bisqEasy.createOffer.paymentMethods.headline.fiat".i18n())

        BisqGap.V1()

        PaymentMethodCard(
            title = presenter.quoteSideHeadline,
            imagePaths = presenter.getQuoteSidePaymentMethodsImagePaths(),
            availablePaymentMethods = presenter.availableQuoteSidePaymentMethods,
            selectedPaymentMethods = selectedQuoteSidePaymentMethods,
            onToggle = { selected -> presenter.onToggleQuoteSidePaymentMethod(selected) },
        )
    }
}
