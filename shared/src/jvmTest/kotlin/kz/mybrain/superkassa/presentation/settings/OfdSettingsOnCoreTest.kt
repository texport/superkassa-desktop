package kz.mybrain.superkassa.presentation.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.settings.kkm.kkmSettingsModel
import kz.mybrain.superkassa.presentation.settings.ofd.ofdSettingsModel
import kz.mybrain.superkassa.presentation.shell.frame.shellModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Связь кассы с БФД в настройках на настоящем ядре и тестовом БФД:
 * сверка, токен, сведения о кассе у БФД и снятие блокировки.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfdSettingsOnCoreTest {
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
    fun `сверка сведений с БФД — удача словами кассира`() {
        ofdSettingsModel(desk.app.services).syncService()

        assertIs<Message.Done>(desk.notices.last)
    }

    /**
     * Ответ БФД на запрос сведений без отчёта о счётчиках ядро не разбирает:
     * падает `IllegalArgumentException` без кода. Удачей это на экране
     * не называется — кассир видит, что сверка не выполнена.
     */
    @Test
    fun `сверка счётчиков, которую ядро не разобрало, удачей не названа`() {
        ofdSettingsModel(desk.app.services).syncCounters()

        assertFalse(desk.notices.last is Message.Done, "неразобранная сверка названа удачей")
    }

    @Test
    fun `сверка без связи с БФД — не удача, а отказ или сбой на экране`() {
        val model = ofdSettingsModel(desk.app.services)
        desk.kassa.bfd.disconnect()

        model.syncService()

        val said = desk.notices.last
        assertTrue(said is Message.Refusal || said is Message.Failed, "без связи сказано: $said")
    }

    @Test
    fun `сведения о кассе у БФД читаются и видны на карточке`() {
        val model = ofdSettingsModel(desk.app.services)

        model.askInfo()

        val summary = assertNotNull(model.state.value.summary, "сведения БФД не показаны")
        assertEquals(desk.kassa.bfd.kgdNumber(desk.kassa.systemId), summary.kgdNumber)
    }

    @Test
    fun `сведения о кассе у БФД снимают блокировку, и окно это видит`() {
        desk.kassa.openShift()
        runCatching { desk.kassa.rejectedSale(code = BLOCKING_CODE) }
        shellModel(desk.app).refresh()
        assertEquals("BLOCKED", desk.signIn.state.value.kkm?.state, "касса не встала после отказа БФД")

        ofdSettingsModel(desk.app.services).askInfo()

        assertEquals("ACTIVE", desk.kassa.info().state, "ядро не сняло блокировку ответом на запрос сведений")
        assertEquals("ACTIVE", desk.signIn.state.value.kkm?.state, "окно не узнало, что касса снова в строю")
        assertNull(desk.signIn.state.value.kkm?.blockReasonCode)
    }

    @Test
    fun `новый токен принимается в режиме программирования и не остаётся в поле`() {
        kkmSettingsModel(desk.app.services, desk.app.areas.settings).switchProgramming()
        val model = ofdSettingsModel(desk.app.services)

        model.typeToken("12ab34 56")
        assertEquals("123456", model.state.value.token, "в поле токена попало не только цифры")
        model.saveToken()

        assertIs<Message.Done>(desk.notices.last)
        assertEquals("", model.state.value.token)
        assertFalse(desk.journal.lines.any { it.contains("123456") }, "токен попал в журнал")
    }

    @Test
    fun `вне режима программирования поле токена закрыто`() {
        val model = ofdSettingsModel(desk.app.services)

        assertFalse(model.state.value.tokenEditable)
    }

    @Test
    fun `проверка связи с ОФД отвечает итогом, а не состоянием`() {
        val model = ofdSettingsModel(desk.app.services)

        model.checkLink()
        assertEquals(true, model.state.value.linkAlive)
        desk.kassa.bfd.disconnect()
        model.checkLink()
        assertEquals(false, model.state.value.linkAlive)
    }

    private companion object {
        /** Отказ БФД, по которому касса встаёт: касса заблокирована в БФД. */
        const val BLOCKING_CODE = 5
    }
}
