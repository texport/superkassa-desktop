package kz.mybrain.superkassa.desktop.ui.theme

/**
 * Основной тон кассы.
 *
 * Тон задаётся одним оттенком, а не набором цветов: по правилам
 * тональных палитр Material 3 из оттенка выводится вся схема — главная,
 * вторичная и третичная роли, поверхности, обводки. Так восемь тонов
 * не расходятся ни в контрасте, ни в яркости поверхностей: меняется
 * только оттенок, а тона остаются теми же.
 *
 * Оттенок задан углом в цветовом круге Lab, а не образцовым цветом:
 * из образца сначала пришлось бы вычислять тот же угол.
 *
 * Индиго — тон по умолчанию: касса им и была, и без выбора её вид не
 * меняется.
 */
enum class Accent(val code: String, val hue: Float) {

    Indigo("indigo", 294.5f),

    Blue("blue", 265f),

    Teal("teal", 195f),

    Green("green", 150f),

    Amber("amber", 80f),

    Orange("orange", 55f),

    Red("red", 30f),

    Violet("violet", 320f);

    companion object {
        fun byCode(code: String?): Accent = entries.firstOrNull { it.code == code } ?: Indigo
    }
}
