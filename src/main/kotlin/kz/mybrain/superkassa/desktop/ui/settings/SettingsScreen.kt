package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Настройки кассы.
 *
 * Здесь только то, что делают редко и осознанно: касса в работе, сверка
 * с ОФД, диагностика и снятие с учёта. Выбор кассы и вход кассира живут
 * на экране входа, а заведение новой кассы — своим разделом: первую кассу
 * заводят тогда, когда войти ещё некуда.
 *
 * Каждый раздел — своя карточка, и порядок идёт от повседневного
 * к необратимому: снятие с учёта стоит последним, чтобы его не нажимали
 * по дороге к диагностике.
 */
@Composable
fun SettingsScreen(session: Session) {
    val texts = LocalStrings.current
    ScrollableColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        spacing = Spacing.roomy
    ) {
        Text(texts.settings.title, style = MaterialTheme.typography.headlineSmall)

        CurrentKkmCard(session)

        GroupTitle(texts.settings.groupAppearance)
        AppearanceCard(session)
        PrintFormCard(session)
        PrintTargetCard(session)
        PanelBehaviourCard(session)

        GroupTitle(texts.settings.groupService)
        TaxSettingsCard(session)
        OfdSyncCard(session)
        NodeAddressCard(session)
        CabinetAddressCard(session)
        MapServicesCard(session)
        OfdTokenCard(session)
        DiagnosticsCard(session)
        DebugCard(session)
        NodeFactsCard(session)

        GroupTitle(texts.settings.groupIrreversible)
        DecommissionCard(session)
    }
}

/**
 * Заголовок группы настроек.
 *
 * Карточек в настройках десяток, и без групп они читаются одним списком:
 * кассир ищет нужную глазами по всему экрану. Группы идут от повседневного
 * к необратимому — снятие с учёта последним.
 */
@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
