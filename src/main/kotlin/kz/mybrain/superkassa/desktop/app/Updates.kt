package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.server.releases.Release
import kz.mybrain.superkassa.desktop.server.releases.ReleaseAnswer
import kz.mybrain.superkassa.desktop.server.releases.ReleaseClient
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Новая версия кассы, которую можно поставить.
 *
 * @param download ссылка на установщик под эту систему; без него —
 *   страница выпуска, где кассир выберет файл сам.
 */
data class AvailableUpdate(
    val version: AppVersion,
    val page: String,
    val download: String?
)

/** Чем закончилась проверка, запущенная рукой. */
sealed interface UpdateOutcome {
    data object UpToDate : UpdateOutcome
    data class Available(val update: AvailableUpdate) : UpdateOutcome
    data object Unreachable : UpdateOutcome
}

/**
 * Проверка выпусков кассы.
 *
 * Спрашивает GitHub о последнем выпуске и сравнивает его с установленной
 * версией. Сама по себе проверка ничего не ставит: касса — фискальный
 * инструмент, и менять её посреди смены без ведома кассира нельзя.
 * Найденное показывается в углу окна и в настройках, а ставит кассир
 * руками, скачав установщик.
 *
 * Отказ сети при проверке по расписанию не показывается: кассир в нём
 * не виноват и сделать с ним ничего не может. Он остаётся в журнале.
 */
class Updates(
    private val preferences: UpdatePreferences,
    private val client: ReleaseClient = ReleaseClient(),
    private val installed: AppVersion = AppVersion.current,
    private val installer: Installer? = Installer.forSystem(System.getProperty("os.name")),
    private val now: () -> Instant = Instant::now
) {
    /** Установленная версия: одна на всё приложение. */
    val version: AppVersion get() = installed

    /** Найденная новая версия; `null` — установлена последняя или ещё не проверяли. */
    var available: AvailableUpdate? by mutableStateOf(null)
        private set

    /** Проверка идёт сейчас: кнопку «Проверить» на это время гасят. */
    var checking: Boolean by mutableStateOf(false)
        private set

    var lastChecked: Instant? by mutableStateOf(preferences.lastChecked)
        private set

    var automatic: Boolean by mutableStateOf(preferences.automatic)
        private set

    fun switchAutomatic(on: Boolean) {
        automatic = on
        preferences.automatic = on
    }

    /**
     * Проверка по расписанию: через паузу после запуска и дальше раз в сутки.
     *
     * Пауза — чтобы не спорить за сеть со входом кассира и загрузкой касс.
     * Пропущенный день не догоняется дважды: срок считается от записанной
     * проверки, а не от запуска, и касса, включаемая каждое утро,
     * спрашивает GitHub один раз в день, а не при каждом включении.
     */
    suspend fun watch() {
        delay(START_DELAY)
        while (true) {
            if (automatic && due()) check()
            delay(WAKE_EVERY)
        }
    }

    private fun due(): Boolean {
        val last = lastChecked ?: return true
        return Duration.between(last, now()) >= BETWEEN_CHECKS
    }

    /** Одна проверка; итог — для того, кто нажал кнопку. */
    suspend fun check(): UpdateOutcome {
        checking = true
        try {
            return when (val answer = client.latest()) {
                is ReleaseAnswer.Found -> adopt(answer.release)
                is ReleaseAnswer.Unreachable -> unreachable(answer.reason)
            }
        } finally {
            checking = false
        }
    }

    private fun adopt(release: Release): UpdateOutcome {
        remember()
        val latest = AppVersion.parse(release.tag)
        if (latest == null) {
            AppLog.warn(LogSource.App, "выпуск с нечитаемой меткой: ${release.tag}")
            return UpdateOutcome.Unreachable
        }
        if (latest <= installed) {
            available = null
            return UpdateOutcome.UpToDate
        }
        val update = AvailableUpdate(latest, release.page, release.installerFor(installer)?.url)
        available = update
        AppLog.record(LogSource.App, LogLevel.Info, "доступна версия $latest")
        return UpdateOutcome.Available(update)
    }

    private fun unreachable(reason: String): UpdateOutcome {
        AppLog.warn(LogSource.App, "выпуски не проверены: $reason")
        return UpdateOutcome.Unreachable
    }

    private fun remember() {
        val moment = now()
        lastChecked = moment
        preferences.lastChecked = moment
    }

    companion object {
        /** Пауза после запуска: вход кассира важнее вопроса о выпусках. */
        private val START_DELAY = 30.seconds

        /** Как часто просыпаться и смотреть, не пора ли. */
        private val WAKE_EVERY = 1.hours

        /** Раз в сутки: выпуски выходят реже, а GitHub считает обращения. */
        private val BETWEEN_CHECKS: Duration = Duration.ofDays(1)
    }
}
