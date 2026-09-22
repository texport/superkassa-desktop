package kz.mybrain.superkassa.desktop.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.server.Kkm

/**
 * Касса, которая рисует печатную форму, и её пин.
 *
 * Форму рисует касса, а не приложение, и узел пускает к ней по пину.
 * Кассир при этом мог и не входить: владелец открывает кабинет прямо
 * с экрана входа и смотрит там чужие чеки. Прежде в этом случае просмотр
 * молча выходил на `session.selected ?: return` — владелец нажимал
 * и не узнавал ничего.
 *
 * Теперь рисовальщик выбирается сам: выбранная касса, иначе первая
 * из списка узла. Пина в сеансе нет — его спрашивают отдельным окном
 * и держат только здесь, в памяти. На диск он не пишется, в журнал
 * не попадает и входом кассира не считается: разделы кассы от него
 * не открываются.
 */
class PrintDrawer(private val session: Session) {

    /** Пин, введённый владельцем ради печатной формы. Только в памяти. */
    private var entered: String by mutableStateOf("")

    /** Открытый запрос пина; `null` — ничего не спрашиваем. */
    var request: PinRequest? by mutableStateOf(null)
        private set

    /**
     * Касса, чей пин спрашивали последним.
     *
     * Держится отдельно от запроса: ввод пина запрос закрывает, и когда
     * узел отвечает на введённый отказом, спрашивать заново было уже
     * не от чьего имени — окно открывалось с пустой первой строкой.
     */
    private var asked: String = ""

    /**
     * Касса, которая рисует и печатает.
     *
     * Выбранная кассиром, иначе первая из списка узла. Узел не отдал
     * ни одной — владельцу сказано об этом словами, а не молчанием.
     */
    fun kkm(): Kkm? {
        val kkm = session.selected ?: session.kkms.firstOrNull()
        if (kkm == null) session.report(session.texts.preview.noDrawer)
        return kkm
    }

    /**
     * Касса и пин для обращения к узлу.
     *
     * `null` означает, что работа не пошла, и владельцу об этом уже
     * сказано: либо сообщением о том, что касс нет, либо открытым
     * окном пина.
     *
     * @param again что повторить, когда пин введут.
     */
    fun resolve(again: () -> Unit): Pair<Kkm, String>? {
        val kkm = kkm() ?: return null
        val pin = session.pin.ifBlank { entered }
        if (pin.isBlank()) {
            asked = session.displayName(kkm)
            request = PinRequest(asked, again)
            return null
        }
        return kkm to pin
    }

    /**
     * Узел не нарисовал форму — спросить пин заново, если дело в нём.
     *
     * Отказ узла владелец уже прочитал его же словами; пином лечится
     * только отказ по пину, и тогда владельцу дают ввести его ещё раз,
     * а не повторяют неверный молча.
     *
     * Прежде пин спрашивался на любой отказ. Браузера для образов
     * на машине нет, узел молчит, документа не нашлось — над настоящей
     * причиной вставало окно ввода пина и выдавало её за неверный пин,
     * а верный введённый пин при этом стирался: следующая попытка
     * начиналась с того же вопроса, и так по кругу.
     *
     * Вошедшего кассира это не касается: его пин узел принял на входе.
     */
    fun refused(again: () -> Unit) {
        if (session.pin.isNotBlank() || entered.isBlank()) return
        if (!pinRefused()) return
        entered = ""
        request = PinRequest(asked, again)
    }

    /** Отказал ли узел из-за пина: решает код отказа, а не сам факт отказа. */
    private fun pinRefused(): Boolean {
        val message = session.lastMessage
        return message is Message.Refusal && message.code in PIN_REFUSED
    }

    /** Владелец ввёл пин: работа продолжается с того места, где встала. */
    fun adopt(pin: String) {
        val pending = request?.again
        entered = pin
        request = null
        pending?.invoke()
    }

    /** Владелец закрыл окно: ничего не рисуем и ничего не запоминаем. */
    fun cancel() {
        request = null
    }

    /** Пин забывается вместе с рабочим днём. */
    fun forget() {
        entered = ""
        request = null
    }
}

/**
 * Открытый запрос пина.
 *
 * Название кассы обязательно: пин у касс разный, и владелец должен
 * видеть, к какой именно его спрашивают.
 */
data class PinRequest(val kkmTitle: String, val again: () -> Unit)
