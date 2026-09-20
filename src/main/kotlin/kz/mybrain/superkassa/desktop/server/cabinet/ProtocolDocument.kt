package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Пакет протокола, приложенный к документу кабинета.
 *
 * Отдельный предмет, потому что он один и тот же у чека, отчёта и движения
 * денег: пакет — запрос кассы и ответ ОФД на него — это всё, что о документе
 * известно вне кассы, где он пробит, и по нему узел рисует печатную форму.
 *
 * Кабинет хранит пакет одним полем `jsonb` и отдаёт его то объектом,
 * то строкой с тем же объектом внутри: [packet] снимает эту разницу.
 */
interface ProtocolDocument {

    /** Пакет протокола, как его отдал кабинет. */
    val payload: JsonElement?

    /** Пакет текстом, готовым уйти на узел; `null` — кабинет его не отдал. */
    val packet: String?
        get() = when (val stored = payload) {
            null -> null
            is JsonPrimitive -> stored.content.takeIf { stored.isString && it.isNotBlank() }
            else -> stored.toString()
        }
}
