package kz.mybrain.superkassa.presentation.returns

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.data.node.Dictionary
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.data.node.ReceiptPayment
import kz.mybrain.superkassa.data.node.SoldItem
import kz.mybrain.superkassa.data.node.buyReturn
import kz.mybrain.superkassa.data.node.sellReturn
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.session.refreshSelected
import kz.mybrain.superkassa.presentation.session.titleOf
import kz.mybrain.superkassa.presentation.strings.AppStrings
import kz.mybrain.superkassa.presentation.theme.Glyphs

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
        lineName = refundLineName(texts.returns.refundFor, basis),
        payments = payments,
        // Отрасль кассы — без подблока, и это решено осознанно: номер
        // машины и время стоянки принадлежат чеку-основанию, а пустой
        // реквизит на их месте — выдуманный реквизит в фискальном
        // документе. Если БФД откажет в возврате по отрасли с обязательным
        // подблоком, реквизиты придётся спрашивать на экране возврата,
        // а не подставлять здесь.
        domain = session.domain.plain,
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

/**
 * Как названа единственная строка чека возврата суммой.
 *
 * Номер берётся тот, что стоит на бумаге покупателя: номер от БФД
 * с бумажным не совпадает, и по нему покупатель свой чек не опознает.
 * Весь остальной экран возврата — список оснований, заголовок панели
 * и поиск — называет основание именно бумажным номером.
 */
internal fun refundLineName(caption: String, basis: Document): String =
    "$caption ${basis.number ?: Glyphs.DASH}"
