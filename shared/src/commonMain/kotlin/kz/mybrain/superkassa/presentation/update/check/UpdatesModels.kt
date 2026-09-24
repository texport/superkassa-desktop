package kz.mybrain.superkassa.presentation.update.check

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Модель обновлений окна.
 *
 * Одна на окно: проверка по расписанию живёт, пока открыто окно, и найденная
 * версия видна и в углу рельса, и в настройках.
 */
@Composable
fun updatesViewModel(app: AppContainer): UpdatesViewModel = viewModel { updatesModel(app) }

/**
 * Модель со сценариями, собранными из портов окна; проверки зовут её без окна.
 *
 * @param installed установленная версия — задаётся проверками.
 * @param now часы — задаются проверками.
 */
fun updatesModel(
    app: AppContainer,
    installed: AppVersion = AppVersion.current,
    now: () -> Instant = { Clock.System.now() }
): UpdatesViewModel {
    val ports = app.areas.settings
    return UpdatesViewModel(UpdatesCases(ports.releases, ports.updateMemory, app.journal, installed, now), app.talk)
}
