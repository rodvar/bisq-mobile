package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

@Composable
fun SplashScreen(
) {
    val strings = LocalStrings.current
    val presenter: SplashPresenter = koinInject()

    RememberPresenterLifecycle(presenter)

    BisqStaticScaffold(verticalArrangement = Arrangement.SpaceBetween) {
        BisqLogo()

        Column {
            BisqProgressBar(presenter.progress.collectAsState().value)

            // TODO: Get this from presenter
            val networkType = strings.application.splash_bootstrapState_network_TOR

            BisqText.baseRegularGrey(
                text = presenter.state.collectAsState().value,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
