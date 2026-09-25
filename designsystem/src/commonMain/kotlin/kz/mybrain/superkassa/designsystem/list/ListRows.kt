package kz.mybrain.superkassa.designsystem.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Строки списка подряд — вплотную, как список Material 3.
 *
 * Группа настроек разводит свои части шагом полей, и переключатели,
 * стоящие в группе друг за другом, получали тот же шаг: между двумя
 * нажатыми строками виднелась пустая полоска. Строки списка стоят
 * стык в стык, поля у них свои.
 */
@Composable
fun ListRows(content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), content = content)
}
