package kz.mybrain.superkassa.presentation.cabinet.register.component

import kz.mybrain.superkassa.domain.cabinet.model.documents.TechnicalState
import kz.mybrain.superkassa.presentation.cabinet.register.component.StateScene.answer
import kz.mybrain.superkassa.presentation.cabinet.register.component.StateScene.bfd
import kz.mybrain.superkassa.presentation.cabinet.register.component.StateScene.cabinetRegister
import kz.mybrain.superkassa.presentation.cabinet.register.component.StateScene.node
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Сверка состояния словами: каждое утверждение законченное, и молчание
 * источника объяснено, а не оставлено пустой строкой.
 */
class StateWordsTest {

    private val texts = cabinetTexts(Language.Ru)

    /**
     * О смене не высказался никто: касса заведена на другой машине,
     * а БФД её ещё не видел. Пустой строки на этом месте быть не должно.
     */
    @Test
    fun `о смене молчат все — ответ всё равно сказан словами`() {
        val claims = stateClaims(null, cabinetRegister("REGISTERED"), null)
        val shift = answer(claims, StateQuestion.Shift)

        assertEquals(Headline.ShiftUnknown, shift.headline)
        assertEquals(3, shift.claims.size, "молчащие источники остаются на экране")
        assertTrue(texts.headlineWords(shift.headline).isNotBlank())
    }

    /**
     * Плашка читается отдельно от всего: в ней сказано, кто говорит
     * и что именно, а не «Нет» под подписью строкой выше.
     */
    @Test
    fun `плашка источника — законченное утверждение`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = cabinetRegister("DEREGISTERED"),
            technical = bfd(active = true, shift = "CLOSED")
        )
        val snapshot = bfd(active = true, shift = "CLOSED")
        val words = claims.map { texts.claimWords(it, StateQuestion.Usable, snapshot) }

        assertEquals(
            listOf(
                "${texts.sourceNode}${Glyphs.SEPARATOR}${texts.claimWorking}",
                "${texts.sourceCabinet}${Glyphs.SEPARATOR}${texts.claimOffRecord}",
                "${texts.sourceBfd}${Glyphs.SEPARATOR}${texts.claimWorking}"
            ),
            words
        )
    }

    /**
     * Кабинет говорит о кассе то, что у него записано.
     *
     * У него не «да и нет», а учёт КГД с пятью состояниями, и все, кроме
     * учтённого, сводились к одному «касса снята с учёта». Так карточка
     * говорила о черновике, который владелец завёл час назад и никуда
     * ещё не подавал, — а черновиков у сети показа три тысячи из трёх
     * тысяч трёхсот. Снятие с учёта владелец затевает сам и знает о нём;
     * прочесть о нём там, где его не было, — повод бежать разбираться.
     */
    @Test
    fun `кабинет не объявляет снятым с учёта то, что на учёт не ставилось`() {
        val said = listOf("DRAFT", "REGISTRATION_IN_ISNA_PROCESS", "REGISTRATION_IN_ISNA_ERROR").map { status ->
            val claims = stateClaims(null, cabinetRegister(status), null)
            val cabinet = claims.first { it.source == StateSource.Cabinet }
            status to texts.claimWords(cabinet, StateQuestion.Usable, null).substringAfter(Glyphs.SEPARATOR)
        }

        said.forEach { (status, words) ->
            assertFalse(words == texts.claimOffRecord, "о состоянии $status сказано «$words»")
        }
        assertEquals(said.map { it.second }.distinct().size, said.size, "три разных состояния названы одинаково")
        val deregistered = stateClaims(null, cabinetRegister("DEREGISTERED"), null)
        assertEquals(
            texts.claimOffRecord,
            texts.claimWords(
                deregistered.first { it.source == StateSource.Cabinet },
                StateQuestion.Usable,
                null
            ).substringAfter(Glyphs.SEPARATOR),
            "снятая с учёта перестала называться снятой"
        )
    }

    /**
     * Молчание тоже сказано словами, и у каждого оно своё: БФД не ответил
     * вовсе — перечитать карточку; БФД кассу ещё не видел — ждать её
     * первого обращения; кабинет смену не ведёт по устройству, и ответа
     * от него не будет никогда.
     */
    @Test
    fun `молчание каждого источника объяснено своими словами`() {
        val bfd = StateClaim(StateSource.Bfd, Verdict.Unknown, Verdict.Unknown)
        val cabinet = StateClaim(StateSource.Cabinet, Verdict.Yes, Verdict.Unknown)

        assertEquals(
            listOf(texts.claimBfdNoAnswer, texts.claimBfdNoKkm, texts.claimShiftNotKept),
            listOf(
                texts.claimWords(bfd, StateQuestion.Usable, null),
                texts.claimWords(bfd, StateQuestion.Usable, TechnicalState()),
                texts.claimWords(cabinet, StateQuestion.Shift, null)
            ).map { it.substringAfter(Glyphs.SEPARATOR) }
        )
    }
}
