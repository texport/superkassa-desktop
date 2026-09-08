package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Замена токена ОФД.
 *
 * Токен выдаёт ОФД, и он же его отзывает: по коду 2 «неверный токен» касса
 * встаёт и не выходит из блокировки, пока не введён новый. Без этого поля
 * заблокированную кассу с рабочего места было не вернуть в строй вовсе.
 */
@Composable
fun OfdTokenCard(session: Session) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    val kkm = session.selected ?: return
    var token by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val programming = kkm.isProgramming

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(texts.settings.ofdToken, style = MaterialTheme.typography.titleMedium)
                InfoTip(texts.settings.tokenHint)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.filter(Char::isDigit) },
                    label = { Text(texts.settings.newToken) },
                    singleLine = true,
                    enabled = programming && !busy,
                    modifier = Modifier.fieldWidth(texts.settings.newToken, Sizes.fieldChoice)
                )
                FieldButton(
                    text = texts.settings.saveToken,
                    enabled = programming && !busy && token.isNotBlank(),
                    onClick = {
                        busy = true
                        scope.launch {
                            val saved = session.guard(texts.settings.saveToken) {
                                session.client.updateOfdToken(kkm.kkmId, token, session.pin)
                            }
                            busy = false
                            if (saved != null) {
                                token = ""
                                session.refreshKkms()
                                session.report(texts.settings.tokenSaved)
                            }
                        }
                    }
                )
            }
            if (!programming) ProgrammingGate(session)
        }
    }
}
