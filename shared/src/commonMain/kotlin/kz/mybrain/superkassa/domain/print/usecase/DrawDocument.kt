package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.domain.print.model.drawerPin
import kz.mybrain.superkassa.domain.print.model.render
import kz.mybrain.superkassa.domain.print.port.PrintOut
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Лента документа для экрана и принтера.
 *
 * Касса рисует ленту картинкой на странице; поля страницы вокруг неё
 * срезаются: на экране кассир принимал их за часть документа, а на печати
 * они съедали ширину бумаги.
 */
class DrawDocument(private val kassa: Kassa, private val printOut: PrintOut, private val signed: SignedKkm) {

    /** @param entered пин, введённый владельцем ради формы; у вошедшего кассира не нужен. */
    suspend operator fun invoke(source: PrintSource, kkmId: String, entered: String?): Answer<ByteArray> {
        val pin = signed.drawerPin(kkmId, entered) ?: return Answer.Failed(NO_PIN)
        return when (val drawn = kassa.ask { source.render(it, kkmId, pin, PrintKind.Png) }) {
            is Answer.Done -> Answer.Done(printOut.tape(drawn.value))
            is Answer.Refused -> drawn
            is Answer.Failed -> drawn
        }
    }
}

/** Имя сбоя «пина нет»: форму позвали, не спросив пин, — для журнала, не для кассира. */
internal const val NO_PIN = "NoPinToDraw"
