package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.runtime.staticCompositionLocalOf
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement

/**
 * Единицы измерения на экране продажи.
 *
 * Перечень целиком от узла — справочник ИС ЭСФ. Держится общим на экран,
 * а не читается каждой строкой корзины: строк в чеке десятки, а справочник
 * один и тот же.
 */
val LocalUnits = staticCompositionLocalOf<List<UnitOfMeasurement>> { emptyList() }

/**
 * Как назвать единицу в строке чека.
 *
 * Неизвестный код показывается как есть: он пришёл из справочника узла
 * вместе с товаром, и прятать его значит показать количество без единицы.
 * Пустой код — штука по умолчанию узла, и дописывать её незачем.
 */
fun unitTitle(units: List<UnitOfMeasurement>, code: String?): String {
    val wanted = code?.takeIf { it.isNotBlank() } ?: return ""
    return units.firstOrNull { it.code == wanted }?.title ?: wanted
}

/** Штука по классификатору ИС ЭСФ: ею торгуют чаще всего. */
const val PIECE: String = "796"
