package kz.mybrain.superkassa.presentation.cabinet.register.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RegistrationCardVersion
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationCard
import kz.mybrain.superkassa.presentation.cabinet.actionTitle
import kz.mybrain.superkassa.presentation.cabinet.cardFieldTitle
import kz.mybrain.superkassa.presentation.common.format.Dates
import kz.mybrain.superkassa.presentation.common.list.RecordRow
import kz.mybrain.superkassa.presentation.common.section.DetailLine
import kz.mybrain.superkassa.presentation.common.section.SectionTitle
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Версии регистрационной карты кассы. Зачем они нужны владельцу —
 * в [RegistrationCardVersion].
 *
 * Новые версии сверху: последняя перерегистрация нужнее той, что была
 * три года назад.
 */
@Composable
fun RegistrationCardVersions(
    state: RegistrationCardUiState,
    texts: CabinetTexts,
    register: CabinetRegister,
    busy: Boolean,
    model: RegistrationCardViewModel
) {
    val rows = state.versions.orEmpty().sortedByDescending { it.version }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        SectionTitle(texts.cardVersions)
        ScreenSlot(versionsState(state.versions != null, rows, texts), dense = true) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
                rows.forEach { version ->
                    val chosen = state.opened == version.version
                    VersionRow(
                        texts = texts,
                        line = VersionLine(version, chosen, busy),
                        onOpen = { model.open(register, version.version) },
                        onSave = { model.save(register, version.version) }
                    )
                    if (chosen) VersionCard(texts, version.version, state.versionCards)
                }
            }
        }
    }
}

/**
 * Что стоит на месте списка версий.
 *
 * Пустой список — не пустота: версия появляется при перерегистрации
 * и при снятии с учёта, и у кассы, с которой ничего этого не случалось,
 * список пуст по существу.
 */
private fun versionsState(asked: Boolean, rows: List<RegistrationCardVersion>, texts: CabinetTexts): ScreenState =
    when {
        !asked -> ScreenState.Working
        rows.isEmpty() -> ScreenState.Empty(AppIcons.print, texts.cardVersionsEmpty, texts.hints.cardVersionsEmpty)
        else -> ScreenState.Ready
    }

/** Строка версии: какая это версия, раскрыта ли она и занят ли кабинет. */
private data class VersionLine(val version: RegistrationCardVersion, val chosen: Boolean, val busy: Boolean)

/**
 * Одна версия: срок действия, чем открыта и закрыта, что менялось.
 *
 * Строка раскрывается: что именно было записано в карте той версии —
 * адрес, точка, модель, — кабинет отдаёт отдельным обращением, и
 * спрашивать его за все версии разом ради одной незачем.
 */
@Composable
private fun VersionRow(texts: CabinetTexts, line: VersionLine, onOpen: () -> Unit, onSave: () -> Unit) {
    val version = line.version
    // Срок стоит внутри служебной части: строка списка показывает либо
    // подпись, либо служебную часть, и подпись со сроком молча пропадала —
    // владелец не видел того, за чем в этот раздел и приходит.
    RecordRow(
        title = "${texts.cardVersion} ${version.version}",
        selected = line.chosen,
        onClick = onOpen,
        support = { VersionFacts(version, texts) },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (version.open) Chip(texts.cardCurrentVersion, StatusColors.delivered)
                TextButton(enabled = !line.busy, onClick = onSave) { Text(texts.savePdf) }
            }
        }
    )
}

/**
 * Карта выбранной версии целиком.
 *
 * Список отвечает, когда версия действовала и что в ней поменялось;
 * на вопрос «что в ней было записано» отвечает сама карта — её кабинет
 * отдаёт по номеру версии.
 */
@Composable
private fun VersionCard(texts: CabinetTexts, version: Int, cards: Map<Int, RegistrationCard?>) {
    val shown = cards[version]
    val state = when {
        version !in cards -> ScreenState.Working
        shown == null -> ScreenState.Empty(AppIcons.print, texts.cardMissing, texts.hints.cardMissing)
        else -> ScreenState.Ready
    }
    ScreenSlot(state, dense = true) {
        if (shown == null) return@ScreenSlot
        Column(
            modifier = Modifier.padding(start = Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            DetailLine(texts.registrationNumber, shown.registrationNumber)
            DetailLine(texts.placeName, shown.retailPlaceName)
            // Адрес точки, а не сетевой адрес кабинета.
            DetailLine(texts.placeAddress, shown.address)
            DetailLine(texts.model, shown.modelName)
        }
    }
}

/** Когда версия действовала, чем открыта и закрыта и что в ней стало другим. */
@Composable
private fun VersionFacts(version: RegistrationCardVersion, texts: CabinetTexts) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
        // Первой строкой и без подписи: диапазон дат говорит сам за себя.
        Text(
            text = versionPeriod(version),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DetailLine(texts.openedAt, version.openedBy?.let { actionTitle(it, texts) })
        DetailLine(texts.closedAt, version.closedBy?.let { actionTitle(it, texts) })
        DetailLine(texts.cardChanged, changedWords(version, texts))
    }
}

/** Что менялось — словами кабинета; нечему меняться у первой версии. */
private fun changedWords(version: RegistrationCardVersion, texts: CabinetTexts): String? = version.changed
    .joinToString(", ") { cardFieldTitle(it, texts) }
    .takeIf { it.isNotBlank() }

/** Срок действия версии: у действующей конца ещё нет. */
private fun versionPeriod(version: RegistrationCardVersion): String {
    val from = Dates.dayOf(version.validFrom)
    val until = version.validTo?.takeIf { it.isNotBlank() } ?: return from
    return "$from — ${Dates.dayOf(until)}"
}
