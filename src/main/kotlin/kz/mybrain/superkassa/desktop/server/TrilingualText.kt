package kz.mybrain.superkassa.desktop.server

import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf

/**
 * Текст узла на трёх языках, разобранный по языкам.
 *
 * Узел склеивает три языка в одну строку и делает это двумя способами:
 * `RU: … | KK: … | EN: …` у отказов по существу и `[EN] … / [RU] … / [KK] …`
 * у собственных сбоев. Кассиру нужен один язык — тот, на котором он
 * работает. Прежде приложение всегда брало русскую часть, и казахский
 * экран показывал отказ по-русски; второй способ не разбирался вовсе,
 * и кассир читал в строке все три языка сразу.
 *
 * Разбор снисходительный: строка без разметки языков остаётся как есть,
 * недостающий язык подменяется тем, который пришёл.
 */
data class TrilingualText(val ru: String, val kk: String, val en: String) {

    /** Часть на языке кассира; пустая подменяется первой непустой. */
    fun of(language: Language): String = when (language) {
        Language.Kk -> kk.ifBlank { ru.ifBlank { en } }
        Language.Ru -> ru.ifBlank { kk.ifBlank { en } }
        Language.En -> en.ifBlank { ru.ifBlank { kk } }
    }

    companion object {

        /**
         * Собственные слова приложения на трёх языках.
         *
         * Нужны там, где сказать за узел приходится самому: язык кассира
         * выбирается позже, при показе, и выбирать его здесь нечем.
         * Надписи берутся из наборов строк, а не пишутся рядом.
         */
        fun byLanguage(pick: (AppStrings) -> String): TrilingualText = TrilingualText(
            ru = pick(stringsOf(Language.Ru)),
            kk = pick(stringsOf(Language.Kk)),
            en = pick(stringsOf(Language.En))
        )

        /** Разбирает строку узла; без разметки языков весь текст идёт во все три. */
        fun of(message: String?): TrilingualText {
            val text = message?.trim().orEmpty()
            return when {
                TAGGED.containsMatchIn(text) -> byParts(
                    TAGGED.findAll(text).associate { found ->
                        found.groupValues[1].uppercase() to found.groupValues[2].trim().trim('/').trim()
                    }
                )
                text.contains(MARK) -> byParts(
                    text.split(SEPARATOR).associate { piece ->
                        piece.substringBefore(MARK).trim().uppercase() to piece.substringAfter(MARK).trim()
                    }
                )
                else -> TrilingualText(text, text, text)
            }
        }

        /** Собирает целое из частей, как бы они ни были помечены. */
        private fun byParts(parts: Map<String, String>): TrilingualText = TrilingualText(
            ru = parts[Language.Ru.code.uppercase()].orEmpty(),
            kk = parts[Language.Kk.code.uppercase()].orEmpty(),
            en = parts[Language.En.code.uppercase()].orEmpty()
        )

        private const val MARK = ":"
        private const val SEPARATOR = " | "

        /** Метка языка в квадратных скобках: `[RU] текст` до следующей метки. */
        private val TAGGED = Regex("""\[(RU|KK|EN)]([^\[]*)""", RegexOption.IGNORE_CASE)
    }
}
