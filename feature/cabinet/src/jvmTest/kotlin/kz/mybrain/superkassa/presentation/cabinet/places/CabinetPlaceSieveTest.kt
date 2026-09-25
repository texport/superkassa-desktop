package kz.mybrain.superkassa.presentation.cabinet.places

import kz.mybrain.superkassa.domain.cabinet.model.KkmRecord
import kz.mybrain.superkassa.domain.cabinet.model.kkmRecord
import kz.mybrain.superkassa.presentation.cabinet.places.SieveScene.allPlaces
import kz.mybrain.superkassa.presentation.cabinet.places.SieveScene.allRegisters
import kz.mybrain.superkassa.presentation.cabinet.places.SieveScene.kkms
import kz.mybrain.superkassa.presentation.cabinet.places.SieveScene.names
import kz.mybrain.superkassa.presentation.cabinet.places.SieveScene.rows
import kz.mybrain.superkassa.presentation.cabinet.places.component.treeEmpty
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Отбор колонки торговых точек; порядок проверяет [CabinetPlaceOrderTest].
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
            val open = allPlaces.first { point ->
                allRegisters.any { it.retailPlace?.id == point.id && kkmRecord(it.status) == meaning }
            }
            val left = kkms(PlaceSieve(record = meaning), open.id)
            assertTrue(left.isNotEmpty(), "$meaning: не нашлось ни одной кассы")
            left.forEach { id ->
                val status = allRegisters.first { it.id == id }.status
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
        val texts = textsOf(Language.Ru).cabinet
        val sieved = treeEmpty(texts, PlaceSieve(record = KkmRecord.Refused))
        val searched = treeEmpty(texts, PlaceSieve(needle = "аптека"))
        val nothing = treeEmpty(texts, PlaceSieve())

        assertEquals(texts.places.sieve.empty, sieved.title, "пустой отбор назван не отбором")
        assertEquals(texts.places.notFound, searched.title, "ненайденное поиском названо не поиском")
        assertEquals(texts.places.empty, nothing.title, "пустое хозяйство названо не пустым хозяйством")
        val titles = listOf(sieved, searched, nothing).map { it.title }
        assertEquals(titles.size, titles.toSet().size, "два разных случая пустоты названы одинаково")
    }
}
