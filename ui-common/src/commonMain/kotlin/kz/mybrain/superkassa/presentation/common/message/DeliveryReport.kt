package kz.mybrain.superkassa.presentation.common.message

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kz.mybrain.superkassa.strings.api.common.CommonTexts

/**
 * Что сказать кассиру после фискального действия: что сделано и что стало с документом в БФД.
 *
 * Итог собирается из надписей словаря: то же сообщение по-казахски или
 * по-английски нельзя собрать, дописав русский хвост к переведённому началу.
 * Одни слова на смену, чек, возврат и деньги: одно событие не называется
 * на разных экранах по-разному, и кода доставки кассир не читает.
 *
 * @param done что сделано, словами кассира: «Смена закрыта».
 * @param delivery что стало с документом в БФД; `null` — документа в БФД нет.
 */
fun deliveryReport(done: String, delivery: DeliveryStatus?, texts: CommonTexts): String = when (delivery) {
    null -> done
    DeliveryStatus.ONLINE_OK -> "$done: ${texts.general.deliveredToOfd}"
    DeliveryStatus.OFFLINE_QUEUED -> "$done: ${texts.general.queuedNoLink}"
    DeliveryStatus.ONLINE_ERROR -> "$done. ${texts.general.deliveryState}: ${texts.status.refused}"
    DeliveryStatus.NOT_SENT -> "$done. ${texts.general.deliveryState}: ${texts.status.queued}"
}
