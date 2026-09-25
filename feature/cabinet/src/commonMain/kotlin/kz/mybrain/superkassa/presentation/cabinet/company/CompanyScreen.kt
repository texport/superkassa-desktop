package kz.mybrain.superkassa.presentation.cabinet.company

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.CardSequence
import kz.mybrain.superkassa.designsystem.list.ScrollableColumn
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.CabinetOwner
import kz.mybrain.superkassa.domain.cabinet.model.CompanyProfile
import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.signin.ownerIdentifier
import kz.mybrain.superkassa.presentation.words.cabinet.addressIn
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Компания владельца и её виды деятельности.
 *
 * Название и БИН приходят из ЭЦП и правке не поддаются: их выдал КГД,
 * и менять их в кабинете было бы подлогом. Виды деятельности владелец
 * ведёт сам — от основного ОКЭД зависит, что уйдёт в регистрационное
 * заявление.
 */
@Composable
fun CompanyScreen(cabinet: CabinetViewModel, language: Language, texts: CabinetTexts) {
    val model = companyViewModel(cabinet)
    val state by model.state.collectAsState()
    val window by cabinet.state.collectAsState()
    CompanyContent(state, window, language, texts, model.actions())
}

/**
 * Раздел компании по готовому состоянию: запросов здесь нет.
 *
 * Пока ответа нет, показывать нечего: реквизиты компании и её виды
 * деятельности приходят одним обращением. Отказ — не ожидание: раздел
 * стоял с кружком «Читается…» навсегда, а причина успевала мигнуть
 * строкой внизу окна и погаснуть.
 */
@Composable
internal fun CompanyContent(
    state: CompanyUiState,
    window: CabinetUiState,
    language: Language,
    texts: CabinetTexts,
    actions: CompanyActions
) {
    val profile = state.profile
    val trouble = state.refused.takeIf { profile == null }
    val screen = when {
        trouble != null -> ScreenState.Trouble(trouble, onRetry = actions::load)
        profile == null -> ScreenState.Working
        else -> ScreenState.Ready
    }
    val title: (Oked) -> String = { oked -> okedTitle(oked, state, language) }
    ScreenSlot(screen, Modifier.fillMaxWidth(), centered = true) {
        // Реквизиты, виды деятельности и добавление вида читаются по порядку —
        // одна карточка под другой: столбцами они переставлялись, когда
        // высота одной менялась от набранного.
        ScrollableColumn(modifier = Modifier.fillMaxWidth(), spacing = Spacing.fieldGap) {
            CardSequence(Modifier.fillMaxWidth()) {
                CompanyCard(window.owner, profile, texts)
                OkedsCard(texts, state, window.busy, title, actions)
                AddOkedCard(state.search, language, texts, actions)
            }
        }
    }
}

/**
 * Наименование вида деятельности на языке владельца.
 *
 * Компания хранит одно наименование — то, на каком языке вид завели,
 * и казахский экран показывал из-за этого русскую строку классификатора.
 * Наименование берётся по коду из классификатора и переводится при показе;
 * сохранённое не трогается: в заявление уходит то, что приняла ИСНА.
 * Без классификатора стоит сохранённое, а при пустом — код: строка без
 * имени нечитаема.
 */
internal fun okedTitle(oked: Oked, state: CompanyUiState, language: Language): String =
    state.titles[oked.code]?.let { addressIn(language, it.name, it.nameKz) }
        ?: oked.name?.takeIf { it.isNotBlank() }
        ?: oked.code

/**
 * Карточка компании.
 *
 * Название — главное на этом экране, и набрано оно шкалой заголовка,
 * а не тем же кеглем, что реквизит под ним. Объяснение о происхождении
 * данных уведено под значок: читают его один раз, а место оно занимало
 * бы всегда.
 */
@Composable
private fun CompanyCard(owner: CabinetOwner?, profile: CompanyProfile?, texts: CabinetTexts) {
    SectionCard(title = texts.company.title, info = texts.company.fromEds) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.inline)
        ) {
            Text(
                text = profile?.name ?: owner?.company?.name.orEmpty(),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = owner?.let { ownerIdentifier(it, texts) }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
