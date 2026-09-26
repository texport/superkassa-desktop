package kz.mybrain.superkassa.strings.api.update

/**
 * Надписи о версии кассы и её обновлениях.
 *
 * Своим набором: живут в углу рельса, в окне о новой версии и в карточке
 * настроек, и ни к одному экрану целиком не относятся.
 */
data class UpdateTexts(
    /** Название приложения на языке кассира — для подсказки к версии. */
    val appName: String,
    val title: String,
    val hint: String,
    val installed: String,
    val automatic: String,
    val lastChecked: String,
    val neverChecked: String,
    val checkNow: String,
    val checking: String,
    val upToDate: String,
    /** «Доступна версия»: за ней ставится номер. */
    val available: String,
    val availableHint: String,
    val download: String,
    /** Установщик скачивается и сверяется: кнопку «Скачать» гасят. */
    val downloading: String,
    val later: String,
    val unreachable: String,
    /** Установщик скачан, совпал с выпуском и открыт. */
    val installerOpened: String,
    /** Сверить установщик нечем: открыта страница выпуска. */
    val pageOpened: String,
    /** Скачанный установщик не совпал с выпуском. */
    val installerTampered: String,
    /**
     * Android не разрешил кассе ставить приложения: открыта настройка
     * системы, и сказано, что в ней включить и что нажать потом.
     */
    val installPermission: String,
    /** Сборка разработчика: выпуски ей не предлагаются, и почему. */
    val development: String
)
