package kz.mybrain.superkassa.strings.impl

/**
 * Подстановка значений в места шаблона: `%s` — по порядку, `%номер$s` — по номеру.
 *
 * Разбор свой, без форматирования платформы: общий код собирается и для
 * Android, и для iOS. Места, которым не хватило значения, остаются как есть.
 */
internal fun fillHoles(template: String, values: Array<out Any>): String {
    var next = 0
    return HOLE.replace(template) { hole ->
        val at = hole.groupValues[1].toIntOrNull()?.minus(1) ?: next++
        values.getOrNull(at)?.toString() ?: hole.value
    }
}

/** Место для значения в шаблоне надписи: `%s` или `%номер$s`. */
private val HOLE = Regex("""%(?:(\d+)\$)?s""")
