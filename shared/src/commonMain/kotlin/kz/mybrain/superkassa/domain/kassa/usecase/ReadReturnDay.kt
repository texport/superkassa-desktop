package kz.mybrain.superkassa.domain.kassa.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ReturnDay
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.model.trouble
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.kassa.port.cashInDrawer
import kz.mybrain.superkassa.domain.kassa.port.documentsBetween
import kz.mybrain.superkassa.domain.signin.port.SignedKkm
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * День кассы для возврата: все документы дня, смена, остаток ящика, отрасль.
 *
 * Основание ищется по дню, а не в открытой смене: покупатель приходит
 * с чеком позавчерашнего дня. Каждое чтение — своим обращением.
 *
 * @return `null` — за кассой никто не работает, читать нечего.
 */
class ReadReturnDay(
    private val kassa: Kassa,
    private val signed: SignedKkm,
    private val memory: WorkplaceMemory
) {
    suspend operator fun invoke(from: Long, to: Long): ReturnDay? {
        val (kkmId, pin) = signed.seat() ?: return null
        val documents = kassa.documentsBetween(kkmId, from, to, pin)
        val shift = kassa.ask { it.getLocalOpenShift(kkmId, pin) }
        val cash = kassa.cashInDrawer(kkmId, pin)
        return ReturnDay(
            shiftOpen = (shift as? Answer.Done)?.let { it.value != null },
            cash = (cash as? Answer.Done)?.value,
            domainCode = memory.domain(kkmId),
            documents = (documents as? Answer.Done)?.value,
            trouble = listOf(documents, shift, cash).firstNotNullOfOrNull { it.trouble() }
        )
    }
}
