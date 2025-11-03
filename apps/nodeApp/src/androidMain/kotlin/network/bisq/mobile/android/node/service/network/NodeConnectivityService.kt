package network.bisq.mobile.android.node.service.network

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.android.node.BuildNodeConfig

import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.network.ConnectivityService

class NodeConnectivityService(
    private val nodeNetworkServiceFacade: NodeNetworkServiceFacade,
    private val settingsRepository: SettingsRepository,
) : ConnectivityService() {

    private var hasOnceReceivedAllData: Boolean = false
    private var collectJob: Job? = null

    // Activated after application service is initialized.
    override fun activate() {
        collectJob?.cancel()
        collectJob = serviceScope.launch {
            // Load persisted flag before collecting status changes, with version gating
            val settings = try {
                settingsRepository.fetch()
            } catch (e: Exception) {
                log.w(e) { "Failed to load settings during connectivity activation" }
                null
            }
            val isFirstRunAfterUpgrade = settings?.lastSeenNodeAppVersion != BuildNodeConfig.APP_VERSION
            hasOnceReceivedAllData = if (isFirstRunAfterUpgrade) false else (settings?.everReceivedAllData ?: false)
            // Do not update lastSeenNodeAppVersion yet; wait until we have full data in this version

            combine(nodeNetworkServiceFacade.numConnections, nodeNetworkServiceFacade.allDataReceived) { numConnections, allDataReceived ->
                numConnections to allDataReceived
            }.collect { (numConnections, allDataReceived) ->
                // allDataReceived in the network layer will get reset to false when we lose all connections.
                // We keep whether we've ever seen full data to distinguish reconnect flows
                if (allDataReceived && !hasOnceReceivedAllData) {
                    hasOnceReceivedAllData = true
                    // Persist flags for this version once we have full data
                    try {
                        settingsRepository.update {
                            it.copy(
                                everReceivedAllData = true,
                                lastSeenNodeAppVersion = BuildNodeConfig.APP_VERSION
                            )
                        }
                    } catch (e: Exception) {
                        log.w(e) { "Failed to persist connectivity flags" }
                    }
                }

                if (numConnections < 0) {
                    _status.value = ConnectivityStatus.BOOTSTRAPPING
                } else if (numConnections == 0) {
                    if (hasOnceReceivedAllData) {
                        _status.value = ConnectivityStatus.RECONNECTING
                    } else {
                        _status.value = ConnectivityStatus.DISCONNECTED
                    }
                } else {
                    if (allDataReceived) {
                        _status.value = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
                    } else {
                        _status.value = ConnectivityStatus.REQUESTING_INVENTORY
                    }
                }
            }
        }
    }

    override fun deactivate() {
        collectJob?.cancel()
        collectJob = null
    }
}
