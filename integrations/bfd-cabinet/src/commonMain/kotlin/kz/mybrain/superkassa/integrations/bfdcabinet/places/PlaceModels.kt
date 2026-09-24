package kz.mybrain.superkassa.integrations.bfdcabinet.places

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal

/**
 * Торговая точка компании.
 *
 * @property addressRef ссылка на адрес регистра — код РКА.
 * @property cato код КАТО.
 * @property address адрес на русском.
 * @property addressKz адрес на казахском (`addressKk` у кабинета).
 * @property latitude широта, заданная владельцем; `null` — не задана.
 * @property longitude долгота, заданная владельцем; `null` — не задана.
 * @property cashRegisterCount сколько касс у точки.
 */
@Serializable
data class RetailPlace(
    val id: String,
    val name: String,
    val addressRef: String? = null,
    val rka: String? = null,
    val cato: String? = null,
    val address: String? = null,
    @SerialName("addressKk") val addressKz: String? = null,
    val latitude: CabinetDecimal? = null,
    val longitude: CabinetDecimal? = null,
    val cashRegisterCount: Long = 0
)

/**
 * Заведение торговой точки: адрес — только из регистра, место — обязательно.
 *
 * Кабинет требует широту и долготу (`CreateRetailPlaceRequest`), а без них
 * отвечает `400 VALIDATION_ERROR`, не называя полей: необязательными они
 * здесь быть не могут.
 */
@Serializable
data class RetailPlaceCreate(
    val name: String,
    val addressRef: String,
    val latitude: CabinetDecimal,
    val longitude: CabinetDecimal
)

/** Смена адреса торговой точки: место нового дома так же обязательно (`ChangeAddressRequest`). */
@Serializable
data class RetailPlaceAddress(
    val addressRef: String,
    val latitude: CabinetDecimal,
    val longitude: CabinetDecimal
)

/** Переименование торговой точки. */
@Serializable
internal class RetailPlaceRename(val name: String)

/**
 * Чем кончилась смена адреса.
 *
 * Кабинет отвечает не точкой, а результатом проверки — и в обоих исходах
 * HTTP 200: прямо адрес меняется только у точки без касс или с одними
 * черновиками. Иначе адрес остаётся прежним, приходит
 * `REREGISTRATION_REQUIRED` и кассы, которые этому мешают.
 *
 * @property updated сменён ли адрес.
 * @property changeMode как кабинет назвал исход.
 * @property blockingCashRegisters кассы, из-за которых адрес не сменён.
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
 * Константа файла, а не `companion` разбираемого класса: свой companion
 * у `@Serializable`-класса легко спутать с тем, где сериализация ищет разборщик.
 */
private const val REREGISTRATION_REQUIRED = "REREGISTRATION_REQUIRED"

/** Касса, из-за которой адрес точки нельзя сменить прямо. */
@Serializable
data class BlockingRegister(
    val id: String? = null,
    val internalName: String? = null,
    val registrationNumber: String? = null,
    val factoryNumber: String? = null
)
