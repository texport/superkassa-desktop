package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.server.DomainGasOil
import kz.mybrain.superkassa.desktop.server.DomainParking
import kz.mybrain.superkassa.desktop.server.DomainServices
import kz.mybrain.superkassa.desktop.server.DomainTaxi
import kz.mybrain.superkassa.desktop.server.ReceiptDomain
import kz.mybrain.superkassa.desktop.ui.components.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
     * Узел такой чек пропускает — отвергает его уже БФД, когда исправлять
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
     */
    fun toDomain(kind: DomainKind): ReceiptDomain = when (kind) {
        DomainKind.Trading -> ReceiptDomain(type = kind.code)
        DomainKind.Services, DomainKind.Hotels -> ReceiptDomain(
            type = kind.code,
            services = DomainServices(accountNumber.trim())
        )
        DomainKind.GasOil -> ReceiptDomain(
            type = kind.code,
            gasOil = DomainGasOil(cardNumber = cardNumber.trim())
        )
        DomainKind.Taxi -> ReceiptDomain(
            type = kind.code,
            taxi = DomainTaxi(
                carNumber = carNumber.trim(),
                isOrder = isOrder,
                currentFee = fee() ?: BigDecimal.ZERO
            )
        )
        DomainKind.Parking -> ReceiptDomain(type = kind.code, parking = parking())
    }

    private fun filled(field: DomainField): Boolean = when (field) {
        DomainField.Fee -> fee() != null
        DomainField.ParkingFrom -> clock(parkingFrom) != null
        DomainField.ParkingTo -> clock(parkingTo) != null
        else -> value(field).isNotBlank()
    }

    private fun fee(): BigDecimal? = Money.parse(currentFee)?.takeIf { it >= BigDecimal.ZERO }

    /**
     * Время въезда и выезда — из часов и минут, набранных кассиром.
     *
     * Кассир набирает время, а не дату: машина стоит часы, а чек
     * выписывают при выезде. Въезд позже выезда означает ночь: стоянка
     * с вечера до утра — обычное дело, а такой чек БФД не примет.
     */
    private fun parking(): DomainParking {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val exit = LocalDateTime.of(today, clock(parkingTo) ?: LocalTime.MIDNIGHT)
        val entered = LocalDateTime.of(today, clock(parkingFrom) ?: LocalTime.MIDNIGHT)
        val entry = if (entered > exit) entered.minusDays(1) else entered
        return DomainParking(
            beginTimeMillis = entry.atZone(zone).toInstant().toEpochMilli(),
            endTimeMillis = exit.atZone(zone).toInstant().toEpochMilli()
        )
    }
}

/** Часы и минуты, как их набирает кассир: и «9:30», и «09:30». */
private fun clock(text: String): LocalTime? =
    runCatching { LocalTime.parse(text.trim(), CLOCK) }.getOrNull()

private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
