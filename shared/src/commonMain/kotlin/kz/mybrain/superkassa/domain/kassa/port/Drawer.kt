package kz.mybrain.superkassa.domain.kassa.port

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask

/**
 * Наличные в денежном ящике, в тиынах: счётчик всей кассы, а не смены.
 *
 * Тот же счётчик, по которому касса решает, хватает ли денег на изъятие
 * и на возврат наличными: свой подсчёт по документам разошёлся бы с ним.
 * Счётчика, которого касса ещё не завела, — ноль, как его читает сама
 * касса: прежде у новой кассы ящик был «неизвестен», и экран спрашивал
 * подтверждение изъятия из пустого ящика, чтобы получить отказ.
 */
suspend fun Kassa.cashInDrawer(kkmId: String, pin: String): Answer<Long> = ask { api ->
    api.listCounters(kkmId, pin).firstOrNull { it.scope == GLOBAL_SCOPE && it.key == CASH_SUM }?.value ?: 0L
}

/**
 * Все документы кассы за срок, новые первыми.
 *
 * Касса отдаёт их страницами, и читается весь срок: утреннее внесение
 * в обычном дне уходило за предел одной страницы, и экран писал
 * «внесений не было» о дне, в котором оно было.
 */
suspend fun Kassa.documentsBetween(
    kkmId: String,
    fromInclusive: Long,
    toExclusive: Long,
    pin: String
): Answer<List<FiscalDocumentResponse>> {
    val all = mutableListOf<FiscalDocumentResponse>()
    while (true) {
        val offset = all.size
        val answer = ask { it.listFiscalDocumentsByPeriod(kkmId, fromInclusive, toExclusive, PAGE, offset, pin) }
        if (answer !is Answer.Done) return answer
        all += answer.value
        if (answer.value.size < PAGE) return Answer.Done(all)
    }
}

/** Больше за одно обращение касса документов не отдаёт. */
private const val PAGE = 500
private const val GLOBAL_SCOPE = "GLOBAL"
private const val CASH_SUM = "cash.sum"
