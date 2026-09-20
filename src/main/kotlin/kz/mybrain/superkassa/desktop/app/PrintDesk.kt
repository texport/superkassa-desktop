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
import kz.mybrain.superkassa.desktop.server.printPacket

/**
 * Печать, просмотр и сохранение печатной формы.
 *
 * Работа живёт в сеансе, а не на экране: обращение к узлу идёт секунду-две,
 * и кассир успевает уйти в другой раздел. Пока это делал экран, его уход
 * обрывал запрос — полоска ожидания гасла, а форма не открывалась.
 *
 * Кассу-рисовальщика и её пин подбирает [PrintDrawer]: вошедшего кассира
 * здесь может и не быть — владелец смотрит документы в кабинете прямо
 * с экрана входа.
 */
class PrintDesk(private val session: Session) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Кто рисует форму и по какому пину. */
    val drawer = PrintDrawer(session)

    /** Документ, открытый в просмотре: печать и сохранение работают с ним. */
    private var shown: String? = null

    /**
     * Пакет протокола открытого документа.
     *
     * Документ кабинета рисуется по переданным данным, а не по хранимому:
     * пробит он мог быть на другой машине, и его идентификатора в журнале
     * этой кассы нет. Сохранение в файл поэтому повторяет не запрос
     * по идентификатору, а тот же пакет в нужном виде.
     */
    private var shownPacket: String? = null

    /**
     * Как назвать файл, если форму сохранят.
     *
     * Имя складывается из вида документа и его номера: идентификатор
     * в имени файла владельцу ничего не говорит.
     */
    private var shownFile: String? = null

    /**
     * Какой по счёту просмотр идёт сейчас.
     *
     * Окно закрывают, не дождавшись формы, и открывают следующую. Без счёта
     * запоздавшая картинка открывала бы окно заново — поверх того, чем
     * владелец уже занят.
     */
    private var generation = 0

    /** Открывает печатную форму документа поверх любого раздела. */
    fun preview(document: Document) {
        shownFile = PrintFileName.of(document)
        previewDocument(document.id)
    }

    /** То же по идентификатору: у Z-отчёта смены самого документа под рукой нет. */
    fun previewDocument(documentId: String?) {
        val id = documentId
        if (id == null) {
            // Молчание здесь — тот же дефект, что и пустой экран: владелец
            // нажал и обязан узнать, почему ничего не открылось.
            session.report(session.texts.preview.missing)
            return
        }
        shown = id
        shownPacket = null
        draw({ previewDocument(id) }) { kkm, pin ->
            session.client.printDocument(kkm.kkmId, id, pin, PrintKind.Png)
        }
    }

    /**
     * Показывает печатную форму документа, переданного данными.
     *
     * Рисует её тот же узел и тот же рисовальщик, что и свои чеки: вид
     * документа один, и второго рисовальщика для кабинета здесь нет.
     *
     * @param packet пакет протокола документа.
     * @param name как назвать файл, если форму сохранят.
     */
    fun previewPacket(packet: String, name: String, file: String? = null) {
        shown = name
        shownFile = file
        shownPacket = packet
        draw({ previewPacket(packet, name, file) }) { kkm, pin ->
            session.client.printPacket(kkm.kkmId, packet, pin, PrintKind.Png)
        }
    }

    /**
     * Рисует форму и открывает её окно.
     *
     * Окно открывается сразу, до ответа узла: рисование длится секунду-две,
     * и без открытого окна нажатие не отзывается ничем. Признак работы
     * снимается и по картинке, и по отказу — вечно крутиться нечему.
     */
    private fun draw(again: () -> Unit, ask: suspend (Kkm, String) -> ByteArray) {
        val (kkm, pin) = drawer.resolve(again) ?: return
        val at = ++generation
        session.drawing = true
        scope.launch {
            val image = try {
                session.guard(session.texts.dashboard.printForm) { ask(kkm, pin) }
            } finally {
                if (at == generation) session.drawing = false
            }
            if (at != generation) return@launch
            if (image == null) return@launch drawer.refused(again)
            session.preview = withContext(Dispatchers.IO) { Printing.trim(image) }
        }
    }

    /**
     * Владелец закрыл окно просмотра.
     *
     * Закрывается и ожидание: форма, которую узел ещё рисует, экрана
     * уже не займёт — иначе кружок крутился бы в закрытом окне, а готовая
     * картинка открывала бы его снова.
     */
    fun closePreview() {
        generation += 1
        session.preview = null
        session.drawing = false
    }

    /** Печатает документ, переданный данными, не открывая его. */
    fun printPacket(packet: String): Unit = send({ printPacket(packet) }) { kkm, pin ->
        session.client.printPacket(kkm.kkmId, packet, pin, PrintKind.Png)
    }

    /** Печатает документ на принтере рабочего места. */
    fun print(document: Document) = printDocument(document.id)

    /** То же по идентификатору: у записи кабинета документа под рукой нет. */
    fun printDocument(documentId: String): Unit = send({ printDocument(documentId) }) { kkm, pin ->
        session.client.printDocument(kkm.kkmId, documentId, pin, PrintKind.Png)
    }

    /** Спрашивает форму у узла и сразу отправляет её на принтер. */
    private fun send(again: () -> Unit, ask: suspend (Kkm, String) -> ByteArray) {
        val (kkm, pin) = drawer.resolve(again) ?: return
        scope.launch {
            val image = session.guard(session.texts.preview.print) { ask(kkm, pin) }
                ?: return@launch drawer.refused(again)
            sendToPrinter(session, kkm, Printing.trim(image))
        }
    }

    /** Печатает то, что открыто в просмотре: узел для этого уже не нужен. */
    fun printShown() {
        val image = session.preview ?: return
        val kkm = drawer.kkm() ?: return
        scope.launch { sendToPrinter(session, kkm, image) }
    }

    /**
     * Сохраняет открытую форму в файл.
     *
     * Вид файла берётся из настроек, а не с экрана: на экране всегда
     * картинка — иначе её нечем показать, — а покупателю чаще нужен PDF.
     * Поэтому форма запрашивается у узла заново в выбранном виде.
     */
    fun saveShown() {
        val name = shown ?: return
        val packet = shownPacket
        val kind = session.preferences.printKind()
        val (kkm, pin) = drawer.resolve(::saveShown) ?: return
        scope.launch {
            val bytes = session.guard(session.texts.preview.save) {
                if (packet != null) {
                    session.client.printPacket(kkm.kkmId, packet, pin, kind)
                } else {
                    session.client.printDocument(kkm.kkmId, name, pin, kind)
                }
            } ?: return@launch drawer.refused(::saveShown)
            keepFile(session, bytes, "${shownFile ?: name}.${kind.extension}")
        }
    }
}
