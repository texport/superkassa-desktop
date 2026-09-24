package kz.mybrain.superkassa.presentation.update.check

import kz.mybrain.superkassa.domain.update.model.AvailableUpdate
import kz.mybrain.superkassa.domain.update.model.UpdateOutcome
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kotlin.time.Instant

/**
 * Обновления кассы, как их видит кассир: в углу рельса и в настройках.
 *
 * @property version установленная версия: одна на всё приложение.
 * @property available найденная новая версия; `null` — установлена
 *   последняя или ещё не проверяли.
 * @property checking проверка идёт сейчас: кнопку «Проверить» гасят.
 * @property installing установщик скачивается и сверяется: кнопку
 *   «Скачать» гасят.
 * @property outcome итог проверки, запущенной рукой; по расписанию
 *   итог не показывается — кассир её не затевал.
 */
data class UpdatesUiState(
    val version: AppVersion = AppVersion.current,
    val available: AvailableUpdate? = null,
    val checking: Boolean = false,
    val installing: Boolean = false,
    val lastChecked: Instant? = null,
    val automatic: Boolean = true,
    val outcome: UpdateOutcome? = null
)
