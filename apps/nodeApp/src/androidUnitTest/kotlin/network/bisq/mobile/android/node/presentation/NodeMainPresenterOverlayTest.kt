package network.bisq.mobile.android.node.presentation

import network.bisq.mobile.domain.service.network.ConnectivityService.ConnectivityStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NodeMainPresenterOverlayTest {


    @Test
    fun bootstrapping_status_does_not_show_overlay() {
        val show = NodeMainPresenter.shouldShowReconnectOverlay(
            status = ConnectivityStatus.BOOTSTRAPPING
        )
        assertFalse(show)
    }

    @Test
    fun requesting_inventory_status_does_not_show_overlay() {
        val show = NodeMainPresenter.shouldShowReconnectOverlay(
            status = ConnectivityStatus.REQUESTING_INVENTORY
        )
        assertFalse(show)
    }

    @Test
    fun reconnecting_always_shows() {
        val show = NodeMainPresenter.shouldShowReconnectOverlay(
            status = ConnectivityStatus.RECONNECTING
        )
        assertTrue(show)
    }

    @Test
    fun connected_and_disconnected_do_not_show() {
        val connected = NodeMainPresenter.shouldShowReconnectOverlay(
            status = ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED
        )
        val disconnected = NodeMainPresenter.shouldShowReconnectOverlay(
            status = ConnectivityStatus.DISCONNECTED
        )
        assertFalse(connected)
        assertFalse(disconnected)
    }
}

