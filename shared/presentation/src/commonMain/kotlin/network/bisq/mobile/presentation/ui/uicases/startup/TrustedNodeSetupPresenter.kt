package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

class TrustedNodeSetupPresenter(
    mainPresenter: MainPresenter,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val webSocketClientProvider: WebSocketClientProvider
) : BasePresenter(mainPresenter), ITrustedNodeSetupPresenter {

    companion object {
        const val SAFEGUARD_TEST_TIMEOUT = 20000L
    }

    private val _isBisqApiUrlValid = MutableStateFlow(true)
    override val isBisqApiUrlValid: StateFlow<Boolean> = _isBisqApiUrlValid

    private val _isBisqApiVersionValid = MutableStateFlow(true)
    override val isBisqApiVersionValid: StateFlow<Boolean> = _isBisqApiVersionValid

    private val _bisqApiUrl = MutableStateFlow("ws://10.0.2.2:8090")
    override val bisqApiUrl: StateFlow<String> = _bisqApiUrl

    private val _trustedNodeVersion = MutableStateFlow("")
    override val trustedNodeVersion: StateFlow<String> = _trustedNodeVersion

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading

    override fun onViewAttached() {
        super.onViewAttached()
        initialize()
    }

    private fun initialize() {
        log.i { "View attached to Trusted node presenter" }

        launchUI {
            try {
                val data = withContext(IODispatcher) {
                    settingsRepository.fetch()
                }
                data?.let {
                    updateBisqApiUrl(it.bisqApiUrl, true)
                    validateVersion()
                }
            } catch (e: Exception) {
                log.e("Failed to load from repository", e)
            }
        }
    }

    override fun updateBisqApiUrl(newUrl: String, isValid: Boolean) {
        // log.w { "$newUrl: $isValid" }
        _isBisqApiUrlValid.value = isValid
        _bisqApiUrl.value = newUrl
        _isConnected.value = false
    }

    override fun validateWsUrl(url: String): String? {
        val wsUrlPattern =
            """^(ws|wss):\/\/(([a-zA-Z0-9.-]+\.[a-zA-Z]{2,}|localhost)|(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}))(:\d{1,5})$""".toRegex()
        if (url.isEmpty()) {
            return "URL cannot be empty" //TODO:i18n
        }
        if (!wsUrlPattern.matches(url)) {
            return "Invalid WebSocket URL. Must be ws:// or wss:// followed by a domain/IP and port" //TODO:i18n
        }
        return null
    }

    override fun testConnection(isWorkflow: Boolean) {
        if (!isWorkflow) {
            // FIXME temporary solution to avoid changing node without the corresponding profile reset
            showSnackbar("If you want to use a different node, you need to remove the app storage or uninstall/reinstall")
            return
        }
        _isLoading.value = true
        log.d { "Test: ${_bisqApiUrl.value} isWorkflow $isWorkflow" }
        val connectionSettings = WebSocketClientProvider.parseUri(_bisqApiUrl.value)

        val connectionJob = launchUI {
            try {
                // Add a timeout to prevent indefinite waiting
                val success = withTimeout(15000) { // 15 second timeout
                    withContext(IODispatcher) {
                        return@withContext webSocketClientProvider.testClient(connectionSettings.first, connectionSettings.second)
                    }
                }

                if (success) {
                    val isCompatibleVersion = withContext(IODispatcher) {
                        updateTrustedNodeSettings()
                        delay(DEFAULT_DELAY)
                        webSocketClientProvider.get().await()
                        validateVersion()
                    }
                    if (isCompatibleVersion) {
                        log.d { "Connected successfully to ${_bisqApiUrl.value} is workflow: $isWorkflow" }
                        showSnackbar("Connected successfully to ${_bisqApiUrl.value}, settings updated")
                        if (!isWorkflow) {
                            _isLoading.value = false
                            navigateBack()
                        }
                        _isConnected.value = true
                    } else {
                        webSocketClientProvider.get().disconnect(isTest = true)
                        log.d { "Invalid version cannot connect" }
                        showSnackbar("Trusted node incompatible version, cannot connect")
                        _isConnected.value = false
                    }
                } else {
                    showSnackbar("Could not connect to given url ${_bisqApiUrl.value}, please try again with another setup")
                    _isConnected.value = false
                }
            } catch (e: TimeoutCancellationException) {
                log.e(e) { "Connection test timed out after 15 seconds" }
                showSnackbar("Connection timed out. Please check if the trusted node is running and accessible.")
                _isConnected.value = false
            } catch (e: Exception) {
                log.e(e) { "Error testing connection: ${e.message}" }
                showSnackbar("Error connecting: ${e.message ?: "Unknown error"}")
                _isConnected.value = false
            } finally {
                _isLoading.value = false
            }
        }

        launchUI {
            delay(SAFEGUARD_TEST_TIMEOUT) // 20 seconds as a fallback
            if (_isLoading.value) {
                log.w { "Force stopping connection test after 20 seconds" }
                connectionJob.cancel()
                _isLoading.value = false
                showSnackbar("Connection test took too long. Please try again.")
            }
        }
    }

    private suspend fun updateTrustedNodeSettings() {
        val currentSettings = settingsRepository.fetch()
        val updatedSettings = Settings().apply {
            this.bisqApiUrl = _bisqApiUrl.value
            this.firstLaunch = currentSettings?.firstLaunch ?: this.firstLaunch
            this.showChatRulesWarnBox = currentSettings?.showChatRulesWarnBox ?: this.showChatRulesWarnBox
            this.id = currentSettings?.id ?: this.id
            this.forceReconnect = true
        }
        settingsRepository.update(updatedSettings)
    }

    override fun navigateToNextScreen() {
        launchUI {
            try {
                // Check if user exists in local repository
                val user = withContext(IODispatcher) {
                    userRepository.fetch()
                }
                if (user?.uniqueAvatar != null) {
                    // If user exists locally, navigate to home
                    log.d { "User profile exists locally, navigating to home" }
                    navigateTo(Routes.TabContainer) {
                        it.popUpTo(Routes.Splash.name) { inclusive = true }
                    }
                } else {
                    // If no local user, navigate to profile creation
                    log.d { "No local user profile, navigating to profile creation" }
                    navigateTo(Routes.CreateProfile)
                }
            } catch (e: Exception) {
                log.e(e) { "Error checking user profile status" }
                // Default to going back if we can't determine profile status
                goBackToSetupScreen()
            }
        }
    }

    override fun goBackToSetupScreen() {
        navigateBack()
    }

    override suspend fun validateVersion(): Boolean {
        _isBisqApiVersionValid.value = false
        _trustedNodeVersion.value = settingsServiceFacade.getTrustedNodeVersion()
        if (settingsServiceFacade.isApiCompatible()) {
            _isBisqApiVersionValid.value = true
            return true
        } else {
            _isBisqApiVersionValid.value = false
            return false
        }
    }
}
