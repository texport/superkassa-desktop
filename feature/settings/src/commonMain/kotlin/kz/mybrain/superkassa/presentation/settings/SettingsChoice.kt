package kz.mybrain.superkassa.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.navigation.LocalNavigator
import kz.mybrain.superkassa.navigation.settings.SettingsSectionKey

/**
 * Какой раздел настроек открыт и как открыть другой.
 *
 * @property open раздел в панели подробностей.
 * @property over раздел открыт поверх списка — шагом истории окна.
 * @property pick открыть раздел, нажатый в списке.
 */
internal class SettingsChoice(
    val open: SettingsSection,
    val over: Boolean,
    val pick: (SettingsSection) -> Unit
)

/**
 * Выбор раздела: рядом со списком — подсветкой в нём, поверх списка —
 * шагом истории окна.
 *
 * Нажатый раздел запоминается и на узком окне: раздвинули окно, пока
 * раздел открыт поверх списка, — шаг снимается, а раздел остаётся открытым
 * рядом со списком. Раздел, которого вошедшему не видно, — кассир сменил
 * администратора, — уступает место первому видимому, а не остаётся пустой
 * панелью.
 */
@Composable
internal fun settingsChoice(
    sections: List<SettingsSection>,
    opened: SettingsSectionKey?,
    beside: Boolean
): SettingsChoice {
    val navigator = LocalNavigator.current
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val stepped = opened?.let { sectionOf(it.section) }?.takeIf { it in sections }
    LaunchedEffect(beside, opened) { if (beside && opened != null) navigator.back() }
    return SettingsChoice(
        open = stepped ?: picked?.let(::sectionOf)?.takeIf { it in sections } ?: sections.first(),
        over = stepped != null && !beside
    ) { chosen ->
        picked = chosen.name
        if (!beside) navigator.open(SettingsSectionKey(chosen.name))
    }
}

/** Раздел настроек по имени; незнакомое имя — прежней версии — не раздел. */
private fun sectionOf(name: String): SettingsSection? = SettingsSection.entries.firstOrNull { it.name == name }
