package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.LocalDarkTheme

/**
 * Переключатель светлой и тёмной темы — значком в шапке.
 *
 * Выбор темы жил только в настройках, за входом кассира: на экране входа
 * и в кабинете сменить её было нечем, а зал и ночная смена освещены
 * по-разному. Значок показывает, что получится после нажатия, а не то,
 * что сейчас: кассир нажимает на солнце, когда хочет светлую.
 *
 * Системная тема при первом нажатии становится своей — обратной нынешней.
 * Вернуть «как в системе» можно в настройках: третье состояние в значке
 * кассиру не объяснить.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSwitch(session: Session) {
    val texts = LocalStrings.current
    val dark = LocalDarkTheme.current
    val label = if (dark) texts.settings.appearanceLight else texts.settings.appearanceDark
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = { session.switchAppearance(if (dark) Appearance.Light else Appearance.Dark) }
        ) {
            Icon(
                imageVector = if (dark) AppIcons.lightTheme else AppIcons.darkTheme,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
