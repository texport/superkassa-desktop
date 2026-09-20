package kz.mybrain.superkassa.desktop.app

/**
 * Сообщение кассиру. Разделено по смыслу, а не по цвету.
 *
 * Кассир действует по-разному: отказ узла исправляют чеком, недоступность
 * узла — обслуживанием. Одна строка «ошибка» на оба случая не говорит,
 * что делать сейчас.
 */
sealed interface Message {
    data class Done(val text: String) : Message
    data class Refusal(val text: String, val code: String) : Message
    data class NodeUnavailable(val what: String) : Message

    /** Одной строкой, для мест, где сообщение показывается не всплывающей строкой. */
    fun words(): String = when (this) {
        is Done -> text
        is Refusal -> text
        is NodeUnavailable -> what
    }
}

/** Код отказа узла, означающий закрытую смену. */
internal const val SHIFT_NOT_OPEN = "SHIFT_NOT_OPEN"

/** Роль администратора, как её называет узел. */
internal const val ADMIN_ROLE = "ADMIN"
