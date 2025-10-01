package network.bisq.mobile.client.di

import network.bisq.mobile.client.ClientMainActivity
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.presentation.notification.NotificationControllerImpl
import org.koin.dsl.module

/**
 * Android Client-specific service module
 * Provides the NotificationControllerImpl with the Activity class
 */
val clientServiceModule = module {
    single<NotificationControllerImpl> {
        NotificationControllerImpl(get<AppForegroundController>(), ClientMainActivity::class.java)
    }
}