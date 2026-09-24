package kz.mybrain.superkassa.integrations.bfdcabinet

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive

/**
 * Десятичное число кабинета — сумма в тенге, количество, ставка, координата —
 * в той записи, в какой его прислал кабинет.
 *
 * Разбирается сама запись числа, а не `Double`: десяти тиынов в двоичной дроби
 * не существует. Экспонента разворачивается (`1E+3` → `1000`), знаки после
 * запятой сохраняются: `690.00` остаётся `690.00`. Сравнение — по значению:
 * `690.00` равно `690`.
 *
 * @property plain запись без экспоненты — её и отправляет модуль кабинету,
 *   и из неё приложение строит свои денежные типы.
 */
@Serializable(with = CabinetDecimalSerializer::class)
class CabinetDecimal private constructor(val plain: String) {

    /**
     * Сумма в тиынах, если число — тенге.
     *
     * Знаков больше двух кабинет не шлёт; если пришлют, тиын округляется
     * половиной от нуля — так же, как у ядра кассы.
     */
    fun tiyn(): Long {
        val negative = plain.startsWith('-')
        val unsigned = plain.removePrefix("-")
        val fraction = unsigned.substringAfter('.', "").padEnd(TIYN_DIGITS + 1, '0')
        val whole = (unsigned.substringBefore('.') + fraction.take(TIYN_DIGITS)).toLong()
        val rounded = if (fraction[TIYN_DIGITS] >= '5') whole + 1 else whole
        return if (negative) -rounded else rounded
    }

    override fun equals(other: Any?): Boolean = other is CabinetDecimal && other.value() == value()

    override fun hashCode(): Int = value().hashCode()

    override fun toString(): String = plain

    /** Значение без незначащих нулей дроби: по нему сравнивают. */
    private fun value(): String = if ('.' in plain) plain.trimEnd('0').trimEnd('.') else plain

    /** Разбор записи числа. */
    companion object {
        /**
         * Число по записи: `12.5`, `-0.40`, `1E+3`.
         *
         * @throws IllegalArgumentException запись не число.
         */
        fun of(text: String): CabinetDecimal {
            val parts = requireNotNull(NUMBER.matchEntire(text.trim())) { "not a decimal: $text" }.groupValues
            val whole = parts[WHOLE]
            val fraction = parts[FRACTION]
            require(whole.isNotEmpty() || fraction.isNotEmpty()) { "not a decimal: $text" }
            val plain = unfolded(whole + fraction, whole.length + (parts[EXPONENT].toIntOrNull() ?: 0))
            val negative = parts[SIGN] == "-" && plain.any { it in '1'..'9' }
            return CabinetDecimal(if (negative) "-$plain" else plain)
        }

        /** Цифры с запятой на месте [point], без ведущих нулей целой части. */
        private fun unfolded(digits: String, point: Int): String {
            val padded = "0".repeat(maxOf(0, -point)) + digits.padEnd(maxOf(point, 0), '0')
            val cut = maxOf(point, 0)
            val whole = padded.take(cut).trimStart('0').ifEmpty { "0" }
            val fraction = padded.drop(cut)
            return if (fraction.isEmpty()) whole else "$whole.$fraction"
        }

        private val NUMBER = Regex("""([+-]?)(\d*)(?:\.(\d*))?(?:[eE]([+-]?\d+))?""")

        private const val SIGN = 1
        private const val WHOLE = 2
        private const val FRACTION = 3
        private const val EXPONENT = 4
        private const val TIYN_DIGITS = 2
    }
}

/**
 * Число кабинета в JSON: читается и из числа, и из строки, пишется числом —
 * той же записью, без округления через `Double`.
 */
internal object CabinetDecimalSerializer : KSerializer<CabinetDecimal> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CabinetDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CabinetDecimal {
        val text = (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonPrimitive?.content ?: decoder.decodeString()
        return try {
            CabinetDecimal.of(text)
        } catch (notNumber: IllegalArgumentException) {
            throw SerializationException(notNumber.message, notNumber)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: CabinetDecimal) {
        val json = encoder as? JsonEncoder ?: return encoder.encodeString(value.plain)
        json.encodeJsonElement(JsonUnquotedLiteral(value.plain))
    }
}
