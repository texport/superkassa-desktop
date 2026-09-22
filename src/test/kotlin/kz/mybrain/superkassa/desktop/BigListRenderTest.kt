package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.OrgInfo
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.cabinet.ActionKind
import kz.mybrain.superkassa.desktop.ui.cabinet.ApplicationFields
import kz.mybrain.superkassa.desktop.ui.cabinet.DeregistrationReason
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceTree
import kz.mybrain.superkassa.desktop.ui.cabinet.placeRows
import kz.mybrain.superkassa.desktop.ui.login.KkmList
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kotlin.test.Test
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

    private val texts = cabinetTexts(Language.Ru)

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
                retailPlaceId = place.id
            )
        }
    }

    private fun kkms(count: Int) = (1..count).map {
        Kkm(
            kkmId = "kkm-$it",
            kkmKgdId = "%012d".format(it),
            factoryNumber = "SK-$it",
            ofdServiceInfo = OrgInfo(orgTitle = "ТОО «Пример»", orgAddress = "Алматы, Абая $it")
        )
    }

    @Composable
    private fun Tree(
        count: Int,
        collapsed: Boolean = false,
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
            collapsed = collapsed,
            onToggle = {},
            rows = placeRows(all, registers(all), open = open, query = query),
            total = all.size,
            loading = false,
            query = query,
            onQuery = {},
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

    @Composable
    private fun Logins(count: Int) {
        val all = kkms(count)
        KkmList(kkms = all, nameOf = { it.title }, chosenId = all.first().kkmId, rememberedId = null) {}
    }

    @Test
    fun `дерево из двух тысяч точек рисуется быстро и не зависит от длины списка`() {
        renderMillis { Tree(SMALL) }
        val small = renderMillis { Tree(SMALL) }
        val large = renderMillis { Tree(LARGE) }
        println("дерево: $SMALL точек — $small мс, $LARGE точек — $large мс")
        assertTrue(large < BUDGET, "$LARGE точек рисуются $large мс")
        assertTrue(large < small * FACTOR + SLACK, "рост отрисовки с длиной списка: $small → $large мс")
    }

    @Test
    fun `свёрнутая колонка держит те же две тысячи точек`() {
        val collapsed = renderMillis { Tree(LARGE, collapsed = true) }
        println("свёрнутая колонка: $LARGE точек — $collapsed мс")
        assertTrue(collapsed < BUDGET, "свёрнутая колонка рисуется $collapsed мс")
    }

    @Test
    fun `свёрнутая колонка не пустая и показывает выбранное`() {
        RenderProbe { Tree(0, collapsed = true) }.use { empty ->
            RenderProbe { Tree(LARGE, collapsed = true) }.use { filled ->
                RenderProbe { Tree(LARGE, collapsed = true, chosen = "p3") }.use { other ->
                    val nothing = empty.frame()
                    assertTrue(!filled.frame().contentEquals(nothing), "в свёрнутой колонке не видно точек")
                    assertTrue(!other.frame().contentEquals(filled.frame()), "выбранная точка не выделена")
                }
            }
        }
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
     */
    @Test
    fun `раскрытие списка точек не зависит от их числа`() {
        openMillis(SMALL)
        val small = openMillis(SMALL)
        val large = openMillis(LARGE)
        println("раскрытие: $SMALL точек — $small мс, $LARGE точек — $large мс")
        assertTrue(large < small * FACTOR + SLACK, "рост раскрытия с длиной набора: $small → $large мс")
    }

    /** Сколько миллисекунд занимает раскрыть список точек нажатием по полю. */
    private fun openMillis(count: Int): Long = RenderProbe { PlacesPicker(count) }.use { probe ->
        repeat(SETTLE) { probe.frame() }
        val started = System.nanoTime()
        probe.click(Offset(FIELD_X, FIELD_Y))
        (System.nanoTime() - started) / 1_000_000
    }

    @Test
    fun `список касс на входе держит две тысячи строк`() {
        renderMillis { Logins(SMALL) }
        val small = renderMillis { Logins(SMALL) }
        val large = renderMillis { Logins(LARGE) }
        println("вход: $SMALL касс — $small мс, $LARGE касс — $large мс")
        assertTrue(large < BUDGET, "$LARGE касс рисуются $large мс")
        assertTrue(large < small * FACTOR + SLACK, "рост отрисовки с длиной списка: $small → $large мс")
    }

    @Test
    fun `список касс на входе прокручивается`() {
        RenderProbe { Logins(LARGE) }.use { probe ->
            val before = probe.frame()
            probe.wheel(at = Offset(400f, 400f), ticks = 6f)
            assertTrue(probe.changedFrom(before), "картинка списка не изменилась после прокрутки")
        }
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
         * Во сколько раз длинный список вправе оказаться дороже короткого.
         *
         * Порог выбран замером: тот же состав строк в собранном целиком
         * столбце стоил 396 мс против 45 мс списком, а короткий — 46 мс.
         * Двух с запасом хватает, чтобы отличить одно от другого и не
         * ловить дрожание машины.
         */
        const val FACTOR = 2

        /** Запас на разогрев машины, не зависящий от длины списка. */
        const val SLACK = 150L

        /** Где на сцене стоит первое поле формы: по нему и нажимают. */
        const val FIELD_X = 300f
        const val FIELD_Y = 40f

        /** Сколько кадров даётся сцене, чтобы встать до нажатия. */
        const val SETTLE = 20
    }
}
