package kz.mybrain.superkassa.presentation.dashboard

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.kassa.Answer
import kz.mybrain.superkassa.domain.kassa.ask
import kz.mybrain.superkassa.domain.shift.ShiftState
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.strings.AppStrings
import kz.mybrain.superkassa.presentation.strings.moneyTexts

/**
 * Что касса рассказала о выбранной кассе за одно перечитывание.
 *
 * @property kkm касса как она есть сейчас; `null` — перечитать не удалось.
 * @property open открытая смена; `null` — смена закрыта или о ней неизвестно.
 * @property documents документы открытой смены; `null` — касса их не отдала.
 * @property trouble первая беда перечитывания; `null` — прочиталось всё.
 */
class ShiftSnapshot(
    val kkm: KkmResponse?,
    val shift: ShiftState,
    val open: ShiftResponse?,
    val documents: List<FiscalDocumentResponse>?,
    val cash: Long?,
    val trouble: Trouble?
)

/**
 * Беда перечитывания: что не прочиталось и что на это сказала касса.
 *
 * @property what что читалось, словами кассира.
 * @property action что читалось, для журнала.
 */
class Trouble(val answer: Answer<Nothing>, val what: String, val action: String)

/**
 * Перечитывает кассу, её смену, документы смены и наличные в ящике.
 *
 * Каждое чтение идёт своим обращением, и беда одного не отменяет
 * остальных: касса, снятая с учёта, отказывает в документах, а смену
 * называет — и экран обязан показать смену открытой, а не неизвестной.
 */
class ShiftReader(private val app: AppContainer) {
    private val troubles = mutableListOf<Trouble>()

    suspend fun read(kkmId: String, pin: String, texts: AppStrings): ShiftSnapshot {
        troubles.clear()
        val kkm = valueOf(app.kassa.ask { it.getKkm(kkmId) }, texts.login.reload, "read kkm")
        val answer = app.kassa.ask { it.getLocalOpenShift(kkmId, pin) }
        val open = (answer as? Answer.Done)?.value
        val documents = open?.let { readDocuments(kkmId, it, pin, texts.dashboard.shiftDocuments) }
        val state = when {
            answer !is Answer.Done -> ShiftState.Unknown.also { valueOf(answer, texts.dashboard.shift, "read shift") }
            open == null || documents is Documents.Gone -> ShiftState.Closed
            else -> ShiftState.Open
        }
        val cash = readCash(kkmId, pin, moneyTexts(app.language()).drawer.inDrawer)
        return ShiftSnapshot(
            kkm = kkm,
            shift = state,
            open = open.takeIf { state == ShiftState.Open },
            documents = (documents as? Documents.Read)?.list,
            cash = cash,
            trouble = troubles.firstOrNull()
        )
    }

    /** Кто оформил отклонённые документы: без имени отказ — «кто-то в 14:53». Молча: это подробность. */
    suspend fun operators(
        kkmId: String,
        pin: String,
        refused: List<FiscalDocumentResponse>,
        known: Map<String, String>
    ): Map<String, String> = refused.filter { it.id !in known }.mapNotNull { document ->
        val details = (app.kassa.ask { it.getDocumentDetails(kkmId, document.id, pin) } as? Answer.Done)?.value
        details?.operatorName?.let { document.id to it }
    }.toMap()

    /** Названия видов документов на трёх языках; `null` — справочник не прочитан. */
    suspend fun documentTypes(): Map<String, TrilingualMessageResponse>? =
        (app.kassa.ask { it.getDocumentTypes() } as? Answer.Done)?.value?.associate { it.code to it.name }

    /**
     * Документы открытой смены, страницами: за раз касса отдаёт не больше [PAGE].
     *
     * Смена, закрытая между двумя обращениями, — состояние кассы, а не отказ.
     */
    private suspend fun readDocuments(kkmId: String, shift: ShiftResponse, pin: String, what: String): Documents {
        val all = mutableListOf<FiscalDocumentResponse>()
        var outcome: Documents? = null
        while (outcome == null) {
            val answer = app.kassa.ask { it.listShiftDocuments(kkmId, shift.id, PAGE, all.size, pin) }
            val page = if (answer.closedShift()) null else valueOf(answer, what, "read shift documents")
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

    /** Наличные в ящике: счётчик всей кассы, в тиынах. */
    private suspend fun readCash(kkmId: String, pin: String, what: String): Long? =
        valueOf(app.kassa.ask { it.listCounters(kkmId, pin) }, what, "read counters")
            ?.firstOrNull { it.scope == GLOBAL_SCOPE && it.key == CASH_SUM }
            ?.value

    private fun <T> valueOf(answer: Answer<T>, what: String, action: String): T? = when (answer) {
        is Answer.Done -> answer.value
        is Answer.Refused -> null.also { troubles += Trouble(answer, what, action) }
        is Answer.Failed -> null.also { troubles += Trouble(answer, what, action) }
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
        const val GLOBAL_SCOPE = "GLOBAL"
        const val CASH_SUM = "cash.sum"
    }
}
