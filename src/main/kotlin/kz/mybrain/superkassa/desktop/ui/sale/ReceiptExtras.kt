package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.server.DomainGasOil
import kz.mybrain.superkassa.desktop.server.DomainParking
import kz.mybrain.superkassa.desktop.server.DomainServices
import kz.mybrain.superkassa.desktop.server.DomainTaxi
import kz.mybrain.superkassa.desktop.server.ReceiptDomain
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.EnumStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleStrings
import java.math.BigDecimal

/**
 * Виды отрасли, которые понимает ОФД, и их обязательные реквизиты.
 *
 * Торговля дополнительных полей не требует и потому идёт первой: это
 * обычный чек магазина, и кассиру не нужно ничего заполнять.
 */
enum class DomainKind(val code: String, val title: (EnumStrings) -> String) {
    Trading("DOMAIN_TRADING", { it.domainTrading }),
    Services("DOMAIN_SERVICES", { it.domainServices }),
    Hotels("DOMAIN_HOTELS", { it.domainHotels }),
    GasOil("DOMAIN_GASOIL", { it.domainGasOil }),
    Taxi("DOMAIN_TAXI", { it.domainTaxi }),
    Parking("DOMAIN_PARKING", { it.domainParking })
}

/**
 * Отраслевое поле, без которого чек не примут.
 *
 * Названо тем же словом, что и подпись поля на экране: кассир должен
 * найти взглядом ровно то поле, которое от него просят.
 */
enum class DomainField(
    val label: (SaleStrings) -> String,
    /** Поле принимает только число: пустым и «Городской» оно негодно одинаково. */
    val numeric: Boolean = false
) {
    AccountNumber({ it.accountNumber }),
    CardNumber({ it.cardNumber }),
    CarNumber({ it.carNumber }),
    Fee({ it.fee }, numeric = true),
    ParkingHours({ it.parkingHours }, numeric = true)
}

/** Заполненные кассиром отраслевые поля. */
data class DomainInput(
    val kind: DomainKind = DomainKind.Trading,
    val accountNumber: String = "",
    val cardNumber: String = "",
    val carNumber: String = "",
    val isOrder: Boolean = false,
    val currentFee: String = "",
    val parkingHours: String = "1"
) {
    /**
     * Поле, которого не хватает ОФД, или `null`, если хватает всего.
     *
     * Узел такой чек пропускает — отвергает его уже ОФД, когда исправлять
     * нечего. Поэтому проверка живёт здесь.
     */
    val missing: DomainField?
        get() = when (kind) {
            DomainKind.Trading -> null
            DomainKind.Services, DomainKind.Hotels ->
                DomainField.AccountNumber.takeIf { accountNumber.isBlank() }
            DomainKind.GasOil -> DomainField.CardNumber.takeIf { cardNumber.isBlank() }
            DomainKind.Taxi -> missingTaxiField()
            DomainKind.Parking -> DomainField.ParkingHours.takeIf { hours() == null }
        }

    /** Хватает ли введённого, чтобы ОФД принял чек этого вида отрасли. */
    val complete: Boolean get() = missing == null

    fun toDomain(): ReceiptDomain? = when (kind) {
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
        DomainKind.Parking -> parkingDomain()
    }

    private fun missingTaxiField(): DomainField? = when {
        carNumber.isBlank() -> DomainField.CarNumber
        fee() == null -> DomainField.Fee
        else -> null
    }

    private fun fee(): BigDecimal? = Money.parse(currentFee)?.takeIf { it >= BigDecimal.ZERO }

    private fun hours(): Int? = parkingHours.toIntOrNull()?.takeIf { it > 0 }

    /**
     * Стоянка: ОФД нужны время въезда и выезда, а кассир считает часами.
     * Выезд — текущий момент, въезд отсчитывается от него назад.
     */
    private fun parkingDomain(): ReceiptDomain {
        val end = System.currentTimeMillis()
        val hours = hours() ?: 1
        return ReceiptDomain(
            type = kind.code,
            parking = DomainParking(
                beginTimeMillis = end - hours * HOUR_MILLIS,
                endTimeMillis = end
            )
        )
    }
}

private const val HOUR_MILLIS = 60L * 60L * 1000L
