package kz.mybrain.superkassa.presentation.kassa.sale.entry

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.entry.LookupProblem
import kz.mybrain.superkassa.domain.kassa.model.entry.PositionDraft
import kz.mybrain.superkassa.domain.kassa.model.entry.barcodeOf
import kz.mybrain.superkassa.domain.kassa.model.entry.lookupProblemOf
import kz.mybrain.superkassa.domain.kassa.model.entry.positionOf
import kz.mybrain.superkassa.domain.kassa.model.entry.priceMissing
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.usecase.LookupGoods
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.kassa.sale.BarcodeSearch
import kz.mybrain.superkassa.presentation.kassa.sale.EntryActions
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Позиция встаёт в чек: по штрихкоду из справочника кассы или руками.
 *
 * Справочник ведёт ОФД, поэтому цена, наименование и ставка берутся
 * у него, а не вводятся кассиром. Цены в справочнике может не быть —
 * национальный каталог её не несёт: такая позиция не встаёт в чек молча,
 * её цену спрашивают у кассира. Любая добавленная позиция очищает поле
 * штрихкода и снимает надпись о беде — товар заведён, говорить не о чем.
 */
class EntryEditor(
    private val scope: CoroutineScope,
    private val screen: MutableStateFlow<SaleUiState>,
    private val lookup: LookupGoods,
    private val talk: Talk
) : EntryActions {

    override fun typeBarcode(text: String) =
        screen.update { it.copy(search = it.search.copy(barcode = barcodeOf(text), problem = null)) }

    override fun search(): Boolean {
        val barcode = screen.value.search.barcode
        if (!screen.value.search.ready || screen.value.kkm == null) return false
        screen.update { it.copy(search = it.search.copy(searching = true)) }
        scope.launch {
            val answer = lookup(barcode)
            // Беда кассы — её словами в строке сообщений; отсутствие товара
            // бедой не считается и названо под полем.
            if (answer !is Answer.Done) {
                answer.shown(textsOf(talk.language()).common.sale.barcodeSearch, "barcode lookup", talk)
            }
            val item = (answer as? Answer.Done)?.value?.takeIf { it.found }?.item
            screen.update { now ->
                found(now, item?.let { positionOf(it, now.kkm, now.vatRates) }, lookupProblemOf(answer))
            }
        }
        return true
    }

    override fun editDraft(draft: PositionDraft) = screen.update { it.copy(draft = draft) }

    override fun addDraft(): Boolean {
        val ready = screen.value.draft.position ?: return false
        screen.update { added(it, ready).copy(draft = it.draft.cleared()) }
        return true
    }

    override fun addPriced(position: Position) = screen.update { added(it, position) }

    override fun dismissPriced() = screen.update { it.copy(search = it.search.copy(asking = null)) }

    /** Итог поиска: позиция встаёт в чек, уходит спросить цену или беда называется. */
    private fun found(state: SaleUiState, position: Position?, problem: LookupProblem?): SaleUiState = when {
        position == null -> state.copy(search = state.search.copy(searching = false, problem = problem))
        priceMissing(position) -> state.copy(search = BarcodeSearch(asking = position))
        else -> added(state, position)
    }

    private fun added(state: SaleUiState, position: Position): SaleUiState =
        state.copy(basket = state.basket.add(position), search = BarcodeSearch()).toBarcode()
}
