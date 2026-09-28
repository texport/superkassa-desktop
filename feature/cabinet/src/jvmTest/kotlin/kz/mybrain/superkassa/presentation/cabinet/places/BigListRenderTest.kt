package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceRef
import kz.mybrain.superkassa.presentation.cabinet.applications.ActionKind
import kz.mybrain.superkassa.presentation.cabinet.applications.ApplicationFields
import kz.mybrain.superkassa.presentation.cabinet.applications.DeregistrationReason
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceTree
import kz.mybrain.superkassa.presentation.cabinet.places.component.placeRows
import kz.mybrain.superkassa.renderMillis
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Две тысячи торговых точек и две тысячи касс на экране.
 *
 * Столько их будет у сети, ради которой кабинет и делается. Проверять это
 * на кабинете владельца нельзя — там его боевые данные, поэтому списки
 * собираются здесь и рисуются сценой без окна. Мерится не красота
 * картинки, а два свойства: экран собирается за доли секунды и время его
 * сборки почти не зависит от длины списка. Столбец, собранный целиком,
 * провалил бы второе — на тысячах строк он растёт пропорционально
 * их числу.
 */
class BigListRenderTest {

    private val texts = textsOf(Language.Ru).cabinet

    private fun places(count: Int) = (1..count).map {
        RetailPlace(
            id = "p$it",
            name = "Торговая точка $it",
            address = "Алматы, проспект Абая $it",
            cashRegisterCount = PER_PLACE.toLong()
        )
    }

    private fun registers(places: List<RetailPlace>) = places.flatMap { place ->
        (1..PER_PLACE).map { at ->
            CabinetRegister(
                id = "${place.id}-$at",
                kkmId = at,
                internalName = "Касса $at, ${place.name}",
                status = "REGISTERED",
                registrationNumber = "%012d".format(place.id.drop(1).toInt() * PER_PLACE + at),
                retailPlace = RetailPlaceRef(place.id)
            )
        }
    }

    @Composable
    private fun Tree(
        count: Int,
        query: String = "",
        // Раскрытая точка и выделенная — разные вещи: так проверка
        // выделения не путается с появлением касс под точкой.
        open: String? = "p1",
        chosen: String? = open
    ) {
        val all = places(count)
        PlaceTree(
            texts = texts,
            language = Language.Ru,
            onCollapse = {},
            rows = placeRows(all, registers(all), open = open, sieve = PlaceSieve(needle = query)),
            total = all.size,
            loading = false,
            trouble = null,
            onRetry = {},
            sieve = PlaceSieve(needle = query),
            onSieve = {},
            locksKnown = true,
            place = chosen,
            register = null,
            onPlace = {},
            onRegister = {},
            footer = {}
        )
    }

    /** Поле выбора точки в заявлении о перерегистрации — как его видит владелец. */
    @Composable
    private fun PlacesPicker(count: Int) {
        Column(modifier = Modifier.fillMaxSize()) {
            ApplicationFields(
                kind = ActionKind.Reregistration,
                texts = texts,
                language = Language.Ru,
                places = places(count),
                placeId = "",
                reason = DeregistrationReason.CessationOfUse,
                comment = "",
                onPlace = {},
                onReason = {},
                onComment = {}
            )
        }
    }

    /**
     * Дерево на две тысячи точек рисуется в пределах бюджета и собирает
     * строки только видимой части.
     *
     * Рост с длиной списка считается собранными строками, а не сравнением
     * миллисекунд: на общей машине проверки время скачет. Бюджет целиком
     * остаётся временем — он щедрый и ловит только настоящую беду.
     */
    @Test
    fun `дерево из двух тысяч точек рисуется быстро и не зависит от длины списка`() {
        renderMillis { Tree(SMALL) }
        val large = renderMillis { Tree(LARGE) }
        val rows = RenderProbe { Tree(LARGE) }.use { probe ->
            probe.frame()
            probe.nodes().count { it.text.startsWith(PLACE_NAME) }
        }
        println("дерево: $LARGE точек — $large мс, собрано строк $rows")
        assertTrue(large < BUDGET, "$LARGE точек рисуются $large мс")
        assertTrue(rows <= SHOWN, "дерево собрало $rows строк из $LARGE — список собирается целиком")
    }

