package kz.mybrain.superkassa.data.releases

import kz.mybrain.superkassa.data.local.DataHome
import kz.mybrain.superkassa.data.local.openInBrowser
import kz.mybrain.superkassa.data.local.openWithSystem
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.Release
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kz.mybrain.superkassa.domain.update.port.Releases
import kz.mybrain.superkassa.integrations.releases.GithubReleases
import kz.mybrain.superkassa.integrations.releases.ReleasePlatform
import java.io.File
import kz.mybrain.superkassa.integrations.releases.Release as GithubRelease
import kz.mybrain.superkassa.integrations.releases.ReleaseAnswer as GithubAnswer

/**
 * Выпуски настольной кассы на GitHub — порт обновлений поверх модуля выпусков.
 *
 * Модуль приносит выпуск и выбирает файл под систему; здесь выпуск
 * переводится в слова обновлений, а установщик скачивается, сверяется
 * и открывается средствами этой машины.
 *
 * @param platform система этой машины; по умолчанию — по имени, как его
 *   называет Java.
 */
class GithubUpdates(
    private val github: GithubReleases = GithubReleases(),
    private val download: InstallerDownload = InstallerDownload(File(DataHome.directory(), DOWNLOADS)),
    private val platform: ReleasePlatform? = ReleasePlatform.forSystem(System.getProperty("os.name"))
) : Releases {

    override suspend fun latest(): ReleaseAnswer = when (val answer = github.latest()) {
        is GithubAnswer.Found -> ReleaseAnswer.Found(answer.release.forThisSystem())
        is GithubAnswer.Unreachable -> ReleaseAnswer.Unreachable(answer.reason)
    }

    override suspend fun fetch(installer: Installer): Fetched = download.fetch(installer)

    override fun openFile(file: String): Boolean = openWithSystem(File(file))

    override fun openPage(url: String): Boolean = openInBrowser(url)

    private fun GithubRelease.forThisSystem(): Release =
        Release(tag, page, fileFor(platform)?.let { Installer(it.name, it.url, it.sha256) })

    private companion object {
        /** Каталог скачанных установщиков внутри каталога данных. */
        const val DOWNLOADS = "updates"
    }
}
