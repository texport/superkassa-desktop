package kz.mybrain.superkassa.domain.journal.model

/**
 * Прочитанное страницами и то, есть ли за ним ещё.
 *
 * @property more последняя страница пришла целиком: список читается дальше.
 */
data class Paged<T>(val items: List<T>, val more: Boolean)

/**
 * Прочитанное вместе со следующей страницей.
 *
 * Страницы касса отрезает по счёту, а не по последней показанной строке:
 * документ, пробитый между двумя обращениями, сдвигает счёт, и в следующей
 * странице приходит уже показанный. Строка журнала различается своим
 * ключом, и такой повтор ронял список целиком — поэтому он отбрасывается.
 *
 * @param size сколько просили: пришло столько же — за страницей есть ещё.
 */
fun <T> List<T>.withPage(page: List<T>, size: Int, id: (T) -> String): Paged<T> {
    val already = mapTo(mutableSetOf(), id)
    return Paged(this + page.filterNot { id(it) in already }, more = page.size == size)
}
