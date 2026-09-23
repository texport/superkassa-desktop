package kz.mybrain.superkassa.presentation.dashboard

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kz.mybrain.superkassa.presentation.strings.AppStrings

/**
 * Что сказать кассиру после действия над сменой.
 *
 * Итог собирается из надписей словаря: то же сообщение по-казахски или
 * по-английски нельзя собрать, дописав русский хвост к переведённому началу.
 *
 * @param done что сделано, словами кассира: «Смена закрыта».
 * @param delivery что стало с документом в БФД; `null` — документа в БФД нет.
 */
fun deliveryReport(done: String, delivery: DeliveryStatus?, texts: AppStrings): String = when (delivery) {
    null -> done
    DeliveryStatus.ONLINE_OK -> "$done: ${texts.common.deliveredToOfd}"
    DeliveryStatus.OFFLINE_QUEUED -> "$done: ${texts.common.queuedNoLink}"
    else -> "$done. ${texts.common.deliveryState}: $delivery"
}

/**
 * Смена идёт дольше суток.
 *
 * Считается по часам кассы, а не по сроку жизни экрана: смену открыли
 * вчера, и за машиной с тех пор сменился кассир. Ровно сутки — уже предел:
 * по нему касса и блокируется.
 */
fun shiftTooLong(openedAt: Long?, now: Long): Boolean =
    openedAt != null && now - openedAt >= DAY

/** Принимает перечитанное: касса, смена, документы и наличные. */
internal fun DashboardUiState.adopt(snapshot: ShiftSnapshot): DashboardUiState = copy(
    kkm = snapshot.kkm ?: kkm,
    shift = snapshot.shift,
    shiftNumber = snapshot.open?.shiftNo,
    shiftOpenedAt = snapshot.open?.openedAt,
    documents = snapshot.documents.orEmpty(),
    documentsRead = snapshot.documents != null,
    cashInDrawer = snapshot.cash,
    reading = false
)

/** Сутки в миллисекундах: столько касса держит смену открытой. */
private const val DAY = 24L * 60 * 60 * 1000
