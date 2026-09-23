package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.server.ReceiptDomain
import kz.mybrain.superkassa.desktop.ui.strings.EnumStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts

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
enum class DomainKind(val code: String, val title: (EnumStrings) -> String) {
    Trading("DOMAIN_TRADING", { it.domainTrading }),
    Services("DOMAIN_SERVICES", { it.domainServices }),
    Hotels("DOMAIN_HOTELS", { it.domainHotels }),
    GasOil("DOMAIN_GASOIL", { it.domainGasOil }),
    Taxi("DOMAIN_TAXI", { it.domainTaxi }),
    Parking("DOMAIN_PARKING", { it.domainParking });

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
    val plain: ReceiptDomain get() = ReceiptDomain(type = code)

    companion object {
        /** Отрасль по коду настройки; незнакомый код и пустая настройка — торговля. */
        fun byCode(code: String?): DomainKind = entries.firstOrNull { it.code == code } ?: Trading
    }
}

/**
 * Отраслевой реквизит, без которого чек не примут.
 *
 * Назван тем же словом, что и подпись поля на экране: кассир должен
 * найти взглядом ровно то поле, которое от него просят.
 */
enum class DomainField(
    val label: (SaleTexts) -> String,
    /**
     * Чем поле негодно, когда просто «заполните» ничего не объясняет.
     *
     * «Заполните: Тариф» над заполненным полем со словом «Городской» —
     * это не причина, а загадка: такому полю сказано, что от него нужно
     * число, а полю времени — что нужны часы и минуты.
     */
    private val malformed: ((SaleTexts) -> String)? = null
) {
    AccountNumber({ it.accountNumber }),
    CardNumber({ it.cardNumber }),
    CarNumber({ it.carNumber }),
    Fee({ it.fee }, { it.numberField }),
    ParkingFrom({ it.parkingFrom }, { it.timeField }),
    ParkingTo({ it.parkingTo }, { it.timeField });

    /** Чего не хватает — словами кассира и с именем поля. */
    fun reason(texts: SaleTexts): String =
        malformed?.invoke(texts)?.format(label(texts)) ?: "${texts.fillIn}: ${label(texts)}"
}
