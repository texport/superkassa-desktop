package kz.mybrain.superkassa.presentation.cabinet.applications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.component.PlacePicker
import kz.mybrain.superkassa.presentation.common.picker.WideChoiceSegments
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Что нужно уточнить у выбранного вида заявления.
 *
 * Отдельно от самой подачи: та читается сверху вниз — правила, кнопка,
 * итог, — а здесь поля трёх разных заявлений, и вместе файл перестал
 * укладываться в одну мысль.
 */
@Composable
internal fun ApplicationFields(
    kind: ActionKind,
    texts: CabinetTexts,
    language: Language,
    places: List<RetailPlace>,
    placeId: String,
    reason: DeregistrationReason,
    comment: String,
    onPlace: (String) -> Unit,
    onReason: (DeregistrationReason) -> Unit,
    onComment: (String) -> Unit
) {
    when (kind) {
        // Постановке на учёт уточнять нечего: всё нужное уже в паспорте кассы.
        ActionKind.Registration -> Unit
        // Точка выбирается из списка компании: прежде здесь стоял ввод
        // идентификатора, а взять его владельцу было неоткуда. Выбирается
        // набором: у сети точек тысячи, и перебрать их глазами нельзя.
        // О том, что точка не выбрана, говорит строка под кнопкой подачи:
        // в поле набора её место занимает сам набор.
        ActionKind.Reregistration -> PlacePicker(
            label = texts.newPlace,
            texts = texts,
            language = language,
            places = places,
            selected = places.firstOrNull { it.id == placeId },
            onSelect = { onPlace(it.id) }
        )
        ActionKind.Deregistration -> DeregistrationFields(texts, reason, comment, onReason, onComment)
    }
}

/** Причина и пояснение к снятию с учёта. */
@Composable
private fun DeregistrationFields(
    texts: CabinetTexts,
    reason: DeregistrationReason,
    comment: String,
    onReason: (DeregistrationReason) -> Unit,
    onComment: (String) -> Unit
) {
    // Причина и пояснение — строками во всю ширину карточки, одна под другой.
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        WideChoiceSegments(
            options = DeregistrationReason.entries,
            selected = reason,
            label = { it.title(texts) },
            onSelect = onReason
        )
        OutlinedTextField(
            value = comment,
            onValueChange = onComment,
            label = { Text(texts.comment) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
