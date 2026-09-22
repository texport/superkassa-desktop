package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceOrder
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceRow
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceSieve
import kz.mybrain.superkassa.desktop.ui.cabinet.kkmRecord
import kz.mybrain.superkassa.desktop.ui.cabinet.placeRows
import kz.mybrain.superkassa.desktop.ui.cabinet.treeEmpty
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Отбор и порядок колонки торговых точек.
 *
 * У владельца показа две тысячи точек и пять тысяч касс, из которых
 * на учёте шесть, снятых с учёта две, остальные — черновики. Найти среди
 * них кассу с нужным состоянием поиском по названию нельзя: владелец
 * не знает ни названия такой кассы, ни точки, в которой она стоит.
 *
 * Считается это отдельно от разметки: от строк зависят обе раскладки
 * колонки, а проверять порядок двух тысяч точек по картинке нельзя.
 */
class CabinetPlaceSieveTest {

    private val places = listOf(
        // Названия и адреса нарочно вразнобой: по порядку списка кабинета
        // ни название, ни адрес, ни число касс не возрастают.
        place("p1", "Магазин на Абая", "Алматы, Абая, 10", registers = 3),
        place("p2", "Склад у вокзала", "Алматы, Жандосова, 4", registers = 1),
        place("p3", "Ларёк в парке", "Астана, Кенесары, 7", registers = 2)
    )

    private val registers = listOf(
        register("r1", "p1", "REGISTERED"),
        register("r2", "p1", "DRAFT"),
        register("r3", "p1", "REGISTRATION_IN_ISNA_ERROR"),
        register("r4", "p2", "DEREGISTERED"),
        register("r5", "p3", "REGISTRATION_IN_ISNA_PROCESS"),
        register("r6", "p3", "DRAFT")
    )

    private fun rows(sieve: PlaceSieve, open: String? = null, locked: Set<String> = emptySet()) =
        placeRows(places, registers, open, sieve, locked)

    private fun names(sieve: PlaceSieve, open: String? = null, locked: Set<String> = emptySet()) =
        rows(sieve, open, locked).filterIsInstance<PlaceRow.Point>().map { it.place.name }

    private fun kkms(sieve: PlaceSieve, open: String?, locked: Set<String> = emptySet()) =
        rows(sieve, open, locked).filterIsInstance<PlaceRow.Register>().map { it.register.id }

    /**
     * Каждый смысл учёта отбирается сам по себе.
     *
     * Все пять разом, а не один для примера: смыслы разводились именно
     * затем, чтобы черновик не считался поданным заявлением, и забытая
     * ветка разбора выглядела бы на экране как ещё одна касса.
     */
    @Test
    fun `отбор оставляет кассы одного спрошенного смысла`() {
        KkmRecord.entries.forEach { meaning ->
            val open = places.first { point ->
                registers.any { it.retailPlace?.id == point.id && kkmRecord(it.status) == meaning }
            }
            val left = kkms(PlaceSieve(record = meaning), open.id)
            assertTrue(left.isNotEmpty(), "$meaning: не нашлось ни одной кассы")
            left.forEach { id ->
                val status = registers.first { it.id == id }.status
                assertEquals(meaning, kkmRecord(status), "$meaning: в списке осталась касса $id")
            }
        }
    }

    /**
     * Точка без спрошенной кассы уходит, даже когда подходит названием.
     *
     * На вопрос «где у меня отказ КГД» точка, в которой отказов нет, —
     * не ответ: иначе владелец получил бы прежний список из двух тысяч
     * строк и решил бы, что отбор не работает.
     */
    @Test
    fun `точка без спрошенной кассы уходит из списка`() {
        assertEquals(listOf("Магазин на Абая"), names(PlaceSieve(record = KkmRecord.Refused)))
        assertEquals(listOf("Склад у вокзала"), names(PlaceSieve(record = KkmRecord.Deregistered)))
        assertTrue(
            names(PlaceSieve(needle = "склад", record = KkmRecord.Refused)).isEmpty(),
            "точка осталась в списке по названию, хотя отказов КГД в ней нет"
        )
    }

    @Test
    fun `отбор складывается с поиском`() {
        assertEquals(listOf("Ларёк в парке"), names(PlaceSieve(needle = "парк", record = KkmRecord.Entered)))
        assertEquals(listOf("Магазин на Абая"), names(PlaceSieve(needle = "абая", record = KkmRecord.Entered)))
        // Поиск по номеру КГД вместе с отбором: номер ведёт к точке,
        // а отбор оставляет под ней только спрошенное.
        assertEquals(listOf("r1"), kkms(PlaceSieve(needle = "000000000001", record = KkmRecord.OnRecord), "p1"))
        // Подошедшая своим именем точка показывает не все свои кассы,
        // а только те, о которых спросил отбор.
        assertEquals(listOf("r2"), kkms(PlaceSieve(needle = "абая", record = KkmRecord.Entered), "p1"))
    }

    @Test
    fun `отбор заблокированных оставляет только заблокированные кассы`() {
        val locked = setOf("r2")
        assertEquals(listOf("Магазин на Абая"), names(PlaceSieve(blocked = true), locked = locked))
        assertEquals(listOf("r2"), kkms(PlaceSieve(blocked = true), "p1", locked))
        // Блокировка складывается с учётом, а не исключает его: заблокирован
        // здесь черновик, и вместе с «на учёте» отбор пуст.
        assertTrue(
            names(PlaceSieve(record = KkmRecord.OnRecord, blocked = true), locked = locked).isEmpty(),
            "отбор оставил кассу, которая подошла только одному из двух условий"
        )
        assertTrue(
            rows(PlaceSieve(blocked = true), "p1").isEmpty(),
            "без сведений о блокировках отбор что-то оставил"
        )
    }

