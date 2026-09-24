package kz.mybrain.superkassa.presentation.common.look

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.color.Accent
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.kassa.MemoryLook
import kz.mybrain.superkassa.strings.api.Language
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Вид окна: выбор виден сразу и запоминается рабочим местом.
 *
 * Язык из того же выбора читает строка сообщений окна: язык, сменённый
 * в шапке, обязан дойти и до отказов кассы, а не только до надписей.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LookViewModelTest {
    private val memory = MemoryLook()
    private val look = WorkplaceLook(memory)

    /**
     * Модель заводится после подмены главного потока: заведённая раньше,
     * она слушала вид из настоящего потока, и запоздавшее чтение
     * перекрывало только что сделанный выбор — проверка падала через прогон.
     */
    private val model by lazy { LookViewModel(LookCases(look)) }

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `первое состояние — сохранённый выбор`() {
        val memory = MemoryLook(LookChoice(language = "en", appearance = "dark"))
        val saved = LookViewModel(LookCases(WorkplaceLook(memory)))

        assertEquals(Language.En, saved.state.value.language)
        assertEquals(Appearance.Dark, saved.state.value.appearance)
    }

    @Test
    fun `выбор виден сразу и уходит в память рабочего места`() {
        model.switchLanguage(Language.Ru)
        model.switchAppearance(Appearance.Dark)
        model.chooseAccent(Accent.Teal)
        model.chooseTextScale(TextScale.Larger)

        val shown = model.state.value
        assertEquals(Language.Ru, shown.language)
        assertEquals(Appearance.Dark, shown.appearance)
        assertEquals(Accent.Teal, shown.look.accent)
        assertEquals(TextScale.Larger, shown.look.textScale)
        assertEquals(LookChoice("ru", "dark", Accent.Teal.code, null, TextScale.Larger.code), memory.look)
    }

    @Test
    fun `рельс и колонка точек сворачиваются и разворачиваются`() {
        model.toggleRail()
        model.togglePlaces()
        assertTrue(model.state.value.railCollapsed && model.state.value.placesCollapsed)

        model.toggleRail()
        assertEquals(false, memory.look.railCollapsed)
        assertEquals(true, memory.look.placesCollapsed)
    }

    /** Модель вида — не единственный читатель: язык берёт и строка сообщений окна. */
    @Test
    fun `выбранный язык виден всем, кто читает вид рабочего места`() {
        model.switchLanguage(Language.En)

        assertEquals("en", look.state.value.language)
    }
}
