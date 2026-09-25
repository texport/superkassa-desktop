package kz.mybrain.superkassa.designsystem.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip

/**
 * Группа настроек под подзаголовком — как в списке Material 3.
 *
 * Раздел настроек — это не стопка карточек, а список: подзаголовок
 * группы цветом `primary`, под ним строки, поля и действия группы.
 * Рамок нет: группы разделяет воздух и подзаголовок, и на широком окне
 * они не разбегаются по столбцам, а стоят одна под другой.
 *
 * Содержимое отступает от краёв панели на внутреннее поле строки списка
 * Material 3: строки-переключатели идут от края до края панели, а их
 * подписи, поля и кнопки группы начинаются с одной вертикали.
 *
 * @param info объяснение группы под значком у подзаголовка.
 * @param danger группа необратимого: подзаголовок цветом отказа.
 * @param trailing состояние группы в строке подзаголовка справа.
 */
@Composable
fun SettingGroup(
    title: String,
    modifier: Modifier = Modifier,
    info: String? = null,
    danger: Boolean = false,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        GroupHead(title, info, danger, trailing)
        content()
    }
}

/**
 * Строка подзаголовка: подзаголовок со значком объяснения — и состояние
 * группы в конце строки. Остаток строки целиком отдан подзаголовку.
 */
@Composable
private fun GroupHead(title: String, info: String?, danger: Boolean, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = LocalMinimumInteractiveComponentSize.current),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f, fill = false).semantics { heading() }
            )
            info?.let { InfoTip(it) }
        }
        trailing()
    }
}
