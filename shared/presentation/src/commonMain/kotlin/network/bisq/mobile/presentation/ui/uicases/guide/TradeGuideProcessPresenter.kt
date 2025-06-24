package network.bisq.mobile.presentation.ui.uicases.guide

import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqConfig
import network.bisq.mobile.presentation.ui.navigation.Routes

class TradeGuideProcessPresenter(
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter) {

    fun prevClick() {
        navigateBack()
    }

    fun processNextClick() {
        navigateTo(Routes.TradeGuideTradeRules)
    }

    fun navigateSecurityLearnMore() {
        navigateToUrl(BisqConfig.BISQ_EASY_WIKI_URL)
    }

}
