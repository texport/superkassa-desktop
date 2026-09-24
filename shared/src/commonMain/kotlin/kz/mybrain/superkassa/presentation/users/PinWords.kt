package kz.mybrain.superkassa.presentation.users

import kz.mybrain.superkassa.domain.users.model.PinRefusal
import kz.mybrain.superkassa.domain.users.model.UserRules
import kz.mybrain.superkassa.presentation.strings.kassa.CashierTexts

/**
 * Что не так с набранным пином.
 *
 * Пустое поле ошибкой не считается: кассир ещё не начал вводить, и красное
 * поле встречало бы его до первого нажатия.
 */
fun pinProblem(pin: String, money: CashierTexts): String? =
    when (UserRules.checkPin(pin)) {
        PinRefusal.TooShort -> money.pinTooShort
        PinRefusal.TooLong -> money.pinTooLong
        null -> null
    }
