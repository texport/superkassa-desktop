package kz.mybrain.superkassa.desktop.server

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable

/**
 * Значение справочника узла.
 *
 * Узел отдаёт названия сразу на трёх языках, и приложение не переводит их
 * заново: перевод, разошедшийся с узлом, — это два разных названия одного
 * и того же вида оплаты на чеке и на экране.
 */
@Serializable
data class DictionaryEntry(
    val code: String,
    val name: Map<String, String> = emptyMap(),
    /**
     * Принимает ли узел это значение сейчас.
     *
     * Виды оплаты живут дольше версий протокола: кредит и тара остались
     * в справочнике, но 2.0.4 их не принимает. Допустимость называет узел,
     * а приложение её показывает — свой список «плохих кодов» разошёлся бы
     * с узлом при первой же смене версии.
     */
    val supported: Boolean = true
) {
    /** Название на языке кассира; если его нет — по-русски, потом сам код. */
    fun title(language: String): String =
        name[language] ?: name["ru"] ?: code
}

/** Справочники, которыми пользуется касса. */
enum class Dictionary(val path: String) {
    PaymentTypes("payment-types"),
    DeliveryStatuses("delivery-statuses"),
    DocumentTypes("document-types"),
    KkmStates("kkm-states"),
    UserRoles("user-roles"),
    OfdProviders("ofd-providers"),
    OfdEnvironments("ofd-environments"),
    ReceiptOperationTypes("receipt-operation-types"),
    CashOperationTypes("cash-operation-types"),
    VatGroups("vat-groups"),
    TaxRegimes("tax-regimes"),
    PaperWidths("paper-widths")
}

suspend fun ServerClient.dictionary(dictionary: Dictionary): List<DictionaryEntry> =
    request(HttpMethod.Get, "/dictionaries/${dictionary.path}")
