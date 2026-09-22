package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsTab
import kz.mybrain.superkassa.desktop.ui.analytics.of
import kz.mybrain.superkassa.desktop.ui.analytics.recordCount
import kz.mybrain.superkassa.desktop.ui.analytics.recordKkms
import kz.mybrain.superkassa.desktop.ui.analytics.recordRegions
import kz.mybrain.superkassa.desktop.ui.analytics.refusedKkms
import kz.mybrain.superkassa.desktop.ui.analytics.regionOf
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.cabinet.recordTitle
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.sieveTexts
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Парк касс раскладывается по смыслам учёта и по областям.
 *
 * Считается это по боевым числам показа: из 3294 касс на учёте четыре,
 * и вкладка обязана сказать об этом ровно так. Ошибка здесь не видна
 * глазами — четыре и сорок четыре на экране выглядят одинаково
 * правдоподобно, — поэтому числа проверяются, а не осматриваются.
 */
class AnalyticsRecordTest {

    private val unknown = "Без адреса"

    /** Учёт стоит в разделе четвёртой вкладкой и назван своим набором. */
    @Test
    fun `учёт касс — вторая вкладка аналитики`() {
        val texts = analyticsTexts(Language.Ru)

        assertEquals(4, AnalyticsTab.entries.size)
        assertEquals(texts.record.tab, AnalyticsTab.Record.title(texts))
        // Учёт идёт сразу за картой: следующий вопрос к найденной кассе —
        // вправе ли она торговать.
        assertEquals(AnalyticsTab.Record, AnalyticsTab.entries[1])
    }

    @Test
    fun `сеть показа считается по смыслам учёта`() {
        val count = recordCount(RecordFleet.show())

        assertEquals(RecordFleet.WHOLE, count.total)
        assertEquals(RecordFleet.ON_RECORD, count.onRecord, "на учёте должно быть четыре кассы")
        assertEquals(RecordFleet.DEREGISTERED, count.deregistered)
        assertEquals(RecordFleet.WHOLE - RecordFleet.ON_RECORD - RecordFleet.DEREGISTERED, count.entered)
        assertEquals(0, count.applied, "ни одного заявления в КГД не подано")
        assertEquals(0, count.refused, "отказов в сети показа нет")
        assertEquals(RecordFleet.PLACES, count.places)
    }

    /** Сумма по четырём смыслам — это весь парк: касса не бывает вне их. */
    @Test
    fun `смыслы учёта складываются в парк целиком`() {
        val count = recordCount(RecordFleet.show())

        assertEquals(count.total, KkmRecord.entries.sumOf { count.of(it) })
    }

    @Test
    fun `отказы и блокировки видны в смешанном парке`() {
        val count = recordCount(RecordFleet.mixed())

        assertEquals(10, count.total)
        assertEquals(3, count.onRecord, "перерегистрация прошла — касса на учёте")
        assertEquals(2, count.refused)
        assertEquals(3, count.entered, "заведённые: заявление по ним не подавалось")
        assertEquals(1, count.applied, "заявление подано по одной кассе")
        assertEquals(1, count.deregistered)
        assertEquals(2, count.trading, "торгуют кассы на учёте с открытой сменой")
        assertEquals(2, count.blocked)
    }

    /**
     * У каждого смысла учёта своё слово на каждом языке.
     *
     * Из этих слов собраны и плашка отбора карты, и заголовки столбцов
     * вкладки. Забытая ветка разбора осталась бы незаметной: на экране
     * она выглядела бы как ещё одна касса «на учёте».
     */
    @Test
    fun `каждый смысл учёта назван на трёх языках`() {
        Language.entries.forEach { language ->
            val words = KkmRecord.entries.map { recordTitle(it, sieveTexts(language)) }

            assertEquals(words.size, words.toSet().size, "$language: два смысла названы одинаково")
            assertTrue(words.none(String::isBlank), "$language: смысл учёта остался без слова")
        }
    }

    /**
     * Смена, открытая у кассы вне учёта, торговлей не считается.
     *
     * Иначе экран обещал бы работающую сеть там, где КГД не учёл ни одной
     * кассы: смену касса открывает сама, разрешения на это ей не нужно.
     */
    @Test
    fun `торгующими считаются только кассы на учёте`() {
        val open = RecordFleet.mixed().count { it.shiftStatus == "OPEN" }
        val count = recordCount(RecordFleet.mixed())

        assertEquals(3, open, "в наборе три открытые смены")
        assertEquals(2, count.trading)
        assertTrue(count.trading <= count.onRecord, "торгующих больше, чем касс на учёте")
    }

    /** Отказы вынесены отдельным списком: по ним владелец действует. */
    @Test
    fun `отказы отбираются в свой список`() {
        val refused = refusedKkms(RecordFleet.mixed())

        assertEquals(listOf("c4", "c5"), refused.map { it.cashRegisterId }.sorted())
    }

    @Test
    fun `парк раскладывается по областям`() {
        val regions = recordRegions(RecordFleet.show(), unknown)

        assertEquals(RecordFleet.REGIONS.sorted(), regions.map { it.title }.sorted())
        assertEquals(RecordFleet.WHOLE, regions.sumOf { it.count.total })
        assertEquals(RecordFleet.ON_RECORD, regions.sumOf { it.count.onRecord })
        assertEquals(RecordFleet.PLACES, regions.sumOf { it.count.places }, "точка целиком лежит в своей области")
    }

    /** Крупная область идёт первой: с неё и начинают читать. */
    @Test
    fun `области идут по числу касс`() {
        val regions = recordRegions(RecordFleet.mixed() + RecordFleet.show(count = 20), unknown)

        assertEquals(regions.map { it.count.total }.sortedDescending(), regions.map { it.count.total })
    }

    /** Касса без адреса не выбрасывается: она такая же касса парка. */
    @Test
    fun `касса без адреса попадает в область без названия`() {
        val regions = recordRegions(RecordFleet.mixed(), unknown)
        val nameless = regions.single { it.title == unknown }

        assertEquals(1, nameless.count.total)
        assertEquals(0, nameless.count.places, "торговой точки у неё нет вовсе")
    }

    @Test
    fun `пустой парк не даёт ни областей, ни отказов`() {
        val count = recordCount(emptyList())

        assertTrue(count.empty, "пустой парк должен сообщать о себе сам")
        assertEquals(0, count.total)
        assertEquals(emptyList(), recordRegions(emptyList(), unknown))
        assertEquals(emptyList(), refusedKkms(emptyList()))
    }

    /** Кассы берутся из обеих половин ответа: непоставленная на карту — тоже касса парка. */
    @Test
    fun `в парк входят и кассы без места на карте`() {
        val kkms = recordKkms(RecordFleet.view(RecordFleet.mixed()))

        assertEquals(RecordFleet.mixed().size, kkms.size)
        assertEquals(1, kkms.count { it.address == null })
    }

    /** Область берётся из адреса тем же разбором, что и у торговой сводки. */
    @Test
    fun `область — первое звено адреса`() {
        assertEquals("Алматы", regionOf("Алматы, проспект Абая, 5", unknown))
        assertEquals("Актобе", regionOf("  Актобе , улица Тәуелсіздік", unknown))
        assertEquals(unknown, regionOf(null, unknown))
        assertEquals(unknown, regionOf("   ", unknown))
    }
}
