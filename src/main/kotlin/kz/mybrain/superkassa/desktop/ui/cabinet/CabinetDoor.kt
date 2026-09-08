package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Кабинет ОФД, открытый с экрана входа.
 *
 * Владелец приходит в кабинет до всякой кассы: пока она не заведена,
 * пина кассира не существует, а завести её без кабинета нельзя. Поэтому
 * дверь в кабинет стоит рядом с входом кассира, а не за ним.
 *
 * Экран тот же, что и в разделе после входа, — добавлен только возврат:
 * два одинаковых кабинета разошлись бы на первой же правке.
 */
@Composable
fun CabinetDoor(session: Session, cabinet: CabinetSession, onBack: () -> Unit) {
    val texts = LocalStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(AppIcons.back, contentDescription = texts.settings.back)
            }
            Text(
                text = cabinetTexts(session.language).title,
                style = MaterialTheme.typography.titleLarge
            )
        }
        CabinetScreen(session, cabinet)
    }
}