    @Test
    fun `дерево прокручивается колесом мыши`() {
        RenderProbe { Tree(LARGE) }.use { probe ->
            val before = probe.frame()
            probe.wheel(at = Offset(200f, 400f), ticks = 6f)
            assertTrue(probe.changedFrom(before), "картинка списка не изменилась после прокрутки")
        }
    }

    @Test
    fun `поиск оставляет на экране найденное`() {
        RenderProbe { Tree(LARGE) }.use { whole ->
            RenderProbe { Tree(LARGE, query = "Торговая точка 1999") }.use { found ->
                assertTrue(!found.frame().contentEquals(whole.frame()), "поиск не сузил список")
            }
        }
    }

    /**
     * Точку в заявлении выбирают набором, а не перебором.
     *
     * Здесь стоял простой выпадающий список, раскрытый целиком: две тысячи
     * строк владелец крутил бы колесом, а найти среди них «Торговую точку
     * 1999» глазами нельзя. Проверяется то, ради чего поле поменяли:
     * список раскрывается и набранное его сужает.
     */
    @Test
    fun `выбор точки в заявлении ищет среди двух тысяч`() {
        RenderProbe { PlacesPicker(LARGE) }.use { probe ->
            val closed = probe.frame()
            probe.click(Offset(FIELD_X, FIELD_Y))
            assertTrue(probe.changedFrom(closed), "список точек не раскрылся")
            val opened = probe.frame()
            probe.type("точка 1999")
            assertTrue(probe.changedFrom(opened), "набранное не сузило список точек")
        }
    }

    /**
     * Раскрытие списка точек стоит одинаково при пяти и при двух тысячах.
     *
     * Меню собирается целиком — ленивого списка в нём быть не может, —
     * и весь набор в нём стоил полсекунды на каждое нажатие по полю.
     * Сразу показана первая полусотня совпадений, остальное просят
     * последней строкой.
     *
     * Стоимость считается строками раскрытого меню, а не секундомером:
     * на общей машине проверки время скачет, и сравнение миллисекунд
     * падало там, где меню было в порядке.
     */
    @Test
    fun `раскрытие списка точек не зависит от их числа`() {
        val small = openedRows(SMALL)
        val large = openedRows(LARGE)
        println("раскрытие показало строк: из $SMALL — $small, из $LARGE — $large")
        assertEquals(SMALL, small, "короткий набор раскрылся не целиком")
        assertTrue(large <= SHOWN, "раскрытие растёт с длиной набора: $LARGE точек — $large строк")
    }

    /** Сколько строк точек показало меню, раскрытое нажатием по полю. */
    private fun openedRows(count: Int): Int = RenderProbe { PlacesPicker(count) }.use { probe ->
        repeat(SETTLE) { probe.frame() }
        probe.click(Offset(FIELD_X, FIELD_Y))
        probe.frame()
        probe.nodes().count { it.text.startsWith(PLACE_NAME) }
    }

    private companion object {
        const val SMALL = 5

        /** Сколько точек и касс будет у сети, ради которой кабинет и делается. */
        const val LARGE = 2000

        /** По одной кассе на точку: тогда касс на экране столько же, сколько точек. */
        const val PER_PLACE = 1

        /** Сколько миллисекунд отводится на сборку и отрисовку экрана целиком. */
        const val BUDGET = 1500L

        /**
         * Сколько строк точек вправе собрать дерево или меню из двух тысяч:
         * видимая часть и первая полусотня совпадений меню помещаются в этот
         * предел, а собранный целиком список дал бы все две тысячи.
         */
        const val SHOWN = 50

        /** Начало названия каждой точки набора: по нему строки меню и считаются. */
        const val PLACE_NAME = "Торговая точка"

        /** Где на сцене стоит первое поле формы: по нему и нажимают. */
        const val FIELD_X = 300f
        const val FIELD_Y = 40f

        /** Сколько кадров даётся сцене, чтобы встать до нажатия. */
        const val SETTLE = 20
    }
}
