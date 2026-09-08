package kz.mybrain.superkassa.desktop.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.PrintKind
import kz.mybrain.superkassa.desktop.server.printDocument

/**
 * Печать, просмотр и сохранение печатной формы.
 *
 * Работа живёт в сеансе, а не на экране: обращение к узлу идёт секунду-две,
 * и кассир успевает уйти в другой раздел. Пока это делал экран, его уход
 * обрывал запрос — полоска ожидания гасла, а форма не открывалась.
 */
class PrintDesk(private val session: Session) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Документ, открытый в просмотре: печать и сохранение работают с ним. */
    private var shown: String? = null

    /** Открывает печатную форму документа поверх любого раздела. */
    fun preview(document: Document) = previewDocument(document.id)

    /** То же по идентификатору: у Z-отчёта смены самого документа под рукой нет. */
    fun previewDocument(documentId: String?) {
        val kkm = session.selected ?: return
        val id = documentId ?: return
        shown = id
        scope.launch {
            val image = session.guard(session.texts.dashboard.printForm) {
                session.client.printDocument(kkm.kkmId, id, session.pin, PrintKind.Png)
            } ?: return@launch
            session.preview = withContext(Dispatchers.IO) { Printing.trim(image) }
        }
    }

    /** Печатает то, что открыто в просмотре. */
    fun printShown() {
        val image = session.preview ?: return
        val kkm = session.selected ?: return
        val texts = session.texts
        scope.launch {
            val sent = runCatching {
                withContext(Dispatchers.IO) {
                    Printing.print(image, session.preferences.printer(kkm.kkmId), tapeWidthMm(kkm), session.preferences.printCopies)
                }
            }
            session.report(if (sent.getOrDefault(false)) texts.preview.printSent else texts.preview.printFailed)
        }
    }

    /**
     * Сохраняет открытую форму в файл.
     *
     * Вид файла берётся из настроек, а не с экрана: на экране всегда
     * картинка — иначе её нечем показать, — а покупателю чаще нужен PDF.
     * Поэтому форма запрашивается у узла заново в выбранном виде.
     */
    fun saveShown() {
        val documentId = shown ?: return
        save(documentId)
    }

    /**
     * Печатает документ на принтере рабочего места.
     *
     * Принтер задаётся в настройках; пока не задан, берётся системный
     * по умолчанию. Кассир не должен выбирать принтер на каждый чек.
     */
    fun print(document: Document) {
        val kkm = session.selected ?: return
        val texts = session.texts
        scope.launch {
            val image = session.guard(texts.preview.print) {
                session.client.printDocument(kkm.kkmId, document.id, session.pin, PrintKind.Png)
            } ?: return@launch
            val sent = runCatching {
                withContext(Dispatchers.IO) {
                    Printing.print(
                        Printing.trim(image),
                        session.preferences.printer(kkm.kkmId),
                        tapeWidthMm(kkm),
                        session.preferences.printCopies
                    )
                }
            }
            session.report(if (sent.getOrDefault(false)) texts.preview.printSent else texts.preview.printFailed)
        }
    }

    /**
     * Сохраняет печатную форму в файл.
     *
     * Вид файла берётся из настроек: чек покупателю уходит PDF, бухгалтерии
     * бывает нужен HTML, а картинка — то же, что видно на экране.
     */
    fun save(document: Document) = save(document.id)

    private fun save(documentId: String) {
        val kkm = session.selected ?: return
        val texts = session.texts
        val kind = session.preferences.printKind()
        scope.launch {
            val bytes = session.guard(texts.preview.save) {
                session.client.printDocument(kkm.kkmId, documentId, session.pin, kind)
            } ?: return@launch
            val target = withContext(Dispatchers.IO) {
                askWhereToSave("$documentId.${kind.extension}", texts.preview.save)
            } ?: return@launch
            withContext(Dispatchers.IO) { Printing.save(bytes, target) }
            session.report("${texts.preview.saved}: ${target.name}")
        }
    }
}

/** Ширина ленты кассы в миллиметрах; ноль — печать по ширине страницы. */
private fun tapeWidthMm(kkm: Kkm): Int = kkm.branding?.paperWidthMm ?: 0
