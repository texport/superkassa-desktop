package kz.mybrain.superkassa.desktop.app

import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/**
 * Куда сохранить файл — спрашивает система, а не приложение.
 *
 * Свой список папок в окне кассы разошёлся бы с тем, что человек видит
 * в проводнике. Диалог один на все сохранения: печатная форма чека
 * и регистрационная карта кассы сохраняются одинаково.
 *
 * @param name имя, предложенное по умолчанию.
 * @param title заголовок окна; по умолчанию его ставит система.
 * @return выбранный файл или `null`, если человек закрыл окно.
 */
fun askWhereToSave(name: String, title: String = ""): File? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
    dialog.file = name
    dialog.isVisible = true
    val chosen = dialog.file ?: return null
    return File(dialog.directory ?: "", chosen)
}
