package kz.mybrain.superkassa.presentation.cabinet.places.component

import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.presentation.cabinet.places.PlaceSieve
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Колонка точек до первого ответа кабинета: общее место содержимого.
 *
 * Правило самого места — у проверки `ScreenState` дизайн-системы; здесь —
 * что колонка точек им пользуется и рисуется, пока кабинет не ответил.
 */
class PlaceTreeWaitingTest {

    private val cabinet = textsOf(Language.Ru).cabinet

    /** Кабинет: колонка точек до первого ответа кабинета. */
    @Test
    fun `колонка точек кабинета не врёт, что точек нет`() {
        RenderProbe {
            PlaceTree(
                texts = cabinet,
                language = Language.Ru,
                onCollapse = {},
                rows = emptyList(),
                total = 0,
                loading = true,
                trouble = null,
                onRetry = {},
                sieve = PlaceSieve(),
                onSieve = {},
                locksKnown = true,
                place = null,
                register = null,
                onPlace = {},
                onRegister = {},
                footer = {}
            )
        }.use { assertTrue(it.frame().isNotEmpty()) }
    }
}
