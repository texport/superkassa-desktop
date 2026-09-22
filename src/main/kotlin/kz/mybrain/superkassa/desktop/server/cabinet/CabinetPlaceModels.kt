package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/** Торговая точка компании. */
@Serializable
data class RetailPlace(
    val id: String,
    val name: String,
    val addressRef: String? = null,
    val rka: String? = null,
    val cato: String? = null,
    val address: String? = null,
    @SerialName("addressKk") val addressKz: String? = null,
    @Contextual val latitude: BigDecimal? = null,
    @Contextual val longitude: BigDecimal? = null,
    val cashRegisterCount: Long = 0
)

/** Заведение торговой точки. */
@Serializable
data class RetailPlaceCreate(
    val name: String,
    val addressRef: String,
    @Contextual val latitude: BigDecimal? = null,
    @Contextual val longitude: BigDecimal? = null
)

/** Переименование торговой точки. */
@Serializable
data class RetailPlaceRename(val name: String)

/** Смена адреса торговой точки. */
@Serializable
data class RetailPlaceAddress(
    val addressRef: String,
    @Contextual val latitude: BigDecimal? = null,
    @Contextual val longitude: BigDecimal? = null
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
@Serializable
data class ChangeAddressResult(
    val retailPlaceId: String? = null,
    val updated: Boolean = false,
    val changeMode: String? = null,
    val blockingCashRegisters: List<BlockingRegister> = emptyList()
) {
    /** Адрес не сменён: мешают кассы, которые надо перерегистрировать. */
    val needsReregistration: Boolean get() = changeMode == REREGISTRATION_REQUIRED
}

/**
 * Как кабинет называет случай, когда адрес не сменён.
 *
 * Константа на уровне файла, а не в `companion` разбираемого класса:
 * свой companion у `@Serializable`-класса подменяет тот, в котором
 * сериализация ищет разборщик, и ответ кабинета перестаёт читаться.
 */
private const val REREGISTRATION_REQUIRED = "REREGISTRATION_REQUIRED"

/** Касса, из-за которой адрес точки нельзя сменить прямо. */
@Serializable
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
}

/** Адрес из регистра. */
@Serializable
data class RegisterAddress(
    val addressRef: String,
    val address: String? = null,
    val addressKz: String? = null,
    val rka: String? = null,
    val cato: String? = null
)

/** Список адресов регистра. */
/** Подсказка адресного регистра на одном шаге каскада; `rka` заполнен только у строения. */
@Serializable
data class AddressSuggestion(val id: Long, val name: String, val rka: String? = null, val level: String? = null)

@Serializable
data class AddressSuggestions(val items: List<AddressSuggestion> = emptyList())

/** Адрес, подтверждённый регистром по коду РКА. */
@Serializable
data class ResolvedAddress(
    val rka: String,
    val cato: String? = null,
    val displayAddress: String? = null,
    val displayAddressKk: String? = null
) {
    fun toRegisterAddress(): RegisterAddress =
        RegisterAddress(addressRef = rka, address = displayAddress, addressKz = displayAddressKk, rka = rka, cato = cato)
}

/**
 * Вид деятельности из классификатора ОКЭД.
 *
 * Отличается от [Oked] компании: там — что владелец за собой записал
 * и какой из видов основной, здесь — что вообще существует в классификаторе.
 */

/** Модель кассового аппарата из справочника. */
@Serializable
data class KkmModel(val modelCode: String, val name: String? = null, val active: Boolean = true)

/** Модель в карточке кассы. */
@Serializable
data class CashRegisterModel(val modelCode: String, val name: String? = null)

/** Торговая точка в карточке кассы. */
@Serializable
data class RetailPlaceRef(val id: String, val name: String? = null)

/** Последнее регистрационное действие в карточке. */
@Serializable
data class LastRegistrationAction(
    @SerialName("actionType") val type: String,
    val status: String,
    @SerialName("completedAt") val at: String? = null,
    val sentAt: String? = null
)

/** Касса в списке кабинета. */
@Serializable
data class CabinetRegister(
    val id: String,
    val kkmId: Int,
    val internalName: String? = null,
    val status: String,
    val registrationNumber: String? = null,
    val factoryNumber: String? = null,
    val manufactureYear: Int = 0,
    @SerialName("model") private val modelView: CashRegisterModel? = null,
    @SerialName("retailPlace") private val retailPlaceView: RetailPlaceRef? = null,
    private val modelCode: String? = null,
    private val modelName: String? = null,
    private val retailPlaceId: String? = null,
    private val retailPlaceName: String? = null,
    val lastRegistrationAction: LastRegistrationAction? = null,
    val registrationCardAvailable: Boolean = false
) {
    /**
     * Модель и точка приходят по-разному: карточка несёт их объектами,
     * список и ответы на правку — плоскими полями. Экранам отдаётся одно.
     */
    val model: CashRegisterModel?
        get() = modelView ?: if (modelCode == null && modelName == null) null else CashRegisterModel(modelCode.orEmpty(), modelName)

    val retailPlace: RetailPlaceRef?
        get() = retailPlaceView ?: retailPlaceId?.let { RetailPlaceRef(it, retailPlaceName) }
}

/** Заведение кассы. */
@Serializable
data class RegisterCreate(
    val retailPlaceId: String,
    val modelCode: String,
    val factoryNumber: String,
    val manufactureYear: Int,
    val internalName: String? = null
)

/** Правка заведённой кассы. */
@Serializable
data class RegisterEdit(
    val retailPlaceId: String? = null,
    val modelCode: String? = null,
    val factoryNumber: String? = null,
    val manufactureYear: Int? = null
)

/** Своё название кассы. */
@Serializable
data class InternalNameRequest(val internalName: String?)

/** Выданный кассе технический токен. */
/**
 * Выпущенный токен. Кабинет отдаёт его знаковым 32-битным числом, касса
 * ждёт беззнаковое: отрицательное значение здесь — не ошибка, а старший бит.
 * `PENDING` — сервис приёма ещё не подтвердил запись: повторить запрос
 * с тем же ключом идемпотентности.
 */
@Serializable
data class TokenIssued(val kkmId: Int, @SerialName("token") private val rawToken: Long, val status: String? = null) {
    val token: Long get() = rawToken and UNSIGNED_32

    val pending: Boolean get() = status == "PENDING"
}

private const val UNSIGNED_32 = 0xFFFFFFFFL
