package kz.mybrain.superkassa.integrations.releases

/**
 * Выпуск кассы, как его описывает GitHub.
 *
 * @property tag метка выпуска, например `v1.0.3`: по ней сравнивают
 *   с установленной версией.
 * @property page страница выпуска: туда отправляют, когда своего файла нет.
 * @property title заголовок выпуска; `null` — не задан.
 * @property publishedAt момент публикации ISO-8601; `null` — не сообщён.
 * @property files файлы выпуска.
 */
data class Release(
    val tag: String,
    val page: String,
    val title: String?,
    val publishedAt: String?,
    val files: List<ReleaseFile>
) {
    /**
     * Файл выпуска для этой системы.
     *
     * Нет подходящего файла или система неизвестна — нет файла: тогда
     * открывают страницу выпуска, где выбирают сами.
     */
    fun fileFor(platform: ReleasePlatform?): ReleaseFile? =
        platform?.let { wanted -> files.firstOrNull { it.name.endsWith(wanted.extension, ignoreCase = true) } }
}

/**
 * Файл выпуска.
 *
 * @property name имя файла.
 * @property url прямая ссылка на скачивание.
 * @property size размер в байтах; `0` — GitHub его не сообщил.
 * @property sha256 контрольная сумма SHA-256 шестнадцатеричной строкой
 *   в нижнем регистре; `null` — GitHub её не посчитал. Скачанный файл
 *   сверяют с ней перед установкой: подменённый по дороге установщик
 *   ставить нельзя.
 */
data class ReleaseFile(val name: String, val url: String, val size: Long, val sha256: String?) {

    /** Совпадает ли посчитанная у скачанного файла сумма с объявленной. */
    fun matches(sha256Hex: String): Boolean = sha256 != null && sha256.equals(sha256Hex.trim(), ignoreCase = true)
}

/**
 * Установщик какой системы нужен.
 *
 * Выпуск несёт установщики под macOS, Windows и Debian, а кассе нужен
 * ровно один, свой. Опознаётся по расширению файла.
 *
 * @property extension расширение файла установщика.
 */
enum class ReleasePlatform(val extension: String, private val systemName: String) {
    /** macOS: образ диска. */
    MacOs(".dmg", "mac"),

    /** Windows: пакет MSI. */
    Windows(".msi", "windows"),

    /** Linux: пакет Debian. */
    Debian(".deb", "linux");

    /** Опознание системы по её имени. */
    companion object {
        /**
         * Система по имени, как его называет платформа (`os.name` на JVM).
         *
         * @return система; `null` — неизвестна, и установщика под неё нет.
         */
        fun forSystem(osName: String?): ReleasePlatform? {
            val lowered = osName?.lowercase() ?: return null
            return entries.firstOrNull { lowered.contains(it.systemName) }
        }
    }
}

/** Чем кончился вопрос о последнем выпуске. */
sealed interface ReleaseAnswer {

    /** Выпуск найден. */
    data class Found(val release: Release) : ReleaseAnswer

    /**
     * Выпуск узнать не удалось: сети нет, GitHub отказал или ответил не
     * о выпуске.
     *
     * @property reason причина для журнала, по-английски; кассиру её не показывают.
     */
    data class Unreachable(val reason: String) : ReleaseAnswer
}
