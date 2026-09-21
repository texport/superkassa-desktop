package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kotlin.test.Test

/**
 * Снимки раздела торговых точек.
 *
 * Отказные состояния здесь не про связь, а про хозяйство владельца:
 * точки без адреса, без координат, с десятком касс под собой и с кассами,
 * снятыми с учёта. У каждого на экране должно быть объяснение, а не
 * пустая колонка.
 */
class PlaceShots {

    /** Точек нет вовсе: в колонке приглашение завести первую. */
    @Test
    fun `точек нет вовсе`() = look("place-none", emptyList(), emptyList())

    /** Ответа кабинета ещё не было: на месте строк ожидание, а не пустота. */
    @Test
    fun `ответа кабинета ещё не было`() = look("place-loading", emptyList(), emptyList(), loading = true)

    /** Одна точка: её карточка справа со всем, что о ней записано. */
    @Test
    fun `одна точка`() {
        val place = PlaceLook.place(1)
        look("place-one", listOf(place), listOf(PlaceLook.register(1, place.id)), open = place.id)
    }

    /** Десяток точек с кассами под раскрытой: дерево со отступом. */
    @Test
    fun `десяток точек с кассами`() {
        val places = (1..TEN).map { PlaceLook.place(it, registers = PER_PLACE.toLong()) }
        val registers = places.flatMap { place ->
            (1..PER_PLACE).map { PlaceLook.register(it + place.id.drop(1).toInt() * PER_PLACE, place.id) }
        }
        look("place-tree", places, registers, open = "p3")
    }

    /** Точка без адреса: строка адреса обязана сказать, что его нет. */
    @Test
    fun `точка без адреса`() {
        val place = PlaceLook.place(1, address = null, registers = 0)
        look("place-no-address", listOf(place), emptyList(), open = place.id)
    }

    /** Точка без координат: карта не знает, где она стоит. */
    @Test
    fun `точка без координат`() {
        val place = PlaceLook.place(1, point = false, registers = 0)
        look("place-no-point", listOf(place), emptyList(), open = place.id)
    }

    /** Кассы на учёте и снятые с него под одной точкой. */
    @Test
    fun `кассы на учёте и снятые`() {
        val place = PlaceLook.place(1, registers = 4)
        val registers = listOf(
            PlaceLook.register(1, place.id),
            PlaceLook.register(2, place.id, onRecord = false),
            PlaceLook.register(3, place.id),
            PlaceLook.register(4, place.id, onRecord = false)
        )
        look("place-on-and-off-record", listOf(place), registers, open = place.id)
    }

    /** Удаление точки с кассами: должно быть объяснено, а не молча отказано. */
    @Test
    fun `удаление точки с кассами`() {
        val withKkm = PlaceLook.place(1, registers = 3)
        look("place-remove-blocked", listOf(withKkm), listOf(PlaceLook.register(1, withKkm.id)), open = withKkm.id)
        val empty = PlaceLook.place(2, registers = 0)
        look("place-remove-allowed", listOf(empty), emptyList(), open = empty.id)
    }

    /** Поиск ничего не нашёл: у владельца с сотней точек это не «точек нет». */
    @Test
    fun `поиск ничего не нашёл`() {
        val places = (1..TEN).map { PlaceLook.place(it) }
        look("place-not-found", places, emptyList(), query = "ничего такого нет")
    }

    /** Свёрнутая колонка: рельс значков вместо пустоты. */
    @Test
    fun `колонка свёрнута`() {
        val places = (1..TEN).map { PlaceLook.place(it) }
        look("place-rail", places, emptyList(), open = "p3", collapsed = true)
    }

    /** Узкое окно: карточка точки не должна обрезаться. */
    @Test
    fun `узкое окно`() {
        val place = PlaceLook.place(1)
        look("place-narrow", listOf(place), emptyList(), open = place.id, width = NARROW, height = SHORT)
    }

    @Suppress("LongParameterList")
    private fun look(
        name: String,
        places: List<RetailPlace>,
        registers: List<CabinetRegister>,
        open: String? = null,
        query: String = "",
        loading: Boolean = false,
        collapsed: Boolean = false,
        width: Int = WIDE,
        height: Int = HIGH
    ) {
        RenderProbe(width, height) { PlacesLook(places, registers, open, query, loading, collapsed) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot(name, probe.frame())
            }
    }

    private companion object {
        const val TEN = 10
        const val PER_PLACE = 3
        const val SETTLE = 24
        const val WIDE = 1180
        const val HIGH = 820
        const val NARROW = 820
        const val SHORT = 560
    }
}
