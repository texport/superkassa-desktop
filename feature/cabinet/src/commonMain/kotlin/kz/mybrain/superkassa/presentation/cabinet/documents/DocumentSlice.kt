package kz.mybrain.superkassa.presentation.cabinet.documents

import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetCashMovement
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetPage
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceipt
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReport
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetShift
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/** Прочитанная страница списка и сколько всего строк за сроком. */
internal data class DocumentSlice(val rows: List<CabinetDocumentRow> = emptyList(), val total: Long = 0)

/**
 * Страница кабинета строками общего журнала.
 *
 * Виды кабинет отдаёт разными списками, а показывается у них одно и то
 * же — когда документ пробит, чем он является, на сколько он и что с ним
 * стало. Поэтому страница приводится к строкам здесь, а не четырьмя
 * похожими разметками на экране.
 */
internal fun sliceOf(page: CabinetPage<*>, texts: CabinetTexts): DocumentSlice =
    DocumentSlice(page.items.mapNotNull { rowOf(it, texts) }, page.totalElements)

private fun rowOf(item: Any?, texts: CabinetTexts): CabinetDocumentRow? = when (item) {
    is CabinetReceipt -> receiptRow(item, texts)
    is CabinetShift -> shiftRow(item, texts)
    is CabinetReport -> reportRow(item, texts)
    is CabinetCashMovement -> movementRow(item, texts)
    else -> null
}
