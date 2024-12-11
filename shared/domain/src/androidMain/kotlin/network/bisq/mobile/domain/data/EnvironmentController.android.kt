package network.bisq.mobile.domain.data

import network.bisq.mobile.client.shared.BuildConfig

actual fun provideIsSimulator(): Boolean {
    return (android.os.Build.FINGERPRINT.contains("generic") ||
            android.os.Build.FINGERPRINT.startsWith("unknown") ||
            android.os.Build.MODEL.contains("Emulator") ||
            android.os.Build.MODEL.contains("Android SDK built for x86") ||
            android.os.Build.BOARD == "goldfish" ||
            android.os.Build.HOST.startsWith("android") ||
            android.os.Build.MANUFACTURER.contains("Genymotion") ||
            (android.os.Build.BRAND.startsWith("generic") &&
            android.os.Build.DEVICE.startsWith("generic"))
            )
}

actual fun provideApiHost(): String {
    return BuildConfig.WS_ANDROID_HOST.takeIf { it.isNotEmpty() } ?: "10.0.2.2"
}

actual fun provideApiPort(): Int {
    return (BuildConfig.WS_PORT.takeIf { it.isNotEmpty() } ?: "8090").toInt()
}