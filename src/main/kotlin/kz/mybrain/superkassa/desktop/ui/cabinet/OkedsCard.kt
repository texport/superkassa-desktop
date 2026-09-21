package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.Oked
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.CollapsibleCard
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.components.stripedAt
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Виды деятельности: список с одним основным.
 *
 * Основной ОКЭД ровно один — он уходит в заявление; выбор основного
 * снимает пометку с прежнего, а не даёт поставить вторую. Пустой список
 * тоже допустим: у только что заведённой компании их ещё нет.
 */
@Composable
fun OkedsCard(
    texts: CabinetTexts,
    okeds: MutableList<Oked>,
    busy: Boolean,
    title: (Oked) -> String,
    onSave: () -> Unit
) {
    SectionCard(title = texts.okeds, info = texts.hints.okeds) {
        if (okeds.isEmpty()) {
            EmptyState(AppIcons.settings, texts.okedsEmpty, texts.hints.okedsEmpty)
        }
        okeds.toList().forEachIndexed { at, oked ->
            OkedRow(
                oked,
                texts,
                title = title(oked),
                striped = stripedAt(at),
                onPrimary = { markPrimary(okeds, oked) }
            ) {
                okeds.removeAt(at)
            }
        }
        // На пустом списке кнопка гаснет: кабинет требует ровно один
        // основной вид и пустой набор отвергает. Прежде главным действием
        // пустой карточки стояло сохранение того, чего нет, а отказ
        // приходил английской строкой сервера.
        BusyButton(text = texts.saveOkeds, busy = busy, enabled = okeds.isNotEmpty(), onClick = onSave)
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
fun AddOkedCard(session: Session, cabinet: CabinetSession, texts: CabinetTexts, okeds: MutableList<Oked>) {
    var expanded by remember { mutableStateOf(false) }
    CollapsibleCard(
        title = texts.addOked,
        expanded = expanded,
        onToggle = { expanded = !expanded }
    ) {
        OkedPicker(session, cabinet, texts, okeds.map { it.code }) { chosen ->
            okeds.add(chosen.copy(primary = okeds.isEmpty()))
            expanded = false
        }
    }
}

/**
 * Одна строка вида деятельности.
 *
 * Наименование стоит главным, код — служебной строкой под ним: владелец
 * ищет «розничная торговля», а не 47.11. Прежде код и название шли одной
 * строкой через точку и читались как единое имя.
 */
@Composable
private fun OkedRow(
    oked: Oked,
    texts: CabinetTexts,
    title: String,
    striped: Boolean,
    onPrimary: () -> Unit,
    onRemove: () -> Unit
) {
    RecordRow(
        title = title,
        subtitle = oked.code,
        striped = striped,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // У основного — пометка, у прочих — действие. Прежде рядом
                // с каждой строкой стояла кнопка «Основной», и список читался
                // так, будто основными объявлены все сразу.
                if (oked.primary) {
                    Chip(texts.primaryOked, MaterialTheme.colorScheme.primary)
                } else {
                    TextButton(onClick = onPrimary) { Text(texts.makePrimary) }
                }
                IconButton(onClick = onRemove) {
                    Icon(AppIcons.close, contentDescription = texts.remove)
                }
            }
        }
    )
}

/**
 * Пометить один вид основным, сняв пометку с прежнего.
 *
 * Список переписывается целиком: править элемент на месте у списка
 * состояния Compose нельзя — перерисовки не будет.
 */
private fun markPrimary(okeds: MutableList<Oked>, chosen: Oked) {
    val marked = okeds.map { it.copy(primary = it.code == chosen.code) }
    okeds.clear()
    okeds.addAll(marked)
}
