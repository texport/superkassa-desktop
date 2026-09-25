package kz.mybrain.superkassa.presentation.kassa.payment

import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kassa.model.payment.PaymentSplit

/**
 * Что кассир делает с оплатами чека.
 *
 * Одни и те же действия у продажи и у возврата, а выполняет их модель
 * того экрана, где они набраны. По умолчанию пустые — для снимков вида.
 */
interface PaymentActions {
    fun addPayment(type: String) = Unit

    fun removePayment(at: Int) = Unit

    fun retypePayment(at: Int, type: String) = Unit

    fun enterPayment(at: Int, amount: String) = Unit
}

/**
 * Правка оплат — одна на продажу и возврат.
 *
 * Где лежит разбиение, знает тот, кто его держит: [edit] получает правку
 * и применяет её к своему состоянию.
 */
internal class SplitEditor(private val edit: ((PaymentSplit) -> PaymentSplit) -> Unit) : PaymentActions {
    override fun addPayment(type: String) = edit { it.add(type) }

    override fun removePayment(at: Int) = edit { it.remove(at) }

    override fun retypePayment(at: Int, type: String) = edit { it.retype(at, type) }

    override fun enterPayment(at: Int, amount: String) = edit { it.enter(at, amount) }
}
