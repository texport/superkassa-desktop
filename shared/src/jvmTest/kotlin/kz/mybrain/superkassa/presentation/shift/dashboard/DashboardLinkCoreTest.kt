package kz.mybrain.superkassa.presentation.shift.dashboard

import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.kkm.model.isAutonomous
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Связь с БФД на главном экране: проверка связи, автономный режим
 * и досылка очереди — на настоящем ядре, итог сверяется с тестовым БФД.
 */
class DashboardLinkCoreTest {
    private val desk = CoreDesk()
    private val texts = stringsOf(Language.Ru)

    @AfterTest
    fun close() = desk.close()

    @Test
    fun `связь есть — кассиру так и сказано`() {
        desk.seated()
        val model = dashboardModel(desk.app)

        model.checkLink()

        assertEquals(Message.Done(texts.autonomous.linkBack), desk.said)
    }

    @Test
    fun `связи нет — кассиру сказано, что БФД молчит, а не «готово»`() {
        desk.seated()
        desk.bfd.disconnect()
        val model = dashboardModel(desk.app)

        model.checkLink()

        assertEquals(texts.settings.ofdSilent, desk.saidText)
    }

    @Test
    fun `чек без связи — касса в автономном режиме, документ ждёт в очереди`() {
        val kassa = desk.seated()
        kassa.offlineSale()
        val model = dashboardModel(desk.app)

        val kkm = model.state.value.kkm

        assertTrue(kkm?.isAutonomous == true, "экран не показывает автономный режим: ${kkm?.state}")
        assertEquals(1, kkm.offlineQueueCount)
        assertTrue(desk.bfd.countedTickets().isEmpty())
    }

    @Test
    fun `кассиру при открытой смене досылку не предлагают — она ушла бы отказом «нет прав»`() {
        val kassa = desk.seated()
        kassa.offlineSale()
        val state = dashboardModel(desk.app).state.value

        val shown = RenderProbe(width = WIDE, height = TALL) { DashboardContent(state) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.nodes().mapNotNull { it.text }
        }

        assertFalse(state.canSendQueued)
        assertTrue(texts.autonomous.checkLink in shown, "проверки связи нет в автономном режиме")
        assertFalse(texts.autonomous.sendQueued in shown, "кнопка досылки предложена там, где касса откажет")
        assertTrue(texts.autonomous.sendQueuedRules in shown, "условия досылки не названы")
    }

    @Test
    fun `связь вернулась — накопленное уходит в БФД один раз, экран выходит из автономного режима`() {
        val kassa = desk.seated()
        kassa.offlineSale()
        val model = dashboardModel(desk.app)

        kassa.resendQueue()
        model.refresh()

        assertEquals(1, desk.bfd.countedTickets().size, "очередь не дошла или дошла дважды")
        assertEquals(0, model.state.value.kkm?.offlineQueueCount)
        assertFalse(model.state.value.kkm?.isAutonomous == true, "касса осталась автономной")
    }

    private companion object {
        const val WIDE = 1400
        const val TALL = 1600
        const val SETTLE = 10
    }
}
