package kz.mybrain.superkassa.presentation.session

/** Код отказа узла, означающий закрытую смену. */
internal const val SHIFT_NOT_OPEN = "SHIFT_NOT_OPEN"

/**
 * Коды отказа узла, означающие непринятый пин.
 *
 * Узел либо не знает такого пользователя кассы, либо не пускает его
 * к тому, что попросили. И то, и другое исправляется вводом другого
 * пина; всякий иной отказ пином не лечится.
 */
internal val PIN_REFUSED = setOf("USER_NOT_FOUND", "USER_FORBIDDEN", "FORBIDDEN")

/** Роль администратора, как её называет узел. */
internal const val ADMIN_ROLE = "ADMIN"
