package kz.mybrain.superkassa.presentation.cabinet.company

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import kz.mybrain.superkassa.designsystem.button.BusyButton
import kz.mybrain.superkassa.designsystem.list.ListRows
import kz.mybrain.superkassa.designsystem.list.RecordRow
import kz.mybrain.superkassa.designsystem.section.BarAction
import kz.mybrain.superkassa.designsystem.section.CollapsibleCard
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Виды деятельности: список с одним основным.
 *
 * Основной ОКЭД ровно один — он уходит в заявление; выбор основного
 * снимает пометку с прежнего, а не даёт поставить вторую. Пустой список
 * тоже допустим: у только что заведённой компании их ещё нет.
 *
 * Сохранять нечего, пока список остаётся тем, что отдал кабинет: залитая
 * кнопка на нетронутой карточке обещает работу, которой нет, и отправляла
 * в кабинет то же самое (см. [CompanyUiState.changed]).
 */
@Composable
internal fun OkedsCard(
    texts: CabinetTexts,
    state: CompanyUiState,
    busy: Boolean,
    title: (Oked) -> String,
    actions: CompanyActions
) {
    val okeds = state.okeds
    SectionCard(title = texts.company.okeds, info = texts.hints.okeds) {
        if (okeds.isEmpty()) {
            EmptyState(AppIcons.settings, texts.company.okedsEmpty, texts.hints.okedsEmpty)
        }
        ListRows {
            okeds.forEachIndexed { at, oked ->
                if (at > 0) HorizontalDivider()
                OkedRow(oked, texts, title(oked), { actions.markPrimary(oked.code) }) { actions.remove(oked.code) }
            }
        }
        // На пустом списке кнопка гаснет: кабинет требует ровно один
        // основной вид и пустой набор отвергает. Прежде главным действием
        // пустой карточки стояло сохранение того, чего нет, а отказ
        // приходил английской строкой сервера.
        //
        // Гаснет она и на нетронутом списке: сохранять то же, что пришло
        // от кабинета, незачем.
        BusyButton(text = texts.company.saveOkeds, busy = busy, enabled = state.changed, onClick = actions::save)
    }
}

/**
 * Заведение вида деятельности — отдельной сворачиваемой карточкой.
 *
 * Свёрнута по умолчанию и стоит под списком: развёрнутая форма уезжала
 * вниз с каждым добавленным видом, и владелец, заводя пятый, пролистывал
 * до неё весь список заново. Свёрнутая, она остаётся строкой заголовка
 * на виду.
 *
 * Вид выбирается из классификатора, а не набирается: ИСНА сверяется
 * с тем же классификатором, и набранное руками возвращалось отказом.
 *
 * Первый заведённый вид становится основным сам: заявление без основного
 * ОКЭД кабинет не примет, а выбирать из одного нечего.
 */
@Composable
internal fun AddOkedCard(search: OkedSearch, language: Language, texts: CabinetTexts, actions: CompanyActions) {
    var expanded by remember { mutableStateOf(false) }
    CollapsibleCard(
        title = texts.company.addOked,
        expanded = expanded,
        onToggle = {
            expanded = !expanded
            // Пустая строка отдаёт начало классификатора: с неё поиск
            // и начинается, но только когда владелец раскрыл карточку.
            if (expanded && !search.searched) actions.search("")
        }
    ) {
        OkedPicker(search, language, texts, actions) { entry, title ->
            actions.add(entry, title)
            expanded = false
        }
    }
}

/**
 * Одна строка вида деятельности — пункт одиночного выбора основного.
 *
 * Основной отмечен переключателем в начале строки, как выбор одного из
 * списка по Material 3; нажатие на строку делает её основной. Прежде
 * у основного стояла плашка, у прочих — кнопка «Сделать основным»,
 * а строки через одну были затенены: затенение читалось как пометка,
 * и основной вид выглядел выделенным иначе, чем соседний затенённый.
 *
 * Наименование стоит главным, код — служебной строкой под ним: владелец
 * ищет «розничная торговля», а не 47.11. У основного в служебной строке
 * сказано и это — для тех, кто не различает переключатели.
 */
@Composable
private fun OkedRow(
    oked: Oked,
    texts: CabinetTexts,
    title: String,
    onPrimary: () -> Unit,
    onRemove: () -> Unit
) {
    RecordRow(
        title = title,
        modifier = Modifier.semantics { role = Role.RadioButton },
        subtitle = if (oked.primary) "${texts.company.primaryOked} · ${oked.code}" else oked.code,
        // Формулировка классификатора длинная, и владелец выбирает вид
        // по словам, а не по коду: обрезанная на середине строка отнимала
        // ровно то, чем один вид отличается от соседнего.
        titleLines = OKED_TITLE_LINES,
        leading = { RadioButton(selected = oked.primary, onClick = onPrimary) },
        trailing = { BarAction(AppIcons.remove, texts.remove, onRemove) },
        onClick = onPrimary
    )
}

/** Сколько строк отводится формулировке вида деятельности. */
private const val OKED_TITLE_LINES = 2
