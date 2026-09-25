package kz.mybrain.superkassa.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.domain.cabinet.port.SavedFiles

/**
 * Сохранение файла окном системы: место выбирает владелец.
 *
 * Окно и запись — в потоке ввода-вывода: окно выбора держит поток, пока
 * владелец выбирает папку, а экран в это время должен отзываться — иначе
 * главное окно кассы не перерисовывается, и владелец видит за окном
 * выбора белое пятно.
 */
class DialogFiles : SavedFiles {
    override suspend fun save(bytes: ByteArray, name: String, title: String): String? = withContext(Dispatchers.IO) {
        val target = askWhereToSave(name, title) ?: return@withContext null
        target.writeBytes(bytes)
        target.name
    }
}
