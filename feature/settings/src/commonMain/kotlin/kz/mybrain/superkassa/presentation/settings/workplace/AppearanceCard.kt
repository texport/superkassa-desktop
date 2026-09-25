package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.picker.ColorChoice
import kz.mybrain.superkassa.designsystem.picker.WideChoiceSegments
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.Typeface
import kz.mybrain.superkassa.designsystem.theme.color.Accent
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.designsystem.theme.color.swatch
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.look.LookActions
import kz.mybrain.superkassa.presentation.common.look.LookUiState
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.SettingsScreenTexts
import kz.mybrain.superkassa.strings.api.settings.LookTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Как выглядит касса: тема, тон, шрифт и размер — по группе на каждое.
 *
 * Выбор живёт здесь, а не только в системе: касса стоит на общей машине,
 * кассиры за ней сменяются, и лезть в настройки операционной системы ради
 * читаемости экрана им нельзя. Залитый солнцем зал и ночная смена — разные
 * требования к одному и тому же экрану; зрение кассиров — тоже.
 *
 * Каждый выбор — своя группа под подзаголовком, а не часть одной группы:
 * заголовки раздела одного вида, и тема не выглядит названием всего
 * раздела. Всё применяется сразу: раздел и есть предпросмотр.
 */
@Composable
internal fun AppearanceCard(look: LookUiState, actions: LookActions) {
    val texts = LocalStrings.current.settingsScreen
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)) {
        SettingGroup(title = texts.look.theme, info = texts.appearanceHint) {
            WideChoiceSegments(
                options = Appearance.entries,
                selected = look.appearance,
                label = { it.title(texts) },
                onSelect = actions::switchAppearance
            )
        }
        AccentChoice(look, actions, texts.look)
        TypefaceChoice(look, actions, texts.look)
        TextScaleChoice(look, actions, texts.look)
    }
}

@Composable
private fun AccentChoice(look: LookUiState, actions: LookActions, texts: LookTexts) {
    SettingGroup(title = texts.accent, info = texts.accentHint) {
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
private fun TypefaceChoice(look: LookUiState, actions: LookActions, texts: LookTexts) {
    SettingGroup(title = texts.typeface) {
        WideChoiceSegments(
            options = Typeface.entries,
            selected = look.look.typeface,
            label = { it.title(texts) },
            onSelect = actions::chooseTypeface
        )
    }
}

@Composable
private fun TextScaleChoice(look: LookUiState, actions: LookActions, texts: LookTexts) {
    SettingGroup(title = texts.textScale, info = texts.textScaleHint) {
        WideChoiceSegments(
            options = TextScale.entries,
            selected = look.look.textScale,
            label = { it.title(texts) },
            onSelect = actions::chooseTextScale
        )
    }
}

/**
 * Язык надписей приложения.
 *
 * Своим разделом приложения, а не только значком в шапке: язык выбирают
 * для этой программы на этой машине, раз в жизни рабочего места. Названия
 * языков — на них самих: так их узнают, не читая остального.
 */
@Composable
internal fun LanguageCard(look: LookUiState, actions: LookActions) {
    val texts = textsOf(LocalLanguage.current).settings.sections
    SettingGroup(title = texts.appLanguage) {
        WideChoiceSegments(
            options = Language.entries,
            selected = look.language,
            label = { it.title },
            onSelect = actions::switchLanguage
        )
    }
}

/** Выбранные тема и тон одной строкой: сводка раздела оформления в списке. */
internal fun LookUiState.summary(texts: SettingsScreenTexts): String =
    "${appearance.title(texts)}${Glyphs.SEPARATOR}${look.accent.title(texts.look)}"

/** Название темы для кассира. */
internal fun Appearance.title(texts: SettingsScreenTexts): String = when (this) {
    Appearance.System -> texts.appearanceSystem
    Appearance.Light -> texts.appearanceLight
    Appearance.Dark -> texts.appearanceDark
}

private fun Accent.title(texts: LookTexts): String = when (this) {
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

private fun Typeface.title(texts: LookTexts): String = when (this) {
    Typeface.System -> texts.typefaceSystem
    Typeface.Sans -> texts.typefaceSans
    Typeface.Serif -> texts.typefaceSerif
    Typeface.Mono -> texts.typefaceMono
}

private fun TextScale.title(texts: LookTexts): String = when (this) {
    TextScale.Dense -> texts.textScaleDense
    TextScale.Compact -> texts.textScaleCompact
    TextScale.Normal -> texts.textScaleNormal
    TextScale.Large -> texts.textScaleLarge
    TextScale.Larger -> texts.textScaleLarger
}
