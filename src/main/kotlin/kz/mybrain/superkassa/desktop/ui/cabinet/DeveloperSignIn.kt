package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Вход без ЭЦП — средство отладки: кабинет пускает по ИИН и БИН
 * из заголовков.
 *
 * Показывается только при включённом режиме отладки. Кассиру и владельцу
 * этот вход не нужен вовсе, а на экране входа он выглядит как вторая
 * дверь, в которую пускают по любым двенадцати цифрам.
 *
 * Поля помнятся между запусками: при отладке кабинет открывается не один
 * раз, и набирать двенадцать цифр перед каждым разом незачем.
 */
@Composable
internal fun DeveloperSignIn(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    var iin by remember { mutableStateOf(session.preferences.cabinetDeveloperIin) }
    var bin by remember { mutableStateOf(session.preferences.cabinetDeveloperBin) }
    val complete = iin.trim().length == IDENTIFIER_LENGTH && bin.trim().length == IDENTIFIER_LENGTH
    HorizontalDivider()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        Text(text = texts.developerSignIn, style = MaterialTheme.typography.titleSmall)
        InfoTip(texts.hints.developerSignIn)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        OutlinedTextField(
            value = iin,
            onValueChange = { iin = it.filter(Char::isDigit) },
            label = { Text(texts.iin) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = bin,
            onValueChange = { bin = it.filter(Char::isDigit) },
            label = { Text(texts.bin) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
    FieldButton(text = texts.developerSignIn, enabled = complete && !cabinet.busy, modifier = Modifier.fillMaxWidth()) {
        session.preferences.cabinetDeveloperIin = iin
        session.preferences.cabinetDeveloperBin = bin
        scope.launch {
            if (cabinet.signInAsDeveloper(iin, bin)) {
                cabinet.refreshRegisters()
            }
        }
    }
}

/** ИИН и БИН — двенадцать цифр. */
private const val IDENTIFIER_LENGTH = 12
