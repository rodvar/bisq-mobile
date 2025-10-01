package network.bisq.mobile.presentation.di

import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.presentation.notification.ForegroundServiceController
import network.bisq.mobile.presentation.notification.ForegroundServiceControllerImpl
import network.bisq.mobile.presentation.notification.NotificationController
import network.bisq.mobile.presentation.notification.NotificationControllerImpl
import org.koin.core.scope.Scope

/**
 * Android-specific implementations of service factory functions
 * Note: The Activity class is provided by the app-specific modules (androidClient/androidNode)
 * through the NotificationControllerImpl constructor
 */
actual fun createAppForegroundController(scope: Scope): AppForegroundController {
    return AppForegroundController(scope.get())
}

actual fun createNotificationController(scope: Scope, foregroundDetector: ForegroundDetector): NotificationController {
    // Android needs the AppForegroundController (which is the ForegroundDetector) and the Activity class
    // The Activity class is injected separately by the app-specific modules
    return scope.get<NotificationControllerImpl>()
}

actual fun createForegroundServiceController(scope: Scope, notificationController: NotificationController): ForegroundServiceController {
    // Android needs AppForegroundController, not NotificationController
    return ForegroundServiceControllerImpl(scope.get<AppForegroundController>())
}

