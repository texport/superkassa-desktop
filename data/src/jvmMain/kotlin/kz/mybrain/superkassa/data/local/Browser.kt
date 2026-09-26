package kz.mybrain.superkassa.data.local

import kz.mybrain.superkassa.data.log.AppLog
import kz.mybrain.superkassa.data.log.LogSource
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Открывает страницу в браузере системы.
 *
 * Так открывается страница выпуска, когда своего установщика у выпуска
 * нет или сверить его нечем: кассир видит в браузере, что скачивает.
 *
 * @return удалось ли передать адрес системе; отказ — в журнале.
 */
internal fun openInBrowser(url: String): Boolean = opened { Desktop.getDesktop().browse(URI(url)) }

/**
 * Открывает файл программой, которую система назначила его виду: образ
 * диска — в Finder, пакет MSI — установщиком Windows, пакет Debian —
 * центром приложений.
 *
 * @return удалось ли передать файл системе; отказ — в журнале.
 */
internal fun openWithSystem(file: File): Boolean = opened { Desktop.getDesktop().open(file) }

/**
 * Открывает письмо в почтовой программе системы по адресу `mailto:`.
 *
 * @return удалось ли передать адрес системе; отказ — в журнале.
 */
internal fun openMail(uri: String): Boolean = opened { Desktop.getDesktop().mail(URI(uri)) }

private fun opened(open: () -> Unit): Boolean = try {
    open()
    true
} catch (failure: URISyntaxException) {
    refuse(failure)
} catch (failure: IOException) {
    refuse(failure)
} catch (failure: UnsupportedOperationException) {
    refuse(failure)
} catch (failure: IllegalArgumentException) {
    refuse(failure)
}

/** Адрес и путь в журнал не пишутся: в адресе бывает токен, в пути — имя владельца. */
private fun refuse(failure: Exception): Boolean {
    AppLog.warn(LogSource.App, "system did not open it: ${failure::class.simpleName}")
    return false
}
