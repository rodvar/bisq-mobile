package network.bisq.mobile.android.node.service

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import bisq.common.platform.MemoryReportService
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.domain.utils.Logging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Enhanced memory report service for Bisq2 JAR integration with Android-specific optimizations
 * Provides memory monitoring, reporting, and automatic cleanup to prevent OutOfMemoryErrors
 */
class AndroidMemoryReportService(private val context: Context) : MemoryReportService, Logging {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    // Memory tracking
    private val lastReportTime = AtomicLong(0)
    private val memoryWarningIssued = AtomicBoolean(false)
    private val criticalMemoryReached = AtomicBoolean(false)

    companion object {
        private const val MEMORY_WARNING_THRESHOLD = 0.85f // 85% of max heap
        private const val MEMORY_CRITICAL_THRESHOLD = 0.95f // 95% of max heap
        private const val MIN_REPORT_INTERVAL_MS = 30000 // 30 seconds minimum between reports
        private const val FORCE_GC_THRESHOLD = 0.90f // Force GC at 90% memory usage
    }

    override fun logReport() {
        val currentTime = System.currentTimeMillis()

        // Throttle frequent reports to avoid log spam
        if (currentTime - lastReportTime.get() < MIN_REPORT_INTERVAL_MS) {
            return
        }
        lastReportTime.set(currentTime)

        val usedMemory = usedMemoryInMB
        val freeMemory = freeMemoryInMB
        val totalMemory = totalMemoryInMB

        // Get heap memory information
        val runtime = Runtime.getRuntime()
        val heapUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val heapMax = runtime.maxMemory() / (1024 * 1024)
        val heapUtilization = heapUsed.toFloat() / heapMax.toFloat()

        // Enhanced logging with heap information
        log.i {
            "📊 KMP Memory Report - System: ${usedMemory}MB used, ${freeMemory}MB free, ${totalMemory}MB total | " +
            "Heap: ${heapUsed}MB/${heapMax}MB (${(heapUtilization * 100).toInt()}%)"
        }

        // Check for memory pressure and take action
        checkMemoryPressure(heapUtilization)

        // Additional detailed logging for debug builds
        if (BuildNodeConfig.ENABLE_MEMORY_PROFILING) {
            logDetailedMemoryInfo()
        }
    }

    override fun getUsedMemoryInBytes(): Long {
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        return memoryInfo.totalMem - memoryInfo.availMem
    }

    override fun getUsedMemoryInMB(): Long {
        return bytesToMegabytes(getUsedMemoryInBytes())
    }

    override fun getFreeMemoryInMB(): Long {
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        return bytesToMegabytes(memoryInfo.availMem)
    }

    override fun getTotalMemoryInMB(): Long {
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        return bytesToMegabytes(memoryInfo.totalMem)
    }

    override fun initialize(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            log.i { "🧠 Initializing AndroidMemoryReportService" }
            log.i { "   Package: ${context.packageName}" }
            log.i { "   Memory profiling: ${BuildNodeConfig.ENABLE_MEMORY_PROFILING}" }

            // Log initial memory state
            logInitialMemoryState()

            true
        }
    }

    override fun shutdown(): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            log.i { "🧠 Shutting down AndroidMemoryReportService" }

            // Final memory report
            logReport()

            true
        }
    }

    /**
     * Check memory pressure and take appropriate action
     */
    private fun checkMemoryPressure(heapUtilization: Float) {
        when {
            heapUtilization >= MEMORY_CRITICAL_THRESHOLD -> {
                if (!criticalMemoryReached.getAndSet(true)) {
                    log.w { "🚨 CRITICAL memory usage: ${(heapUtilization * 100).toInt()}% - Performing aggressive cleanup" }
                    performAggressiveMemoryCleanup()
                }
            }
            heapUtilization >= FORCE_GC_THRESHOLD -> {
                log.w { "⚠️ High memory usage: ${(heapUtilization * 100).toInt()}% - Forcing garbage collection" }
                forceGarbageCollection()
            }
            heapUtilization >= MEMORY_WARNING_THRESHOLD -> {
                if (!memoryWarningIssued.getAndSet(true)) {
                    log.w { "⚠️ Memory warning: ${(heapUtilization * 100).toInt()}% - Consider reducing memory usage" }
                }
            }
            else -> {
                // Reset warning flags when memory usage returns to normal
                if (memoryWarningIssued.get() || criticalMemoryReached.get()) {
                    log.i { "✅ Memory usage returned to normal: ${(heapUtilization * 100).toInt()}%" }
                    memoryWarningIssued.set(false)
                    criticalMemoryReached.set(false)
                }
            }
        }
    }

    /**
     * Force garbage collection
     */
    private fun forceGarbageCollection() {
        log.d { "🗑️ Forcing garbage collection" }
        System.gc()

        // Give GC time to work
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Perform aggressive memory cleanup
     */
    private fun performAggressiveMemoryCleanup() {
        log.w { "🧹 Performing aggressive memory cleanup" }

        // Multiple GC passes
        repeat(3) {
            System.gc()
            try {
                Thread.sleep(50)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }

        // Request system to trim memory
        try {
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            if (memoryInfo.lowMemory) {
                log.w { "📱 System reports low memory - requesting memory trim" }
                // Note: We can't directly call onTrimMemory here as we're not an Activity/Service
                // But we can suggest it through logging for the application to handle
            }
        } catch (e: Exception) {
            log.e(e) { "Error during aggressive memory cleanup" }
        }
    }

    /**
     * Log initial memory state
     */
    private fun logInitialMemoryState() {
        val runtime = Runtime.getRuntime()
        val maxHeap = runtime.maxMemory() / (1024 * 1024)
        val totalSystem = getTotalMemoryInMB()

        log.i {
            """
            📱 Initial Memory State:
               Max Heap: ${maxHeap}MB
               Total System: ${totalSystem}MB
               Package: ${context.packageName}
               Process: ${android.os.Process.myPid()}
            """.trimIndent()
        }
    }

    /**
     * Log detailed memory information for debugging
     */
    private fun logDetailedMemoryInfo() {
        try {
            val runtime = Runtime.getRuntime()
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            val nativeHeap = Debug.getNativeHeapSize() / (1024 * 1024)
            val nativeAllocated = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
            val nativeFree = Debug.getNativeHeapFreeSize() / (1024 * 1024)

            log.d {
                """
                🔍 Detailed Memory Profile:
                   Java Heap: ${(runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)}MB / ${runtime.maxMemory() / (1024 * 1024)}MB
                   Native Heap: ${nativeAllocated}MB / ${nativeHeap}MB (${nativeFree}MB free)
                   System Available: ${memoryInfo.availMem / (1024 * 1024)}MB
                   System Threshold: ${memoryInfo.threshold / (1024 * 1024)}MB
                   Low Memory: ${memoryInfo.lowMemory}
                   Active Threads: ${Thread.activeCount()}
                """.trimIndent()
            }
        } catch (e: Exception) {
            log.e(e) { "Error logging detailed memory info" }
        }
    }

    private fun bytesToMegabytes(bytes: Long): Long {
        return bytes / (1024 * 1024)
    }
}
