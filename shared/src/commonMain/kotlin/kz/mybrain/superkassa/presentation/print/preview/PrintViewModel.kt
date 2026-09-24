package kz.mybrain.superkassa.presentation.print.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.domain.print.usecase.PrintTape
import kz.mybrain.superkassa.presentation.common.message.words
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.strings.common.stringsOf

/**
 * Печать, просмотр и сохранение печатной формы — одни на окно.
 *
 * Работа живёт в модели окна, а не на экране: касса рисует форму
 * секунду-другую, и кассир успевает уйти в другой раздел. Пока это делал
 * экран, его уход обрывал работу — полоска ожидания гасла, а форма
 * не открывалась.
 */
class PrintViewModel(private val cases: PrintCases, private val talk: Talk) : ViewModel() {
    private val screen = MutableStateFlow(PrintUiState())

    val state: StateFlow<PrintUiState> = screen.asStateFlow()

    private val drawer = PrintDrawer(cases, talk)

    /** Документ, открытый в просмотре: сохранение повторяет его в нужном виде. */
    private var shown: PrintSource? = null

    /** Касса, нарисовавшая открытую форму: печатают по ширине её ленты. */
    private var drawnBy: KkmResponse? = null

    /** Что повторить по кнопке в окне отказа. */
    private var again: (() -> Unit)? = null

    /**
     * Какой по счёту просмотр идёт сейчас.
     *
     * Окно закрывают, не дождавшись формы, и открывают следующую. Без счёта
     * запоздавшая картинка открывала бы окно заново — поверх того, чем
     * владелец уже занят.
     */
    private var generation = 0

    init {
        // Пин, введённый ради печатной формы, уходит вместе с кассиром:
        // он и жил только в памяти этого рабочего дня.
        follow(cases.observe().map { it.cashier }.distinctUntilChanged().drop(1)) {
            drawer.forget()
            screen.update { it.copy(pinFor = null) }
        }
    }

    /**
     * Открывает форму поверх любого раздела.
     *
     * Окно открывается сразу, до ответа кассы: без открытого окна нажатие
     * не отзывается ничем. Отказ окна не закрывает — владелец остаётся там,
     * где нажал, с причиной и повтором перед глазами.
     *
     * @param source что рисовать; `null` — формы у документа нет, и молчать
     *   об этом нельзя: владелец нажал и обязан узнать почему.
     * @param file как назвать файл, если форму сохранят.
     */
    fun preview(source: PrintSource?, file: String?) {
        if (source == null) return talk.done(stringsOf(talk.language()).preview.missing)
        shown = source
        screen.update { it.copy(savingName = file ?: source.name) }
        val at = ++generation
        val retry = { preview(source, file) }
        again = retry
        screen.update { it.copy(drawing = true, trouble = null) }
        viewModelScope.launch {
            val answer = drawer.render(source, PrintKind.Png, retry)
            if (at != generation) return@launch
            showDrawn(answer)
        }
    }

    /** Печатает документ, не открывая его. */
    fun print(source: PrintSource) {
        viewModelScope.launch {
            // Принтера нет вовсе — отказ сразу, а не после того, как касса нарисует форму.
            if (!cases.print.hasPrinter()) return@launch talk.printed(PrintTape.Result.NoPrinter, PRINT)
            val drawn = drawer.render(source, cases.print.kind) { print(source) }
            asked()
            val done = drawn?.shown(stringsOf(talk.language()).preview.print, PRINT, talk) ?: return@launch
            talk.printed(cases.print(done.kkm, done.bytes), PRINT)
        }
    }

    /**
     * Печатает то, что открыто в просмотре.
     *
     * Ленту на принтер кассы касса второй раз не рисует: она уже на экране.
     * Системному диалогу нужен документ, и форма рисуется для него заново.
     */
    fun printShown() {
        val image = screen.value.image
        val kkm = drawnBy
        when {
            image == null || kkm == null -> Unit
            cases.print.kind != PrintKind.Png -> shown?.let(::print)
            else -> viewModelScope.launch { talk.printed(cases.print(kkm, image), PRINT) }
        }
    }

    /**
     * Сохраняет открытую форму в файл.
     *
     * Вид файла берётся из настроек, а не с экрана: на экране всегда
     * картинка, а покупателю чаще нужен PDF. Форма рисуется заново
     * в выбранном виде.
     */
    fun saveShown() {
        val source = shown ?: return
        val kind = cases.kind()
        val name = "${screen.value.savingName ?: source.name}.${kind.extension}"
        viewModelScope.launch {
            val drawn = drawer.render(source, kind) { saveShown() }
            asked()
            val texts = stringsOf(talk.language()).preview
            val done = drawn?.shown(texts.save, SAVE, talk) ?: return@launch
            talk.kept(cases.keep(done.bytes, name, texts.save), SAVE)
        }
    }

    fun retry() {
        again?.invoke()
    }

    /**
     * Владелец закрыл окно просмотра.
     *
     * Закрывается и ожидание: форма, которую касса ещё рисует, экрана
     * уже не займёт.
     */
    fun close() {
        generation += 1
        screen.update { it.copy(image = null, drawing = false, trouble = null) }
    }

    fun enterPin(pin: String) {
        val work = drawer.adopt(pin)
        asked()
        work?.invoke()
    }

    fun cancelPin() {
        drawer.cancel()
        asked()
        if (screen.value.image == null) close()
    }

    /** Форма нарисована или касса отказала: окно показывает итог. */
    private suspend fun showDrawn(answer: Answer<PrintDrawer.Drawn>?) {
        asked()
        val texts = stringsOf(talk.language())
        val trouble = when (answer) {
            null -> null
            is Answer.Done -> null
            is Answer.Refused -> PrintTrouble(answer.words(talk.language()))
            is Answer.Failed -> PrintTrouble(null)
        }
        answer?.shown(texts.dashboard.printForm, "draw print form", talk)
        val drawn = (answer as? Answer.Done)?.value
        drawn?.let { drawnBy = it.kkm }
        val image = drawn?.bytes
        screen.update { it.copy(drawing = false, trouble = trouble, image = image ?: it.image) }
    }

    /** Окно пина открыто, если рисовальщик его спрашивает. */
    private fun asked() = screen.update { it.copy(pinFor = drawer.asking) }

    private companion object {
        const val PRINT = "print document"
        const val SAVE = "save document"
    }
}
