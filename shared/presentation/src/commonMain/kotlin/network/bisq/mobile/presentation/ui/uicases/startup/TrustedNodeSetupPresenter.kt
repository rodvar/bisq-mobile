package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _isChangingTrustedNode = MutableStateFlow(false)
    override val isChangingTrustedNode: StateFlow<Boolean> = _isChangingTrustedNode

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
        val previousUrl = _bisqApiUrl.value
        _bisqApiUrl.value = newUrl
        _isBisqApiUrlValid.value = isValid
        
        // Check if this is a change from a previously valid URL
        if (previousUrl.isNotEmpty() && previousUrl != newUrl && _isConnected.value) {
            _isChangingTrustedNode.value = true
        }
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
                        _isConnected.value = true
                        
                        // If we're changing trusted nodes, reset profile and redirect
                        if (_isChangingTrustedNode.value) {
                            withContext(IODispatcher) {
                                userRepository.fetch()?.let {
                                    userRepository.delete(it)
                                }
                            }
                            
                            showSnackbar("Trusted node changed. You'll need to set up your profile again.")
                            _isChangingTrustedNode.value = false
                            
                            // Navigate to onboarding
                            if (!isWorkflow) {
                                navigateTo(Routes.Onboarding)
                            }
                        } else if (!isWorkflow) {
                            navigateBack()
                        }
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
            bisqApiUrl = _bisqApiUrl.value
            firstLaunch = currentSettings?.firstLaunch ?: true
        }
        settingsRepository.update(updatedSettings)
    }

    override fun navigateToNextScreen(isWorkflow: Boolean) {
        // access to profile setup should be handled by splash
        log.d { "Navigating to next screen (Workflow: ${isWorkflow}" }
        if (isWorkflow) {
            navigateTo(Routes.Onboarding)
        } else {
            navigateTo(Routes.TabContainer)
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
