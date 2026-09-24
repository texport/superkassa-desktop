package kz.mybrain.superkassa.presentation.cabinet.company

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.OKED_PAGE
import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.domain.cabinet.model.OkedEntry
import kz.mybrain.superkassa.presentation.cabinet.CabinetReply
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.cabinetMessage
import kz.mybrain.superkassa.presentation.cabinet.component.askableQuery
import kz.mybrain.superkassa.presentation.cabinet.problem
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.theme.motion.Durations

/**
 * Компания владельца: карточка и виды деятельности.
 *
 * Название и БИН приходят из ЭЦП и правке не поддаются: их выдал КГД.
 * Виды деятельности владелец ведёт сам — от основного ОКЭД зависит, что
 * уйдёт в регистрационное заявление, — и выбирает их из классификатора:
 * ИСНА сверяется с тем же классификатором, и набранное руками возвращалось
 * отказом.
 */
class CompanyViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val screen = MutableStateFlow(CompanyUiState())
    private val cases = cabinet.useCases
    private var searching: Job? = null

    val state: StateFlow<CompanyUiState> = screen.asStateFlow()

    init {
        // Вошёл владелец — карточка его компании читается; вышел — забывается.
        viewModelScope.launch {
            cabinet.state.map { it.owner }.distinctUntilChanged().collect { owner ->
                screen.value = CompanyUiState()
                if (owner != null) load()
            }
        }
    }

    /** Перечитывает карточку компании; правка видов, не сохранённая владельцем, сбрасывается. */
    fun load() {
        viewModelScope.launch { read() }
    }

    /** Сохраняет виды деятельности; удалось — карточка перечитывается. */
    fun save() {
        val okeds = screen.value.okeds
        viewModelScope.launch {
            if (cabinet.work.run("save okeds") { cases.saveOkeds(okeds) } is CabinetReply.Done) read()
        }
    }

    /** Первый заведённый вид становится основным сам: выбирать из одного нечего. */
    fun add(entry: OkedEntry, title: String) = screen.update {
        val added = Oked(code = entry.code, name = title, primary = it.okeds.isEmpty())
        it.copy(okeds = it.okeds + added, search = OkedSearch())
    }

    fun remove(code: String) = screen.update { now -> now.copy(okeds = now.okeds.filterNot { it.code == code }) }

    /** Основной вид ровно один: выбор основного снимает пометку с прежнего. */
    fun markPrimary(code: String) = screen.update { now ->
        now.copy(okeds = now.okeds.map { it.copy(primary = it.code == code) })
    }

    /**
     * Поиск за набором: однобуквенный запрос кабинет отвергает, поэтому ищем
     * либо с пустой строки — она отдаёт начало классификатора, — либо от двух знаков.
     */
    fun search(query: String) {
        screen.update { it.copy(search = it.search.copy(query = query)) }
        val needle = query.trim()
        if (!askableQuery(needle)) return
        searching?.cancel()
        searching = viewModelScope.launch {
            if (needle.isNotEmpty()) delay(Durations.afterTyping)
            page(needle, from = 0)
        }
    }

    /** Следующая страница классификатора. */
    fun more() {
        val now = screen.value.search
        viewModelScope.launch { page(now.needle, from = now.taken) }
    }

    private suspend fun read() {
        val reply = cabinet.work.run("read company") { cases.readCompany() }
        val profile = reply.value
        if (profile == null) {
            val words = reply.problem?.let { cabinetMessage(it, cabinet.texts).words() }
            screen.update { it.copy(refused = words) }
            return
        }
        screen.update { it.copy(profile = profile, okeds = profile.okeds, refused = null) }
        readTitles(profile.okeds.map { it.code })
    }

    /**
     * Наименования видов по классификатору — по разу на код и молча:
     * без них строка показывает сохранённое наименование, и это не беда.
     */
    private suspend fun readTitles(codes: List<String>) {
        codes.filterNot { it in screen.value.titles }.forEach { code ->
            val entry = cabinet.work.quiet("read oked title") { cases.readOked(code) } ?: return@forEach
            screen.update { it.copy(titles = it.titles + (code to entry)) }
        }
    }

    /**
     * Просит у кабинета страницу и складывает её к показанному.
     *
     * Кабинет на стенде может смещения не понимать — тогда он отдаёт то же
     * начало списка, ничего нового в странице нет, и продолжение больше
     * не предлагается: обещать страницы, которых нет, нельзя.
     */
    private suspend fun page(needle: String, from: Int) {
        val got = cabinet.work.run("search okeds") { cases.searchOkeds(needle, from) }.value ?: return
        screen.update { now ->
            val known = now.okeds.map { it.code }.toSet()
            val shown = if (from == 0) emptySet() else now.search.found.map { it.code }.toSet()
            val fresh = got.items.filterNot { it.code in known || it.code in shown }
            val found = if (from == 0) fresh else now.search.found + fresh
            val ended = got.items.size < OKED_PAGE || (from > 0 && fresh.isEmpty())
            val search = now.search.copy(found = found, taken = from + got.items.size, total = got.total, ended = ended)
            now.copy(search = search.copy(searched = true))
        }
    }
}
