package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language

/**
 * Отбор и порядок колонки торговых точек.
 *
 * У сети две тысячи точек и пять тысяч касс, из которых на учёте стоят
 * единицы: поиском по названию кассу с нужным состоянием не найти —
 * владелец не знает ни её названия, ни точки, в которой она стоит. Он
 * знает только состояние: «где у меня отказ КГД».
 *
 * Поэтому отбор спрашивает о самой кассе — состояние учёта и блокировку, —
 * а порядок выстраивает то, что осталось. Поиск, отбор и порядок работают
 * вместе: набранное сужает, отбор сужает ещё, порядок выстраивает.
 */
data class PlaceSieve(
    val needle: String = "",
    val record: KkmRecord? = null,
    val blocked: Boolean = false,
    val order: PlaceOrder = PlaceOrder.Name,
    val descending: Boolean = false
) {

    /**
     * Спрошено ли хоть что-нибудь.
     *
     * По этому колонка называет пустой результат отбором, а не пустым
     * хозяйством владельца, и по этому же выходит кнопка сброса.
     * Порядок сюда не входит: он ничего не убирает из списка.
     */
    val set: Boolean get() = needle.isNotBlank() || marked

    /**
     * Спрошено ли о самой кассе.
     *
     * Тогда точка без подошедшей кассы уходит из списка целиком: на вопрос
     * «где отказ КГД» точка, в которой отказов нет, — не ответ, даже когда
     * её название подходит набранному.
     */
    val marked: Boolean get() = record != null || blocked
}

/**
 * По чему выстраивается колонка.
 *
 * Названия берутся у надписей кабинета, а не пишутся в разметке: ряд
 * плашек перечисляет это перечисление, и добавленный порядок появляется
 * в списке выбора без второй правки.
 */
enum class PlaceOrder(val title: (CabinetTexts) -> String) {
    Name({ it.orderByName }),
    Address({ it.orderByAddress }),
    Registers({ it.orderByRegisters }),
    Record({ it.orderByRecord })
}

/** Точка и её кассы, оставшиеся после отбора. */
internal data class SievedPlace(val place: RetailPlace, val registers: List<CabinetRegister>)

/**
 * Оставляет ли отбор эту кассу.
 *
 * Состояние учёта спрашивается теми же пятью смыслами, что и в аналитике:
 * [kkmRecord] сводит одиннадцать кодов кабинета к ним, и второй таблицы
 * кодов здесь не заводится.
 *
 * @param locked кассы, которые кабинет считает заблокированными.
 */
internal fun PlaceSieve.keeps(register: CabinetRegister, locked: Set<String>): Boolean =
    (record == null || kkmRecord(register.status) == record) &&
        (!blocked || register.id in locked)

/**
 * Выстраивает отобранные точки.
 *
 * Равные по выбранному признаку идут по названию: при порядке по числу
 * касс у двух тысяч точек это число одно и то же почти у всех, и без
 * второго признака список перетряхивался бы при каждом перечитывании.
 *
 * @param attention насколько состояние точки требует вмешательства.
 */
internal fun sortedPlaces(
    places: List<SievedPlace>,
    sieve: PlaceSieve,
    language: Language,
    attention: (RetailPlace) -> Int
): List<SievedPlace> {
    val byName = compareBy<SievedPlace> { it.place.name.lowercase() }
    val forward = when (sieve.order) {
        PlaceOrder.Name -> places.sortedWith(byName)
        PlaceOrder.Address -> places.sortedWith(compareBy<SievedPlace> { address(it, language) }.then(byName))
        PlaceOrder.Registers -> places.sortedWith(compareBy<SievedPlace> { it.place.cashRegisterCount }.then(byName))
        PlaceOrder.Record -> places.sortedWith(compareBy<SievedPlace> { attention(it.place) }.then(byName))
    }
    return if (sieve.descending) forward.reversed() else forward
}

/** Адрес точки на языке владельца: по нему и упорядочивается. */
private fun address(row: SievedPlace, language: Language): String =
    addressIn(language, row.place.address, row.place.addressKz).lowercase()

/**
 * Насколько состояние кассы требует вмешательства.
 *
 * Порядок не тот, в каком смыслы идут жизнью кассы, а тот, в каком
 * владелец в них вмешивается: отказ КГД чинить сейчас, поданное заявление
 * ждать, заведённую кассу подавать, а работающая и снятая с учёта
 * не спрашивают ни о чём.
 */
internal fun KkmRecord.attention(): Int = when (this) {
    KkmRecord.Refused -> REFUSED_FIRST
    KkmRecord.Applied -> APPLIED_NEXT
    KkmRecord.Entered -> ENTERED_NEXT
    KkmRecord.OnRecord -> ON_RECORD_LAST
    KkmRecord.Deregistered -> DEREGISTERED_LAST
}

/**
 * Насколько требует вмешательства сама точка: по самой беспокойной кассе.
 *
 * Точка без касс идёт последней: заводить в ней кассу владелец, может,
 * и собирался, но чинить в ней нечего.
 */
internal fun attentionOf(registers: List<CabinetRegister>): Int =
    registers.minOfOrNull { kkmRecord(it.status).attention() } ?: WITHOUT_REGISTERS

private const val REFUSED_FIRST = 0
private const val APPLIED_NEXT = 1
private const val ENTERED_NEXT = 2
private const val ON_RECORD_LAST = 3
private const val DEREGISTERED_LAST = 4
private const val WITHOUT_REGISTERS = 5
