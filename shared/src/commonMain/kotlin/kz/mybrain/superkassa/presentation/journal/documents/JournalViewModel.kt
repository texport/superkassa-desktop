package kz.mybrain.superkassa.presentation.journal.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.document.model.number
import kz.mybrain.superkassa.domain.journal.model.ReceiptDeliveryRules
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.presentation.common.document.JournalQuery
import kz.mybrain.superkassa.presentation.common.message.words
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.followSeat
import kz.mybrain.superkassa.presentation.common.model.latest
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.journal.PageOutcome
import kz.mybrain.superkassa.presentation.journal.outcome
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Журнал документов кассы за выбранный срок.
 *
 * Срок читается сам при его смене — экран, который до нажатия кнопки
 * показывает пустоту, ничему не учит, — и дочитывается страницами:
 * за месяц оживлённой кассы документов десятки тысяч.
 *
 * Открытый чек показывает, дошёл ли он до покупателя по каждому каналу,
 * и отправляет его ещё раз туда, куда не дошёл. Отказ кассы остаётся
 * в окне чека её словами: строка сообщений под окном не видна.
 */
class JournalViewModel(private val cases: JournalCases, private val talk: Talk) : ViewModel(), JournalActions {
    private val screen = MutableStateFlow(JournalUiState())
    private val reading = latest()
    private val delivering = latest()

    val state: StateFlow<JournalUiState> = screen.asStateFlow()

    init {
        // Сел другой кассир или ушёл: прочитанный срок принадлежал прежнему.
        followSeat(cases.observe(), seated = { restart() })
        viewModelScope.launch {
            cases.readDocumentTypes()?.let { types -> screen.update { it.copy(documentTypes = types) } }
        }
    }

    override fun show(view: HistoryView) = screen.update { it.copy(view = view) }

    /**
     * Новый срок читается с начала.
     *
     * Прошлый итог к новому сроку отношения не имеет: иначе «есть ещё»
     * от прежнего срока переживало бы переход на день без документов.
     */
    override fun choose(period: JournalPeriod) {
        screen.update { it.copy(period = period) }
        restart()
    }

    override fun filter(query: JournalQuery) = screen.update { it.copy(query = query) }

    override fun more() {
        if (screen.value.loading) return
        reading.restart { read() }
    }

    override fun open(key: String) {
        val document = screen.value.documents.firstOrNull { it.id == key } ?: return
        if (!ReceiptDeliveryRules.delivers(document)) return
        val number = document.number?.toString() ?: Glyphs.DASH
        val opened = ReceiptDeliveryUi(document.id, number, awaitingBfd = ReceiptDeliveryRules.awaitsBfd(document))
        screen.update { it.copy(delivery = opened) }
        delivering.restart { delivered(document.id, "read receipt delivery") { cases.readDelivery(document.id) } }
    }

    /** Повтор по каналам, где доставка не удалась; ответ кассы сразу становится списком окна. */
    override fun resend() {
        val shown = screen.value.delivery?.takeIf { it.canResend } ?: return
        screen.update { it.copy(delivery = shown.copy(sending = true, problem = null)) }
        delivering.restart {
            delivered(shown.documentId, "resend receipt") {
                cases.resendReceipt(shown.documentId, shown.deliveries) ?: Answer.Done(shown.deliveries)
            }
        }
    }

    override fun closeDelivery() {
        delivering.cancel()
        screen.update { it.copy(delivery = null) }
    }

    /** Ответ о доставке чека [id] — в окно, если оно ещё открыто на этом чеке. */
    private suspend fun delivered(
        id: String,
        action: String,
        request: suspend () -> Answer<List<ReceiptDeliveryResponse>>
    ) {
        val answer = request()
        val problem = problemOf(answer, action, talk)
        screen.update { now ->
            val open = now.delivery?.takeIf { it.documentId == id } ?: return@update now
            val deliveries = (answer as? Answer.Done)?.value ?: open.deliveries
            now.copy(delivery = open.copy(deliveries = deliveries, reading = false, sending = false, problem = problem))
        }
    }

    private fun restart() {
        delivering.cancel()
        screen.update {
            it.copy(documents = emptyList(), page = PageOutcome.unread, loading = true, delivery = null)
        }
        reading.restart { read() }
    }

    /** Следующая страница срока; касса без кассира — не пустой срок, а непрочитанный. */
    private suspend fun read() {
        if (!cases.observe().value.signedIn) {
            screen.update { it.copy(page = PageOutcome.unreadAfter(it.page), loading = false) }
            return
        }
        screen.update { it.copy(loading = true) }
        val now = screen.value
        val what = textsOf(talk.language()).common.sections.history
        val page = cases.readPeriodDocuments(now.bounds(), now.documents).shown(what, "read journal", talk)
        screen.update { latest ->
            val (documents, outcome) = page.outcome(latest.documents, latest.page)
            latest.copy(documents = documents, page = outcome, loading = false)
        }
    }
}

/** Отказ — словами кассы, сбой — словами окна; в журнал — код и имя, а не слова. */
private fun problemOf(answer: Answer<*>, action: String, talk: Talk): String? = when (answer) {
    is Answer.Done -> null
    is Answer.Refused -> answer.words(talk.language()).also { talk.journal.warn("$action: refused ${answer.code}") }
    is Answer.Failed -> textsOf(talk.language()).journal.delivery.unread.also {
        talk.journal.failure("$action: failed ${answer.reason}")
    }
}
