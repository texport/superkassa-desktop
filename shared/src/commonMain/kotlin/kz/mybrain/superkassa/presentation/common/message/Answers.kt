package kz.mybrain.superkassa.presentation.common.message

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.strings.api.Language

/**
 * Итог обращения к кассе — в строку сообщений и в журнал окна.
 *
 * Прежняя запись для моделей, получающих весь контейнер; переведённые
 * модели зовут то же у `Talk`, которым их снабжает фабрика.
 *
 * @param what что делалось, словами кассира: «Открыть смену».
 * @param action что делалось, для журнала: `open shift`.
 * @return значение ответа; `null` — касса отказала или не смогла.
 */
fun <T> Answer<T>.shown(what: String, action: String, app: AppContainer): T? = app.talk.shown(this, what, action)

/** Слова отказа на языке кассира. */
fun Answer.Refused.words(language: Language): String = language.choose(ru, kk, en).orEmpty()
