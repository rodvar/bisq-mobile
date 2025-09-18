package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.MainPresenter

class SellerStateMainChain3bPresenter(
    mainPresenter: MainPresenter,
    tradesServiceFacade: TradesServiceFacade,
    explorerServiceFacade: ExplorerServiceFacade
) : BaseTradeStateMainChain3bPresenter(
    mainPresenter,
    tradesServiceFacade,
    explorerServiceFacade
) {
    // This class is now a placeholder for any future Seller-specific logic.
}