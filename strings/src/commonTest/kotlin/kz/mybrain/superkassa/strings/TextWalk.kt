package kz.mybrain.superkassa.strings

/**
 * Все строки формы текстов парами «путь поля — значение».
 *
 * Отражения в общем коде Kotlin нет, а проверять тексты нужно на каждой
 * платформе модуля. Формы — `data class`, и их `toString` по договору
 * языка печатает поля так: `Форма(поле=значение, вложенное=Форма(…))`.
 * Обход опирается на этот вид: имя поля — слово с маленькой буквы перед
 * `=` сразу после `(` или `, `, значение — всё до следующего поля. Поле,
 * за которым сразу открывается скобка, — вложенная форма, а не строка.
 *
 * Лишние закрывающие скобки в конце значения — конец вложенных форм:
 * скобки внутри самих надписей парные («Telegram (ID)»), и их баланс
 * отделяет текст от разметки. Список единиц — одна строка целиком:
 * у словаря свой вид печати, и его ключи — коды, а не поля.
 */
internal fun lines(form: Any): List<Pair<String, String>> {
    val printed = form.toString()
    val marks = FIELD.findAll(printed).toList()
    val path = ArrayDeque<String>()
    return buildList {
        marks.forEachIndexed { at, mark ->
            val next = marks.getOrNull(at + 1)
            val name = mark.groupValues[2]
            if (next?.groupValues?.get(1) == "(") {
                path.addLast(name)
                return@forEachIndexed
            }
            val raw = printed.substring(mark.range.last + 1, next?.range?.first ?: printed.length)
            val closes = raw.count { it == ')' } - raw.count { it == '(' }
            add((path + name).joinToString(".") to raw.dropLast(closes.coerceAtLeast(0)))
            repeat(closes.coerceAtMost(path.size)) { path.removeLast() }
        }
    }
}

/** Поле в печати формы: разделитель перед ним и его имя. */
private val FIELD = Regex("""(\(|, )([a-z][A-Za-z0-9]*)=""")
