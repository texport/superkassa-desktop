package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CompanyProfile
import kz.mybrain.superkassa.desktop.server.cabinet.Oked
import kz.mybrain.superkassa.desktop.server.cabinet.company
import kz.mybrain.superkassa.desktop.server.cabinet.saveOkeds
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Компания владельца и её виды деятельности.
 *
 * Название и БИН приходят из ЭЦП и правке не поддаются: их выдал КГД,
 * и менять их в кабинете было бы подлогом. Виды деятельности владелец
 * ведёт сам — от основного ОКЭД зависит, что уйдёт в регистрационное
 * заявление.
 */
@Composable
fun CompanyPage(cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<CompanyProfile?>(null) }
    val okeds = remember { mutableStateListOf<Oked>() }

    suspend fun reload() {
        val token = cabinet.token ?: return
        cabinet.guard { cabinet.client.company(token) }?.let { loaded ->
            profile = loaded
            okeds.clear()
            okeds.addAll(loaded.okeds)
        }
    }

    LaunchedEffect(cabinet.token) { reload() }

    ScrollableColumn(modifier = Modifier.fillMaxWidth(), spacing = Spacing.snug) {
        CompanyCard(cabinet, profile, texts)
        OkedsCard(texts, okeds, cabinet.busy) {
            scope.launch {
                val token = cabinet.token ?: return@launch
                // Перечитывание только по удаче: guard снимает сообщение
                // в начале обращения, и отказ сохранения стирался прежде,
                // чем владелец успевал его прочитать.
                if (cabinet.guard { cabinet.client.saveOkeds(token, okeds.toList()) } != null) {
                    reload()
                }
            }
        }
    }
}

/**
 * Карточка компании.
 *
 * Название — главное на этом экране, и набрано оно шкалой заголовка,
 * а не тем же кеглем, что реквизит под ним. Объяснение о происхождении
 * данных уведено под значок: читают его один раз, а место оно занимало
 * бы всегда.
 */
@Composable
private fun CompanyCard(cabinet: CabinetSession, profile: CompanyProfile?, texts: CabinetTexts) {
    SectionCard(title = texts.company, trailing = { InfoTip(texts.companyFromEds) }) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            Text(
                text = profile?.name ?: cabinet.company?.name.orEmpty(),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = ownerIdentifier(cabinet, texts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
