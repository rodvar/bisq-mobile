package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.NodeMainActivity
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.presentation.notification.NotificationControllerImpl
import org.koin.dsl.module

/**
 * Android Node-specific service module
 * Provides the NotificationControllerImpl with the Activity class
 */
val nodeServiceModule = module {
    single<NotificationControllerImpl> {
        NotificationControllerImpl(get<AppForegroundController>(), NodeMainActivity::class.java)
    }
}