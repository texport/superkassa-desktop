package kz.mybrain.superkassa.presentation.update.check

import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.update.port.Releases
import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kz.mybrain.superkassa.domain.update.usecase.CheckForUpdate
import kz.mybrain.superkassa.domain.update.usecase.InstallUpdate
import kz.mybrain.superkassa.domain.update.usecase.ReadUpdateSchedule
import kz.mybrain.superkassa.domain.update.usecase.SwitchAutomaticChecks
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kotlin.time.Instant

/**
 * Сценарии обновлений: проверка, установка и расписание.
 *
 * @property installed установленная версия: с ней сравнивается найденная.
 */
class UpdatesCases(
    releases: Releases,
    memory: UpdateMemory,
    journal: Journal,
    val installed: AppVersion,
    now: () -> Instant
) {
    val check = CheckForUpdate(releases, memory, journal, installed, now)
    val install = InstallUpdate(releases, journal)
    val schedule = ReadUpdateSchedule(memory, installed, now)
    val switchAutomatic = SwitchAutomaticChecks(memory)
}
