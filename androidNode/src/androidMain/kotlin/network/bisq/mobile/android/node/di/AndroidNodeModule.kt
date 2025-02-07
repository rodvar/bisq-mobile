package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.presentation.*
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.android.node.service.AndroidNodeCatHashService
import network.bisq.mobile.android.node.service.bootstrap.NodeApplicationBootstrapFacade
import network.bisq.mobile.android.node.service.explorer.NodeExplorerServiceFacade
import network.bisq.mobile.android.node.service.common.NodeLanguageServiceFacade
import network.bisq.mobile.android.node.service.market_price.NodeMarketPriceServiceFacade
import network.bisq.mobile.android.node.service.mediation.NodeMediationServiceFacade
import network.bisq.mobile.android.node.service.network.NodeConnectivityService
import network.bisq.mobile.android.node.service.offers.NodeOffersServiceFacade
import network.bisq.mobile.android.node.service.settings.NodeSettingsServiceFacade
import network.bisq.mobile.android.node.service.trades.NodeTradesServiceFacade
import network.bisq.mobile.android.node.service.user_profile.NodeUserProfileServiceFacade
import network.bisq.mobile.domain.AndroidUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.uicases.GettingStartedPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.GeneralSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IGeneralSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ISettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodeModule = module {
    single<AndroidMemoryReportService> {
        AndroidMemoryReportService(androidContext())
    }

    single<AndroidNodeCatHashService> {
        val context = androidContext()
        AndroidNodeCatHashService(context, context.filesDir.toPath())
    }

    single { AndroidApplicationService.Provider() }

    single<ApplicationBootstrapFacade> { NodeApplicationBootstrapFacade(get()) }

    single<MarketPriceServiceFacade> { NodeMarketPriceServiceFacade(get()) }

    single<UserProfileServiceFacade> { NodeUserProfileServiceFacade(get()) }

    single<OffersServiceFacade> { NodeOffersServiceFacade(get(), get()) }

    single<ExplorerServiceFacade> { NodeExplorerServiceFacade(get()) }

    single<TradesServiceFacade> { NodeTradesServiceFacade(get()) }

    single<MediationServiceFacade> { NodeMediationServiceFacade(get()) }

    single<SettingsServiceFacade> { NodeSettingsServiceFacade(get()) }

    single<LanguageServiceFacade> { NodeLanguageServiceFacade(get()) }

    single { NodeConnectivityService() } bind ConnectivityService::class

    single<UrlLauncher> { AndroidUrlLauncher(androidContext()) }

    // this line showcases both, the possibility to change behaviour of the app by changing one definition
    // and binding the same obj to 2 different abstractions
    single<MainPresenter> {
        NodeMainPresenter(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    } bind AppPresenter::class

    single<SplashPresenter> {
        NodeSplashPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }

    single<GettingStartedPresenter> {
        NodeGettingStartedPresenter(get(), get(), get(), get())
    }

    single<SettingsPresenter> { NodeSettingsPresenter(get(), get()) } bind ISettingsPresenter::class

    single<GeneralSettingsPresenter> { NodeGeneralSettingsPresenter(get(), get(), get(), get()) } bind IGeneralSettingsPresenter::class

    single<IOnboardingPresenter> { OnBoardingNodePresenter(get(), get(), get()) } bind IOnboardingPresenter::class
}