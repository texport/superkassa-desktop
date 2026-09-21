package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SettingStrings
import kz.mybrain.superkassa.desktop.ui.theme.Appearance

/**
 * Светлая или тёмная касса.
 *
 * Выбор живёт здесь, а не только в системе: касса стоит на общей машине,
 * кассиры за ней сменяются, и лезть в настройки операционной системы ради
 * читаемости экрана им нельзя. Залитый солнцем зал и ночная смена — разные
 * требования к одному и тому же экрану.
 */
@Composable
fun AppearanceCard(session: Session) {
    val texts = LocalStrings.current
    SectionCard(title = texts.settings.appearance, info = texts.settings.appearanceHint) {
        ChoiceSegments(
            options = Appearance.entries,
            selected = session.appearance,
            label = { it.title(texts.settings) }
        ) { session.switchAppearance(it) }
    }
}

/** Название темы для кассира. */
private fun Appearance.title(texts: SettingStrings): String = when (this) {
    Appearance.System -> texts.appearanceSystem
    Appearance.Light -> texts.appearanceLight
    Appearance.Dark -> texts.appearanceDark
}
