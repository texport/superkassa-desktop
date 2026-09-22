package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Branding
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.updateBranding
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
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

    fun save(changed: Branding, onSaved: () -> Unit = {}) {
        scope.launch {
            session.guard(texts.settings.printForm) {
                session.client.updateBranding(kkm.kkmId, changed, session.pin)
            } ?: return@launch
            // Черновик забывается только после согласия узла: отказ
            // оставляет набранное на месте, иначе владелец набирал бы
            // девять строк заново.
            onSaved()
            session.refreshKkms()
            session.report(texts.settings.printFormSaved)
        }
    }

    SectionCard(title = texts.settings.printForm, info = texts.settings.printFormHint) {
        Text(texts.settings.receiptLanguage, style = MaterialTheme.typography.bodyMedium)
        ChoiceSegments(
            options = ReceiptLanguageChoice.entries,
            selected = ReceiptLanguageChoice.byCode(branding.language),
            label = { it.title(texts.settings) },
            enabled = programming
        ) { save(branding.copy(language = it.code)) }

        LabelWithTip(texts.settings.printLayout, texts.settings.printLayoutHint)
        // Перечень макетов — от узла: свой список не узнал бы о новой
        // ширине ленты, пока приложение не перевыпустят. Правило
        // «код в миллиметры» остаётся здесь: узел везёт ширину числом.
        val layouts = session.dictionaries[Dictionary.PaperWidths].orEmpty()
        ChoiceSegments(
            options = layouts.ifEmpty { null }?.map { it.code } ?: PrintLayout.entries.map { it.code },
            selected = PrintLayout.byMillimetres(branding.paperWidthMm).code,
            label = { code -> layoutTitle(session, code, texts.settings) },
            enabled = programming
        ) { save(branding.copy(paperWidthMm = PrintLayout.millimetresOf(it))) }
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

        ReceiptLinesSection(kkm.kkmId, branding, programming) { changed ->
            save(changed) { forgetReceiptLineDrafts(kkm.kkmId) }
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
enum class PrintLayout(val code: String, val millimetres: Int, val title: (SettingStrings) -> String) {
    Narrow("58", 58, { it.layoutTape58 }),
    Wide("80", 80, { it.layoutTape80 }),
    Fullscreen(FULLSCREEN, 0, { it.layoutFullscreen });

    companion object {
        /** Макет по значению узла; неизвестное читается как широкая лента. */
        fun byMillimetres(value: Int?): PrintLayout =
            entries.firstOrNull { it.millimetres == value } ?: Wide

        /**
         * Ширина в миллиметрах по коду справочника.
         *
         * `FULLSCREEN` — не ширина, а её отсутствие: страница печатается
         * без ограничения, и узел ждёт ноль.
         */
        fun millimetresOf(code: String): Int =
            if (code == FULLSCREEN) 0 else code.toIntOrNull() ?: Wide.millimetres
    }
}

/** Полная страница вместо ленты, как её называет справочник узла. */
private const val FULLSCREEN = "FULLSCREEN"

/**
 * Название макета: своё для знакомых, от узла для остальных.
 *
 * Свои названия точнее — «Лента 58 мм» вместо «58мм», — но кончаются
 * на первом же макете, которого приложение ещё не знает.
 */
private fun layoutTitle(session: Session, code: String, texts: SettingStrings): String =
    PrintLayout.entries.firstOrNull { it.code == code }?.title(texts)
        ?: session.titleOf(Dictionary.PaperWidths, code)

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
