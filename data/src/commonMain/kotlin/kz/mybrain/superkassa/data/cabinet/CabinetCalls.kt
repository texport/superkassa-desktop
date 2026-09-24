package kz.mybrain.superkassa.data.cabinet

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.domain.cabinet.model.CabinetExpired
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUnreachable
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUnreadable
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetExpired as BfdExpired
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetRefusal as BfdRefusal
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetUnreachable as BfdUnreachable
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetUnreadable as BfdUnreadable

/**
 * Обращение к модулю кабинета, чьи неудачи становятся неудачами предметной
 * области.
 *
 * Модуль говорит своими исключениями, а экраны и сценарии знают только
 * исключения домена: отказ по существу, непонятный ответ, конец сеанса
 * и молчание кабинета переводятся здесь, один раз на все порты.
 */
internal suspend fun <T> cabinetCall(request: suspend () -> T): T = try {
    request()
} catch (refusal: BfdRefusal) {
    throw CabinetRefusal(refusal.code, refusal.text, refusal.httpStatus, refusal)
} catch (unreadable: BfdUnreadable) {
    throw CabinetUnreadable(unreadable.path, unreadable)
} catch (expired: BfdExpired) {
    throw CabinetExpired(expired)
} catch (silent: BfdUnreachable) {
    throw CabinetUnreachable(silent.cause?.let { it::class.simpleName } ?: UNKNOWN, silent)
}

/**
 * Число кабинета — десятичным числом кассы, той же записью.
 *
 * Знаков после запятой у денег два, у координат бывает больше, чем держит
 * число кассы: лишние отбрасываются — девятый знак градуса меньше миллиметра.
 */
internal fun CabinetDecimal.toDecimal(): Decimal {
    val point = plain.indexOf('.')
    val kept = if (point < 0) plain else plain.take(point + 1 + Decimal.MAX_SCALE)
    return Decimal.parse(kept)
}

/** Десятичное число кассы — числом кабинета. */
fun Decimal.toCabinet(): CabinetDecimal = CabinetDecimal.of(toString())

/** Помеха сети без имени. */
private const val UNKNOWN = "unknown"
