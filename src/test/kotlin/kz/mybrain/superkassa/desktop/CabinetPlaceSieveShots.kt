package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceOrder
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceSieve
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Колонка точек с отбором так, как её видит владелец.
 *
 * Окно взято рабочее — тысяча на семьсот: в нём колонка узкая, и ряд
 * отбора обязан в неё встать, не съев высоту самого списка. На снимке
 * видно и то, чего по строкам не проверить: перенёсся ли ряд плашек
 * на третью строку и осталось ли место под точки.
 */
class CabinetPlaceSieveShots {

    private val places = (1..TEN).map { PlaceLook.place(it, registers = it.toLong()) }

    private val registers = places.flatMapIndexed { at, place ->
        listOf(
            register("${place.id}-a", place.id, STATUSES[at % STATUSES.size]),
            register("${place.id}-b", place.id, "DRAFT")
        )
    }

    /** Отбор не задан: колонка такая, какой владелец её открывает. */
    @Test
    fun `колонка без отбора`() = look("cabinet-sieve-plain", PlaceSieve())

    /** Отбор по учёту: выбранный смысл стоит на плашке, а не угадывается. */
    @Test
    fun `отбор по учёту КГД`() = look("cabinet-sieve-record", PlaceSieve(record = KkmRecord.OnRecord))

    /** Отбор по учёту вместе с поиском и обратным порядком — всё сразу. */
    @Test
    fun `отбор, поиск и порядок вместе`() = look(
        "cabinet-sieve-all",
        PlaceSieve(needle = "Абая", record = KkmRecord.Entered, order = PlaceOrder.Registers, descending = true)
    )

    /** Заблокированные: плашка нажата, и под ней осталась одна касса. */
    @Test
    fun `отбор заблокированных`() = look(
        name = "cabinet-sieve-blocked",
        sieve = PlaceSieve(blocked = true),
        locked = setOf("p3-b")
    )

    /** Кабинет о блокировках не ответил: плашка погашена, а не обманывает. */
    @Test
    fun `блокировки не прочитаны`() = look("cabinet-sieve-locks-unread", PlaceSieve(), locksKnown = false)

    /**
     * Пусто по-разному, и владелец обязан различать эти случаи.
     *
     * Отбор, под который ничего не подошло, пустое хозяйство и
     * непрочитанный список — три разных ответа о хозяйстве владельца.
     * В кабинете это уже болело: колонка на отказ и на пустой ответ
     * говорила одно и то же — «заведите первую точку».
     */
    @Test
    fun `отбор без ответа не выдаётся за пустое хозяйство и за отказ`() {
        // Заблокированных у владельца показа нет ни одной: спросив
        // о них, он обязан прочитать про отбор, а не про пустое хозяйство.
        val sieved = frame("cabinet-sieve-nothing", PlaceSieve(blocked = true))
        val empty = frame("cabinet-sieve-no-places", PlaceSieve(), places = emptyList())
        val unread = frame("cabinet-sieve-unread", PlaceSieve(), places = emptyList(), trouble = UNREACHABLE)
        val found = frame("cabinet-sieve-nothing-searched", PlaceSieve(needle = "нет такой точки"))

        assertTrue(!sieved.contentEquals(empty), "пустой отбор показан как пустое хозяйство владельца")
        assertTrue(!sieved.contentEquals(unread), "пустой отбор показан как непрочитанный список")
        assertTrue(!empty.contentEquals(unread), "непрочитанный список показан как пустое хозяйство")
        assertTrue(!sieved.contentEquals(found), "ненайденное поиском и пустой отбор названы одинаково")
    }

    private fun look(
        name: String,
        sieve: PlaceSieve,
        locked: Set<String> = emptySet(),
        locksKnown: Boolean = true
    ) {
        frame(name, sieve, locked = locked, locksKnown = locksKnown)
    }

    /** Снимок колонки в рабочем окне: кадров даётся столько, чтобы список встал. */
    private fun frame(
        name: String,
        sieve: PlaceSieve,
        places: List<RetailPlace> = this.places,
        locked: Set<String> = emptySet(),
        locksKnown: Boolean = true,
        trouble: String? = null
    ): ByteArray = RenderProbe(WINDOW_WIDE, WINDOW_HIGH) {
        PlacesLook(
            places = places,
            registers = registers,
            open = places.firstOrNull()?.id,
            sieve = sieve,
            locked = locked,
            locksKnown = locksKnown,
            trouble = trouble
        )
    }.use { probe ->
        repeat(SETTLE) { probe.frame() }
        val shot = probe.frame()
        Look.shot(name, shot)
        shot
    }

    private fun register(id: String, place: String, status: String) = CabinetRegister(
        id = id,
        kkmId = id.hashCode(),
        internalName = "Касса $id",
        status = status,
        registrationNumber = "%012d".format(id.hashCode().toLong().and(NUMBER_MASK)),
        retailPlaceId = place
    )

    private companion object {
        const val TEN = 10

        /** Рабочее окно владельца: в нём колонка точек всего четыреста точек шириной. */
        const val WINDOW_WIDE = 1000
        const val WINDOW_HIGH = 700
        const val SETTLE = 24
        const val NUMBER_MASK = 0xFFFFFFL
        const val UNREACHABLE = "Кабинет временно недоступен"

        /** Все пять смыслов учёта разом: по одному на точку по кругу. */
        val STATUSES = listOf(
            "REGISTERED",
            "DRAFT",
            "REGISTRATION_IN_ISNA_PROCESS",
            "REGISTRATION_IN_ISNA_ERROR",
            "DEREGISTERED"
        )
    }
}
