package kz.mybrain.superkassa.designsystem.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Ширина окна формы по ширине экрана.
 *
 * На среднем и расширенном окне Material 3 окно стоит шириной формы
 * [Sizes.formDialog]. На компактном — на телефоне в 360–412 точек — форма
 * не помещается, и окно встаёт по ширине экрана с полями [Spacing.cardGap]
 * по бокам — шаг шкалы отступов, а не своё число: фиксированная ширина уводила его края за экран или упирала
 * в них. Порог отдельно не проверяется: поле и предел ширины сами дают
 * нужное на любом окне.
 *
 * Ставится окну, у которого снят платформенный предел ширины
 * (`usePlatformDefaultWidth = false`).
 */
fun Modifier.formDialogWidth(): Modifier =
    padding(horizontal = Spacing.cardPadding).widthIn(max = Sizes.formDialog).fillMaxWidth()
