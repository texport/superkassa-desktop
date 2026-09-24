package kz.mybrain.superkassa.presentation.common.strings

import kz.mybrain.superkassa.strings.api.Language

/**
 * Код языка системы — `ru`, `kk`, `en`, — или `null`, если система
 * его не называет.
 *
 * Спрашивается у каждой платформы своим способом: общего способа узнать
 * язык машины у Kotlin нет.
 */
internal expect fun systemLanguage(): String?

/**
 * Язык рабочего места: выбранный владельцем, иначе язык системы.
 *
 * Правило выбора — у [Language.byCode]; здесь к нему добавляется язык
 * машины, который модуль текстов узнать не может.
 *
 * @param code код языка, выбранного владельцем; `null` — не выбран.
 */
fun workplaceLanguage(code: String?): Language = Language.byCode(code, systemLanguage())
