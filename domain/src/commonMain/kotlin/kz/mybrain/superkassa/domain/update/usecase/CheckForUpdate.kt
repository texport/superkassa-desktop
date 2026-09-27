package kz.mybrain.superkassa.domain.update.usecase

import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.update.model.AvailableUpdate
import kz.mybrain.superkassa.domain.update.model.Release
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kz.mybrain.superkassa.domain.update.model.UpdateOutcome
import kz.mybrain.superkassa.domain.update.port.Releases
import kz.mybrain.superkassa.domain.update.port.UpdateMemory
import kz.mybrain.superkassa.domain.version.model.AppVersion
import kotlin.time.Instant

/**
 * Проверка выпусков кассы: есть ли версия новее установленной.
 *
 * Сама ничего не ставит: касса — фискальный инструмент, и менять её посреди
 * смены без ведома кассира нельзя. Отказ сети — итог «недоступно», а не
 * исключение; время проверки записывается, только если служба ответила.
 */
class CheckForUpdate(
    private val releases: Releases,
    private val memory: UpdateMemory,
    private val journal: Journal,
    private val installed: AppVersion,
    private val now: () -> Instant
) {

    suspend operator fun invoke(): UpdateOutcome = when (val answer = latest()) {
        null -> UpdateOutcome.Development
        is ReleaseAnswer.Found -> adopt(answer.release)
        is ReleaseAnswer.Unreachable -> {
            journal.warn("releases not checked: ${answer.reason}")
            UpdateOutcome.Unreachable
        }
    }

    /** Свежий выпуск; сборку разработчика со службой не сверяют — `null`. */
    private suspend fun latest(): ReleaseAnswer? = if (installed.development) null else releases.latest()

    private fun adopt(release: Release): UpdateOutcome {
        memory.lastChecked = now()
        val latest = AppVersion.parse(release.tag) ?: return unreadable(release.tag)
        return when {
            latest <= installed -> UpdateOutcome.UpToDate
            release.installerPending -> pending(latest)
            else -> available(release, latest)
        }
    }

    private fun pending(latest: AppVersion): UpdateOutcome {
        journal.info("update $latest: installer for this system not uploaded yet")
        return UpdateOutcome.UpToDate
    }

    private fun available(release: Release, latest: AppVersion): UpdateOutcome {
        journal.info("update available: $latest")
        return UpdateOutcome.Available(AvailableUpdate(latest, release.page, release.installer))
    }

    private fun unreadable(tag: String): UpdateOutcome {
        journal.warn("release tag not readable: $tag")
        return UpdateOutcome.Unreachable
    }
}
