package network.bisq.mobile.android.node

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import bisq.common.facades.FacadeProvider
import bisq.common.facades.android.AndroidGuavaFacade
import bisq.common.facades.android.AndroidJdkFacade
import bisq.common.network.clear_net_address_types.AndroidEmulatorAddressTypeFacade
import bisq.common.network.clear_net_address_types.LANAddressTypeFacade
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.android.node.di.androidNodeModule
import network.bisq.mobile.android.node.di.nodeServiceModule
import network.bisq.mobile.android.node.service.offers.NodeOffersServiceFacade
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.MainApplication
import network.bisq.mobile.presentation.di.presentationModule
import network.bisq.mobile.presentation.di.serviceModule
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.module.Module
import java.security.Security

/**
 * Bisq Android Node Application definition
 * TODO consider uplift ComponentCallbacks2 to shared app to use also in connect apps
 */
class NodeMainApplication : MainApplication(), ComponentCallbacks2 {

    // Lazy inject to avoid circular dependencies during app startup
    private val nodeOffersServiceFacade: OffersServiceFacade? by inject()

    override fun getKoinModules(): List<Module> {
        return listOf(domainModule, serviceModule, presentationModule, androidNodeModule, nodeServiceModule)
    }

    override fun onCreated() {
        // Use runBlocking for essential system initialization that must complete before app continues
        // This is acceptable here because:
        // 1. It's Application.onCreate() - the right place for critical setup
        // 2. setupBisqCoreStatics() configures essential system components (BouncyCastle, Facades)
        // 3. The app cannot function without these being initialized
        // 4. It's a one-time operation during app startup
        runBlocking(IODispatcher) {
            setupBisqCoreStatics()
        }

        // We start here the initialisation (non blocking) of the core services and tor.
        // The lifecycle of those is tied to the lifecycle of the Application/Process not to the lifecycle of the MainActivity.
        // As Android does not provide any callback when the process gets terminated we cannot gracefully shutdown the services and tor.
        // Only if the user shutdown or restart we can do that.
        val nodeApplicationLifecycleService: NodeApplicationLifecycleService = get()
        nodeApplicationLifecycleService.initialize(filesDir.toPath(), applicationContext)

        // Note: NodeMainApplication already implements ComponentCallbacks2, so onTrimMemory is automatically called
        // No need to registerComponentCallbacks(this) - that would cause infinite recursion
        // Note: Tor initialization is now handled in NodeApplicationBootstrapFacade
        log.i { "Bisq Node Application Created" }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        log.i { "System memory trim requested (level: $level)" }
        try {
            // Memory cleanup of data-intensive facades. More to be added if needed
            (nodeOffersServiceFacade as NodeOffersServiceFacade?).let {
                if (it == null) {
                    log.w { "Offers service not initialized, skipping memory trim" }
                } else {
                    log.i { "Trimming offers service memory" }
                    it.onTrimMemory(level)
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Error handling memory trim" }
        }
    }

    @SuppressLint("WrongConstant")
    @Deprecated("onLowMemory is deprecated in favor of onTrimMemory")
    override fun onLowMemory() {
        super.onLowMemory()
        log.w { "System low memory callback" }
        onTrimMemory(80) // TRIM_MEMORY_COMPLETE equivalent
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // do nth - Required by ComponentCallbacks2 interface
    }

    override fun isDebug(): Boolean {
        return BuildNodeConfig.IS_DEBUG
    }

    private fun setupBisqCoreStatics() {
        val isEmulator = isEmulator()
        val clearNetFacade = if (isEmulator) {
            AndroidEmulatorAddressTypeFacade()
        } else {
            LANAddressTypeFacade()
        }
        FacadeProvider.setClearNetAddressTypeFacade(clearNetFacade)
        FacadeProvider.setJdkFacade(AndroidJdkFacade(Process.myPid()))
        FacadeProvider.setGuavaFacade(AndroidGuavaFacade())

        // Androids default BC version does not support all algorithms we need, thus we remove
        // it and add our BC provider
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
        log.d { "Configured bisq2 for Android${if (isEmulator) " emulator" else ""}" }
    }

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
