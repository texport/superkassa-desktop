package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.LabelledPicker
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
        // идентификатора, а взять его владельцу было неоткуда.
        ActionKind.Reregistration -> LabelledPicker(
            label = texts.newPlace,
            options = places,
            selected = places.firstOrNull { it.id == placeId },
            // Невыбранное названо невыбранным. Пустое поле с подписью
            // «Новая торговая точка» читалось как уже сделанный выбор,
            // и заявление уходило в кабинет за отказом.
            title = { it?.name ?: texts.pointNotChosen },
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
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        ChoiceSegments(
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
            modifier = Modifier.width(Sizes.fieldName)
        )
    }
}
