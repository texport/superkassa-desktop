package kz.mybrain.superkassa.domain.shift.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer

/**
 * Что касса рассказала о выбранной кассе за одно перечитывание.
 *
 * @property kkm касса как она есть сейчас; `null` — перечитать не удалось.
 * @property open открытая смена; `null` — смена закрыта или о ней неизвестно.
 * @property documents документы открытой смены; `null` — касса их не отдала.
 * @property cash наличные в ящике, в тиынах; `null` — касса не сказала.
 * @property trouble первая беда перечитывания; `null` — прочиталось всё.
 * @property lastClosed последняя закрытая смена, пока новая не открыта:
 *   её Z-отчёт кассир открывает с главного экрана сразу после закрытия.
 */
class ShiftSnapshot(
    val kkm: KkmResponse?,
    val shift: ShiftState,
    val open: ShiftResponse?,
    val documents: List<FiscalDocumentResponse>?,
    val cash: Long?,
    val trouble: ShiftTrouble?,
    val lastClosed: ShiftResponse? = null
)

/** Что именно не прочиталось: экран называет это своими словами. */
enum class ShiftPart { Kkm, Shift, Documents, Cash }

/** Беда перечитывания: что не прочиталось и что на это сказала касса. */
class ShiftTrouble(val answer: Answer<Nothing>, val part: ShiftPart)
