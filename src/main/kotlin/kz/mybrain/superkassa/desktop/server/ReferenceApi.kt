package kz.mybrain.superkassa.desktop.server

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable

/**
 * Справочники узла, которые нужны кассиру при наборе чека.
 *
 * Единицы измерения и ставки НДС узел отдаёт отдельными методами, а не
 * общим списком `dictionaries`: у единицы есть краткое и полное название,
 * у ставки — величина в процентах. Своего перечня ни того, ни другого
 * в кассе быть не должно — он разошёлся бы с узлом при первой же смене
 * налогового законодательства.
 */
@Serializable
data class UnitOfMeasurement(
    val code: String,
    val nameShort: String = "",
    val nameFull: String = ""
) {
    /** Как единицу назвать в форме: краткое имя, иначе полное, иначе код. */
    val title: String
        get() = nameShort.takeIf { it.isNotBlank() } ?: nameFull.takeIf { it.isNotBlank() } ?: code
}

/**
 * Ставка НДС, как её знает узел.
 *
 * Величина приходит целыми процентами; в ОФД она уходит в тысячных долях
 * процента, и это преобразование делает узел. Касса величину только
 * показывает — кассир обязан видеть, по какой ставке пробивает.
 */
@Serializable
data class NodeVatRate(
    val code: String,
    val percent: Int = 0,
    val description: String = "",
    val name: Trilingual = Trilingual()
)

/** Название на трёх языках, как его отдаёт узел. */
@Serializable
data class Trilingual(val ru: String = "", val kk: String = "", val en: String = "") {

    fun title(language: String): String = when (language) {
        "kk" -> kk.takeIf { it.isNotBlank() } ?: ru
        "en" -> en.takeIf { it.isNotBlank() } ?: ru
        else -> ru
    }
}

/**
 * Единицы измерения ИС ЭСФ.
 *
 * Читаются целиком один раз: справочник невелик, а искать по нему кассир
 * будет в форме позиции, где обращение к узлу на каждую букву — лишняя
 * задержка под руками.
 */
suspend fun ServerClient.unitsOfMeasurement(): List<UnitOfMeasurement> =
    request<Page<UnitOfMeasurement>>(HttpMethod.Get, "/units-of-measurement?limit=$UNITS_LIMIT").items

suspend fun ServerClient.vatRates(): List<NodeVatRate> =
    request(HttpMethod.Get, "/vat-rates")

/** Сколько единиц измерения читать за раз: в справочнике ИС ЭСФ их меньше сотни. */
private const val UNITS_LIMIT = 100
