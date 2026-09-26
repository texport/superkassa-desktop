package kz.mybrain.superkassa.presentation.print.preview

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.domain.print.model.Shared
import kz.mybrain.superkassa.domain.print.model.SharedReceipt
import kz.mybrain.superkassa.domain.print.model.mime
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Поделиться чеком с покупателем: форма в виде файла из настроек, ссылка
 * на электронный чек и слова к ним — программе, которой пишут и так.
 *
 * Форма рисуется тем же рисовальщиком, что для просмотра и сохранения,
 * и тем же пином: владелец, открывший форму с экрана входа, второй раз
 * пин не набирает.
 *
 * @param asked рисовальщик спросил пин: окно пина открывает модель.
 */
internal class PrintSharing(
    private val cases: PrintCases,
    private val drawer: PrintDrawer,
    private val talk: Talk,
    private val scope: CoroutineScope,
    private val asked: () -> Unit
) {
    /** Пути этой машины; пусто — делиться нечем. */
    val ways: List<ShareWay> get() = cases.share.ways

    /** @param file как назвать файл; `null` — общим словом «чек». */
    fun share(source: PrintSource, file: String?, way: ShareWay) {
        scope.launch {
            val kind = cases.kind()
            val drawn = drawer.render(source, kind) { share(source, file, way) }
            asked()
            val texts = textsOf(talk.language()).common.share
            val done = drawn?.shown(texts.share, SHARE, talk) ?: return@launch
            val link = drawer.link(source, done.kkm)
            val text = link?.let { texts.withLink.fill(it) } ?: texts.withoutLink
            val name = "${file ?: RECEIPT}.${kind.extension}"
            said(cases.share(SharedReceipt(done.bytes, name, kind.mime, texts.subject, text, link), way))
        }
    }

    /** Открылось — дальше отправляет кассир, и говорить нечего; нет — сказано почему. */
    private fun said(shared: Shared) {
        val texts = textsOf(talk.language()).common.share
        when (shared) {
            Shared.Opened -> Unit
            Shared.NoLink -> talk.say(SHARE, Message.Refusal(texts.noLink, NO_LINK))
            Shared.Failed -> talk.say(SHARE, Message.Refusal(texts.failed, NOT_OPENED))
        }
    }

    private companion object {
        const val SHARE = "share document"

        /** Имя файла, когда своего у документа нет. */
        const val RECEIPT = "receipt"

        /** Код для строки сообщений: ссылки на чек ещё нет. */
        const val NO_LINK = "RECEIPT_NO_LINK"

        /** Код для строки сообщений: система не открыла программу. */
        const val NOT_OPENED = "SHARE_NOT_OPENED"
    }
}
