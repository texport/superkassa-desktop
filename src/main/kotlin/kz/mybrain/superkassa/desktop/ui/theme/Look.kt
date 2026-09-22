package kz.mybrain.superkassa.desktop.ui.theme

import androidx.compose.ui.text.font.FontFamily

/**
 * Шрифт кассы.
 *
 * Набор — из семейств, которые есть на любой машине без установки:
 * касса ставится на чужие компьютеры, и файл шрифта в поставке пришлось
 * бы лицензировать и обновлять. Системный — обычный случай; остальные
 * для тех, кому он мелок или непривычен. Суммы и журнал остаются
 * моноширинными при любом выборе: столбец цифр иначе не читается.
 */
enum class Typeface(val code: String, val family: FontFamily) {

    /** Шрифт операционной системы: то, что кассир видит и в других окнах. */
    System("system", FontFamily.Default),

    /** Гротеск без засечек. */
    Sans("sans", FontFamily.SansSerif),

    /** С засечками. */
    Serif("serif", FontFamily.Serif),

    /** Моноширинный: все знаки одной ширины. */
    Mono("mono", FontFamily.Monospace);

    companion object {
        fun byCode(code: String?): Typeface = entries.firstOrNull { it.code == code } ?: System
    }
}

/**
 * Размер шрифта кассы.
 *
 * Множитель ко всей шкале, а не к отдельным ролям: заголовки, подписи
 * и строки списка растут вместе, и соотношение между ними остаётся тем,
 * что задано шкалой. Шаги невелики: на треть крупнее — и колонка чека
 * уже не помещается в окно кассового монитора.
 */
enum class TextScale(val code: String, val factor: Float) {

    /** Больше строк на экране: плотный товарный список. */
    Compact("compact", 0.9f),

    Normal("normal", 1f),

    /** Читается с метра: кассовый стол и зрение постарше. */
    Large("large", 1.15f);

    companion object {
        fun byCode(code: String?): TextScale = entries.firstOrNull { it.code == code } ?: Normal
    }
}

/** Всё, что кассир выбрал глазами, кроме светлой и тёмной темы. */
data class Look(
    val accent: Accent = Accent.Indigo,
    val typeface: Typeface = Typeface.System,
    val textScale: TextScale = TextScale.Normal
)
