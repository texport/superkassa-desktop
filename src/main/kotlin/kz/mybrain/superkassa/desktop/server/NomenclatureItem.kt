package kz.mybrain.superkassa.desktop.server

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal

/**
 * Ответ узла на поиск по штрихкоду.
 *
 * Позиция лежит вложенным объектом, а не в корне: касса разбирала корень
 * и получала пустоту на любой найденный товар — «в справочнике нет такого
 * штрихкода» при том, что узел его нашёл.
 */
@Serializable
data class NomenclatureLookup(
    val found: Boolean = false,
    val item: NomenclatureItem? = null,
    val resultText: String? = null
)

/**
 * Позиция справочника, найденная по штрихкоду.
 *
 * Имена полей — как их отдаёт узел. Цена читается литералом, а не
 * `Double`: двоичная дробь превращает 407.41 в 407.40999… и цена товара
 * перестаёт быть точной ещё до попадания в чек.
 */
@Serializable
data class NomenclatureItem(
    val barcode: String? = null,
    val name: String? = null,
    val nameKk: String? = null,
    val ntin: String? = null,
    val price: JsonPrimitive? = null,
    val measureUnitCode: String? = null,
    val vatGroup: String? = null
) {
    /** Цена продажи точным десятичным. */
    val sellPrice: BigDecimal? get() = price?.content?.toBigDecimalOrNull()

    /** Чем позицию назвать в чеке, если наименования нет. */
    val title: String get() = name?.takeIf { it.isNotBlank() } ?: barcode.orEmpty()
}

/** Плоское представление ответа узла для показа парами «поле — значение». */
fun JsonElement.toStringMap(): Map<String, String> {
    val obj = this as? JsonObject ?: return emptyMap()
    return obj.entries.associate { (key, value) ->
        key to ((value as? JsonPrimitive)?.content ?: value.toString())
    }
}
