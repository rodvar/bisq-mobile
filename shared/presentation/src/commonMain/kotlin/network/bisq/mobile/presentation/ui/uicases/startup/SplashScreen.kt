package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun SplashScreen() {
    val presenter: SplashPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val progress by presenter.progress.collectAsState()
    val state by presenter.state.collectAsState()
    val isTimeoutDialogVisible by presenter.isTimeoutDialogVisible.collectAsState()
    val isBootstrapFailed by presenter.isBootstrapFailed.collectAsState()
    val currentBootstrapStage by presenter.currentBootstrapStage.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        BisqStaticScaffold(
            verticalArrangement = Arrangement.SpaceBetween,
            snackbarHostState = presenter.getSnackState()
        ) {
            BisqLogo()

            Column {
                BisqProgressBar(progress)

                BisqText.baseRegularGrey(
                    text = state,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Retry button overlay for failed bootstrap
        if (isBootstrapFailed && !isTimeoutDialogVisible) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BisqButton(
                    text = "bootstrap.retry".i18n(),
                    onClick = { presenter.onBootstrapFailedRetry() },
                    modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X)
                )
            }
        }

        // Timeout dialog
        if (isTimeoutDialogVisible) {
            ConfirmationDialog(
                headline = "bootstrap.timeout.title".i18n(),
                message = "bootstrap.timeout.message".i18n(currentBootstrapStage),
                confirmButtonText = "bootstrap.timeout.stop".i18n(),
                dismissButtonText = "bootstrap.timeout.continue".i18n(),
                onConfirm = { presenter.onTimeoutDialogStop() },
                onDismiss = { presenter.onTimeoutDialogContinue() }
            )
        }
    }
}
