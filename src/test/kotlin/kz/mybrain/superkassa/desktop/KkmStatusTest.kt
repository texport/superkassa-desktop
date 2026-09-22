package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.components.KkmStatusWords
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.components.kkmStatusChips
import kz.mybrain.superkassa.desktop.ui.components.stateTone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Плашки состояния кассы в шапке окна.
 *
 * Шапка заблокированной кассы писала «Заблокирована» дважды подряд:
 * своей плашкой и словом справочника узла, а оба слова брались из одного
 * признака. Повтор виден только глазами, поэтому набор плашек собирается
 * без композиции — и проверяется здесь.
 */
class KkmStatusTest {

    private val words = KkmStatusWords(
        state = "Заблокирована",
        autonomous = "Автономно",
        shiftOpen = "Смена открыта",
        shiftClosed = "Смена закрыта",
        shiftUnknown = "Смена неизвестна",
        nodeOnline = "Узел на связи",
        nodeOffline = "Узел недоступен"
    )

    /**
     * @param waiting сколько документов ждут отправки: автономной касса
     *   считается по неотправленному, а не по одной отметке о начале —
     *   её узел снимает лишь при следующей фискальной операции.
     */
    private fun kkm(state: String?, autonomousSince: Long? = null, waiting: Int = 0) =
        Kkm(
            kkmId = "kkm-1",
            state = state,
            autonomousSince = autonomousSince,
            offlineQueueCount = waiting
        )

    @Test
    fun `состояние кассы названо ровно один раз`() {
        val chips = kkmStatusChips(kkm("BLOCKED"), shift = ShiftState.Closed, nodeAvailable = true, words = words)
        val texts = chips.map { it.text }
        assertEquals(1, texts.count { it == words.state }, texts.joinToString())
        assertEquals(texts.distinct(), texts, texts.joinToString())
    }

    @Test
    fun `блокировка осталась цветом плашки состояния, а не второй плашкой`() {
        val chips = kkmStatusChips(kkm("BLOCKED"), shift = ShiftState.Closed, nodeAvailable = true, words = words)
        assertEquals(words.state, chips.first().text)
        assertEquals(StatusTone.Bad, chips.first().tone)
        assertEquals(StatusTone.Waiting, stateTone(blocked = false, programming = true))
        assertEquals(StatusTone.Good, stateTone(blocked = false, programming = false))
    }

    @Test
    fun `автономная работа добавляет плашку, обычная — нет`() {
        val offline = kkmStatusChips(kkm("KKM_ACTIVE", autonomousSince = 1L, waiting = 1), CLOSED, true, words)
        val online = kkmStatusChips(kkm("KKM_ACTIVE"), CLOSED, true, words)
        assertTrue(offline.any { it.text == words.autonomous }, offline.joinToString { it.text })
        assertTrue(online.none { it.text == words.autonomous }, online.joinToString { it.text })
        assertEquals(offline.size - 1, online.size)

        // Очередь опустела, а отметка ещё стоит: касса уже не автономна.
        val delivered = kkmStatusChips(kkm("KKM_ACTIVE", autonomousSince = 1L), CLOSED, true, words)
        assertTrue(delivered.none { it.text == words.autonomous }, delivered.joinToString { it.text })
    }

    @Test
    fun `смена и связь с узлом сказаны каждым своим словом`() {
        val chips = kkmStatusChips(kkm("KKM_ACTIVE"), shift = ShiftState.Open, nodeAvailable = false, words = words)
        assertTrue(chips.any { it.text == words.shiftOpen && it.tone == StatusTone.Good })
        assertTrue(chips.any { it.text == words.nodeOffline && it.tone == StatusTone.Bad })
    }

    @Test
    fun `без выбранной кассы остаётся только связь с узлом`() {
        val chips = kkmStatusChips(null, shift = CLOSED, nodeAvailable = true, words = words)
        assertEquals(listOf(words.nodeOnline), chips.map { it.text })
    }

    /**
     * Состояние смены, которого узел не назвал, не выдаётся за закрытое.
     *
     * Шапка знала два состояния из трёх: у кассы, о смене которой узел
     * молчит, она писала «Смена закрыта» — а главный экран под ней в тот
     * же миг писал «Неизвестна». Кассир верит шапке и открывает смену,
     * которую узел держит открытой, и получает отказ.
     */
    @Test
    fun `неизвестная смена не выдаётся за закрытую`() {
        val chips = kkmStatusChips(kkm("KKM_ACTIVE"), ShiftState.Unknown, nodeAvailable = true, words = words)
        val said = chips.map { it.text }
        assertTrue(words.shiftClosed !in said, said.joinToString())
        assertTrue(words.shiftOpen !in said, said.joinToString())
        assertTrue(words.shiftUnknown in said, said.joinToString())
    }

    private companion object {
        /** Закрытая смена: в этих проверках она только фон для другого. */
        val CLOSED = ShiftState.Closed
    }
}
