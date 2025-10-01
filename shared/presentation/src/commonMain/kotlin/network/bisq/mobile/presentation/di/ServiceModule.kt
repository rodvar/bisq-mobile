package network.bisq.mobile.presentation.di

import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.presentation.notification.ForegroundServiceController
import network.bisq.mobile.presentation.notification.NotificationController
import network.bisq.mobile.presentation.service.OpenTradesNotificationService
import org.koin.core.scope.Scope
import org.koin.dsl.module

/**
 * Common service module with platform-specific implementations
 */
val serviceModule = module {
    // AppForegroundController - platform-specific instantiation
    // Create the instance and register it for both AppForegroundController and ForegroundDetector
    single<ForegroundDetector> { createAppForegroundController(this) }
    single<AppForegroundController> { get<ForegroundDetector>() as AppForegroundController }

    // NotificationController - platform-specific instantiation
    single<NotificationController> { createNotificationController(this, get()) }

    // ForegroundServiceController - platform-specific instantiation
    single<ForegroundServiceController> { createForegroundServiceController(this, get()) }

    // OpenTradesNotificationService - common implementation
    single<OpenTradesNotificationService> {
        OpenTradesNotificationService(get(), get(), get(), get())
    }
}

/**
 * Platform-specific factory functions
 */
expect fun createAppForegroundController(scope: Scope): AppForegroundController

expect fun createNotificationController(scope: Scope, foregroundDetector: ForegroundDetector): NotificationController

expect fun createForegroundServiceController(scope: Scope, notificationController: NotificationController): ForegroundServiceController

