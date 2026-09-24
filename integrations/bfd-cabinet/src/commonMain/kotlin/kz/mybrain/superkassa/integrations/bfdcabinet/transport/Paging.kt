package kz.mybrain.superkassa.integrations.bfdcabinet.transport

import kz.mybrain.superkassa.integrations.bfdcabinet.CABINET_PAGE_SIZE
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetPage

/**
 * Список кабинета целиком, страница за страницей.
 *
 * Точки, кассы и модели читаются разом: по ним идёт поиск и выбор,
 * и список, оборванный на первой странице, отвечал бы «ничего не нашлось»
 * о кассе, которая у владельца есть. Прочитанное отдаётся после каждой
 * страницы: сорок страниц — это секунды, и первые записи уже можно показать.
 *
 * Итог `totalElements` кабинет отдаёт не всегда: справочник моделей на всех страницах, кроме
 * последней, отвечает `-1`, и чтение «пока прочитано меньше итога»
 * останавливалось на первых пятидесяти моделях из ста двадцати. Поэтому
 * названный итог читается до конца, а без итога чтение идёт, пока страницы
 * полные; пустая страница — конец в любом случае.
 *
 * @param read чтение одной страницы по её номеру.
 * @param onPart прочитанное на сейчас и сколько записей всего у кабинета —
 *   а пока итог не назван, сколько прочитано.
 */
internal suspend fun <T> allPages(
    read: suspend (Int) -> CabinetPage<T>,
    onPart: (List<T>, Long) -> Unit
): List<T> {
    var last = read(0)
    val all = last.items.toMutableList()
    onPart(all.toList(), total(last, all.size))
    var page = 1
    while (page < PAGE_LIMIT && more(last, all.size)) {
        last = read(page)
        all += last.items
        onPart(all.toList(), total(last, all.size))
        page++
    }
    return all
}

/**
 * Есть ли что читать за страницей [last]: итог назван — пока он не набран,
 * не назван — пока страницы полные. Пустая страница — конец в любом случае.
 */
private fun more(last: CabinetPage<*>, read: Int): Boolean = when {
    last.items.isEmpty() -> false
    last.totalElements >= 0 -> read < last.totalElements
    else -> last.items.size >= CABINET_PAGE_SIZE
}

/** Итог по словам кабинета, а пока он не назван — прочитанное. */
private fun total(last: CabinetPage<*>, read: Int): Long =
    if (last.totalElements >= 0) maxOf(last.totalElements, read.toLong()) else read.toLong()

/**
 * Сколько страниц читается подряд, прежде чем остановиться.
 *
 * Предел на случай, когда кабинет объявит больше, чем отдаёт. Двухсот страниц
 * хватает на десять тысяч записей — вчетверо больше самой крупной сети.
 */
private const val PAGE_LIMIT = 200
