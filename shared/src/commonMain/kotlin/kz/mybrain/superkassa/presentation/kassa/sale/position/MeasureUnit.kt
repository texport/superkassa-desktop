package kz.mybrain.superkassa.presentation.kassa.sale.position

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Единица измерения, как её видит кассир: код ИС ЭСФ и сокращение.
 *
 * Перечень — справочник единиц самой кассы; слово на языке кассира —
 * из текстов кассы (`UnitTexts.short`): у справочника слов только два языка.
 */
data class MeasureUnit(val code: String, val title: String)

/**
 * Единицы измерения на экране продажи.
 *
 * Держится общим на экран, а не собирается каждой строкой корзины:
 * строк в чеке десятки, а справочник один и тот же.
 */
val LocalUnits = staticCompositionLocalOf<List<MeasureUnit>> { emptyList() }

/** Единицы справочника кассы словами кассира; «неизвестная» в выбор не идёт. */
fun measureUnits(language: Language): List<MeasureUnit> = UnitOfMeasurement.entries
    .filter { it != UnitOfMeasurement.UNKNOWN }
    .map { MeasureUnit(it.code, textsOf(language).kassa.units.short(it.code) ?: it.code) }

/**
 * Как назвать единицу в строке чека.
 *
 * Неизвестный код показывается как есть: прятать его значит показать
 * количество без единицы. Пустой код — штука по умолчанию кассы,
 * и дописывать её незачем.
 */
fun unitTitle(units: List<MeasureUnit>, code: String?): String {
    val wanted = code?.takeIf { it.isNotBlank() } ?: return ""
    return units.firstOrNull { it.code == wanted }?.title ?: wanted
}
