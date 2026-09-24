package kz.mybrain.superkassa.presentation.settings.tax

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.domain.settings.model.KkmSettingRules
import kz.mybrain.superkassa.presentation.settings.SettingsScene
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.shot
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
        val closed = card("tax-shift-closed")
        val opened = card("tax-shift-open", shiftOpen = true)

        assertFalse(
            closed.contentEquals(opened),
            "карточка налогов одинакова при открытой и закрытой смене"
        )
    }

    /** Непустая очередь отправки видна там же и по тому же поводу. */
    @Test
    fun `непустая очередь видна на карточке налогов до нажатия`() {
        val empty = card("tax-queue-empty")
        val waiting = card("tax-queue-waiting", queued = 1)

        assertFalse(
            empty.contentEquals(waiting),
            "карточка налогов одинакова при пустой и непустой очереди отправки"
        )
    }

    private fun rule(programming: Boolean, shiftOpen: Boolean, queueWaiting: Boolean) =
        KkmSettingRules.tax(programming, shiftOpen, queueWaiting)

    /**
     * Кадр карточки налогов у кассы в режиме программирования.
     *
     * @param shiftOpen открыта ли смена кассы.
     * @param queued сколько документов ждёт отправки в БФД.
     */
    private fun card(name: String, shiftOpen: Boolean = false, queued: Int = 0): ByteArray {
        val desk = KassaScene.desk(KassaScene.kkm(state = "PROGRAMMING", shiftOpen = shiftOpen))
        val tax = SettingsScene.board(desk, queued = queued).tax
        return KassaScene.shot(name, width = WIDTH, height = HEIGHT) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(Spacing.screen)) { TaxSettingsCard(tax, object : TaxSettingsActions {}) }
            }
        }
    }

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 420
    }
}
