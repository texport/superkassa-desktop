package kz.mybrain.superkassa.presentation.common.status

import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Плашки состояния кассы в шапке окна.
 *
 * Шапка заблокированной кассы писала «Заблокирована» дважды подряд:
 * своей плашкой и словом состояния, а оба слова брались из одного
 * признака. Повтор виден только глазами, поэтому набор плашек собирается
 * без композиции — и проверяется здесь.
 */
class KkmStatusTest {

    private val words = KkmStatusWords(
        state = "Заблокирована",
        autonomous = "Автономно",
        shiftOpen = "Смена открыта",
        shiftClosed = "Смена закрыта"
    )

    /**
     * @param waiting сколько документов ждут отправки: автономной касса
     *   считается по неотправленному, а не по одной отметке о начале —
     *   её касса снимает лишь при следующей фискальной операции.
     */
    private fun kkm(state: String, autonomousSince: Long? = null, waiting: Int = 0, shiftOpen: Boolean = false) =
        KassaScene.kkm(state = state, autonomousSince = autonomousSince, shiftOpen = shiftOpen)
            .copy(offlineQueueCount = waiting)

    @Test
    fun `состояние кассы названо ровно один раз`() {
        val texts = kkmStatusChips(kkm("BLOCKED"), words).map { it.text }
        assertEquals(1, texts.count { it == words.state }, texts.joinToString())
        assertEquals(texts.distinct(), texts, texts.joinToString())
    }

    @Test
    fun `блокировка осталась цветом плашки состояния, а не второй плашкой`() {
        val chips = kkmStatusChips(kkm("BLOCKED"), words)
        assertEquals(words.state, chips.first().text)
        assertEquals(StatusTone.Bad, chips.first().tone)
        assertEquals(StatusTone.Waiting, stateTone(blocked = false, programming = true))
        assertEquals(StatusTone.Good, stateTone(blocked = false, programming = false))
    }

    @Test
    fun `автономная работа добавляет плашку, обычная — нет`() {
        val offline = kkmStatusChips(kkm("ACTIVE", autonomousSince = 1L, waiting = 1), words)
        val online = kkmStatusChips(kkm("ACTIVE"), words)
        assertTrue(offline.any { it.text == words.autonomous }, offline.joinToString { it.text })
        assertTrue(online.none { it.text == words.autonomous }, online.joinToString { it.text })
        assertEquals(offline.size - 1, online.size)

        // Очередь опустела, а отметка ещё стоит: касса уже не автономна.
        val delivered = kkmStatusChips(kkm("ACTIVE", autonomousSince = 1L), words)
        assertTrue(delivered.none { it.text == words.autonomous }, delivered.joinToString { it.text })
    }

    @Test
    fun `смена сказана словом кассы, закрытая — ожиданием, а не отказом`() {
        val open = kkmStatusChips(kkm("ACTIVE", shiftOpen = true), words)
        val closed = kkmStatusChips(kkm("ACTIVE"), words)
        assertTrue(open.any { it.text == words.shiftOpen && it.tone == StatusTone.Good })
        assertTrue(closed.any { it.text == words.shiftClosed && it.tone == StatusTone.Waiting })
    }

    @Test
    fun `без выбранной кассы плашек нет`() {
        assertEquals(emptyList(), kkmStatusChips(null, words))
    }

    /**
     * Каждое состояние ядра названо словом на каждом языке, незнакомое — своим кодом.
     *
     * Голая плашка без слова читалась бы как «всё в порядке».
     */
    @Test
    fun `состояния ядра названы словами`() {
        Language.entries.forEach { language ->
            val enums = textsOf(language).common.enums
            val said = listOf("ACTIVE", "BLOCKED", "PROGRAMMING", "REGISTRATION").map { enums.kkmState(it) }
            assertTrue(said.all { it.isNotBlank() }, "$language: $said")
            assertEquals(said.distinct(), said, "$language: состояния названы одним словом")
            assertEquals("NEW_STATE", enums.kkmState("NEW_STATE"))
        }
    }
}
