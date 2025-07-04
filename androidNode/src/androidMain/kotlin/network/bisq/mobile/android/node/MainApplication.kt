package network.bisq.mobile.android.node

import android.app.Application
import android.content.Context
import android.os.Process
import bisq.common.facades.FacadeProvider
import bisq.common.facades.android.AndroidGuavaFacade
import network.bisq.mobile.android.node.service.facades.CustomAndroidJdkFacade
import network.bisq.mobile.android.node.di.androidNodeModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.di.serviceModule
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.di.presentationModule
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.security.Security

class MainApplication : Application(), Logging {
    companion object {
        private val nodeModules = listOf(domainModule, serviceModule, presentationModule, androidNodeModule)

        fun setupKoinDI(appContext: Context) {
            // very important to avoid issues from the abuse of DI single {} singleton instances
            stopKoin()
            startKoin {
                androidContext(appContext)
                // order is important, last one is picked for each interface/class key
                modules(nodeModules)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        setupKoinDI(this)
        setupBisqCoreStatics()
        setupMemoryOptimizations()
        // Note: Tor initialization is now handled in NodeApplicationBootstrapFacade
        // as the very first step of the bootstrap process
//        setupTorSystemProperties()
        log.i { "Bisq Node Application Created" }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        log.w { "🧠 KMP System requested memory trim, level: $level" }

        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_COMPLETE -> {
                log.w { "🚨 KMP Critical memory pressure - performing aggressive cleanup" }
                performAggressiveMemoryCleanup()
            }
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_MODERATE -> {
                log.w { "⚠️ KMP Moderate memory pressure - performing standard cleanup" }
                performStandardMemoryCleanup()
            }
            else -> {
                log.i { "ℹ️ KMP Background memory trim - performing light cleanup" }
                performLightMemoryCleanup()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        log.w { "🚨 KMP System reports low memory - performing emergency cleanup" }
        performAggressiveMemoryCleanup()
    }

    private fun setupBisqCoreStatics() {
        val isEmulator = isEmulator()
//        TODO this is part of Bisq v2.1.8, uncomment when developing against latest snapshot
//        val clearNetFacade = if (isEmulator) {
//            AndroidEmulatorAddressTypeFacade()
//        } else {
//            LANAddressTypeFacade()
//        }
//        FacadeProvider.setClearNetAddressTypeFacade(clearNetFacade)
        FacadeProvider.setJdkFacade(CustomAndroidJdkFacade(Process.myPid()))
        FacadeProvider.setGuavaFacade(AndroidGuavaFacade())

        // Androids default BC version does not support all algorithms we need, thus we remove
        // it and add our BC provider
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
        log.d { "Configured bisq2 for Android${if (isEmulator) " emulator" else ""}" }
    }

    /**
     * Set up system properties to tell Bisq2 to use external Tor
     * This must be done BEFORE AndroidApplicationService is created
     * and ensures Bisq2 has consistent behaviour waiting for external tor
     * @deprecated this is hanbdled by the TorBootstrapOrchestrator,
     * we can delete this when we are sure bootstrap is consistently reliable
     */
    private fun setupTorSystemProperties() {
        println("🔧🔧🔧 Setting up system properties to force Bisq2 to skip Tor launch")
        android.util.Log.i("MainApplication", "🔧🔧🔧 Setting up system properties to force Bisq2 to skip Tor launch")
        log.i { "🔧 Setting up system properties to force Bisq2 to skip Tor launch" }

        // Backup approach: Set system properties that might influence Tor behavior
        System.setProperty("bisq.network.transport", "TOR")

        // Set SOCKS proxy properties to redirect traffic to our kmp-tor
        System.setProperty("socksProxyHost", "127.0.0.1")
        System.setProperty("socksProxyPort", "9050") // Will be updated when kmp-tor is ready
        System.setProperty("socksProxyVersion", "5")

        // Set Bisq-specific SOCKS properties
        System.setProperty("bisq.torSocksHost", "127.0.0.1")
        System.setProperty("bisq.torSocksPort", "9050") // Will be updated when kmp-tor is ready

        log.i { "🔄 Configured system properties for SOCKS proxy hijacking" }
        log.i { "   Strategy: Force Bisq2 to skip embedded Tor and use our kmp-tor via SOCKS" }
    }

    /**
     * Set up memory optimizations for the Android application
     */
    private fun setupMemoryOptimizations() {
        log.i { "🧠 KMP Setting up memory optimizations" }

        // Set system properties for memory management
        System.setProperty("java.awt.headless", "true")

        // Log initial memory state
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        log.i { "📱 KMP Max heap size: ${maxMemory}MB" }

        // Note: BuildConfig constants are available at runtime
        log.i { "✅ KMP Memory monitoring configured" }
    }

    /**
     * Perform light memory cleanup
     */
    private fun performLightMemoryCleanup() {
        log.d { "🧹 KMP Performing light memory cleanup" }
        System.gc()
    }

    /**
     * Perform standard memory cleanup
     */
    private fun performStandardMemoryCleanup() {
        log.i { "🧹 KMP Performing standard memory cleanup" }

        // Multiple GC passes
        repeat(2) {
            System.gc()
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }
    }

    /**
     * Perform aggressive memory cleanup
     */
    private fun performAggressiveMemoryCleanup() {
        log.w { "🧹 KMP Performing aggressive memory cleanup" }

        // Multiple aggressive GC passes
        repeat(3) {
            System.gc()
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }

        // Log memory state after cleanup
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        log.w { "📊 KMP Memory after cleanup: ${usedMemory}MB / ${maxMemory}MB" }
    }

    private fun isEmulator(): Boolean {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT);
    }
}
