package kz.mybrain.superkassa.presentation.common.look

import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.Typeface
import kz.mybrain.superkassa.designsystem.theme.color.Accent
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.presentation.common.strings.workplaceLanguage
import kz.mybrain.superkassa.strings.api.Language

/**
 * Вид окна словами оформления: язык, тема, тон, шрифт и свёрнутые части.
 *
 * Касса работает в Казахстане, поэтому без выбора язык — государственный
 * или язык системы, если он один из трёх.
 */
data class LookUiState(
    val language: Language = workplaceLanguage(null),
    val appearance: Appearance = Appearance.System,
    val look: Look = Look(),
    val railCollapsed: Boolean = false,
    val placesCollapsed: Boolean = false
) {
    companion object {
        /** Сохранённый выбор, прочитанный словами оформления; незнакомый код — умолчание. */
        fun of(choice: LookChoice) = LookUiState(
            language = workplaceLanguage(choice.language),
            appearance = Appearance.byCode(choice.appearance),
            look = Look(
                accent = Accent.byCode(choice.accent),
                typeface = Typeface.byCode(choice.typeface),
                textScale = TextScale.byCode(choice.textScale)
            ),
            railCollapsed = choice.railCollapsed,
            placesCollapsed = choice.placesCollapsed
        )
    }
}
