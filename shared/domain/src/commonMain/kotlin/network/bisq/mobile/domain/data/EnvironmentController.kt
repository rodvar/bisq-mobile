package network.bisq.mobile.domain.data

import network.bisq.mobile.client.shared.BuildConfig

class EnvironmentController {
    fun getApiHost(): String = provideApiHost()

    fun getApiPort(): Int = provideApiPort()

    fun getWebSocketHost(): String = provideApiHost()

    fun getWebSocketPort(): Int = provideApiPort()

    fun isSimulator(): Boolean = provideIsSimulator()
}

expect fun provideIsSimulator(): Boolean

expect fun provideApiHost(): String

expect fun provideApiPort(): Int
