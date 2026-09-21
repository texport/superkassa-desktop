package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.setup.workplaceOfd
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Какой контур БФД мастер подключения предлагает по умолчанию.
 *
 * Предлагал первый из справочника узла и попадал на контур, которого
 * владелец не выбирал. Рабочее место шлёт чеки в один контур, и это
 * записано у касс, которые на нём уже заведены.
 */
class WorkplaceOfdTest {

    private val environments = listOf(entry("DEV"), entry("TEST"), entry("PROD"))

    private fun entry(code: String) = DictionaryEntry(code = code, name = mapOf("ru" to code))

    private fun kkm(environment: String?) =
        Kkm(kkmId = "kkm-$environment", ofdId = "BFD", ofdEnvironment = environment)

    @Test
    fun `берётся контур заведённых касс, а не первый из справочника`() {
        val own = workplaceOfd(listOf(kkm("PROD"), kkm("PROD")), environments)
        assertEquals("PROD", own.environment)
    }

    @Test
    fun `при разных контурах берётся тот, которым пользуется больше касс`() {
        val kkms = listOf(kkm("TEST"), kkm("TEST"), kkm("PROD"))
        assertEquals("TEST", workplaceOfd(kkms, environments).environment)
    }

    @Test
    fun `без касс остаётся первый поднятый контур`() {
        assertEquals("DEV", workplaceOfd(emptyList(), environments).environment)
    }

    /** Погашенный контур выбором не станет: подставить его значило бы запереть мастер. */
    @Test
    fun `неподнятый контур в подстановку не попадает`() {
        val unraised = listOf(entry("TEST"), entry("PROD"), entry("DEV"))
        assertEquals("DEV", workplaceOfd(emptyList(), unraised).environment)
    }

    @Test
    fun `код, которого нет в справочнике, не подставляется`() {
        val own = workplaceOfd(listOf(kkm("НЕИЗВЕСТНЫЙ")), environments)
        assertEquals("DEV", own.environment)
    }

    @Test
    fun `поставщик подставлен всегда, справочника для него не нужно`() {
        assertEquals("BFD", workplaceOfd(emptyList(), environments).provider)
    }
}
