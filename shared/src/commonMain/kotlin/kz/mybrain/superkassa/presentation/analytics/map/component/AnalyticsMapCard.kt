package kz.mybrain.superkassa.presentation.analytics.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.presentation.common.section.Collapsible
import kz.mybrain.superkassa.presentation.common.section.SectionHeader
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Заголовок карточки под картой.
 *
 * Со стрелкой — когда карточку можно свернуть; без неё — когда карточка
 * стоит сама по себе. Один заголовок на карточку кассы и на список места:
 * стрелка должна стоять на одном и том же месте, чем бы карточка
 * ни была занята.
 */
@Composable
internal fun MapCardTitle(title: String, expanded: Boolean, onToggle: (() -> Unit)?) {
    if (onToggle == null) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        return
    }
    SectionHeader(title, expanded, onToggle)
}

/** Содержимое карточки, которое прячется вместе с ней. */
@Composable
internal fun MapCardBody(expanded: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Collapsible(expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.itemGap), content = content)
    }
}
