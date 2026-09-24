package kz.mybrain.superkassa.domain.signin.model

/**
 * Место за кассой: касса и пин того, кто за ней работает.
 *
 * С ними уходит каждая команда кассе. Пина нет в [toString]: строка
 * попадает в журналы и отчёты проверок, а пин даёт право на фискальные
 * команды.
 */
data class Seat(val kkmId: String, val pin: String) {
    override fun toString(): String = "Seat(kkmId=$kkmId)"
}
