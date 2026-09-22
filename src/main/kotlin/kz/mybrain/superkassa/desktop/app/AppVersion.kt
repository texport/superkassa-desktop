package kz.mybrain.superkassa.desktop.app

/**
 * Версия кассы: три числа и, у сборки разработчика, суффикс.
 *
 * Сравнивать версии строками нельзя: «1.0.10» строкой меньше «1.0.9»,
 * и касса звала бы владельца откатиться. Метка выпуска на GitHub идёт
 * с буквой `v` — она отбрасывается при разборе, чтобы «v1.0.3» и «1.0.3»
 * были одной версией.
 *
 * Сборка разработчика — «1.0.0-dev» — по числам равна выпуску 1.0.0,
 * но выпуском не является: любая метка с теми же числами для неё новее.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String? = null
) : Comparable<AppVersion> {

    /** Сборка разработчика: собрана без метки выпуска. */
    val development: Boolean get() = suffix != null

    /** Как версия пишется на экране: числа и суффикс, без буквы `v`. */
    val label: String get() = listOfNotNull("$major.$minor.$patch", suffix).joinToString("-")

    override fun compareTo(other: AppVersion): Int = compareValuesBy(
        this,
        other,
        { it.major },
        { it.minor },
        { it.patch },
        // Выпуск с теми же числами стоит выше сборки разработчика.
        { if (it.development) 0 else 1 }
    )

    override fun toString(): String = label

    companion object {
        /** Версия этой сборки, записанная при сборке. */
        val current: AppVersion = parse(BuildVersion.NAME) ?: AppVersion(0, 0, 0, DEVELOPMENT_SUFFIX)

        private const val DEVELOPMENT_SUFFIX = "dev"

        private const val NUMBER_COUNT = 3

        /**
         * Разбирает «1.0.3», «v1.0.3» и «1.0.0-dev».
         *
         * Всё, что не укладывается в три числа, — не версия: `null`, а не
         * догадка. Метка вроде «nightly» на GitHub не должна читаться
         * как обновление.
         */
        fun parse(text: String?): AppVersion? {
            val trimmed = text?.trim()?.removePrefix("v")?.takeIf { it.isNotEmpty() } ?: return null
            val numbers = trimmed.substringBefore("-")
            val suffix = trimmed.substringAfter("-", missingDelimiterValue = "").takeIf { it.isNotEmpty() }
            val parts = numbers.split(".").map { it.toIntOrNull() ?: return null }
            if (parts.size != NUMBER_COUNT) return null
            return AppVersion(parts[0], parts[1], parts[2], suffix)
        }
    }
}
