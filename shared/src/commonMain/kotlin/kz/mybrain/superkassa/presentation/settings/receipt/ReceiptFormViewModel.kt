package kz.mybrain.superkassa.presentation.settings.receipt

import androidx.lifecycle.ViewModel
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingRequest
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.common.model.whileBusy
import kz.mybrain.superkassa.strings.api.textsOf

/** Что владелец меняет в печатной форме. Пустые действия — для снимков вида. */
interface ReceiptFormActions {
    fun chooseLanguage(language: ReceiptLanguage) = Unit

    fun chooseLayout(code: String) = Unit

    fun switchOfdAds(on: Boolean) = Unit

    fun typeLine(line: ReceiptLine, text: String) = Unit

    fun saveLines() = Unit
}

/**
 * Печатная форма кассы.
 *
 * Это настройки кассы, а не рабочего места: чек печатается одинаково,
 * с какого бы компьютера его ни пробили. Язык, макет и реклама уходят
 * в кассу сразу, свои строки — кнопкой: их девять, и набирают их долго.
 */
class ReceiptFormViewModel(
    private val cases: ReceiptFormCases,
    private val talk: Talk
) : ViewModel(), ReceiptFormActions {
    private val screen = MutableStateFlow(ReceiptFormUiState())
    private val busy = Busy()

    val state: StateFlow<ReceiptFormUiState> = screen.asStateFlow()

    init {
        follow(cases.observe().map { it.kkm }.distinctUntilChanged(), ::onKkm)
        follow(busy.active) { on -> screen.update { it.copy(busy = on) } }
    }

    override fun chooseLanguage(language: ReceiptLanguage) = save(screen.value.branding.copy(language = language))

    override fun chooseLayout(code: String) =
        save(screen.value.branding.copy(paperWidthMm = PrintLayout.millimetresOf(code)))

    override fun switchOfdAds(on: Boolean) = save(screen.value.branding.copy(printOfdTicketAds = on))

    override fun typeLine(line: ReceiptLine, text: String) = screen.update { it.typed(line, text) }

    /** Набранное забывается только после согласия кассы: иначе девять строк набирали бы заново. */
    override fun saveLines() = save(screen.value.edited) { screen.update { it.saved() } }

    private fun save(changed: ReceiptBrandingRequest, onSaved: () -> Unit = {}) {
        whileBusy(busy) {
            val texts = textsOf(talk.language()).common.settings
            cases.save(changed).shown(texts.printForm, "update branding", talk) ?: return@whileBusy
            onSaved()
            talk.done(texts.printFormSaved)
        }
    }

    /**
     * Касса сменилась: строки, набранные для прежней, к новой не относятся,
     * но и не теряются — вернувшись к ней, владелец видит набранное.
     */
    private suspend fun onKkm(kkm: KkmResponse?) {
        screen.update { now ->
            if (now.kkm?.kkmId == kkm?.kkmId) {
                now.copy(kkm = kkm)
            } else {
                ReceiptFormUiState(kkm = kkm, paperWidths = now.paperWidths, drafts = now.drafts, busy = busy.now)
            }
        }
        if (kkm == null || screen.value.paperWidths.isNotEmpty()) return
        val widths = cases.paperWidths()
        if (widths is Answer.Done) screen.update { it.copy(paperWidths = widths.value) }
    }
}
