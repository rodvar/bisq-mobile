package network.bisq.mobile.domain.service.bootstrap

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.ServiceFacade

@Suppress("RedundantOverride")
abstract class ApplicationBootstrapFacade : ServiceFacade() {
    companion object {
        var isDemo = false
    }

    private val _state = MutableStateFlow("")
    val state: StateFlow<String> get() = _state.asStateFlow()
    fun setState(value: String) {
        _state.value = value
    }

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> get() = _progress.asStateFlow()
    fun setProgress(value: Float) {
        _progress.value = value
    }

    private val _isTimeoutDialogVisible = MutableStateFlow(false)
    val isTimeoutDialogVisible: StateFlow<Boolean> get() = _isTimeoutDialogVisible.asStateFlow()
    fun setTimeoutDialogVisible(visible: Boolean) {
        _isTimeoutDialogVisible.value = visible
    }

    private val _isBootstrapFailed = MutableStateFlow(false)
    val isBootstrapFailed: StateFlow<Boolean> get() = _isBootstrapFailed.asStateFlow()
    fun setBootstrapFailed(failed: Boolean) {
        _isBootstrapFailed.value = failed
    }

    private val _currentBootstrapStage = MutableStateFlow("")
    val currentBootstrapStage: StateFlow<String> get() = _currentBootstrapStage.asStateFlow()
    fun setCurrentBootstrapStage(stage: String) {
        _currentBootstrapStage.value = stage
    }

    private val _shouldShowProgressToast = MutableStateFlow(false)
    val shouldShowProgressToast: StateFlow<Boolean> get() = _shouldShowProgressToast.asStateFlow()
    fun setShouldShowProgressToast(show: Boolean) {
        _shouldShowProgressToast.value = show
    }

    protected var isActive = false

    override fun activate() {
        super.activate()
    }

    override fun deactivate() {
        super.deactivate()
    }

    /**
     * Waits for Tor initialization to complete if Tor is required.
     * For CLEARNET-only configurations, this returns immediately.
     * For Tor configurations, this suspends until Tor is fully initialized.
     */
    abstract suspend fun waitForTor()

    /**
     * Stop the current bootstrap process and prepare for retry.
     * This should purposely fail the bootstrap and show the retry button.
     */
    open suspend fun stopBootstrapForRetry() {
        log.i { "Bootstrap: Stopping bootstrap for retry" }
        setBootstrapFailed(true)
        setTimeoutDialogVisible(false)
    }

    /**
     * Retry the bootstrap process from the beginning.
     * This should clean up any existing state and restart the entire bootstrap.
     */
    open fun retryBootstrap() {
        log.i { "Bootstrap: Retrying bootstrap process" }
        setBootstrapFailed(false)
        setTimeoutDialogVisible(false)
        deactivate()
        activate()
    }
}