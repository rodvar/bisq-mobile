package network.bisq.mobile.presentation.ui

import ErrorOverlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.SwipeBackIOSNavigationHandler
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.navigation.graph.RootNavGraph
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import kotlin.js.JsName

interface AppPresenter : ViewPresenter {
    var navController: NavHostController

    var tabNavController: NavHostController

    // Observables for state
    val isContentVisible: StateFlow<Boolean>

    val languageCode: StateFlow<String>

    // Add JsName annotation to avoid name clash
    @JsName("appIsSmallScreen")
    val isSmallScreen: StateFlow<Boolean>

    // Actions
    fun toggleContentVisibility()

    fun navigateToTrustedNode()
}

/**
 * Main composable view of the application that platforms use to draw.
 */
@Composable
@Preview
fun App() {

    val rootNavController = rememberNavController()
    val tabNavController = rememberNavController()
    var isNavControllerSet by remember { mutableStateOf(false) }
    val presenter: AppPresenter = koinInject()

    RememberPresenterLifecycle(presenter, {
        presenter.navController = rootNavController
        presenter.tabNavController = tabNavController
        isNavControllerSet = true
    })

    val languageCode = presenter.languageCode.collectAsState().value
    I18nSupport.initialize(languageCode)

    BisqTheme(darkTheme = true) {
        if (isNavControllerSet) {
            SwipeBackIOSNavigationHandler(rootNavController) {
                RootNavGraph(rootNavController)
            }
        }
        ErrorOverlay()
    }
}