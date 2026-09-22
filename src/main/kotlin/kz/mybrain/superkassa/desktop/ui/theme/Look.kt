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
 * что задано шкалой Material 3. Ступеней пять — столько вариантов
 * Material 3 допускает в ряду сегментов; шестая потребовала бы другого
 * управления ради одного шага.
 *
 * Края набора не подобраны на глаз, а упёрлись в проверенное. Снизу —
 * читаемость: на плотной ступени строка товарного списка остаётся выше
 * одиннадцати точек, ниже её на кассовом мониторе уже не разобрать.
 * Сверху — разметка: на самой крупной ступени экран продажи и настройки
 * ещё собираются в окне тысяча на семьсот, а следующая ступень обрезает
 * ряд сегментов «Продажа / Покупка» и сжимает лист чека в столбец
 * по слову в строке.
 */
enum class TextScale(val code: String, val factor: Float) {

    /** Товарный список: больше строк на экране. */
    Dense("dense", 0.8f),

    Compact("compact", 0.9f),

    Normal("normal", 1f),

    /** Читается с метра: кассовый стол и зрение постарше. */
    Large("large", 1.15f),

    /** Предел разметки: крупнее экраны в низком окне уже не собираются. */
    Larger("larger", 1.3f);

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
