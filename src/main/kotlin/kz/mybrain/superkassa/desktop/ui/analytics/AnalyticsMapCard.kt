package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.ui.components.Collapsible
import kz.mybrain.superkassa.desktop.ui.components.SectionHeader
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Развёрнута ли карточка под картой касс.
 *
 * Высота раздела одна на карту и карточку, и распоряжается ею владелец:
 * ищет кассу глазами — сворачивает карточку, читает кассу — разворачивает.
 * Выбор помнится рабочим местом, как и свёрнутые разделы кассовой колонки:
 * повторять нажатие при каждом открытии раздела владелец не должен.
 *
 * Свёрнутой карточка остаётся заголовком: в нём видно, какая касса
 * выбрана, и раскрыть её можно одним нажатием.
 */
class AnalyticsMapCard(private val preferences: Preferences) {

    var expanded: Boolean by mutableStateOf(!preferences.mapCardCollapsed)
        private set

    /** Сворачивает развёрнутую карточку и наоборот; выбор запоминается. */
    fun toggle() {
        expanded = !expanded
        preferences.mapCardCollapsed = !expanded
    }
}

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
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.tight), content = content)
    }
}
