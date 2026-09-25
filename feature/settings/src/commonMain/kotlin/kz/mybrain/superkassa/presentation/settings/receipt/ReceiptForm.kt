package kz.mybrain.superkassa.presentation.settings.receipt

import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptLanguage
import kz.mybrain.superkassa.strings.api.common.SettingsScreenTexts

/**
 * Язык печатного чека, как его выбирает владелец.
 *
 * Английского здесь нет намеренно: касса печатает чек по-русски,
 * по-казахски или на двух языках сразу, и сегмент, который касса
 * не примет, — обещание, которого она не держит.
 */
internal enum class ReceiptLanguageChoice(val language: ReceiptLanguage, val title: (SettingsScreenTexts) -> String) {
    Mixed(ReceiptLanguage.MIXED, { it.receiptBoth }),
    Kk(ReceiptLanguage.KK, { it.receiptKk }),
    Ru(ReceiptLanguage.RU, { it.receiptRu });

    companion object {
        /** Выбор по языку кассы. */
        fun of(language: ReceiptLanguage): ReceiptLanguageChoice = entries.first { it.language == language }
    }
}

/**
 * Макет печатной формы: узкая лента, широкая лента или полная страница.
 *
 * Касса различает их одним числом — шириной ленты в миллиметрах, где ноль
 * означает страницу без ограничения по ширине (`FULLSCREEN` в справочнике
 * макетов). Здесь три сегмента, а не число: кассиру нужен принтер, а не
 * миллиметры.
 */
internal enum class PrintLayout(val code: String, val millimetres: Int, val title: (SettingsScreenTexts) -> String) {
    Narrow("58", TAPE_NARROW, { it.layoutTape58 }),
    Wide("80", TAPE_WIDE, { it.layoutTape80 }),
    Fullscreen(FULLSCREEN, 0, { it.layoutFullscreen });

    companion object {
        /** Макет по значению кассы; неизвестное читается как широкая лента. */
        fun byMillimetres(value: Int?): PrintLayout = entries.firstOrNull { it.millimetres == value } ?: Wide

        /**
         * Ширина в миллиметрах по коду справочника.
         *
         * `FULLSCREEN` — не ширина, а её отсутствие: страница печатается
         * без ограничения, и касса ждёт ноль.
         */
        fun millimetresOf(code: String): Int = if (code == FULLSCREEN) 0 else code.toIntOrNull() ?: Wide.millimetres
    }
}

/** Узкая и широкая чековая лента, миллиметры. */
private const val TAPE_NARROW = 58
private const val TAPE_WIDE = 80

/** Полная страница вместо ленты, как её называет справочник кассы. */
private const val FULLSCREEN = "FULLSCREEN"
