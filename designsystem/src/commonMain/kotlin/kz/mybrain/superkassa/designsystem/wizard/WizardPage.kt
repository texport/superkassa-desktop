package kz.mybrain.superkassa.designsystem.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.LocalWindowClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.list.ScrollableColumn
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Один шаг мастера на весь раздел.
 *
 * Material 3 отдельного компонента для пошагового мастера не описывает,
 * поэтому шаг собран из его частей так, как Google раскладывает шаги
 * настройки устройства (Android Setup Wizard, раскладка SetupDesign):
 * сверху — ход мастера определённой линейной полосой (Progress indicators →
 * Linear, determinate); затем иллюстрация, заголовок и объяснение шага;
 * под ними — то, что нужно сделать на этом шаге, и ничего больше; внизу —
 * строка действий: второстепенное текстовой кнопкой в начале, главное
 * залитой кнопкой в конце. На расширенном окне объяснение и действие шага
 * стоят двумя колонками, на узком — одно под другим. Строка действий
 * стоит под прокруткой: «Далее» не уезжает за нижний край.
 *
 * @param leading второстепенное действие в начале строки: «Назад».
 * @param trailing главное действие в конце строки: «Далее».
 * @param content что делается на шаге.
 */
@Composable
fun WizardPage(
    progress: WizardProgress,
    heading: WizardHeading,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        // Поля шага — поля окна, как у главной: своих отступов сверху
        // и справа у шага нет, полоса прокрутки лежит в поле окна.
        ProgressLine(progress, Modifier)
        ScrollableColumn(modifier = Modifier.weight(1f).fillMaxWidth(), spacing = Spacing.sectionGap) {
            StepBody(heading, content)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading()
            Spacer(modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

/** Объяснение и действие шага: рядом на расширенном окне, одно под другим — уже. */
@Composable
private fun StepBody(heading: WizardHeading, content: @Composable ColumnScope.() -> Unit) {
    if (LocalWindowClass.current.width >= WidthClass.Expanded) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.paneGap)) {
            WizardHeader(heading, Modifier.weight(1f))
            StepAction(Modifier.weight(1f), content)
        }
    } else {
        WizardHeader(heading, Modifier.fillMaxWidth())
        StepAction(Modifier.fillMaxWidth(), content)
    }
}

@Composable
private fun StepAction(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap), content = content)
}
