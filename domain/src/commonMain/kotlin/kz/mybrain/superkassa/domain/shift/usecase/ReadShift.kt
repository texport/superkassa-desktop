package kz.mybrain.superkassa.domain.shift.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.kassa.port.cashInDrawer
import kz.mybrain.superkassa.domain.shift.model.ShiftPart
import kz.mybrain.superkassa.domain.shift.model.ShiftSnapshot
import kz.mybrain.superkassa.domain.shift.model.ShiftState
import kz.mybrain.superkassa.domain.shift.model.ShiftTrouble
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Перечитывает кассу, её смену, документы смены и наличные в ящике.
 *
 * Каждое чтение идёт своим обращением, и беда одного не отменяет
 * остальных: касса, снятая с учёта, отказывает в документах, а смену
 * называет — и экран обязан показать смену открытой, а не неизвестной.
 * Смена, закрытая между двумя обращениями, — состояние кассы, а не отказ.
 * Касса, ответившая о себе, отдаётся всем разделам.
 */
class ReadShift(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @return снимок кассы; `null` — за кассой никто не работает. */
    suspend operator fun invoke(): ShiftSnapshot? {
        val (kkmId, pin) = signed.seat() ?: return null
        val troubles = mutableListOf<ShiftTrouble>()
        val kkm = kassa.ask { it.getKkm(kkmId) }.valueOr(ShiftPart.Kkm, troubles)?.also(signed::refresh)
        val answer = kassa.ask { it.getLocalOpenShift(kkmId, pin) }
        val open = (answer as? Answer.Done)?.value
        val documents = open?.let { readDocuments(kkmId, it, pin, troubles) }
        val state = when {
            answer !is Answer.Done -> ShiftState.Unknown.also { answer.valueOr(ShiftPart.Shift, troubles) }
            open == null || documents is Documents.Gone -> ShiftState.Closed
            else -> ShiftState.Open
        }
        val cash = kassa.cashInDrawer(kkmId, pin).valueOr(ShiftPart.Cash, troubles)
        return ShiftSnapshot(
            kkm = kkm,
            shift = state,
            open = open.takeIf { state == ShiftState.Open },
            documents = (documents as? Documents.Read)?.list,
            cash = cash,
            trouble = troubles.firstOrNull()
        )
    }

    /** Документы открытой смены, страницами: за раз касса отдаёт не больше [PAGE]. */
    private suspend fun readDocuments(
        kkmId: String,
        shift: ShiftResponse,
        pin: String,
        troubles: MutableList<ShiftTrouble>
    ): Documents {
        val all = mutableListOf<FiscalDocumentResponse>()
        var outcome: Documents? = null
        while (outcome == null) {
            val offset = all.size
            val answer = kassa.ask { it.listShiftDocuments(kkmId, shift.id, PAGE, offset, pin) }
            val page = if (answer.closedShift()) null else answer.valueOr(ShiftPart.Documents, troubles)
            page?.let(all::addAll)
            outcome = when {
                answer.closedShift() -> Documents.Gone
                page == null -> Documents.Unread
                page.size < PAGE -> Documents.Read(all)
                else -> null
            }
        }
        return outcome
    }

    private fun Answer<*>.closedShift(): Boolean = this is Answer.Refused && code == SHIFT_NOT_OPEN

    private fun <T> Answer<T>.valueOr(part: ShiftPart, troubles: MutableList<ShiftTrouble>): T? = when (this) {
        is Answer.Done -> value
        is Answer.Refused -> null.also { troubles += ShiftTrouble(this, part) }
        is Answer.Failed -> null.also { troubles += ShiftTrouble(this, part) }
    }

    /** Что стало с документами смены. */
    private sealed interface Documents {
        class Read(val list: List<FiscalDocumentResponse>) : Documents

        /** Касса их не отдала: о документах неизвестно ничего. */
        data object Unread : Documents

        /** Смена закрылась между обращениями: документов у неё нет по существу. */
        data object Gone : Documents
    }

    private companion object {
        /** Больше за одно обращение касса документов не отдаёт. */
        const val PAGE = 500
        const val SHIFT_NOT_OPEN = "SHIFT_NOT_OPEN"
    }
}
