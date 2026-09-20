package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.setup.workplaceOfd
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Какой ОФД мастер подключения предлагает по умолчанию.
 *
 * Предлагал первый из справочника узла — чужого, — и касса заводилась
 * в него с первого нажатия. Рабочее место шлёт чеки одному ОФД, и это
 * записано у касс, которые на нём уже заведены.
 */
class WorkplaceOfdTest {

    private val providers = listOf(entry("kazakhtelecom"), entry("bfd"), entry("transtelecom"))
    private val environments = listOf(entry("PRODUCTION"), entry("TEST"))

    private fun entry(code: String) = DictionaryEntry(code = code, name = mapOf("ru" to code))

    private fun kkm(ofd: String?, environment: String? = "PRODUCTION") =
        Kkm(kkmId = "kkm-$ofd-$environment", ofdId = ofd, ofdEnvironment = environment)

    @Test
    fun `берётся ОФД заведённых касс, а не первый из справочника`() {
        val own = workplaceOfd(listOf(kkm("bfd"), kkm("bfd")), providers, environments)
        assertEquals("bfd", own.provider)
        assertEquals("PRODUCTION", own.environment)
    }

    @Test
    fun `при разных ОФД берётся тот, которым пользуется больше касс`() {
        val kkms = listOf(kkm("bfd"), kkm("bfd"), kkm("transtelecom"))
        assertEquals("bfd", workplaceOfd(kkms, providers, environments).provider)
    }

    @Test
    fun `контур берётся так же, как и ОФД`() {
        val kkms = listOf(kkm("bfd", "TEST"), kkm("bfd", "TEST"), kkm("bfd", "PRODUCTION"))
        assertEquals("TEST", workplaceOfd(kkms, providers, environments).environment)
    }

    @Test
    fun `без касс остаётся первое значение справочника`() {
        val own = workplaceOfd(emptyList(), providers, environments)
        assertEquals("kazakhtelecom", own.provider)
        assertEquals("PRODUCTION", own.environment)
    }

    @Test
    fun `код, которого нет в справочнике, не подставляется`() {
        val own = workplaceOfd(listOf(kkm("неизвестный", "НЕИЗВЕСТНЫЙ")), providers, environments)
        assertEquals("kazakhtelecom", own.provider)
        assertEquals("PRODUCTION", own.environment)
    }
}
