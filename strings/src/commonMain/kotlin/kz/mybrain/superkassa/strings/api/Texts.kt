package kz.mybrain.superkassa.strings.api

import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.common.AppStrings
import kz.mybrain.superkassa.strings.api.debug.DebugTexts
import kz.mybrain.superkassa.strings.api.journal.JournalTexts
import kz.mybrain.superkassa.strings.api.kassa.KassaTexts
import kz.mybrain.superkassa.strings.api.map.MapTexts
import kz.mybrain.superkassa.strings.api.print.PrintTexts
import kz.mybrain.superkassa.strings.api.settings.SettingsTexts
import kz.mybrain.superkassa.strings.api.setup.SetupTexts
import kz.mybrain.superkassa.strings.api.shell.StartTexts
import kz.mybrain.superkassa.strings.api.shift.CoreTexts
import kz.mybrain.superkassa.strings.api.update.UpdateTexts
import kz.mybrain.superkassa.strings.impl.textsIn

/**
 * Все тексты кассы на одном языке — по областям приложения.
 *
 * Поле — область, внутри — формы её экранов. Экран берёт строку по смыслу
 * и не знает, на каком она языке: язык выбран один раз, в [textsOf].
 */
data class Texts(
    /** Общие надписи всех экранов: вход, каркас, главная, продажа, настройки, состояния. */
    val common: AppStrings,
    /** Аналитика: сводка продаж, учёт касс, отбор. */
    val analytics: AnalyticsTexts,
    /** Личный кабинет БФД: точки, кассы, заявления, подпись, работа на этой машине. */
    val cabinet: CabinetTexts,
    /** Журнал приложения и режим отладки. */
    val debug: DebugTexts,
    /** Возврат, история, смены, очередь, доставка чека и отказы БФД. */
    val journal: JournalTexts,
    /** Продажа, оплата, возврат и деньги. */
    val kassa: KassaTexts,
    /** Выбор точки на карте и связка карты с адресным регистром. */
    val map: MapTexts,
    /** Печать. */
    val print: PrintTexts,
    /** Карточки настроек со своей темой. */
    val settings: SettingsTexts,
    /** Мастер первого запуска. */
    val setup: SetupTexts,
    /** Экран, которым касса говорит, что не открылась. */
    val shell: StartTexts,
    /** Главная и смена. */
    val shift: CoreTexts,
    /** Обновление приложения. */
    val update: UpdateTexts
)

/**
 * Тексты кассы на языке [language] — единственная точка входа в модуль.
 *
 * Таблицы языков неизменяемы и созданы один раз: повторный вызов отдаёт
 * тот же набор, и держать его у себя экрану незачем.
 */
fun textsOf(language: Language): Texts = textsIn(language)
