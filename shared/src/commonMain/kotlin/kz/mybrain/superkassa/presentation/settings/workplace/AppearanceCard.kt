package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.picker.ColorChoice
import kz.mybrain.superkassa.presentation.common.picker.WideChoiceSegments
import kz.mybrain.superkassa.presentation.common.section.PartTitle
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.settings.look.LookUiState
import kz.mybrain.superkassa.presentation.settings.look.LookViewModel
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.theme.TextScale
import kz.mybrain.superkassa.presentation.theme.Typeface
import kz.mybrain.superkassa.presentation.theme.color.Accent
import kz.mybrain.superkassa.presentation.theme.color.Appearance
import kz.mybrain.superkassa.presentation.theme.color.swatch
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.common.SettingStrings
import kz.mybrain.superkassa.strings.api.settings.LookStrings

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
fun AppearanceCard(actions: LookViewModel) {
    val look by actions.state.collectAsScreenState()
    val texts = LocalStrings.current.settings
    SectionCard(title = texts.appearance, info = texts.appearanceHint) {
        WideChoiceSegments(
            options = Appearance.entries,
            selected = look.appearance,
            label = { it.title(texts) },
            onSelect = actions::switchAppearance
        )
        AccentChoice(look, actions, texts.look)
        TypefaceChoice(look, actions, texts.look)
        TextScaleChoice(look, actions, texts.look)
    }
}

/** Название части и под ним выбор: части карточки стоят одним ритмом. */
@Composable
private fun Choice(title: String, info: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)) {
        PartTitle(title, info)
        content()
    }
}

@Composable
private fun AccentChoice(look: LookUiState, actions: LookViewModel, texts: LookStrings) {
    Choice(texts.accent, info = texts.accentHint) {
        ColorChoice(
            options = Accent.entries,
            selected = look.look.accent,
            swatch = { it.swatch },
            label = { it.title(texts) },
            onSelect = actions::chooseAccent
        )
    }
}

@Composable
private fun TypefaceChoice(look: LookUiState, actions: LookViewModel, texts: LookStrings) {
    Choice(texts.typeface) {
        WideChoiceSegments(
            options = Typeface.entries,
            selected = look.look.typeface,
            label = { it.title(texts) },
            onSelect = actions::chooseTypeface
        )
    }
}

@Composable
private fun TextScaleChoice(look: LookUiState, actions: LookViewModel, texts: LookStrings) {
    Choice(texts.textScale, info = texts.textScaleHint) {
        WideChoiceSegments(
            options = TextScale.entries,
            selected = look.look.textScale,
            label = { it.title(texts) },
            onSelect = actions::chooseTextScale
        )
    }
}

/** Название темы для кассира. */
private fun Appearance.title(texts: SettingStrings): String = when (this) {
    Appearance.System -> texts.appearanceSystem
    Appearance.Light -> texts.appearanceLight
    Appearance.Dark -> texts.appearanceDark
}

private fun Accent.title(texts: LookStrings): String = when (this) {
    Accent.Red -> texts.accentRed
    Accent.Orange -> texts.accentOrange
    Accent.Amber -> texts.accentAmber
    Accent.Olive -> texts.accentOlive
    Accent.Lime -> texts.accentLime
    Accent.Green -> texts.accentGreen
    Accent.Emerald -> texts.accentEmerald
    Accent.Teal -> texts.accentTeal
    Accent.Azure -> texts.accentAzure
    Accent.Blue -> texts.accentBlue
    Accent.Indigo -> texts.accentIndigo
    Accent.Violet -> texts.accentViolet
    Accent.Lilac -> texts.accentLilac
    Accent.Pink -> texts.accentPink
}

private fun Typeface.title(texts: LookStrings): String = when (this) {
    Typeface.System -> texts.typefaceSystem
    Typeface.Sans -> texts.typefaceSans
    Typeface.Serif -> texts.typefaceSerif
    Typeface.Mono -> texts.typefaceMono
}

private fun TextScale.title(texts: LookStrings): String = when (this) {
    TextScale.Dense -> texts.textScaleDense
    TextScale.Compact -> texts.textScaleCompact
    TextScale.Normal -> texts.textScaleNormal
    TextScale.Large -> texts.textScaleLarge
    TextScale.Larger -> texts.textScaleLarger
}
