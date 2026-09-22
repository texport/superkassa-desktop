package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.appearance
import kz.mybrain.superkassa.desktop.app.chooseAccent
import kz.mybrain.superkassa.desktop.app.chooseTextScale
import kz.mybrain.superkassa.desktop.app.chooseTypeface
import kz.mybrain.superkassa.desktop.app.look
import kz.mybrain.superkassa.desktop.app.switchAppearance
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.ColorChoice
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.components.SubsectionTitle
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.LookStrings
import kz.mybrain.superkassa.desktop.ui.strings.SettingStrings
import kz.mybrain.superkassa.desktop.ui.theme.Accent
import kz.mybrain.superkassa.desktop.ui.theme.Appearance
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.TextScale
import kz.mybrain.superkassa.desktop.ui.theme.Typeface
import kz.mybrain.superkassa.desktop.ui.theme.swatch

/**
 * Как выглядит касса: тема, тон, шрифт и размер.
 *
 * Выбор живёт здесь, а не только в системе: касса стоит на общей машине,
 * кассиры за ней сменяются, и лезть в настройки операционной системы ради
 * читаемости экрана им нельзя. Залитый солнцем зал и ночная смена — разные
 * требования к одному и тому же экрану; зрение кассиров — тоже.
 *
 * Всё применяется сразу: карточка и есть предпросмотр, второго не нужно.
 */
@Composable
fun AppearanceCard(session: Session) {
    val texts = LocalStrings.current.settings
    SectionCard(title = texts.appearance, info = texts.appearanceHint) {
        ChoiceSegments(
            options = Appearance.entries,
            selected = session.appearance,
            label = { it.title(texts) }
        ) { session.switchAppearance(it) }
        AccentChoice(session, texts.look)
        TypefaceChoice(session, texts.look)
        TextScaleChoice(session, texts.look)
    }
}

/** Название части и под ним выбор: части карточки стоят одним ритмом. */
@Composable
private fun Choice(title: String, info: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        SubsectionTitle(title, info)
        content()
    }
}

@Composable
private fun AccentChoice(session: Session, texts: LookStrings) {
    Choice(texts.accent, info = texts.accentHint) {
        ColorChoice(
            options = Accent.entries,
            selected = session.look.accent,
            swatch = { it.swatch },
            label = { it.title(texts) }
        ) { session.chooseAccent(it) }
    }
}

@Composable
private fun TypefaceChoice(session: Session, texts: LookStrings) {
    Choice(texts.typeface) {
        ChoiceSegments(
            options = Typeface.entries,
            selected = session.look.typeface,
            label = { it.title(texts) }
        ) { session.chooseTypeface(it) }
    }
}

@Composable
private fun TextScaleChoice(session: Session, texts: LookStrings) {
    Choice(texts.textScale) {
        ChoiceSegments(
            options = TextScale.entries,
            selected = session.look.textScale,
            label = { it.title(texts) }
        ) { session.chooseTextScale(it) }
    }
}

/** Название темы для кассира. */
private fun Appearance.title(texts: SettingStrings): String = when (this) {
    Appearance.System -> texts.appearanceSystem
    Appearance.Light -> texts.appearanceLight
    Appearance.Dark -> texts.appearanceDark
}

private fun Accent.title(texts: LookStrings): String = when (this) {
    Accent.Indigo -> texts.accentIndigo
    Accent.Blue -> texts.accentBlue
    Accent.Teal -> texts.accentTeal
    Accent.Green -> texts.accentGreen
    Accent.Amber -> texts.accentAmber
    Accent.Orange -> texts.accentOrange
    Accent.Red -> texts.accentRed
    Accent.Violet -> texts.accentViolet
}

private fun Typeface.title(texts: LookStrings): String = when (this) {
    Typeface.System -> texts.typefaceSystem
    Typeface.Sans -> texts.typefaceSans
    Typeface.Serif -> texts.typefaceSerif
    Typeface.Mono -> texts.typefaceMono
}

private fun TextScale.title(texts: LookStrings): String = when (this) {
    TextScale.Compact -> texts.textScaleCompact
    TextScale.Normal -> texts.textScaleNormal
    TextScale.Large -> texts.textScaleLarge
}
