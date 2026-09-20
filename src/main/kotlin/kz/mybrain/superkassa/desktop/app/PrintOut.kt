package kz.mybrain.superkassa.desktop.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.desktop.server.Kkm

/**
 * Куда уходит готовая печатная форма: на принтер рабочего места или в файл.
 *
 * Отделено от [PrintDesk] намеренно: тот отвечает за разговор с узлом —
 * кто рисует, по какому пину и что показать на экране, — а здесь только
 * принтер и диск. Ни одного обращения к узлу отсюда не идёт.
 */

/**
 * Отправляет готовую форму на принтер рабочего места и говорит, что вышло.
 *
 * Принтер задаётся в настройках; пока не задан, берётся системный
 * по умолчанию. Кассир не должен выбирать принтер на каждый чек.
 */
internal suspend fun sendToPrinter(session: Session, kkm: Kkm, image: ByteArray) {
    val texts = session.texts.preview
    val sent = runCatching {
        withContext(Dispatchers.IO) {
            Printing.print(
                image,
                session.preferences.printer(kkm.kkmId),
                tapeWidthMm(kkm),
                session.preferences.printCopies
            )
        }
    }
    session.report(if (sent.getOrDefault(false)) texts.printSent else texts.printFailed)
}

/** Спрашивает, куда положить файл, и кладёт его туда. */
internal suspend fun keepFile(session: Session, bytes: ByteArray, name: String) {
    val texts = session.texts.preview
    val target = withContext(Dispatchers.IO) { askWhereToSave(name, texts.save) } ?: return
    withContext(Dispatchers.IO) { Printing.save(bytes, target) }
    session.report("${texts.saved}: ${target.name}")
}

/** Ширина ленты кассы в миллиметрах; ноль — печать по ширине страницы. */
private fun tapeWidthMm(kkm: Kkm): Int = kkm.branding?.paperWidthMm ?: 0
