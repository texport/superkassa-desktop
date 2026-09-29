package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.presentation.cabinet.places.PlaceSieve
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Колонка точек до первого ответа кабинета и её шапка.
 *
 * Правило самого места — у проверки `ScreenState` дизайн-системы; здесь —
 * что колонка точек им пользуется и рисуется, пока кабинет не ответил,
 * и что шапка колонки перечитывает кабинет.
 */
class PlaceTreeWaitingTest {

    private val cabinet = textsOf(Language.Ru).cabinet

    /** Пустая колонка точек: ждёт кабинет или уже прочитана. */
    @Composable
    private fun Tree(loading: Boolean, onRetry: () -> Unit = {}) {
        PlaceTree(
            texts = cabinet,
            language = Language.Ru,
            onCollapse = {},
            rows = emptyList(),
            total = 0,
            loading = loading,
            trouble = null,
            onRetry = onRetry,
            sieve = PlaceSieve(),
            onSieve = {},
            locksKnown = true,
            place = null,
            register = null,
            onPlace = {},
            onRegister = {},
            footer = {}
        )
    }

    /** Кабинет: колонка точек до первого ответа кабинета. */
    @Test
    fun `колонка точек кабинета не врёт, что точек нет`() {
        RenderProbe { Tree(loading = true) }.use { assertTrue(it.frame().isNotEmpty()) }
    }

    /**
     * Точку или кассу, заведённую в кабинете с другой машины, список
     * узнавал только после нового входа. Шапка колонки перечитывает кабинет.
     */
    @Test
    fun `шапка колонки перечитывает кабинет`() {
        var read = 0
        RenderProbe { Tree(loading = false) { read++ } }.use { probe ->
            probe.frame()
            probe.tap { it.label == cabinet.refresh }
        }
        assertEquals(1, read, "кнопка обновления кабинет не перечитала")
    }
}
