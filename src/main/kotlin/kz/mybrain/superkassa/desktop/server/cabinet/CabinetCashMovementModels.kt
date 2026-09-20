package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal

/**
 * Внесение и изъятие денег в кабинете: строка списка и движение целиком.
 *
 * Отдельный предмет от чека и отчёта: движение денег не продажа и не итог
 * смены, у него одна сумма и признак работы без связи с ОФД. Собранное
 * с ними в один файл, оно читалось бы как частный случай чека.
 */

/** Внесение или изъятие в списке. */
@Serializable
data class CabinetCashMovement(
    val transactionId: String,
    @SerialName("movementType") val type: String? = null,
    @Contextual val amount: BigDecimal? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val kkmTime: String? = null,
    val sendStatus: String? = null
)

/**
 * Внесение или изъятие целиком: сумма, смена и состояние передачи.
 *
 * Времени кассы, оператора и признака автономной работы у движения нет:
 * кабинет их по нему не хранит. Пока поля стояли в модели, карточка
 * рисовала строку «Пробит —» с прочерком вместо времени.
 */
@Serializable
data class CabinetCashMovementDetails(
    val transactionId: String,
    @SerialName("movementType") val type: String? = null,
    @Contextual val amount: BigDecimal? = null,
    val shiftNumber: Int? = null,
    val createdAt: String? = null,
    val protocolDocumentId: String? = null,
    val sendStatus: String? = null,
    override val payload: JsonElement? = null
) : ProtocolDocument
