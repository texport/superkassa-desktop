package kz.mybrain.superkassa.desktop.ui.history

import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts

/**
 * Порядок строк журнала.
 *
 * Три вопроса, с которыми приходят в журнал: что было последним, что было
 * самым крупным и где чек с таким-то номером. Порядок по тексту столбца
 * не подходит ни одному из них: «1 200,00 ₸» по тексту больше «950,00 ₸».
 */
enum class JournalSort(val title: (HistoryJournalTexts) -> String) {
    Moment({ it.sortTime }),
    Amount({ it.sortAmount }),
    Number({ it.sortNumber })
}

/**
 * Отбор журнала: что показывать и в каком порядке.
 *
 * Один на оба экрана и целиком проверяемый: разбирая расхождение с ОФД,
 * владелец и кассир ищут один и тот же чек, и «нашёлся у кассы, не нашёлся
 * в кабинете» — худшее, что может сделать журнал.
 *
 * Строка поиска ищет по словам, а не по всей строке целиком: «4500 продажа»
 * находит продажу на эту сумму независимо от того, что в каком столбце
 * стоит.
 *
 * @param search что набрано в строке поиска.
 * @param type код вида документа; `null` — все виды.
 * @param delivery состояние доставки; `null` — любое.
 * @param shiftNo номер смены; `null` — все смены.
 * @param descending от большего к меньшему: журнал открывается последним
 *   документом, а не первым чеком за срок.
 */
data class JournalQuery(
    val search: String = "",
    val type: String? = null,
    val delivery: JournalDelivery? = null,
    val shiftNo: Long? = null,
    val sort: JournalSort = JournalSort.Moment,
    val descending: Boolean = true
) {

    /** Отобран ли журнал хоть чем-нибудь, кроме порядка строк. */
    val narrowed: Boolean
        get() = search.isNotBlank() || type != null || delivery != null || shiftNo != null
}

/** Строки, подошедшие под отбор, в выбранном порядке. */
fun List<JournalEntry>.select(query: JournalQuery): List<JournalEntry> =
    filter { query.accepts(it) }.sortedWith(query.order())

/** Подходит ли строка под отбор: сначала дешёвые сравнения, поиск последним. */
private fun JournalQuery.accepts(entry: JournalEntry): Boolean {
    if (type != null && entry.typeCode != type) return false
    if (delivery != null && entry.delivery != delivery) return false
    if (shiftNo != null && entry.shiftNo != shiftNo) return false
    return matchesSearch(entry)
}

private fun JournalQuery.matchesSearch(entry: JournalEntry): Boolean {
    val words = search.lowercase().split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return true
    val searchable = entry.searchable
    return words.all { it in searchable }
}

/**
 * Порядок сравнения строк.
 *
 * Пустое значение стоит первым в прямом порядке и последним в обратном:
 * журнал открывается обратным порядком, и чеки без номера не должны
 * занимать его начало.
 */
private fun JournalQuery.order(): Comparator<JournalEntry> {
    val forward: Comparator<JournalEntry> = when (sort) {
        JournalSort.Moment -> compareBy(nullsFirst()) { it.at }
        JournalSort.Amount -> compareBy(nullsFirst()) { it.amountOrder }
        JournalSort.Number -> compareBy(nullsFirst()) { it.numberOrder }
    }
    return if (descending) forward.reversed() else forward
}

/**
 * Виды документов, встретившиеся в журнале.
 *
 * Перечень строится по тому, что действительно пришло: отбор по виду,
 * которого за срок не было, показывал бы пустой список без объяснения.
 * Порядок — тот, в котором виды идут у источника: у узла это порядок его
 * справочника, у кабинета — порядок его разделов.
 */
fun journalTypesIn(entries: List<JournalEntry>): List<JournalType> =
    entries.mapNotNull { entry -> entry.typeCode?.let { JournalType(it, entry.type) } }
        .distinctBy { it.code }

/** Смены, встретившиеся в журнале: от последней к первой. */
fun shiftsIn(entries: List<JournalEntry>): List<Long> =
    entries.mapNotNull { it.shiftNo }.distinct().sortedDescending()

/** Состояния доставки, встретившиеся в журнале, в порядке перечисления. */
fun deliveriesIn(entries: List<JournalEntry>): List<JournalDelivery> =
    JournalDelivery.entries.filter { state -> entries.any { it.delivery == state } }

/**
 * Снимает отбор, которого в пришедших строках больше нет.
 *
 * Сменился срок или вид документов — отбор по смене и состоянию мог
 * остаться от прошлых строк. Пустой список без объяснения владелец читает
 * как потерю документов, а не как свой же отбор, заданный минуту назад.
 */
fun JournalQuery.presentIn(entries: List<JournalEntry>): JournalQuery = copy(
    type = type?.takeIf { code -> journalTypesIn(entries).any { it.code == code } },
    delivery = delivery?.takeIf { it in deliveriesIn(entries) },
    shiftNo = shiftNo?.takeIf { it in shiftsIn(entries) }
)
