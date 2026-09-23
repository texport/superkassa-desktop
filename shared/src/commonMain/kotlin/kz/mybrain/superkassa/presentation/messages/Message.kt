package kz.mybrain.superkassa.presentation.messages

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

    /**
     * Ответа не дождались, а операция могла состояться.
     *
     * Отличается от недоступного узла намеренно: узел принял обращение
     * и мог довести его до конца — фискальный документ при этом уже
     * записан и принят БФД. Сказать здесь «узел недоступен» значит
     * отправить кассира пробивать чек второй раз.
     */
    data class NoAnswer(val what: String) : Message

    /**
     * Касса в процессе приложения не выполнила действие.
     *
     * Не отказ по существу — у того есть код и слова, — а сбой: база,
     * файлы, внутренняя ошибка. Кассиру это повод позвать обслуживание,
     * а не исправлять чек.
     */
    data class Failed(val what: String) : Message

    /** Одной строкой, для мест, где сообщение показывается не всплывающей строкой. */
    fun words(): String = when (this) {
        is Done -> text
        is Refusal -> text
        is NodeUnavailable -> what
        is NoAnswer -> what
        is Failed -> what
    }
}
