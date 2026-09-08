package kz.mybrain.superkassa.desktop.server

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import java.math.BigDecimal

/**
 * Сумма в запросе — числом, но без плавающей точки.
 *
 * Рабочее место считает корзину в [BigDecimal], а в запрос сумма уходила
 * через `Double`: 150.55 превращалось в 150.54999999999998 ещё до узла.
 * Число уезжает своей десятичной записью и такой же приходит обратно.
 */
object DecimalAsNumber : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DecimalAsNumber", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val json = decoder as? JsonDecoder ?: return BigDecimal(decoder.decodeString())
        return BigDecimal((json.decodeJsonElement() as JsonPrimitive).content)
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        val json = encoder as? JsonEncoder ?: return encoder.encodeString(value.toPlainString())
        json.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
    }
}
