package kz.mybrain.superkassa.data.releases

import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.Release
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kz.mybrain.superkassa.integrations.releases.GithubReleases
import kz.mybrain.superkassa.integrations.releases.ReleasePlatform
import kz.mybrain.superkassa.integrations.releases.Release as GithubRelease
import kz.mybrain.superkassa.integrations.releases.ReleaseAnswer as GithubAnswer

/**
 * Свежий выпуск с GitHub словами обновлений: метка, страница и установщик
 * под систему [platform] с его контрольной суммой.
 *
 * Один перевод на компьютер и Android: какой файл выпуска чей, решает
 * модуль выпусков по расширению, а не каждая платформа по-своему.
 */
internal suspend fun GithubReleases.latestFor(platform: ReleasePlatform?): ReleaseAnswer =
    when (val answer = latest()) {
        is GithubAnswer.Found -> ReleaseAnswer.Found(answer.release.forSystem(platform))
        is GithubAnswer.Unreachable -> ReleaseAnswer.Unreachable(answer.reason)
    }

private fun GithubRelease.forSystem(platform: ReleasePlatform?): Release {
    val file = fileFor(platform)
    return Release(
        tag = tag,
        page = page,
        installer = file?.let { Installer(it.name, it.url, it.sha256) },
        installerPending = platform != null && file == null
    )
}
