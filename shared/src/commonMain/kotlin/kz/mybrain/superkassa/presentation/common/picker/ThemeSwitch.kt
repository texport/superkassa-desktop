package kz.mybrain.superkassa.presentation.common.picker

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.presentation.settings.look.LookViewModel
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.theme.LocalDarkTheme
import kz.mybrain.superkassa.presentation.theme.color.Appearance
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons

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
fun ThemeSwitch(look: LookViewModel) {
    val flip = themeFlip()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(flip.label) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = { look.switchAppearance(flip.next) }) {
            Icon(
                imageVector = flip.icon,
                contentDescription = flip.label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Смена темы пунктом меню — тот же выбор, что у значка в шапке.
 *
 * В узком окне значок уходит в меню «Ещё», и тема меняется оттуда:
 * пункт называет и рисует то, что получится, как и значок.
 *
 * @param onPicked закрыть меню после выбора.
 */
@Composable
fun ThemeMenuItem(look: LookViewModel, onPicked: () -> Unit) {
    val flip = themeFlip()
    DropdownMenuItem(
        text = { Text(flip.label) },
        leadingIcon = { Icon(flip.icon, contentDescription = null) },
        onClick = {
            look.switchAppearance(flip.next)
            onPicked()
        }
    )
}

/** Что получится после нажатия: обратная нынешней тема, её название и значок. */
private data class ThemeFlip(val next: Appearance, val label: String, val icon: ImageVector)

@Composable
private fun themeFlip(): ThemeFlip {
    val texts = LocalStrings.current
    return if (LocalDarkTheme.current) {
        ThemeFlip(Appearance.Light, texts.settings.appearanceLight, AppIcons.lightTheme)
    } else {
        ThemeFlip(Appearance.Dark, texts.settings.appearanceDark, AppIcons.darkTheme)
    }
}
