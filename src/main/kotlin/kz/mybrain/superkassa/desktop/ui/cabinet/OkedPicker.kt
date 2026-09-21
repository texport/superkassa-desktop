package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.OKEDS
import kz.mybrain.superkassa.desktop.server.cabinet.Oked
import kz.mybrain.superkassa.desktop.server.cabinet.OkedEntry
import kz.mybrain.superkassa.desktop.server.cabinet.okedSuggestions
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Durations
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Ввод вида деятельности: выбор из классификатора ОКЭД.
 *
 * Прежде код и наименование набирались руками, и в заявление в ИСНА уходило
 * написанное владельцем. Классификатор теперь держит кабинет, поэтому вид
 * деятельности выбирается из него: набранное ищется по коду и по части
 * наименования, а в компанию уходит ровно то, что стоит в классификаторе.
 *
 * Уже добавленный код в подсказках не показывается: такой список кабинет
 * отвергнет.
 *
 * @param known добавленные коды — их из подсказок убирают.
 */
@Composable
fun OkedPicker(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    known: List<String>,
    onAdd: (Oked) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<OkedEntry>>(emptyList()) }
    var total by remember { mutableStateOf(0L) }
    var taken by remember { mutableStateOf(0) }
    var ended by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val needle = query.trim()
    // Однобуквенный запрос кабинет отвергает, как и в адресном регистре:
    // ищем либо с пустой строки — она отдаёт начало классификатора, —
    // либо от двух знаков.
    val askable = askableQuery(needle)

    /**
     * Просит у кабинета страницу и складывает её к показанному.
     *
     * Смещение считается по числу полученных, а не показанных: уже
     * добавленные владельцем виды из показа убраны, и считай мы
     * по показанному — страницы разъехались бы и часть классификатора
     * оказалась бы пропущена.
     *
     * Кабинет на стенде может смещения не понимать — тогда он отдаёт
     * то же начало списка, ничего нового в странице нет и продолжение
     * больше не предлагается: обещать страницы, которых нет, нельзя.
     */
    suspend fun page(from: Int) {
        val token = cabinet.token ?: return
        val got = cabinet.guard { cabinet.client.okedSuggestions(token, needle, from) } ?: return
        val shown = if (from == 0) emptySet() else found.map { it.code }.toSet()
        val fresh = got.items.filterNot { it.code in known || it.code in shown }
        found = if (from == 0) fresh else found + fresh
        taken = from + got.items.size
        total = got.total
        ended = got.items.size < OKEDS || (from > 0 && fresh.isEmpty())
    }

    LaunchedEffect(needle) {
        if (!askable) return@LaunchedEffect
        if (needle.isNotEmpty()) delay(Durations.afterTyping)
        page(from = 0)
        searched = true
        open = touched && found.isNotEmpty()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        // Сколько классификатор держит сверх полученного. Кабинет, который
        // общего числа не сообщает, оставляет ноль — тогда о продолжении
        // говорит сама полная страница.
        val rest = (total - taken).coerceAtLeast(0)
        OkedSuggestions(
            texts = texts,
            query = query,
            found = found,
            open = open && found.isNotEmpty(),
            rest = rest.takeIf { !ended && (it > 0 || total == 0L) },
            title = { entry -> titleOf(session, entry) },
            onMore = { scope.launch { page(from = taken) } },
            onOpen = { open = it },
            onQuery = {
                query = it
                touched = true
                open = true
            }
        ) { entry ->
            open = false
            query = ""
            onAdd(Oked(code = entry.code, name = titleOf(session, entry)))
        }
        // «Классификатор длиннее показанного — уточните запрос» уместно
        // только при запросе. Пустая строка отдаёт начало классификатора,
        // и эта подсказка выходила сразу при раскрытии карточки, до единого
        // набранного знака: уточнять было нечего. До запроса стоит обычная
        // подсказка о том, чем искать.
        OkedHint(
            when {
                !searched || needle.isEmpty() -> texts.hints.okedSearch
                found.isEmpty() -> texts.okedNotFound
                !ended -> texts.okedNarrowSearch
                else -> texts.hints.okedSearch
            }
        )
    }
}

/** Наименование на языке интерфейса: в заявление уходит то, что видит владелец. */
internal fun titleOf(session: Session, entry: OkedEntry): String =
    addressIn(session.language, entry.name, entry.nameKz)
