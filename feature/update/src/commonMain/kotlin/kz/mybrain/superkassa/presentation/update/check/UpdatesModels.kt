package kz.mybrain.superkassa.presentation.update.check

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.mybrain.superkassa.domain.update.port.UpdatePorts
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kz.mybrain.superkassa.presentation.common.model.WindowServices
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Модель обновлений окна.
 *
 * Одна на окно: проверка по расписанию живёт, пока открыто окно, и найденная
 * версия видна и в углу рельса, и в настройках.
 */
@Composable
fun updatesViewModel(services: WindowServices, ports: UpdatePorts): UpdatesViewModel =
    viewModel { updatesModel(services, ports) }

/**
 * Модель со сценариями, собранными из портов окна; проверки зовут её без окна.
 *
 * @param installed установленная версия — задаётся проверками.
 * @param now часы — задаются проверками.
 */
internal fun updatesModel(
    services: WindowServices,
    ports: UpdatePorts,
    installed: AppVersion = AppVersion.current,
    now: () -> Instant = { Clock.System.now() }
): UpdatesViewModel {
    val cases = UpdatesCases(ports.releases, ports.updateMemory, services.talk.journal, installed, now)
    return UpdatesViewModel(cases, services.talk)
}
