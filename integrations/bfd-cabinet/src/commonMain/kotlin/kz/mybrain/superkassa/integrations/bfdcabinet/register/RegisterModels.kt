package kz.mybrain.superkassa.integrations.bfdcabinet.register

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Модель кассового аппарата из справочника ИСНА. */
@Serializable
data class KkmModel(val modelCode: String, val name: String? = null, val active: Boolean = true)

/** Модель в карточке кассы. */
@Serializable
data class CashRegisterModel(val modelCode: String, val name: String? = null)

/** Торговая точка в карточке кассы. */
@Serializable
data class RetailPlaceRef(val id: String, val name: String? = null)

/**
 * Последнее регистрационное действие в карточке.
 *
 * @property type вид действия (`actionType`): постановка, перерегистрация, снятие.
 * @property at когда действие завершено (`completedAt`).
 */
@Serializable
data class LastRegistrationAction(
    @SerialName("actionType") val type: String,
    val status: String,
    @SerialName("completedAt") val at: String? = null,
    val sentAt: String? = null
)

/**
 * Касса в кабинете.
 *
 * Модель и точка приходят по-разному: карточка несёт их объектами, список
 * и ответы на правку — плоскими полями. Наружу отдаётся одно — [model]
 * и [retailPlace].
 *
 * @property kkmId номер кассы в БФД.
 * @property registrationNumber номер КГД; у черновика его нет.
 * @property registrationCardAvailable выдана ли регистрационная карта.
 */
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
    /** Модель кассы — из карточки или из плоских полей списка. */
    val model: CashRegisterModel?
        get() = modelView ?: modelCode?.let { CashRegisterModel(it, modelName) }
            ?: modelName?.let { CashRegisterModel("", it) }

    /** Торговая точка кассы — из карточки или из плоских полей списка. */
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

/** Правка заведённой кассы: незаданное не меняется. */
@Serializable
data class RegisterEdit(
    val retailPlaceId: String? = null,
    val modelCode: String? = null,
    val factoryNumber: String? = null,
    val manufactureYear: Int? = null
)

/** Своё название кассы: обязательное, стереть его кабинет не даёт. */
@Serializable
internal class InternalNameRequest(val internalName: String)

/**
 * Выпущенный технический токен.
 *
 * Кабинет отдаёт его знаковым 32-битным числом, касса ждёт беззнаковое
 * `0..4294967295`: отрицательное значение — не ошибка, а старший бит.
 *
 * Пока сервис приёма выпуск не подтвердил, кабинет отвечает `PENDING`
 * и токена не отдаёт вовсе — `"token": null`. Прежде поле было
 * обязательным, и такой ответ не читался: вместо повтора владелец видел
 * «кабинет ответил непонятно».
 */
@Serializable
data class TokenIssued(
    val kkmId: Int,
    @SerialName("token") private val rawToken: Long? = null,
    val status: String? = null
) {
    /** Токен беззнаковым числом, как его ждёт касса; `null` — ещё не подтверждён. */
    val token: Long? get() = rawToken?.let { it and UNSIGNED_32 }.takeUnless { pending }

    /** Сервис приёма ещё не подтвердил запись: спросить снова с тем же ключом. */
    val pending: Boolean get() = status == PENDING || rawToken == null
}

private const val UNSIGNED_32 = 0xFFFFFFFFL
private const val PENDING = "PENDING"

/**
 * Что о кассе знает сервис приёма.
 *
 * @property found знает ли сервис кассу вовсе.
 * @property active работает ли касса; `null` — не сообщено.
 * @property inactiveReason почему не работает.
 */
@Serializable
data class TechnicalState(
    val found: Boolean = false,
    val active: Boolean? = null,
    val inactiveReason: String? = null,
    val trafficSuspended: Boolean? = null,
    val ofdDisconnected: Boolean? = null,
    val billingStatus: Int? = null,
    val shiftStatus: String? = null,
    val shiftNumber: Int? = null,
    val validationMask: Int? = null,
    val lastContactAt: String? = null,
    val snapshotAt: String? = null
)

/** Состояние кассы: учётное, синхронизация и техническое. */
@Serializable
data class RegisterState(
    val cashRegisterId: String,
    val businessStatus: String,
    val stateSyncStatus: String? = null,
    val technicalState: TechnicalState? = null
)
