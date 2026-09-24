package kz.mybrain.superkassa.domain.kassa.model.sale

import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptDomainRequest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Отраслевые реквизиты, набранные кассиром.
 *
 * Вида отрасли здесь нет: он один на кассу и стоит настройкой рабочего
 * места. Держать его ещё и в чеке значило бы иметь два ответа на вопрос,
 * чем эта касса торгует.
 *
 * Поля лежат все разом, а спрашиваются по [DomainKind.fields]: кассир
 * такси не должен ни видеть номер карты, ни узнавать о его существовании.
 */
data class DomainInput(
    val accountNumber: String = "",
    val cardNumber: String = "",
    val carNumber: String = "",
    val isOrder: Boolean = false,
    val currentFee: String = "",
    val parkingFrom: String = "",
    val parkingTo: String = ""
) {

    /** Что стоит в поле сейчас. */
    fun value(field: DomainField): String = when (field) {
        DomainField.AccountNumber -> accountNumber
        DomainField.CardNumber -> cardNumber
        DomainField.CarNumber -> carNumber
        DomainField.Fee -> currentFee
        DomainField.ParkingFrom -> parkingFrom
        DomainField.ParkingTo -> parkingTo
    }

    /** Набранное кассиром в поле. */
    fun with(field: DomainField, text: String): DomainInput = when (field) {
        DomainField.AccountNumber -> copy(accountNumber = text)
        DomainField.CardNumber -> copy(cardNumber = text)
        DomainField.CarNumber -> copy(carNumber = text)
        DomainField.Fee -> copy(currentFee = text)
        DomainField.ParkingFrom -> copy(parkingFrom = text)
        DomainField.ParkingTo -> copy(parkingTo = text)
    }

    /**
     * Реквизит, которого не хватает БФД, или `null`, если хватает всего.
     *
     * Касса такой чек пропускает — отвергает его уже БФД, когда исправлять
     * нечего. Поэтому проверка живёт здесь, до нажатия кнопки.
     */
    fun missing(kind: DomainKind): DomainField? = kind.fields.firstOrNull { !filled(it) }

    /**
     * Реквизит, негодный тем, что в нём набрано, а не тем, что он пуст.
     *
     * Пустое поле — ещё не ошибка ввода: кассир мог не дойти до него.
     * А «Городской» в тарифе и «вечером» во времени въезда БФД не примет,
     * и сказать об этом нужно сразу.
     */
    fun spoiled(kind: DomainKind): DomainField? = missing(kind)?.takeIf { value(it).isNotBlank() }

    /** Хватает ли набранного, чтобы БФД принял чек этой отрасли. */
    fun complete(kind: DomainKind): Boolean = missing(kind) == null

    /**
     * Отраслевой блок чека: вид отрасли и ровно один подблок под него.
     *
     * Двух подблоков разом протокол не допускает, поэтому подблок здесь
     * один и выбирается видом отрасли, а не набранным.
     *
     * @param now когда выписывается чек: к этому дню относится время стоянки.
     * @param zone часы рабочего места, по которым кассир набирает время.
     */
    fun toDomain(
        kind: DomainKind,
        now: Instant = Clock.System.now(),
        zone: TimeZone = TimeZone.currentSystemDefault()
    ): ReceiptDomainRequest = when (kind) {
        DomainKind.Trading -> ReceiptDomainRequest(type = kind.code)
        DomainKind.Services, DomainKind.Hotels -> ReceiptDomainRequest(
            type = kind.code,
            services = ReceiptDomainRequest.ServicesRequest(accountNumber.trim())
        )
        DomainKind.GasOil -> ReceiptDomainRequest(
            type = kind.code,
            gasOil = ReceiptDomainRequest.GasOilRequest(cardNumber = cardNumber.trim())
        )
        DomainKind.Taxi -> ReceiptDomainRequest(
            type = kind.code,
            taxi = ReceiptDomainRequest.TaxiRequest(
                carNumber = carNumber.trim(),
                isOrder = isOrder,
                currentFee = Tenge.decimal(fee() ?: 0L)
            )
        )
        DomainKind.Parking -> ReceiptDomainRequest(type = kind.code, parking = parking(now, zone))
    }

    private fun filled(field: DomainField): Boolean = when (field) {
        DomainField.Fee -> fee() != null
        DomainField.ParkingFrom -> clockOf(parkingFrom) != null
        DomainField.ParkingTo -> clockOf(parkingTo) != null
        else -> value(field).isNotBlank()
    }

    private fun fee(): Long? = Tenge.parse(currentFee)?.takeIf { it >= 0L }

    /**
     * Время въезда и выезда — из часов и минут, набранных кассиром.
     *
     * Кассир набирает время, а не дату: машина стоит часы, а чек
     * выписывают при выезде. Въезд позже выезда означает ночь: стоянка
     * с вечера до утра — обычное дело, а такой чек БФД не примет.
     */
    private fun parking(now: Instant, zone: TimeZone): ReceiptDomainRequest.ParkingRequest {
        val today = now.toLocalDateTime(zone).date
        val exit = LocalDateTime(today, clockOf(parkingTo) ?: LocalTime(0, 0))
        val entered = LocalDateTime(today, clockOf(parkingFrom) ?: LocalTime(0, 0))
        val entryDay = if (entered > exit) today.minus(1, DateTimeUnit.DAY) else today
        val entry = LocalDateTime(entryDay, entered.time)
        return ReceiptDomainRequest.ParkingRequest(
            beginTimeMillis = entry.toInstant(zone).toEpochMilliseconds(),
            endTimeMillis = exit.toInstant(zone).toEpochMilliseconds()
        )
    }
}

/**
 * Часы и минуты, как их набирает кассир: и «9:30», и «09:30».
 *
 * @return время или `null`, если набрано не время суток.
 */
internal fun clockOf(text: String): LocalTime? = runCatching { CLOCK.parse(text.trim()) }.getOrNull()

private val CLOCK = LocalTime.Format {
    hour(Padding.NONE)
    char(':')
    minute()
}
