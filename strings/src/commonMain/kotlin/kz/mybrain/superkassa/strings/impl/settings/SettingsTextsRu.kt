package kz.mybrain.superkassa.strings.impl.settings

import kz.mybrain.superkassa.strings.api.settings.CoreSettingTexts
import kz.mybrain.superkassa.strings.api.settings.DeliveryFieldTexts
import kz.mybrain.superkassa.strings.api.settings.DeliverySettingTexts
import kz.mybrain.superkassa.strings.api.settings.KassaFactsTexts
import kz.mybrain.superkassa.strings.api.settings.LookTexts
import kz.mybrain.superkassa.strings.api.settings.SettingsTexts

/** Надписи [SettingsTexts] по-русски. */
internal val settingsTextsRu = SettingsTexts(
    core = CoreSettingTexts(
        title = "Сроки обмена с БФД",
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
    ),
    delivery = DeliverySettingTexts(
        title = "Доставка чека покупателю",
        hint = "Включённый канал отправляет чек покупателю через службу: SMS-шлюз, бота Telegram, WhatsApp " +
            "или почтовый сервер. Одни на все кассы рабочего места. Адреса и ключи действуют со следующего чека, " +
            "включение канала — после перезапуска кассы.",
        recipient = "Чек уходит на контакт покупателя, указанный в чеке: телефон — по SMS и в WhatsApp, " +
            "почта — письмом, чат — в Telegram. Без контакта чек покупателю не отправляется.",
        configured = "Настроен",
        notConfigured = "Не настроен",
        secretHint = "Заданные ключи скрыты знаком ***: сотрите ключ, чтобы убрать его, или наберите новый.",
        malformed = "Проверьте написание",
        portRange = "Целое число от 1 до 65535",
        saved = "Доставка чека сохранена. Включение канала подействует после перезапуска кассы",
        channels = deliveryChannels("Почта"),
        fields = DeliveryFieldTexts(
            smsUrl = "Адрес шлюза с {phone} и {text}",
            smsKey = "Ключ SMS-шлюза",
            telegramToken = "Токен бота",
            whatsAppToken = "Ключ доступа",
            whatsAppSender = "Номер отправителя (ID)",
            emailHost = "Почтовый сервер",
            emailPort = "Порт",
            emailUser = "Пользователь",
            emailPassword = "Пароль",
            emailFrom = "Адрес отправителя"
        )
    ),
    facts = KassaFactsTexts(
        title = "Программа и кассовое ядро",
        hint = "Первое, что спрашивает поддержка: какие версии стоят, как работает касса и где лежат её данные. " +
            "Видно и до входа — когда касса не открылась или не пускает.",
        appVersion = "Версия приложения",
        coreVersion = "Версия ядра",
        dataDirectory = "Каталог данных",
        kkmCount = "Касс на этой машине",
        unread = "Касса не ответила",
        ofdAuth = "Данные авторизации БФД",
        nextRequest = "Номер следующего запроса"
    ),
    sections = settingsSectionsRu
)

/** Надписи [LookTexts] по-русски. */
internal val lookTextsRu = LookTexts(
    theme = "Тема",
    accent = "Тон",
    accentHint = "Основной цвет кнопок, выделения и значков. Отказ остаётся красным при любом тоне.",
    accentRed = "Красный",
    accentOrange = "Оранжевый",
    accentAmber = "Янтарный",
    accentOlive = "Оливковый",
    accentLime = "Лаймовый",
    accentGreen = "Зелёный",
    accentEmerald = "Изумрудный",
    accentTeal = "Бирюзовый",
    accentAzure = "Лазурный",
    accentBlue = "Синий",
    accentIndigo = "Индиго",
    accentViolet = "Фиолетовый",
    accentLilac = "Сиреневый",
    accentPink = "Розовый",
    typeface = "Шрифт",
    typefaceSystem = "Системный",
    typefaceSans = "Без засечек",
    typefaceSerif = "С засечками",
    typefaceMono = "Моноширинный",
    textScale = "Размер",
    textScaleHint = "Плотный — для товарного списка, крупные — для кассового стола: с них итог читается с метра.",
    textScaleDense = "Плотный",
    textScaleCompact = "Компактный",
    textScaleNormal = "Обычный",
    textScaleLarge = "Крупный",
    textScaleLarger = "Крупнее"
)
