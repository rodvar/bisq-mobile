package network.bisq.mobile.presentation.di

import network.bisq.mobile.client.ClientMainPresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.getPlatformCurrentTimeProvider
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.ui.helpers.TimeProvider
import network.bisq.mobile.presentation.ui.uicases.GettingStartedPresenter
import network.bisq.mobile.presentation.ui.uicases.ITabContainerPresenter
import network.bisq.mobile.presentation.ui.uicases.TabContainerPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferDirectionPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferMarketPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPricePresenter
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferReviewPresenter
import network.bisq.mobile.presentation.ui.uicases.guide.TradeGuidePresenter
import network.bisq.mobile.presentation.ui.uicases.guide.WalletGuidePresenter
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookMarketPresenter
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.OpenTradeListPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.InterruptedTradePresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.OpenTradePresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeDetailsHeaderPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.TradeFlowPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState1aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState2aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState2bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState3aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerState4Presenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerStateLightning3bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.BuyerStateMainChain3bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState1Presenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState2aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState2bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState3aPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerState4Presenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerStateLightning3bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.SellerStateMainChain3bPresenter
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states.TradeStatesProvider
import network.bisq.mobile.presentation.ui.uicases.open_trades.selected.trade_chat.TradeChatPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.GeneralSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IGeneralSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IPaymentAccountSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ISettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IUserProfileSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.PaymentAccountPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.UserProfileSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.AgreementPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfilePresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IAgreementPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.ITrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferAmountPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPaymentMethodPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferPresenter
import network.bisq.mobile.presentation.ui.uicases.take_offer.TakeOfferReviewPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    single<MainPresenter> {
        ClientMainPresenter(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get())
    } bind AppPresenter::class

    single<TopBarPresenter> { TopBarPresenter(get(), get(), get(), get()) } bind ITopBarPresenter::class

    single<SplashPresenter> {
        SplashPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    factory<AgreementPresenter> { AgreementPresenter(get(), get()) } bind IAgreementPresenter::class

    single { OnBoardingPresenter(get(), get(), get()) } bind IOnboardingPresenter::class
    single { TabContainerPresenter(get(), get()) } bind ITabContainerPresenter::class

    single<SettingsPresenter> { SettingsPresenter(get(), get()) } bind ISettingsPresenter::class

    single<UserProfileSettingsPresenter> { UserProfileSettingsPresenter(get(), get(), get(), get(), get()) } bind IUserProfileSettingsPresenter::class

    single<GettingStartedPresenter> { GettingStartedPresenter(get(), get(), get(), get()) }

    single {
        CreateProfilePresenter(
            get(),
            get(),
            get()
        )
    }

    single {
        TrustedNodeSetupPresenter(
            get(),
            settingsRepository = get(),
            get(),
            get()
        )
    } bind ITrustedNodeSetupPresenter::class

    /*
    single<MarketListPresenter> { MarketListPresenter(get(), get()) }

    single { OffersListPresenter(get(), get(), get(), get()) } bind IOffersListPresenter::class

    single {
        MyTradesPresenter(
            get(),
            get(),
            get(),
        )
    } bind IMyTrades::class

    single { TradeFlowPresenter(get(), get()) } bind ITradeFlowPresenter::class
    */

    single { GeneralSettingsPresenter(get(), get(), get(), get()) } bind IGeneralSettingsPresenter::class

    single { PaymentAccountPresenter(get(), get(), get()) } bind IPaymentAccountSettingsPresenter::class

    // Offerbook
    single<OfferbookMarketPresenter> { OfferbookMarketPresenter(get(), get()) }
    single<OfferbookPresenter> { OfferbookPresenter(get(), get(), get(), get()) }

    // Take offer
    single { TakeOfferPresenter(get(), get(), get()) }
    single { TakeOfferAmountPresenter(get(), get(), get()) }
    single { TakeOfferPaymentMethodPresenter(get(), get()) }
    single { TakeOfferReviewPresenter(get(), get(), get()) }

    // Create offer
    single { CreateOfferPresenter(get(), get(), get()) }
    single { CreateOfferDirectionPresenter(get(), get(), get(), get()) }
    single { CreateOfferMarketPresenter(get(), get(), get()) }
    single { CreateOfferPricePresenter(get(), get(), get()) }
    single { CreateOfferAmountPresenter(get(), get(), get(), get(), get(), get()) }
    single { CreateOfferPaymentMethodPresenter(get(), get()) }
    single { CreateOfferReviewPresenter(get(), get()) }

    // Trade Seller
    factory { SellerState1Presenter(get(), get(), get()) }
    factory { SellerState2aPresenter(get(), get()) }
    factory { SellerState2bPresenter(get(), get()) }
    factory { SellerState3aPresenter(get(), get()) }
    factory { SellerStateMainChain3bPresenter(get(), get(), get()) }
    factory { SellerStateLightning3bPresenter(get(), get()) }
    factory { SellerState4Presenter(get(), get()) }

    // Trade Buyer
    factory { BuyerState1aPresenter(get(), get()) }
    // BuyerState1bPresenter does not exist as it a static UI
    factory { BuyerState2aPresenter(get(), get()) }
    factory { BuyerState2bPresenter(get(), get()) }
    factory { BuyerState3aPresenter(get(), get()) }
    factory { BuyerStateMainChain3bPresenter(get(), get(), get()) }
    factory { BuyerStateLightning3bPresenter(get(), get()) }
    factory { BuyerState4Presenter(get(), get()) }

    // Trade General process
    factory { TradeStatesProvider(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { OpenTradeListPresenter(get(), get(), get()) }
    factory { TradeDetailsHeaderPresenter(get(), get(), get()) }
    factory { InterruptedTradePresenter(get(), get(), get()) }
    factory { TradeFlowPresenter(get(), get(), get()) }
    factory { OpenTradePresenter(get(), get(), get()) }

    single { TradeChatPresenter(get(), get(), get()) }

    single { TradeGuidePresenter(get(), get()) } bind TradeGuidePresenter::class
    single { WalletGuidePresenter(get()) } bind WalletGuidePresenter::class

    factory<TimeProvider> { getPlatformCurrentTimeProvider() }

}