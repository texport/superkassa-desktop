package kz.mybrain.superkassa.designsystem.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Заголовок панели «списка и подробностей» и возврат из неё.
 *
 * По Material 3 (Canonical layouts → List-detail) у каждой панели свой
 * заголовок, а на узком окне, где подробности сменяют список, — стрелка
 * назад к списку. Шапка окна одна и называет кассу; панель называет то,
 * что открыто в ней, и начинает строку с той же вертикали, что строки
 * списка под ней.
 *
 * @param onBack возврат к списку; `null` — список стоит рядом, и стрелки нет.
 * @param backLabel подпись стрелки для чтения с экрана.
 */
@Composable
fun PaneTitle(title: String, onBack: (() -> Unit)? = null, backLabel: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = LocalMinimumInteractiveComponentSize.current),
        horizontalArrangement = Arrangement.spacedBy(Spacing.inline),
        verticalAlignment = Alignment.CenterVertically
    ) {
        onBack?.let { back ->
            IconButton(onClick = back) { Icon(AppIcons.back, contentDescription = backLabel) }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = if (onBack == null) Spacing.cardPadding else Spacing.flush)
                .semantics { heading() }
        )
    }
}
