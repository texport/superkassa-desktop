package kz.mybrain.superkassa.domain.kassa.model.sale

import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptDomainRequest

/**
 * Виды отрасли, которые принимает БФД.
 *
 * Отрасль у кассы одна: она стоит настройкой рабочего места, а не
 * спрашивается в каждом чеке. Торговля идёт первой и заполнять
 * не требует ничего — это обычный чек магазина.
 *
 * Гостиницы уходят тем же подблоком, что и услуги: своего подблока
 * у них в протоколе нет, а лицевой счёт номера БФД ждёт в `services`.
 */
enum class DomainKind(val code: String) {
    Trading("DOMAIN_TRADING"),
    Services("DOMAIN_SERVICES"),
    Hotels("DOMAIN_HOTELS"),
    GasOil("DOMAIN_GASOIL"),
    Taxi("DOMAIN_TAXI"),
    Parking("DOMAIN_PARKING");

    /**
     * Обязательные реквизиты этой отрасли — и ровно они.
     *
     * Один перечень на экран, на правило и на проверку: кассир видит
     * те поля, без которых БФД чек отвергнет, и ни одного чужого.
     * У торговли перечень пуст, поэтому на её экране отраслевого
     * блока нет вовсе.
     */
    val fields: List<DomainField>
        get() = when (this) {
            Trading -> emptyList()
            Services, Hotels -> listOf(DomainField.AccountNumber)
            GasOil -> listOf(DomainField.CardNumber)
            Taxi -> listOf(DomainField.CarNumber, DomainField.Fee)
            Parking -> listOf(DomainField.ParkingFrom, DomainField.ParkingTo)
        }

    /**
     * Вид отрасли без подблока.
     *
     * Годится чеку, у которого своих отраслевых реквизитов нет: возврат
     * оформляется по чеку-основанию, и номер машины с временем стоянки
     * принадлежат тому чеку, а не этому. Подставить вместо них пустые
     * значило бы отправить в БФД выдуманный реквизит.
     */
    val plain: ReceiptDomainRequest get() = ReceiptDomainRequest(type = code)

    companion object {
        /** Отрасль по коду настройки; незнакомый код и пустая настройка — торговля. */
        fun byCode(code: String?): DomainKind = entries.firstOrNull { it.code == code } ?: Trading
    }
}

/**
 * Отраслевой реквизит, без которого чек не примут.
 *
 * На экране он назван тем же словом, что и подпись поля: кассир должен
 * найти взглядом ровно то поле, которое от него просят.
 *
 * @property typed чего ждёт поле, когда просто «заполните» ничего не
 *   объясняет: числа — у тарифа, часов и минут — у времени стоянки.
 */
enum class DomainField(val typed: FieldKind = FieldKind.Text) {
    AccountNumber,
    CardNumber,
    CarNumber,
    Fee(FieldKind.Number),
    ParkingFrom(FieldKind.Time),
    ParkingTo(FieldKind.Time)
}

/** Что набирается в отраслевом поле. */
enum class FieldKind { Text, Number, Time }
