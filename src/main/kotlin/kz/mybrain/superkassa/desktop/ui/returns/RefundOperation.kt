package kz.mybrain.superkassa.desktop.ui.returns

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.ReceiptPayment
import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.server.buyReturn
import kz.mybrain.superkassa.desktop.server.sellReturn
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/**
 * Оформляет возврат и сообщает исход словами узла.
 *
 * Состояние доставки берётся из справочника узла: прежде здесь стоял
 * голый код протокола, и кассир читал «ONLINE_OK» вместо «Доставлен».
 */
internal suspend fun refund(
    session: Session,
    texts: AppStrings,
    kind: ReturnKind,
    basis: Document,
    refundTiyn: Long,
    payments: List<ReceiptPayment>,
    key: String,
    returned: List<SoldItem>
): Boolean {
    val kkm = session.selected ?: return false
    val request = refundRequest(
        basis = basis,
        kgdKkmId = kkm.kkmKgdId.orEmpty(),
        refundTiyn = refundTiyn,
        idempotencyKey = key,
        lineName = "${texts.returns.refundFor} ${basis.docNo}",
        payments = payments,
        returned = returned
    ) ?: return false
    val result = session.guard(kind.title(texts.returns)) {
        when (kind) {
            ReturnKind.Sell -> session.client.sellReturn(kkm.kkmId, request, session.pin)
            ReturnKind.Buy -> session.client.buyReturn(kkm.kkmId, request, session.pin)
        }
    } ?: return false
    session.report(
        "${kind.title(texts.returns)} ${texts.returns.done}: " +
            "${session.titleOf(Dictionary.DeliveryStatuses, result.deliveryStatus)}${Glyphs.SEPARATOR}${Money.formatTiyn(refundTiyn)}"
    )
    session.refreshSelected()
    return true
}
