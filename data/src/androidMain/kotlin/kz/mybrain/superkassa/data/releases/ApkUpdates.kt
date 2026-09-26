package kz.mybrain.superkassa.data.releases

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kz.mybrain.superkassa.data.local.ForegroundActivity
import kz.mybrain.superkassa.domain.update.model.Fetched
import kz.mybrain.superkassa.domain.update.model.Installer
import kz.mybrain.superkassa.domain.update.model.ReleaseAnswer
import kz.mybrain.superkassa.domain.update.port.Releases
import kz.mybrain.superkassa.integrations.releases.GithubReleases
import kz.mybrain.superkassa.integrations.releases.ReleasePlatform
import java.io.File

/**
 * Выпуски кассы на Android — с GitHub, мимо магазинов приложений.
 *
 * Выпуск несёт APK, подписанный ключом выпусков; касса скачивает его,
 * сверяет с суммой выпуска и отдаёт установщику системы. Ставит кассир:
 * система сама показывает окно «Обновить приложение?», и молча касса
 * не обновляется. Ставить приложения из своего источника владелец
 * устройства разрешает кассе один раз в настройках системы — без этого
 * скачивать незачем, и касса открывает эту настройку.
 *
 * @param screen активность на экране: из неё открываются окна системы.
 */
class ApkUpdates(
    private val context: Context,
    private val screen: ForegroundActivity,
    private val github: GithubReleases = GithubReleases(),
    private val download: ApkDownload = ApkDownload(File(context.cacheDir, DOWNLOADS))
) : Releases {

    override suspend fun latest(): ReleaseAnswer = github.latestFor(ReleasePlatform.Android)

    override suspend fun fetch(installer: Installer): Fetched = download.fetch(installer)

    /** Отдаёт сверенный APK установщику системы через поставщика файлов кассы. */
    override fun openFile(file: String): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY", File(file))
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return start(intent)
    }

    override fun openPage(url: String): Boolean = start(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    /** До Android 8 разрешение одно на всю систему, и спросить его у кассы нельзя. */
    override fun installAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    override fun askInstallPermission(): Boolean = start(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    )

    /** Окно системы — поверх кассы, а без экрана — новой задачей. */
    private fun start(intent: Intent): Boolean = try {
        val shown = screen.activity
        if (shown != null) shown.startActivity(intent) else context.startActivity(intent.addFlags(NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private companion object {
        /** Каталог скачанных APK в кэше приложения; его же отдаёт поставщик файлов. */
        const val DOWNLOADS = "updates"

        /** Окончание имени поставщика файлов после имени пакета — как в манифесте. */
        const val AUTHORITY = ".files"

        const val APK = "application/vnd.android.package-archive"

        const val NEW_TASK = Intent.FLAG_ACTIVITY_NEW_TASK
    }
}
