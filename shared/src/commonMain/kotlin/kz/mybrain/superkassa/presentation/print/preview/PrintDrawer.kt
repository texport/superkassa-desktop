package kz.mybrain.superkassa.presentation.print.preview

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.print.model.PinRefusal
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Касса, которая рисует печатную форму, и пин, введённый ради неё.
 *
 * Кто рисует и чьим пином, решают сценарии печати; здесь — пин, который
 * владелец ввёл ради формы. Пин вошедшего кассира сюда не попадает: его
 * берёт сценарий у входа. Введённый пин держится только здесь, в памяти:
 * на диск он не пишется, в журнал не попадает и входом кассира не считается.
 */
internal class PrintDrawer(private val cases: PrintCases, private val talk: Talk) {

    /** Пин, введённый владельцем ради печатной формы. Только в памяти. */
    private var entered = ""

    /** Что повторить, когда пин введут. */
    private var pending: (() -> Unit)? = null

    /** Название кассы, чей пин спрашивается; `null` — не спрашиваем. */
    var asking: String? = null
        private set

    /** Касса и сама форма: печатают по ширине ленты этой кассы. */
    class Drawn(val kkm: KkmResponse, val bytes: ByteArray)

    /**
     * Рисует форму у кассы: картинку — лентой без полей, прочее — как есть.
     *
     * @param again что повторить, когда владелец введёт пин.
     * @return итог обращения; `null` — к кассе не ходили: кассы нет
     *   (об этом сказано) или спрашивается пин.
     */
    suspend fun render(source: PrintSource, kind: PrintKind, again: () -> Unit): Answer<Drawn>? {
        val kkm = kkm() ?: return null
        val waits = cases.asksPin(kkm.kkmId) && entered.isBlank()
        if (waits) ask(kkm, again)
        return if (waits) null else draw(source, kind, kkm, again)
    }

    private suspend fun draw(source: PrintSource, kind: PrintKind, kkm: KkmResponse, again: () -> Unit): Answer<Drawn> {
        val pin = entered.ifBlank { null }
        val answer = if (kind == PrintKind.Png) {
            cases.draw(source, kkm.kkmId, pin)
        } else {
            cases.render(source, kkm.kkmId, pin, kind)
        }
        refused(answer, kkm, again)
        return answer.map { Drawn(kkm, it) }
    }

    /** Касса-рисовальщик; не заведено ни одной — владельцу сказано об этом словами, а не молчанием. */
    private suspend fun kkm(): KkmResponse? {
        val texts = textsOf(talk.language()).common.preview
        val answer = cases.findDrawer()
        if (answer !is Answer.Done) return answer.shown(texts.title, "read kkm to draw", talk)
        if (answer.value == null) talk.done(texts.noDrawer)
        return answer.value
    }

    /** Владелец ввёл пин: возвращается то, что встало без него. */
    fun adopt(pin: String): (() -> Unit)? {
        val work = pending
        entered = pin
        pending = null
        asking = null
        return work
    }

    /** Владелец закрыл окно пина: ничего не рисуем и ничего не запоминаем. */
    fun cancel() {
        pending = null
        asking = null
    }

    /** Пин забывается вместе с кассиром. */
    fun forget() {
        entered = ""
        cancel()
    }

    private fun ask(kkm: KkmResponse, again: () -> Unit) {
        asking = cases.nameDrawer(kkm)
        pending = again
    }

    /**
     * Касса отказала — спросить пин заново, если дело в нём.
     *
     * Вошедшего кассира это не касается: его пин касса приняла на входе.
     */
    private fun refused(answer: Answer<*>, kkm: KkmResponse, again: () -> Unit) {
        if (answer !is Answer.Refused || !cases.asksPin(kkm.kkmId) || entered.isBlank()) return
        when (PinRefusal.next(answer.code)) {
            PinRefusal.Next.AskAgain -> {
                entered = ""
                ask(kkm, again)
            }
            PinRefusal.Next.Forget -> entered = ""
            PinRefusal.Next.Keep -> Unit
        }
    }
}
