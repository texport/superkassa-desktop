package kz.mybrain.superkassa.desktop.ui.sale

import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts

/**
 * Что считается акцизной маркой.
 *
 * Марку кассир не набирает, а сканирует: сканер присылает её целиком
 * и сам жмёт Enter. Поэтому правила здесь про сам перечень, а не про
 * набор — пустое не добавляется, повтор не удваивается.
 *
 * Повтор в чеке значит, что одну и ту же бутылку поднесли к сканеру
 * дважды: в ОФД такой чек уходит с двумя одинаковыми марками, и КГД
 * считает вторую продажу непрослеженной.
 */
object ExciseRules {

    /** Марка, годная для чека, либо `null`. */
    fun stampOf(scanned: String): String? =
        scanned.trim().takeIf { it.isNotBlank() && it.length <= MAX_LENGTH }

    /** Добавляет марку к перечню; повтор и пустое перечень не меняют. */
    fun accept(stamps: List<String>, scanned: String): List<String> {
        val stamp = stampOf(scanned) ?: return stamps
        return if (stamp in stamps) stamps else stamps + stamp
    }

    /** Почему марка не принята; `null` — принята. */
    fun refusal(stamps: List<String>, scanned: String, texts: SaleTexts): String? {
        val stamp = stampOf(scanned) ?: return texts.exciseTooLong.takeIf { scanned.isNotBlank() }
        return texts.exciseRepeated.takeIf { stamp in stamps }
    }

    /**
     * Сколько знаков бывает в марке.
     *
     * Марки КГД короче сотни знаков; всё длиннее — не марка, а слипшийся
     * поток сканера, и слать такое в ОФД значит получить отказ на весь чек.
     */
    private const val MAX_LENGTH = 100
}
