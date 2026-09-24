package kz.mybrain.superkassa.presentation.kassa.sale.position

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import kz.mybrain.superkassa.presentation.strings.common.Language

/**
 * Единица измерения, как её видит кассир: код ИС ЭСФ и сокращение.
 *
 * Перечень — справочник единиц самой кассы; здесь только выбор слова
 * на языке кассира, у справочника их два — русское и казахское.
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
    .map { MeasureUnit(it.code, if (language == Language.Kk) it.shortKaz else it.shortRus) }

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
