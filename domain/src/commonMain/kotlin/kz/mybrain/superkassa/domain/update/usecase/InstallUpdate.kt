package kz.mybrain.superkassa.domain.update.usecase

import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.update.model.AvailableUpdate
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.InstallOutcome
import kz.mybrain.superkassa.domain.update.port.Releases

/**
 * Установка найденной версии: скачать, сверить, открыть — ставит кассир сам.
 *
 * Открывается только сверенный с выпуском установщик: он ставится с правами
 * владельца машины, и подменённый по дороге файл получил бы кассу целиком.
 * Сверить нечем — своего установщика нет или выпуск не объявил сумму, —
 * открывается страница выпуска, где кассир видит, что скачивает.
 */
class InstallUpdate(private val releases: Releases, private val journal: Journal) {

    suspend operator fun invoke(update: AvailableUpdate): InstallOutcome {
        val installer = update.installer?.takeIf { it.verifiable } ?: return openPage(update)
        return when (val fetched = releases.fetch(installer)) {
            is Fetched.Verified -> opened(releases.openFile(fetched.file), InstallOutcome.Started)
            Fetched.Mismatch -> InstallOutcome.Tampered.also {
                journal.failure("installer checksum mismatch, file removed: ${installer.name}")
            }
            is Fetched.Failed -> InstallOutcome.Failed.also {
                journal.warn("installer not downloaded: ${fetched.reason}")
            }
        }
    }

    private fun openPage(update: AvailableUpdate): InstallOutcome {
        journal.info("release page opened: installer ${if (update.installer == null) "missing" else "unverifiable"}")
        return opened(releases.openPage(update.page), InstallOutcome.PageOpened)
    }

    private fun opened(done: Boolean, outcome: InstallOutcome): InstallOutcome =
        if (done) outcome else InstallOutcome.Failed.also { journal.warn("system did not open $outcome") }
}
