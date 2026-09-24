package kz.mybrain.superkassa.integrations.releases.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.integrations.releases.Release
import kz.mybrain.superkassa.integrations.releases.ReleaseFile

/** Выпуск, как его отдаёт `releases/latest`: остальные поля не читаются. */
@Serializable
internal class GithubRelease(
    @SerialName("tag_name") val tag: String,
    @SerialName("html_url") val page: String,
    val name: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GithubAsset> = emptyList()
) {
    fun release() = Release(tag, page, name?.takeIf { it.isNotBlank() }, publishedAt, assets.map { it.file() })
}

/**
 * Файл выпуска.
 *
 * Контрольную сумму GitHub пишет в `digest` видом `sha256:<hex>`; у файлов,
 * выложенных до появления поля, его нет.
 */
@Serializable
internal class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val url: String,
    val size: Long = 0,
    val digest: String? = null
) {
    fun file() = ReleaseFile(name, url, size, sha256Of(digest))
}

/** Шестнадцатеричная сумма из `digest`; другой алгоритм или пусто — суммы нет. */
internal fun sha256Of(digest: String?): String? =
    digest?.takeIf { it.startsWith(SHA256, ignoreCase = true) }
        ?.substring(SHA256.length)
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }

private const val SHA256 = "sha256:"

/** Разбор ответа GitHub: незнакомые поля пропускаются. */
internal val githubJson: Json = Json { ignoreUnknownKeys = true }
