package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.model.TradeReadState
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler
import kotlin.collections.orEmpty
import kotlin.collections.toMutableMap

abstract class State4Presenter(
    mainPresenter: MainPresenter,
    private val tradesServiceFacade: TradesServiceFacade,
    private val tradeReadStateRepository: TradeReadStateRepository,
) : BasePresenter(mainPresenter) {
    val selectedTrade: StateFlow<TradeItemPresentationModel?> = tradesServiceFacade.selectedTrade

    private val _showCloseTradeDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showCloseTradeDialog: StateFlow<Boolean> = _showCloseTradeDialog

    override fun onViewUnattaching() {
        _showCloseTradeDialog.value = false
        super.onViewUnattaching()
    }

    fun onCloseTrade() {
        _showCloseTradeDialog.value = true
    }

    fun onDismissCloseTrade() {
        _showCloseTradeDialog.value = false
    }

    fun onConfirmCloseTrade() {
        launchUI {
            val tradeId = selectedTrade.value!!.tradeId
            val result = withContext(IODispatcher) { tradesServiceFacade.closeTrade() }

            when {
                result.isFailure -> {
                    _showCloseTradeDialog.value = false
                    result.exceptionOrNull()?.let { exception -> GenericErrorHandler.handleGenericError(exception.message) }
                        ?: GenericErrorHandler.handleGenericError("No Exception is set in result failure")
                }

                result.isSuccess -> {
                    val readState = tradeReadStateRepository.fetch()?.map.orEmpty().toMutableMap()
                    readState.remove(tradeId)
                    tradeReadStateRepository.update(TradeReadState().apply { map = readState })
                    _showCloseTradeDialog.value = false
                    navigateBack()
                }
            }
        }
    }

    fun onExportTradeDate() {
            launchIO {
            tradesServiceFacade.exportTradeDate()
        }
    }

    abstract fun getMyDirectionString(): String

    abstract fun getMyOutcomeString(): String
}