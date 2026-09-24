package kz.mybrain.superkassa.domain.cabinet.model

import io.github.texport.superkassa.core.domain.api.model.common.Decimal

/** Торговая точка компании. */
data class RetailPlace(
    val id: String,
    val name: String,
    val addressRef: String? = null,
    val rka: String? = null,
    val cato: String? = null,
    val address: String? = null,
    val addressKz: String? = null,
    val latitude: Decimal? = null,
    val longitude: Decimal? = null,
    val cashRegisterCount: Long = 0
)

/**
 * Чем кончилось заведение точки.
 *
 * @property existed кабинет отдал уже заведённую точку с тем же адресом
 *   и местом, а новой не завёл.
 */
data class PlaceAdded(val place: RetailPlace, val existed: Boolean)

/** Заведение торговой точки: кабинет без места на карте её не заводит. */
data class RetailPlaceCreate(
    val name: String,
    val addressRef: String,
    val latitude: Decimal,
    val longitude: Decimal
)

/** Смена адреса торговой точки: место нового дома обязательно. */
data class RetailPlaceAddress(
    val addressRef: String,
    val latitude: Decimal,
    val longitude: Decimal
)

/**
 * Чем кончилась смена адреса.
 *
 * Кабинет отвечает на смену адреса не точкой, а результатом проверки:
 * прямо адрес меняется только у точки, где касс нет или все они черновики,
 * ни разу не уходившие в КГД. Иначе адрес остаётся прежним, приходит
 * `REREGISTRATION_REQUIRED` и список касс, которые этому мешают, —
 * и в обоих случаях HTTP 200.
 *
 * Прежде приложение ждало здесь саму точку: успешная смена адреса падала
 * на разборе ответа, и владелец читал «Кабинет не отвечает» о смене,
 * которая состоялась.
 */
data class ChangeAddressResult(
    val retailPlaceId: String? = null,
    val updated: Boolean = false,
    val changeMode: String? = null,
    val blockingCashRegisters: List<BlockingRegister> = emptyList()
) {
    /** Адрес не сменён: мешают кассы, которые надо перерегистрировать. */
    val needsReregistration: Boolean get() = changeMode == REREGISTRATION_REQUIRED
}

/** Как кабинет называет случай, когда адрес не сменён. */
private const val REREGISTRATION_REQUIRED = "REREGISTRATION_REQUIRED"

/** Касса, из-за которой адрес точки нельзя сменить прямо. */
data class BlockingRegister(
    val id: String? = null,
    val internalName: String? = null,
    val registrationNumber: String? = null,
    val factoryNumber: String? = null
) {
    /** Как назвать кассу владельцу: своё название, иначе номер учёта, иначе заводской. */
    fun title(): String =
        internalName?.takeIf { it.isNotBlank() }
            ?: registrationNumber?.takeIf { it.isNotBlank() }
            ?: factoryNumber.orEmpty()

    /** Та же касса, названная по списку [known], если кабинет назвал её одним идентификатором. */
    fun namedBy(known: List<CabinetRegister>): BlockingRegister {
        val same = known.firstOrNull { it.id == id }?.takeIf { title().isBlank() } ?: return this
        return copy(
            internalName = same.internalName,
            registrationNumber = same.registrationNumber,
            factoryNumber = same.factoryNumber
        )
    }
}

/** Модель кассового аппарата из справочника. */
data class KkmModel(val modelCode: String, val name: String? = null, val active: Boolean = true)

/** Модель в карточке кассы. */
data class CashRegisterModel(val modelCode: String, val name: String? = null)

/** Торговая точка в карточке кассы. */
data class RetailPlaceRef(val id: String, val name: String? = null)

/** Последнее регистрационное действие в карточке. */
data class LastRegistrationAction(
    val type: String,
    val status: String,
    val at: String? = null,
    val sentAt: String? = null
)

/** Касса в списке кабинета. */
data class CabinetRegister(
    val id: String,
    val kkmId: Int,
    val internalName: String? = null,
    val status: String,
    val registrationNumber: String? = null,
    val factoryNumber: String? = null,
    val manufactureYear: Int = 0,
    val model: CashRegisterModel? = null,
    val retailPlace: RetailPlaceRef? = null,
    val lastRegistrationAction: LastRegistrationAction? = null,
    val registrationCardAvailable: Boolean = false
)

/** Заведение кассы. */
data class RegisterCreate(
    val retailPlaceId: String,
    val modelCode: String,
    val factoryNumber: String,
    val manufactureYear: Int,
    val internalName: String? = null
)

/** Правка заведённой кассы. */
data class RegisterEdit(
    val retailPlaceId: String? = null,
    val modelCode: String? = null,
    val factoryNumber: String? = null,
    val manufactureYear: Int? = null
)

/**
 * Выпущенный токен кассы.
 *
 * @property token токен беззнаковым 32-битным числом — так его ждёт касса;
 *   `null` — сервис приёма запись ещё не подтвердил, и показывать или
 *   вписывать в кассу нечего.
 */
data class TokenIssued(val kkmId: Int, val token: Long?)
