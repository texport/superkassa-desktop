package kz.mybrain.superkassa.presentation.analytics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kz.mybrain.superkassa.presentation.analytics.map.MAP_TAG
import kz.mybrain.superkassa.presentation.strings.analytics.AnalyticsTexts
import kz.mybrain.superkassa.presentation.strings.analytics.analyticsTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.theme.size.Sizes

/**
 * Проход по четырём вкладкам аналитики с замерами и кадрами.
 *
 * Меряется то, на что жаловался владелец: высота карты до и после выбора
 * кассы, видна ли «Аналитика кассы», лежат ли друг на друге кнопки
 * и легенда, не ушло ли «Обновить» за край.
 */
internal class AnalyticsWalk(private val window: AnalyticsWindow, private val tag: String) {

    private val texts: AnalyticsTexts = analyticsTexts(window.language)
    private val map = cabinetTexts(window.language).map

    /** Что намерено; печатается строкой и проверяется тестом. */
    data class Measure(
        val mapHeight: Float,
        val mapHeightChosen: Float,
        val kkmSales: Rect?,
        val kkmSalesVisible: Boolean,
        val controlsOverLegend: Boolean,
        val refreshInside: Boolean,
        val mapWidth: Float,
        val marksOverlapping: Int
    )

    fun mapPart(): Measure {
        window.settle(STARTUP)
        window.click(analyticsTexts(window.language).title)
        window.settle(STARTUP)
        window.shot("$tag-map")
        val before = mapBox()
        val overlapping = overlappingMarks(before)
        val refresh = window.find(texts.refresh)
        chooseFirstKkm()
        window.shot("$tag-map-chosen")
        val after = mapBox()
        val sales = window.find(texts.openKkmSales)
        return Measure(
            mapHeight = before?.height ?: 0f,
            mapHeightChosen = after?.height ?: 0f,
            kkmSales = sales?.whole,
            kkmSalesVisible = sales != null && visible(sales.shown, sales.whole),
            controlsOverLegend = controlsOverLegend(),
            refreshInside = refresh != null && refresh.shown.right <= window.width && refresh.shown.width > 0,
            mapWidth = after?.width ?: 0f,
            marksOverlapping = overlapping
        )
    }

    /**
     * «Аналитика кассы» за нижним краем: докручивается ли она колесом.
     *
     * Колесо — у нижнего края под заголовком списка, то есть над карточкой:
     * сперва едет раздел, пока карточка не покажется, потом сама карточка.
     */
    fun kkmSalesReachable(): Boolean {
        repeat(SCROLLS) {
            val sales = window.find(texts.openKkmSales) ?: return false
            if (visible(sales.shown, sales.whole)) return true
            val list = window.all().firstOrNull { it.words.startsWith("${texts.kkmCount} · ") }?.shown ?: return false
            window.probe.wheel(Offset(list.center.x, window.height - BOTTOM), WHEEL)
        }
        return window.find(texts.openKkmSales)?.let { visible(it.shown, it.whole) } == true
    }

    /**
     * Окно аналитики кассы целиком в окне приложения: его «Закрыть» видно.
     *
     * Окно открывается с карточки и закрывается той же кнопкой.
     */
    fun kkmDialogFits(): Boolean {
        window.click(texts.openKkmSales)
        window.settle(STARTUP)
        window.shot("$tag-kkm-dialog")
        val close = window.all().lastOrNull { it.words == cabinetTexts(window.language).close }
        val fits = close != null && visible(close.shown, close.whole)
        close?.let { window.probe.click(it.shown.center) }
        window.settle()
        return fits
    }

    fun otherTabs() {
        window.click(texts.record.tab)
        window.settle(STARTUP)
        window.shot("$tag-record")
        window.probe.wheel(Offset(window.width * PAGE_X, window.height * PAGE_Y), TO_THE_END)
        window.shot("$tag-record-regions")
        window.click(texts.exchangeTab)
        window.settle(STARTUP)
        window.shot("$tag-exchange")
        window.click(texts.sales.tab)
        window.settle(STARTUP)
        window.shot("$tag-sales")
        repeat(SALES_PAGES) { scrollShot("$tag-sales-${it + 1}") }
    }

    private fun scrollShot(name: String) {
        window.probe.wheel(Offset(window.width * PAGE_X, window.height * PAGE_Y), PAGE_WHEEL)
        window.shot(name)
    }

    private fun visible(shown: Rect, whole: Rect) =
        whole.height > 0 && shown.height >= whole.height - 1 && shown.bottom <= window.height &&
            shown.right <= window.width

    /** Окно карты по его метке; `null` — карты на экране нет. */
    private fun mapBox(): Rect? = window.find(MAP_TAG)?.whole

    private fun controlsOverLegend(): Boolean {
        val legend = window.find(texts.mapLegend)?.whole ?: return false
        val controls = listOfNotNull(
            window.find(texts.mapFullscreen),
            window.find(map.zoomIn),
            window.find(map.zoomOut),
            window.find(map.myLocation)
        )
        return controls.any { it.whole.overlaps(legend) }
    }

    /**
     * Сколько пар кружков на карте заходят один на другой.
     *
     * Кружок — нажимаемый узел внутри окна карты не шире самого крупного
     * ярлычка, внутри которого нет подписанного значка: у кнопок карты
     * и легенды подпись есть. Кружки круглые, и наезд считается
     * по расстоянию центров.
     */
    private fun overlappingMarks(map: Rect?): Int {
        if (map == null) return 0
        val all = window.all()
        val signed = all.filter { it.words.isNotBlank() && !it.words.all(Char::isDigit) }.map { it.whole.center }
        val marks = all.filter {
            it.clickable && map.contains(it.whole.center) && signed.none(it.whole::contains) &&
                it.whole.width <= Sizes.mapMarkCrowd.value + 1 && it.whole.width >= Sizes.mapMark.value - 1
        }.map { it.whole }
        return marks.indices.sumOf { i ->
            (i + 1 until marks.size).count { j ->
                (marks[i].center - marks[j].center).getDistance() < (marks[i].width + marks[j].width) / 2 - 1
            }
        }
    }

    /** Первая строка списка касс рядом с картой — так владелец выбирает кассу. */
    private fun chooseFirstKkm() {
        val all = window.all()
        val title = all.firstOrNull { it.words.startsWith("${texts.kkmCount} · ") } ?: return
        val row = all.firstOrNull {
            it.clickable && it.shown.top > title.whole.bottom && it.shown.left >= title.whole.left - 1 &&
                it.shown.height > 0
        } ?: return
        window.probe.click(row.shown.center)
        window.settle()
    }

    private companion object {
        const val STARTUP = 80
        const val SCROLLS = 12

        /** Над самым низом окна: поле раздела и край карточки. */
        const val BOTTOM = 40f
        const val WHEEL = 10f
        const val PAGE_WHEEL = 50f

        /** Колесо, которым список доезжает до конца. */
        const val TO_THE_END = 100_000f

        /** Куда крутить колесо страницы: правее рельса и ниже рядов отбора. */
        const val PAGE_X = 0.6f
        const val PAGE_Y = 0.8f
        const val SALES_PAGES = 7
    }
}
