package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Переключатель языка.
 *
 * Значок с меню, а не три слова подряд. Язык переключают редко — раз
 * за смену, а то и раз в жизни кассы, — и по Material 3 такому месту
 * отведён значок в строке заголовка, а не постоянно занятая строка.
 * Три названия рядом соперничали по весу с названием кассы и состоянием
 * смены, то есть с тем, на что кассир смотрит всё время.
 *
 * Названия языков остаются написанными на них самих: так их узнают,
 * не читая остального, — и выбранный отмечен, чтобы меню отвечало
 * на вопрос «какой сейчас», а не только «какие бывают».
 */
@Composable
fun LanguagePicker(session: Session) {
    val texts = LocalStrings.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(AppIcons.language, contentDescription = texts.settings.language)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Language.entries.forEach { language ->
                val chosen = language == session.language
                DropdownMenuItem(
                    text = {
                        Text(
                            text = language.title,
                            color = if (chosen) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    trailingIcon = if (chosen) {
                        { Icon(AppIcons.chosen, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        session.switchLanguage(language)
                        open = false
                    }
                )
            }
        }
    }
}
