package kz.mybrain.superkassa.presentation.theme.choice

import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
import kz.mybrain.superkassa.presentation.theme.Typeface
import kz.mybrain.superkassa.presentation.theme.color.Accent
import kz.mybrain.superkassa.presentation.theme.color.Appearance

/**
 * Вид окна словами оформления: язык, тема, тон, шрифт и свёрнутые части.
 *
 * Касса работает в Казахстане, поэтому без выбора язык — государственный
 * или язык системы, если он один из трёх.
 */
data class LookUiState(
    val language: Language = Language.byCode(null),
    val appearance: Appearance = Appearance.System,
    val look: Look = Look(),
    val railCollapsed: Boolean = false,
    val placesCollapsed: Boolean = false
) {
    companion object {
        /** Сохранённый выбор, прочитанный словами оформления; незнакомый код — умолчание. */
        fun of(choice: LookChoice) = LookUiState(
            language = Language.byCode(choice.language),
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
