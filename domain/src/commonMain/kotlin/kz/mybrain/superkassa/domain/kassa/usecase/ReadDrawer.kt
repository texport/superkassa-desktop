package kz.mybrain.superkassa.domain.kassa.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.DrawerDay
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.model.trouble
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.kassa.port.cashInDrawer
import kz.mybrain.superkassa.domain.kassa.port.documentsBetween
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Смена, остаток ящика и движения наличных за сутки.
 *
 * Каждое чтение — своим обращением, и беда одного не отменяет прочих.
 * Движения берутся из документов за сутки, а не из документов смены:
 * смену закрывают в конце дня, а куда ушли деньги, кассир должен видеть
 * и после закрытия. Сутки читаются целиком; показываются последние.
 *
 * @return `null` — за кассой никто не работает, читать нечего.
 */
class ReadDrawer(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(now: Long): DrawerDay? {
        val (kkmId, pin) = signed.seat() ?: return null
        val shift = kassa.ask { it.getLocalOpenShift(kkmId, pin) }
        val cash = kassa.cashInDrawer(kkmId, pin)
        val documents = kassa.documentsBetween(kkmId, now - DAY_MILLIS, now, pin)
        return DrawerDay(
            shiftOpen = (shift as? Answer.Done)?.let { it.value != null },
            cash = (cash as? Answer.Done)?.value,
            recent = (documents as? Answer.Done)?.value?.filter { it.docType in CASH_TYPES }?.take(RECENT_LIMIT),
            trouble = listOf(cash, documents).firstNotNullOfOrNull { it.trouble() }
        )
    }

    private companion object {
        /** Виды документов, которыми касса записывает движение наличных. */
        val CASH_TYPES = setOf("CASH_IN", "CASH_OUT")

        /** Сколько операций показывать: список под формой, а не журнал. */
        const val RECENT_LIMIT = 10

        const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
