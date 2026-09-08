package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Branding
import kz.mybrain.superkassa.desktop.server.updateBranding
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SettingStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Печатная форма чека: язык, ширина ленты и реклама ОФД.
 *
 * Это настройки кассы, а не рабочего места: чек печатается одинаково,
 * с какого бы компьютера его ни пробили, поэтому они живут на узле
 * и меняются здесь же, где заводится касса.
 */
@Composable
fun PrintFormCard(session: Session) {
    val texts = LocalStrings.current
    val scope = rememberCoroutineScope()
    val kkm = session.selected ?: return
    val branding = kkm.branding ?: Branding()
    // Печатная форма — настройка кассы, и узел меняет настройки только
    // в режиме программирования. Показывать поля рабочими и отвечать
    // отказом на каждое нажатие значит врать кассиру про своё состояние.
    val programming = kkm.isProgramming

    fun save(changed: Branding) {
        scope.launch {
            session.guard(texts.settings.printForm) {
                session.client.updateBranding(kkm.kkmId, changed, session.pin)
            } ?: return@launch
            session.refreshKkms()
            session.report(texts.settings.printFormSaved)
        }
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            Text(texts.settings.printForm, style = MaterialTheme.typography.titleMedium)

            Text(texts.settings.receiptLanguage, style = MaterialTheme.typography.bodyMedium)
            ChoiceSegments(
                options = ReceiptLanguageChoice.entries,
                selected = ReceiptLanguageChoice.byCode(branding.language),
                label = { it.title(texts.settings) },
                enabled = programming
            ) { save(branding.copy(language = it.code)) }

            LabelWithTip(texts.settings.printLayout, texts.settings.printLayoutHint)
            ChoiceSegments(
                options = PrintLayout.entries,
                selected = PrintLayout.byMillimetres(branding.paperWidthMm),
                label = { it.title(texts.settings) },
                enabled = programming
            ) { save(branding.copy(paperWidthMm = it.millimetres)) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = branding.printOfdTicketAds ?: true,
                    enabled = programming,
                    onCheckedChange = { save(branding.copy(printOfdTicketAds = it)) }
                )
                Text(texts.settings.printOfdAds, style = MaterialTheme.typography.bodyMedium)
                InfoTip(texts.settings.printOfdAdsHint)
            }

            ReceiptLinesSection(branding, programming) { save(it) }
            if (!programming) ProgrammingGate(session)
        }
    }
}

/**
 * Язык печатного чека, как его называет узел.
 *
 * Английского здесь нет намеренно: узел принимает `RU`, `KK` и `MIXED`,
 * а на `EN` отвечает `INVALID_FIELD_VALUE`. Сегмент, на который узел
 * отвечает отказом, — обещание, которого касса не держит.
 */
enum class ReceiptLanguageChoice(val code: String, val title: (SettingStrings) -> String) {
    Mixed("MIXED", { it.receiptBoth }),
    Kk("KK", { it.receiptKk }),
    Ru("RU", { it.receiptRu });

    companion object {
        /** Выбор по коду узла; неизвестный код читается как двуязычный чек. */
        fun byCode(code: String?): ReceiptLanguageChoice =
            entries.firstOrNull { it.code == code } ?: Mixed
    }
}

/**
 * Макет печатной формы: узкая лента, широкая лента или полная страница.
 *
 * Узел различает их одним числом — шириной ленты в миллиметрах, где ноль
 * означает страницу без ограничения по ширине (`FULLSCREEN` в справочнике
 * макетов). Здесь три сегмента, а не число: кассиру нужен принтер, а не
 * миллиметры.
 */
enum class PrintLayout(val millimetres: Int, val title: (SettingStrings) -> String) {
    Narrow(58, { it.layoutTape58 }),
    Wide(80, { it.layoutTape80 }),
    Fullscreen(0, { it.layoutFullscreen });

    companion object {
        /** Макет по значению узла; неизвестное читается как широкая лента. */
        fun byMillimetres(value: Int?): PrintLayout =
            entries.firstOrNull { it.millimetres == value } ?: Wide
    }
}

/**
 * Подпись выбора и объяснение под значком рядом.
 *
 * Абзац на экране занимает две-три строки постоянно, а нужен один раз
 * при настройке: по Material 3 его место — в подсказке.
 */
@Composable
private fun LabelWithTip(label: String, explanation: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        InfoTip(explanation)
    }
}