    @Test
    fun `порядок по названию идёт в обе стороны`() {
        assertEquals(
            listOf("Ларёк в парке", "Магазин на Абая", "Склад у вокзала"),
            names(PlaceSieve(order = PlaceOrder.Name))
        )
        assertEquals(
            listOf("Склад у вокзала", "Магазин на Абая", "Ларёк в парке"),
            names(PlaceSieve(order = PlaceOrder.Name, descending = true))
        )
    }

    @Test
    fun `порядок по адресу идёт в обе стороны`() {
        assertEquals(
            listOf("Магазин на Абая", "Склад у вокзала", "Ларёк в парке"),
            names(PlaceSieve(order = PlaceOrder.Address))
        )
        assertEquals(
            listOf("Ларёк в парке", "Склад у вокзала", "Магазин на Абая"),
            names(PlaceSieve(order = PlaceOrder.Address, descending = true))
        )
    }

    @Test
    fun `порядок по числу касс идёт в обе стороны`() {
        assertEquals(
            listOf("Склад у вокзала", "Ларёк в парке", "Магазин на Абая"),
            names(PlaceSieve(order = PlaceOrder.Registers))
        )
        assertEquals(
            listOf("Магазин на Абая", "Ларёк в парке", "Склад у вокзала"),
            names(PlaceSieve(order = PlaceOrder.Registers, descending = true))
        )
    }

    /**
     * Порядок по состоянию ставит первым то, во что надо вмешаться.
     *
     * Отказ КГД — работа на сегодня, поданное заявление — ожидание,
     * снятая с учёта касса не спрашивает ни о чём. Обратная сторона
     * нужна не для симметрии: так владелец видит, что у него уже сделано.
     */
    @Test
    fun `порядок по состоянию ведёт от беды к покою`() {
        assertEquals(
            listOf("Магазин на Абая", "Ларёк в парке", "Склад у вокзала"),
            names(PlaceSieve(order = PlaceOrder.Record))
        )
        assertEquals(
            listOf("Склад у вокзала", "Ларёк в парке", "Магазин на Абая"),
            names(PlaceSieve(order = PlaceOrder.Record, descending = true))
        )
    }

    /** Порядок ничего не убирает: сколько точек было, столько и осталось. */
    @Test
    fun `порядок не сужает список`() {
        PlaceOrder.entries.forEach { order ->
            assertEquals(places.size, names(PlaceSieve(order = order)).size, "$order: список стал короче")
        }
    }

    /**
     * Спрошенное отбором видно из самого отбора.
     *
     * По этому колонка называет пустой результат отбором, а не пустым
     * хозяйством владельца: три разных случая пустоты и одни слова на всех
     * в кабинете уже стоили владельцу «заведите первую точку» при тысяче
     * заведённых.
     */
    @Test
    fun `отбор знает, спрошено ли что-нибудь`() {
        assertTrue(!PlaceSieve().set, "пустой отбор считается заданным")
        assertTrue(!PlaceSieve(order = PlaceOrder.Record, descending = true).set, "порядок принят за отбор")
        assertTrue(PlaceSieve(needle = "абая").set && !PlaceSieve(needle = "абая").marked)
        assertTrue(PlaceSieve(record = KkmRecord.Refused).marked, "отбор по учёту не спрашивает о кассе")
        assertTrue(PlaceSieve(blocked = true).marked, "отбор по блокировке не спрашивает о кассе")
    }

    /**
     * Пусто по-разному, и колонка называет это разными словами.
     *
     * Три случая: отбор ничего не оставил, поиск ничего не нашёл, точек
     * нет вовсе. Четвёртый — непрочитанный список — сюда не доходит,
     * его называет отказом сама колонка. Сравниваются слова, а не
     * картинки: на картинке эти случаи отличались бы и нажатой плашкой,
     * и счётом показанного, а проверять надо сказанное владельцу.
     */
    @Test
    fun `пустой отбор, ненайденное и пустое хозяйство названы по-разному`() {
        val texts = cabinetTexts(Language.Ru)
        val sieved = treeEmpty(texts, PlaceSieve(record = KkmRecord.Refused))
        val searched = treeEmpty(texts, PlaceSieve(needle = "аптека"))
        val nothing = treeEmpty(texts, PlaceSieve())

        assertEquals(texts.sieve.empty, sieved.title, "пустой отбор назван не отбором")
        assertEquals(texts.placeNotFound, searched.title, "ненайденное поиском названо не поиском")
        assertEquals(texts.placesEmpty, nothing.title, "пустое хозяйство названо не пустым хозяйством")
        val titles = listOf(sieved, searched, nothing).map { it.title }
        assertEquals(titles.size, titles.toSet().size, "два разных случая пустоты названы одинаково")
    }

    private fun place(id: String, name: String, address: String, registers: Long) = RetailPlace(
        id = id,
        name = name,
        address = address,
        addressKz = address,
        cashRegisterCount = registers
    )

    private fun register(id: String, place: String, status: String) = CabinetRegister(
        id = id,
        kkmId = id.hashCode(),
        internalName = "Касса $id",
        status = status,
        registrationNumber = "%012d".format(id.drop(1).toLong()),
        retailPlaceId = place
    )
}
