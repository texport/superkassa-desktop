package kz.mybrain.superkassa.desktop

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.setup.ConnectKkmScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * «Начать заново» в мастере подключения спрашивает, а не стирает молча.
 *
 * Пройденное лежит на диске, и нажатие стирало его сразу: заводской номер,
 * уже унесённый в кабинет, и кассу, заведённую там под ним. Сами они
 * из кабинета не исчезают — владелец заводил вторую кассу под вторым
 * номером и разбирался с этим потом.
 *
 * Пока мастер ничего не прошёл, стирать нечего, и кнопки нет вовсе.
 */
class SetupStartOverTest {

    private fun workplace(): Session = Look.session()

    private fun draftOf(session: Session) = KkmSetupDraft(session.preferences)

    @Test
    fun `нажатие только спрашивает, а пройденное остаётся на месте`() {
        val session = workplace()
        draftOf(session).rememberFactory(FACTORY, YEAR)
        val cabinet = mockCabinet("""{"page":0,"size":50,"totalElements":0,"items":[]}""")
        RenderProbe(width = WIDE, height = TALL) { ConnectKkmScreen(session, cabinet) {} }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            probe.click(Offset(START_OVER_X, START_OVER_Y))
            assertTrue(probe.changedFrom(before), "вопрос о начале заново не появился")
            assertEquals(FACTORY, draftOf(session).factoryNumber, "пройденное стёрлось без ответа владельца")
        }
    }

    @Test
    fun `нетронутый мастер стирать нечем`() {
        val session = workplace()
        val cabinet = mockCabinet("""{"page":0,"size":50,"totalElements":0,"items":[]}""")
        RenderProbe(width = WIDE, height = TALL) { ConnectKkmScreen(session, cabinet) {} }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            probe.click(Offset(START_OVER_X, START_OVER_Y))
            assertTrue(probe.frame().contentEquals(before), "на месте кнопки «Начать заново» что-то нажалось")
            assertNull(draftOf(session).factoryNumber)
        }
    }

    private companion object {
        const val FACTORY = "KZT26E2C509A200"
        const val YEAR = "2026"
        const val WIDE = 1372
        const val TALL = 887
        const val SETTLE = 20

        /** Где в шапке мастера стоит «Начать заново»: справа, на одной строке с названием. */
        const val START_OVER_X = 1276f
        const val START_OVER_Y = 33f
    }
}
