package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.domain.print.model.drawerPin
import kz.mybrain.superkassa.domain.print.model.render
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/**
 * Печатная форма в виде, выбранном для файла: PDF, HTML или картинка.
 *
 * На экране всегда картинка — иначе её нечем показать, — а покупателю
 * чаще нужен PDF: для файла форма рисуется заново в нужном виде.
 */
class RenderDocument(private val kassa: Kassa, private val signed: SignedKkm) {

    /** @param entered пин, введённый владельцем ради формы; у вошедшего кассира не нужен. */
    suspend operator fun invoke(
        source: PrintSource,
        kkmId: String,
        entered: String?,
        kind: PrintKind
    ): Answer<ByteArray> {
        val pin = signed.drawerPin(kkmId, entered) ?: return Answer.Failed(NO_PIN)
        return kassa.ask { source.render(it, kkmId, pin, kind) }
    }
}
