package kz.mybrain.superkassa.data.cabinet

import kz.mybrain.superkassa.integrations.bfdcabinet.DevelopmentIdentity

/**
 * Вход разработчика в кабинет — только по явной настройке машины.
 *
 * Кабинет в режиме разработки берёт личность из заголовков `X-Debug-Iin`
 * и `X-Debug-Bin` и не спрашивает подписи. У владельца такого входа быть
 * не должно: он включается двумя переменными окружения разработчика
 * и только когда обе — двенадцатизначные ИИН и БИН. Всё прочее — обычный
 * вход по ЭЦП, без полумер: опечатка в переменной не открывает кабинет
 * чужой компании.
 */
object DeveloperEntry {
    /** ИИН пользователя кабинета в режиме разработки. */
    const val IIN_VARIABLE: String = "SUPERKASSA_CABINET_DEBUG_IIN"

    /** БИН компании в режиме разработки. */
    const val BIN_VARIABLE: String = "SUPERKASSA_CABINET_DEBUG_BIN"

    /** ИИН и БИН — по двенадцать цифр. */
    private const val IDENTIFIER_LENGTH = 12

    /** Личность разработчика из окружения [read]; `null` — вход по ЭЦП. */
    fun fromEnvironment(read: (String) -> String? = System::getenv): DevelopmentIdentity? {
        val iin = identifier(read(IIN_VARIABLE))
        val bin = identifier(read(BIN_VARIABLE))
        return if (iin != null && bin != null) DevelopmentIdentity(iin, bin) else null
    }

    /** Двенадцать цифр без пробелов по краям; иное — `null`. */
    private fun identifier(value: String?): String? =
        value?.trim()?.takeIf { it.length == IDENTIFIER_LENGTH && it.all(Char::isDigit) }
}
