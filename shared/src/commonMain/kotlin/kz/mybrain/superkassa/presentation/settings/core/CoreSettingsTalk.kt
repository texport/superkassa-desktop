package kz.mybrain.superkassa.presentation.settings.core

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.shown
import kz.mybrain.superkassa.presentation.strings.settings.coreSettingTexts

/**
 * Итог сохранения настроек кассы.
 *
 * Закрытую правку касса называет словами об «API» и «файле конфигурации»,
 * а владельцу нужно знать, что делать ему: такой отказ говорится своими
 * словами, остальное — как всякий ответ кассы.
 *
 * @param server касса работает сервером: менять её настройки нужно там.
 * @return сохранённое; `null` — касса отказала или не смогла.
 */
internal fun <T> Talk.savedCore(answer: Answer<T>, what: String, action: String, server: Boolean): T? {
    if (answer !is Answer.Refused || answer.code != SETTINGS_FROZEN) return answer.shown(what, action, this)
    val texts = coreSettingTexts(language())
    journal.warn("$action: refused $SETTINGS_FROZEN")
    say(action, Message.Refusal(if (server) texts.serverHint else texts.frozenHint, SETTINGS_FROZEN))
    return null
}

/** Код отказа кассы, закрывшей правку настроек. */
private const val SETTINGS_FROZEN = "SETTINGS_FROZEN"
