package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.askWhereToSave
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RegistrationCard
import kz.mybrain.superkassa.desktop.server.cabinet.RegistrationCardVersion
import kz.mybrain.superkassa.desktop.server.cabinet.registrationCardVersion
import kz.mybrain.superkassa.desktop.server.cabinet.registrationCardVersionPdf
import kz.mybrain.superkassa.desktop.server.cabinet.registrationCardVersions
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.SectionTitle
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Версии регистрационной карты кассы.
 *
 * Карта отражает состояние кассы на сегодня: перерегистрация переписывает
 * в ней адрес, РКА и торговую точку, и прежней записи в действующей карте
 * не остаётся. А спрашивают обычно именно прежнюю — ею подтверждают, где
 * касса стояла в те дни, за которые пришла проверка. Кабинет версии
 * хранит и отдаёт списком; приложение показывало только действующую.
 *
 * Новые версии сверху: последняя перерегистрация нужнее той, что была
 * три года назад.
 */
@Composable
fun RegistrationCardVersions(cabinet: CabinetSession, texts: CabinetTexts, register: CabinetRegister) {
    val scope = rememberCoroutineScope()
    var versions by remember(register.id) { mutableStateOf<List<RegistrationCardVersion>?>(null) }
    var asked by remember(register.id) { mutableStateOf(false) }
    LaunchedEffect(register.id, cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        versions = cabinet.guard { cabinet.client.registrationCardVersions(token, register.id) }
        asked = true
    }
    var chosen: Int? by remember(register.id) { mutableStateOf(null) }
    val rows = versions.orEmpty().sortedByDescending { it.version }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        SectionTitle(texts.cardVersions)
        ScreenSlot(versionsState(asked, rows, texts), dense = true) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
                rows.forEach { version ->
                    VersionRow(
                        cabinet = cabinet,
                        texts = texts,
                        version = version,
                        chosen = chosen == version.version,
                        onOpen = { chosen = if (chosen == version.version) null else version.version },
                        onSave = { scope.launch { saveVersionPdf(cabinet, register, version.version) } }
                    )
                    if (chosen == version.version) VersionCard(cabinet, texts, register, version.version)
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
        rows.isEmpty() -> ScreenState.Empty(AppIcons.print, texts.cardVersionsEmpty, texts.cardVersionsEmptyHint)
        else -> ScreenState.Ready
    }

/**
 * Одна версия: срок действия, чем открыта и закрыта, что менялось.
 *
 * Строка раскрывается: что именно было записано в карте той версии —
 * адрес, точка, модель, — кабинет отдаёт отдельным обращением, и
 * спрашивать его за все версии разом ради одной незачем.
 */
@Composable
private fun VersionRow(
    cabinet: CabinetSession,
    texts: CabinetTexts,
    version: RegistrationCardVersion,
    chosen: Boolean,
    onOpen: () -> Unit,
    onSave: () -> Unit
) {
    RecordRow(
        title = "${texts.cardVersion} ${version.version}",
        subtitle = versionPeriod(version),
        selected = chosen,
        onClick = onOpen,
        support = { VersionFacts(version, texts) },
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (version.open) Chip(texts.cardCurrentVersion, StatusColors.delivered)
                TextButton(enabled = !cabinet.busy, onClick = onSave) { Text(texts.savePdf) }
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
private fun VersionCard(cabinet: CabinetSession, texts: CabinetTexts, register: CabinetRegister, version: Int) {
    var card by remember(register.id, version) { mutableStateOf<RegistrationCard?>(null) }
    var asked by remember(register.id, version) { mutableStateOf(false) }
    LaunchedEffect(register.id, version, cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        card = cabinet.guard { cabinet.client.registrationCardVersion(token, register.id, version) }
        asked = true
    }
    val shown = card
    val state = when {
        !asked -> ScreenState.Working
        shown == null -> ScreenState.Empty(AppIcons.print, texts.cardMissing, texts.cardMissingHint)
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
            DetailLine(texts.address, shown.address)
            DetailLine(texts.model, shown.modelName)
        }
    }
}

/** Чем версия открыта и закрыта и что в ней стало другим. */
@Composable
private fun VersionFacts(version: RegistrationCardVersion, texts: CabinetTexts) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
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
    val from = cabinetDay(version.validFrom)
    val until = version.validTo?.takeIf { it.isNotBlank() } ?: return from
    return "$from — ${cabinetDay(until)}"
}

/** Просит место на диске и кладёт туда карту нужной версии. */
private suspend fun saveVersionPdf(cabinet: CabinetSession, register: CabinetRegister, version: Int) {
    val token = cabinet.token ?: return
    val bytes = cabinet.guard { cabinet.client.registrationCardVersionPdf(token, register.id, version) } ?: return
    val target = askWhereToSave("registration-card-${register.kkmId}-v$version.pdf") ?: return
    target.writeBytes(bytes)
}
