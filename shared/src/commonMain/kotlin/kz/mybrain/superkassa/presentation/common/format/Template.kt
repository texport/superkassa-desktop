package kz.mybrain.superkassa.presentation.common.format

/**
 * Подстановка в шаблон надписи: каждое `%s` по порядку заменяется значением.
 *
 * Надписи на трёх языках держат место для числа или имени знаком `%s`,
 * а порядок слов у языков разный: «Внести %s?» и «%s салынсын ба?».
 * Разбор шаблона свой, без форматирования платформы: общий код
 * собирается и для Android, и для iOS, а шаблонам нужна только подстановка.
 *
 * Значений меньше, чем мест, — лишние места остаются как есть: недостающее
 * слово видно на экране и в проверке, а не роняет экран кассира.
 */
fun String.fill(vararg values: Any): String {
    val parts = split(HOLE)
    return buildString {
        parts.forEachIndexed { index, part ->
            if (index > 0) append(values.getOrNull(index - 1)?.toString() ?: HOLE)
            append(part)
        }
    }
}

/** Место для значения в шаблоне надписи. */
private const val HOLE = "%s"
