package kz.mybrain.superkassa.presentation.kassa.refund.component

import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsUiState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Непрочитанный день возврата не выдаётся за день без оснований.
 *
 * «Подходящих чеков-оснований нет» — утверждение о кассе, и говорить его
 * можно только вслед за ответом кассы: кассир при покупателе с чеком
 * в руках читал молчание кассы как отказ в возврате.
 */
class BasisUnreadTest {

    private val texts = textsOf(Language.Ru).journal

    /** Кнопка повтора в сравнении не участвует: сравниваются слова, а не замыкания. */
    private fun withoutRetry(state: ScreenState): ScreenState =
        (state as ScreenState.Trouble).copy(onRetry = null)

    /** Значок пустого состояния берётся у него же: проверяются слова, а не картинка. */
    private fun iconOf(state: ScreenState) = (state as ScreenState.Empty).icon

    @Test
    fun `возврат называет непрочитанный день бедой чтения, а не отсутствием оснований`() {
        val journal = texts.returns
        val day = ReturnsUiState(kkm = CoreScene.kkm(), signedIn = true, shiftOpen = true, loading = false)

        val unread = basisState(day.copy(dayRead = false), journal) {}
        val empty = basisState(day.copy(dayRead = true), journal) {}

        assertEquals(ScreenState.Trouble(journal.basisUnread, journal.basisUnreadHint), withoutRetry(unread))
        assertNotNull((unread as ScreenState.Trouble).onRetry, "повторить чтение должно быть чем")
        assertEquals(ScreenState.Empty(iconOf(empty), journal.noSaleBasis, journal.noBasisHint), empty)
    }
}
