package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.QueueTask
import kz.mybrain.superkassa.desktop.ui.history.Shift
import kz.mybrain.superkassa.desktop.ui.settings.KkmSettingRules
import kz.mybrain.superkassa.desktop.ui.settings.TaxSettingsCard
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Налоги кассы не предлагают сохранить то, что узел не примет.
 *
 * Узел меняет налоговый режим только в режиме программирования, при
 * закрытой смене и пустой очереди отправки. Проверялся один режим
 * программирования: при открытой смене кнопка оставалась живой, владелец
 * выбирал режим, нажимал «Сохранить» и получал «сначала закройте смену».
 */
class SettingsTaxTest {

    @Test
    fun `узел примет налоги только при всех трёх условиях`() {
        assertTrue(KkmSettingRules.met(rule(programming = true, shiftOpen = false, queueWaiting = false)))
        assertFalse(
            KkmSettingRules.met(rule(programming = false, shiftOpen = false, queueWaiting = false)),
            "налоги предлагаются к сохранению вне режима программирования"
        )
        assertFalse(
            KkmSettingRules.met(rule(programming = true, shiftOpen = true, queueWaiting = false)),
            "налоги предлагаются к сохранению при открытой смене"
        )
        assertFalse(
            KkmSettingRules.met(rule(programming = true, shiftOpen = false, queueWaiting = true)),
            "налоги предлагаются к сохранению при непустой очереди отправки"
        )
    }

    /**
     * Открытая смена видна на самой карточке.
     *
     * Кадр снимается с той же кассы в режиме программирования: разница
     * между кадрами — только в смене. Совпали — значит карточка о смене
     * не знает, и кнопка зовёт узел впустую.
     */
    @Test
    fun `открытая смена видна на карточке налогов до нажатия`() {
        val closed = card("tax-shift-closed", shift = null)
        val opened = card("tax-shift-open", shift = KassaScene.openShift())

        assertFalse(
            closed.contentEquals(opened),
            "карточка налогов одинакова при открытой и закрытой смене"
        )
    }

    /** Непустая очередь отправки видна там же и по тому же поводу. */
    @Test
    fun `непустая очередь видна на карточке налогов до нажатия`() {
        val empty = card("tax-queue-empty", shift = null)
        val waiting = card("tax-queue-waiting", shift = null, queue = listOf(QueueTask(id = "t-1", status = "PENDING")))

        assertFalse(
            empty.contentEquals(waiting),
            "карточка налогов одинакова при пустой и непустой очереди отправки"
        )
    }

    private fun rule(programming: Boolean, shiftOpen: Boolean, queueWaiting: Boolean) =
        KkmSettingRules.tax(programming, shiftOpen, queueWaiting)

    private fun card(name: String, shift: Shift?, queue: List<QueueTask> = emptyList()): ByteArray {
        val session = session(name, shift, queue)
        return KassaScene.shot(name, width = WIDTH, height = HEIGHT) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(Spacing.screen)) { TaxSettingsCard(session) }
            }
        }
    }

    private fun session(name: String, shift: Shift?, queue: List<QueueTask>): Session {
        val session = KassaScene.session(name, kkm = KassaScene.kkm(state = "PROGRAMMING"), shift = shift)
        session.board.adoptQueue(queue)
        session.dictionaries[Dictionary.TaxRegimes] = listOf(entry("GENERAL", "Общеустановленный"))
        session.dictionaries[Dictionary.VatGroups] = listOf(entry("VAT_12", "НДС 12%"))
        return session
    }

    private fun entry(code: String, ru: String) = DictionaryEntry(code = code, name = mapOf("ru" to ru))

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 420
    }
}
