package kz.mybrain.superkassa.presentation.common.look

import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.Typeface
import kz.mybrain.superkassa.designsystem.theme.color.Accent
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.strings.api.Language

/**
 * Что кассир выбирает в виде окна: язык, тему, тон, шрифт и размер.
 *
 * Экран оформления рисуется по состоянию [LookUiState] и зовёт эти
 * действия, а не модель: так его рисуют и превью, где модели нет.
 * Действия по умолчанию пустые — для превью и снимков вида.
 */
interface LookActions {
    fun switchLanguage(chosen: Language) = Unit

    fun switchAppearance(chosen: Appearance) = Unit

    fun chooseAccent(chosen: Accent) = Unit

    fun chooseTypeface(chosen: Typeface) = Unit

    fun chooseTextScale(chosen: TextScale) = Unit
}
