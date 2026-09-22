package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogSource
import java.awt.Desktop
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * Открывает страницу в браузере системы.
 *
 * Установщик касса не скачивает сама: файл в сотню мегабайт, которому
 * нужно место и подпись системы, — дело браузера, где кассир увидит
 * ход загрузки и где файл окажется в привычной папке.
 *
 * @return удалось ли передать адрес системе; отказ — в журнале.
 */
fun openInBrowser(url: String): Boolean {
    return try {
        Desktop.getDesktop().browse(URI(url))
        true
    } catch (failure: URISyntaxException) {
        refuse(url, failure)
    } catch (failure: IOException) {
        refuse(url, failure)
    } catch (failure: UnsupportedOperationException) {
        refuse(url, failure)
    }
}

private fun refuse(url: String, failure: Exception): Boolean {
    AppLog.warn(LogSource.App, "браузер не открыл $url: ${failure.message ?: failure::class.simpleName}")
    return false
}
