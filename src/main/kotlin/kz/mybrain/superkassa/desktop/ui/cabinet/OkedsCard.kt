package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.Oked
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.components.underFieldLabel
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Виды деятельности: список с одним основным.
 *
 * Основной ОКЭД ровно один — он уходит в заявление; выбор основного
 * снимает пометку с прежнего, а не даёт поставить вторую. Пустой список
 * тоже допустим: у только что заведённой компании их ещё нет.
 */
@Composable
fun OkedsCard(texts: CabinetTexts, okeds: MutableList<Oked>, busy: Boolean, onSave: () -> Unit) {
    SectionCard(
        title = texts.okeds,
        trailing = { Text(okeds.size.toString(), style = MaterialTheme.typography.labelLarge) }
    ) {
        if (okeds.isEmpty()) {
            EmptyState(AppIcons.settings, texts.okedsEmpty, texts.okedsEmptyHint)
        }
        okeds.toList().forEachIndexed { at, oked ->
            OkedRow(oked, texts, striped = at % STRIPE == 1, onPrimary = { markPrimary(okeds, oked) }) {
                okeds.removeAt(at)
            }
        }
        // Первый заведённый вид становится основным сам: заявление без
        // основного ОКЭД кабинет не примет, а выбирать из одного нечего.
        OkedAddRow(texts, okeds.map { it.code }) { okeds.add(it.copy(primary = okeds.isEmpty())) }
        BusyButton(text = texts.saveOkeds, busy = busy, onClick = onSave)
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
    striped: Boolean,
    onPrimary: () -> Unit,
    onRemove: () -> Unit
) {
    RecordRow(
        title = oked.name?.takeIf { it.isNotBlank() } ?: oked.code,
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
 * Строка заведения нового вида деятельности.
 *
 * @param known коды, которые уже в списке: повторный кабинет не примет.
 */
@Composable
private fun OkedAddRow(texts: CabinetTexts, known: List<String>, onAdd: (Oked) -> Unit) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.Top
    ) {
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text(texts.okedCode) },
            supportingText = { Text(texts.required) },
            singleLine = true,
            modifier = Modifier.width(Sizes.fieldPin)
        )
        // Наименование обязательно: кабинет отвергает вид деятельности
        // без него, и прежде отказ приходил уже после нажатия «Сохранить».
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(texts.okedName) },
            supportingText = { Text(texts.required) },
            singleLine = true,
            modifier = Modifier.width(Sizes.fieldName)
        )
        TextButton(
            enabled = code.isNotBlank() && name.isNotBlank() && code.trim() !in known,
            modifier = Modifier.underFieldLabel(),
            onClick = {
                onAdd(Oked(code = code.trim(), name = name.trim(), primary = false))
                code = ""
                name = ""
            }
        ) { Text(texts.addOked) }
    }
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

/** Затеняется каждая вторая строка списка. */
private const val STRIPE = 2
