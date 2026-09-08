package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
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
import kz.mybrain.superkassa.desktop.server.cabinet.registrationCard
import kz.mybrain.superkassa.desktop.server.cabinet.registrationCardPdf
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Регистрационная карта кассы.
 *
 * Карту выдаёт КГД после постановки на учёт; кабинет хранит её и печатает
 * в PDF. Пока карты нет, раздел так и говорит: погашенная кнопка
 * «Сохранить PDF» над несуществующим документом обещала бы то, чего нет.
 */
@Composable
fun RegistrationCardBlock(cabinet: CabinetSession, texts: CabinetTexts, register: CabinetRegister) {
    val scope = rememberCoroutineScope()
    var card by remember(register.id) { mutableStateOf<RegistrationCard?>(null) }

    // Признак наличия карты приходит в карточке кассы, а она догружается
    // после списка: без него в ключе блок читал признак один раз, до
    // загрузки, и у поставленной на учёт кассы говорил «карты нет».
    LaunchedEffect(register.id, register.registrationCardAvailable, cabinet.token) {
        val token = cabinet.token ?: return@LaunchedEffect
        card = if (register.registrationCardAvailable) {
            cabinet.guard { cabinet.client.registrationCard(token, register.id) }
        } else {
            null
        }
    }

    val issued = card
    if (issued == null) {
        EmptyState(AppIcons.print, texts.cardMissing, texts.cardMissingHint)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DetailLine(statusTitle(issued.status, texts), cabinetMoment(issued.updatedAt))
        FilledTonalButton(
            enabled = !cabinet.busy,
            onClick = { scope.launch { savePdf(cabinet, register) } }
        ) { Text(texts.savePdf) }
    }
}

/** Просит место на диске и кладёт туда карту. */
private suspend fun savePdf(cabinet: CabinetSession, register: CabinetRegister) {
    val token = cabinet.token ?: return
    val bytes = cabinet.guard { cabinet.client.registrationCardPdf(token, register.id) } ?: return
    val target = askWhereToSave("registration-card-${register.kkmId}.pdf") ?: return
    target.writeBytes(bytes)
}
