package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.cabinet.AdoptDraft
import kz.mybrain.superkassa.desktop.ui.components.BFD_PROVIDER
import kz.mybrain.superkassa.desktop.ui.components.OfdTarget
import kz.mybrain.superkassa.desktop.ui.components.environmentRaised
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Поставщик один, контур один поднят.
 *
 * Справочник узла называет двух операторов и три контура. Касса этого
 * продукта заводится только в БФД, и отвечает у неё пока только стенд
 * разработки: выбор из одного значения владельцу решать нечего, а чужой
 * оператор или неподнятый контур кончались бы отказом узла уже после
 * нажатия.
 */
class BfdTargetTest {

    private val environments = listOf(entry("DEV"), entry("TEST"), entry("PROD"))

    private fun entry(code: String, supported: Boolean = true) =
        DictionaryEntry(code = code, name = mapOf("ru" to code), supported = supported)

    /**
     * Настройки рабочего места лежат рядом с файлом выбранной кассы,
     * поэтому у каждого хода свой каталог: общий временный каталог
     * переносил запомненный контур из проверки в проверку.
     */
    private fun preferences(): Preferences =
        Preferences(File(Files.createTempDirectory("adopt").toFile(), "kkm"))

    @Test
    fun `поставщик подставлен, а не пуст`() {
        assertEquals(BFD_PROVIDER, OfdTarget().provider)
    }

    /** Полнота считается по обоим полям: подставленный поставщик её не отменяет. */
    @Test
    fun `выбор полон, как только назван контур`() {
        assertFalse(OfdTarget().complete, "без контура заводить нечего")
        assertTrue(OfdTarget(environment = "DEV").complete)
    }

    @Test
    fun `поднят только стенд разработки`() {
        assertTrue(environments.first { it.code == "DEV" }.environmentRaised())
        assertFalse(environments.first { it.code == "TEST" }.environmentRaised())
        assertFalse(environments.first { it.code == "PROD" }.environmentRaised())
    }

    /** Отказ узла сильнее приложения: названный им неподдерживаемый контур тоже гаснет. */
    @Test
    fun `контур, которого не принимает узел, остаётся погашенным`() {
        assertFalse(entry("DEV", supported = false).environmentRaised())
    }

    @Test
    fun `окно заведения подставляет БФД и первый поднятый контур`() {
        val draft = AdoptDraft(preferences())
        draft.preset(environments)
        assertEquals(BFD_PROVIDER, draft.target.provider)
        assertEquals("DEV", draft.target.environment)
    }

    /**
     * Рабочее место помнит контур, но не поставщика.
     *
     * Запомненный чужой оператор пережил бы скрытие выбора и ушёл бы
     * в узел из настроек, которых владелец больше не видит.
     */
    @Test
    fun `запомненное рабочим местом поставщика не меняет`() {
        val shared = preferences()
        val first = AdoptDraft(shared)
        first.target = first.target.copy(environment = "PROD")
        first.remember()

        val next = AdoptDraft(shared)
        next.preset(environments)
        assertEquals(BFD_PROVIDER, next.target.provider)
        assertEquals("PROD", next.target.environment, "контур рабочее место помнит")
    }
}
