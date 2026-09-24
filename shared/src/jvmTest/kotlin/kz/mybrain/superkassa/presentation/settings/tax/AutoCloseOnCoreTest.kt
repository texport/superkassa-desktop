package kz.mybrain.superkassa.presentation.settings.tax

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.presentation.settings.SettingsBench
import kz.mybrain.superkassa.presentation.settings.kkm.kkmSettingsModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * Переключатель «Закрывать смену самой через сутки» действует: ядро
 * закрывает смену по своим часам, когда до предела в сутки от первого
 * расчёта остаётся меньше его запаса.
 *
 * Прежде переключатель убирали как недействующий: закрывать смену было
 * некому. Касса в процессе закрывает её сама — фоном по часам и заходом
 * [io.github.texport.superkassa.embedded.api.Superkassa.closeDueShiftsNow],
 * которым её будит фоновая работа Android. Часы здесь — часы проверки:
 * сутки проходят без ожидания.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoCloseOnCoreTest {
    private lateinit var desk: SettingsBench

    @BeforeTest
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        desk = SettingsBench().enter()
    }

    @AfterTest
    fun close() {
        desk.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `включённое автозакрытие закрывает смену через сутки после первого чека`() {
        autoClose(true)
        assertTrue(desk.kassa.info().autoCloseShift, "касса не приняла переключатель")
        tradedYesterday()

        assertEquals(1, desk.bench.superkassa.closeDueShiftsNow(), "ядро не закрыло смену")
        assertNull(openShift(), "смена осталась открытой")
    }

    @Test
    fun `выключенное автозакрытие смену не трогает`() {
        autoClose(false)
        tradedYesterday()

        assertEquals(0, desk.bench.superkassa.closeDueShiftsNow())
        assertNotNull(openShift(), "смену закрыли без переключателя")
    }

    @Test
    fun `до предела в сутки смена не закрывается и с переключателем`() {
        autoClose(true)
        desk.kassa.openShift()
        desk.kassa.sell()
        desk.kassa.clock.move(EARLY)

        assertEquals(0, desk.bench.superkassa.closeDueShiftsNow())
        assertNotNull(openShift())
    }

    /**
     * Переключатель — как его ставит администратор: настройка кассы меняется
     * в режиме программирования, а закрывает смену касса вне его.
     */
    private fun autoClose(on: Boolean) {
        val kkm = kkmSettingsModel(desk.app.services, desk.app.areas.settings)
        kkm.switchProgramming()
        taxSettingsModel(desk.app.services).switchAutoClose(on)
        kkm.switchProgramming()
        assertFalse(desk.kassa.info().isProgrammingMode, "касса осталась в режиме программирования")
    }

    /** Смена открыта, первый чек пробит, и часы кассы ушли на сутки вперёд. */
    private fun tradedYesterday() {
        desk.kassa.openShift()
        desk.kassa.sell()
        desk.kassa.clock.move(DAY)
    }

    private fun openShift() = desk.kassa.api.getLocalOpenShift(desk.kassa.kkmId, desk.kassa.adminPin)

    private companion object {
        val DAY = 24.hours

        /** Полсуток: до предела ещё далеко, закрывать рано. */
        val EARLY = 12.hours
    }
}
