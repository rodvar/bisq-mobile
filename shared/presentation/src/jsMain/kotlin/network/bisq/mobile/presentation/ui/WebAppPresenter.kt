package network.bisq.mobile.presentation.ui

import network.bisq.mobile.client.ClientMainPresenter
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade

class WebAppPresenter(
    connectivityService: ClientConnectivityService,
    openTradesNotificationService: OpenTradesNotificationService,
    tradesServiceFacade: TradesServiceFacade,
    tradeChatServiceFacade: TradeChatServiceFacade,
    webSocketClientProvider: WebSocketClientProvider,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    offersServiceFacade: OffersServiceFacade,
    marketPriceServiceFacade: MarketPriceServiceFacade,
    settingsServiceFacade: SettingsServiceFacade,
    languageServiceFacade: LanguageServiceFacade,
    urlLauncher: UrlLauncher
) : ClientMainPresenter(
    connectivityService,
    openTradesNotificationService,
    tradesServiceFacade,
    tradeChatServiceFacade,
    webSocketClientProvider,
    applicationBootstrapFacade,
    offersServiceFacade,
    marketPriceServiceFacade,
    settingsServiceFacade,
    languageServiceFacade,
    urlLauncher
) {
    // Web-specific overrides can go here
    override fun isSmallScreen(): Boolean {
        return js("window.innerWidth < 768")
    }
}