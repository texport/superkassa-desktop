package kz.mybrain.superkassa.presentation.strings.settings

import kz.mybrain.superkassa.presentation.strings.common.Language

/**
 * Надписи того, что касса делает сама: её настройки на этой машине
 * и закрытие смены без кассира. Доставка чека — в [DeliveryTexts].
 *
 * Заведено своим файлом: настройки кассы в процессе появились вместе
 * с ней, и строк у них полтора десятка.
 */
data class CoreSettingTexts(
    val title: String,
    val hint: String,
    val unread: String,
    val mode: String,
    val modeDesktop: String,
    val modeServer: String,
    val reconnect: String,
    val seconds: String,
    val protocolFixed: String,
    val frozen: String,
    val frozenHint: String,
    val serverHint: String,
    val saved: String,
    val autoClose: String,
    val autoCloseHint: String
)

/** Надписи настроек кассы на выбранном языке. */
fun coreSettingTexts(language: Language): CoreSettingTexts = when (language) {
    Language.Kk -> kazakhCore
    Language.Ru -> russianCore
    Language.En -> englishCore
}

private val russianCore = CoreSettingTexts(
    title = "Касса на этой машине",
    hint = "Одни на все кассы рабочего места: сколько ждать ответа БФД и когда пробовать связь снова. " +
        "Новые значения действуют после перезапуска кассы.",
    unread = "Касса не отдала свои настройки",
    mode = "Режим работы",
    modeDesktop = "Рабочее место",
    modeServer = "Сервер",
    reconnect = "Повтор связи с БФД, с",
    seconds = "Целое число секунд больше нуля",
    protocolFixed = "Версию протокола задаёт запуск кассы, здесь она не меняется.",
    frozen = "Правка закрыта",
    frozenHint = "Правку настроек кассы запретил владелец рабочего места в её файле настроек: здесь их можно " +
        "только посмотреть.",
    serverHint = "Касса работает сервером: её настройки меняют на сервере, а не на рабочем месте.",
    saved = "Настройки кассы сохранены. Они подействуют после перезапуска кассы",
    autoClose = "Закрывать смену самой через сутки",
    autoCloseHint = "Смена дольше суток запрещена: касса перестаёт оформлять чеки. С этим переключателем касса " +
        "сама закроет смену и снимет Z-отчёт, если кассир не успел."
)

private val kazakhCore = CoreSettingTexts(
    title = "Осы машинадағы касса",
    hint = "Жұмыс орнының барлық кассасына ортақ: БФД жауабын қанша күту және байланысты қашан қайта тексеру. " +
        "Жаңа мәндер касса қайта іске қосылғаннан кейін күшіне енеді.",
    unread = "Касса өз баптауларын бермеді",
    mode = "Жұмыс режимі",
    modeDesktop = "Жұмыс орны",
    modeServer = "Сервер",
    reconnect = "БФД байланысын қайталау, с",
    seconds = "Нөлден үлкен бүтін секунд саны",
    protocolFixed = "Хаттама нұсқасын кассаның іске қосылуы береді, мұнда ол өзгермейді.",
    frozen = "Өзгерту жабық",
    frozenHint = "Касса баптауларын өзгертуге жұмыс орнының иесі оның баптаулар файлында тыйым салған: мұнда " +
        "оларды тек көруге болады.",
    serverHint = "Касса сервер ретінде жұмыс істейді: оның баптаулары жұмыс орнында емес, серверде өзгертіледі.",
    saved = "Касса баптаулары сақталды. Олар касса қайта іске қосылғаннан кейін күшіне енеді",
    autoClose = "Бір тәуліктен кейін ауысымды өзі жабу",
    autoCloseHint = "Ауысым бір тәуліктен ұзақ бола алмайды: касса чек ресімдеуді тоқтатады. Бұл ауыстырғышпен " +
        "кассир үлгермесе, касса ауысымды өзі жауып, Z-есепті алады."
)

private val englishCore = CoreSettingTexts(
    title = "The register on this machine",
    hint = "Shared by every register of the workplace: how long to wait for the BFD and when to try the link " +
        "again. New values take effect after the register restarts.",
    unread = "The register did not return its settings",
    mode = "Operating mode",
    modeDesktop = "Workplace",
    modeServer = "Server",
    reconnect = "BFD reconnect interval, s",
    seconds = "A whole number of seconds above zero",
    protocolFixed = "The protocol version is set when the register starts and is not changed here.",
    frozen = "Editing is locked",
    frozenHint = "The workplace owner locked the register settings in its settings file: here they can only be viewed.",
    serverHint = "The register runs as a server: its settings are changed on the server, not at the workplace.",
    saved = "Register settings saved. They take effect after the register restarts",
    autoClose = "Close the shift by itself after a day",
    autoCloseHint = "A shift longer than a day is not allowed: the register stops issuing receipts. With this switch " +
        "the register closes the shift and takes the Z report itself if the cashier did not."
)
