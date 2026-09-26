package kz.mybrain.superkassa.domain.update.port

import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kotlin.time.Instant

/**
 * Служба выпусков для проверок: отвечает тем, что ей дали.
 *
 * @property fetched чем кончится скачивание установщика.
 * @property allowed разрешила ли система ставить приложения.
 * @property opened что открыто системой: скачанный файл или страница выпуска.
 */
class FakeReleases(
    var answer: ReleaseAnswer = ReleaseAnswer.Unreachable("no network in checks"),
    var fetched: Fetched = Fetched.Verified("/downloads/Superkassa-1.0.3.msi"),
    var allowed: Boolean = true
) : Releases {
    val opened = mutableListOf<String>()

    /** Какие установщики скачивали. */
    val downloaded = mutableListOf<Installer>()

    /** Сколько раз службу спросили. */
    var asked = 0
        private set

    override suspend fun latest(): ReleaseAnswer {
        asked++
        return answer
    }

    override suspend fun fetch(installer: Installer): Fetched {
        downloaded += installer
        return fetched
    }

    override fun openFile(file: String): Boolean = opened.add(file)

    override fun openPage(url: String): Boolean = opened.add(url)

    override fun installAllowed(): Boolean = allowed

    override fun askInstallPermission(): Boolean = opened.add(INSTALL_PERMISSION)

    companion object {
        /** Что записывается в [opened], когда открыта настройка разрешения установки. */
        const val INSTALL_PERMISSION = "install permission"
    }
}

/** Память о проверке выпусков без диска. */
class MemoryUpdates(override var automatic: Boolean = true, override var lastChecked: Instant? = null) : UpdateMemory
