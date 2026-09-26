package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.CommonTexts
import kz.mybrain.superkassa.strings.api.common.GeneralTexts
import kz.mybrain.superkassa.strings.api.common.LoginTexts
import kz.mybrain.superkassa.strings.api.common.SectionTexts
import kz.mybrain.superkassa.strings.api.common.StatusHints
import kz.mybrain.superkassa.strings.api.common.TopBarTexts
import kz.mybrain.superkassa.strings.impl.share.shareTextsRu

/** Надписи [CommonTexts] по-русски. */
internal val commonTextsRu = CommonTexts(
    general = GeneralTexts(
        refresh = "Обновить",
        hide = "Скрыть",
        print = "Печать",
        amount = "Сумма",
        pin = "Пин",
        loading = "Читается…",
        starting = "Касса запускается",
        noAnswer = "Ответ не получен — документ мог быть проведён, проверьте журнал; повтор не задвоит",
        kassaFailed = "Касса не выполнила действие — повторите; если не выйдет, позовите обслуживание",
        refusalCode = "Код отказа",
        deliveredToOfd = "доставлено в БФД",
        queuedNoLink = "связи нет — поставлено в очередь",
        deliveryState = "Состояние доставки",
        collapse = "Свернуть",
        explain = "Пояснение",
        expand = "Развернуть",
        retry = "Повторить",
        nothingToPick = "Выбирать не из чего"
    ),
    login = LoginTexts(
        title = "Вход в кассу",
        search = "Поиск: номер, название, организация",
        noKkmsTitle = "На этом рабочем месте ни одной кассы",
        noKkms = "Здесь ещё не заведено ни одной кассы. Заведите кассу или начните с кабинета.",
        kkmsUnreadTitle = "Список касс не прочитан",
        kkmsUnread = "Касса не отдала список касс, и сколько их здесь — неизвестно. " +
            "Повторите; если не выйдет, позовите обслуживание.",
        yourKkm = "Ваша касса",
        pick = "Выбрать",
        picked = "Выбрана",
        enter = "Войти",
        reload = "Перечитать список",
        noKkmChosen = "Касса не выбрана",
        pickHint = "Выберите кассу в списке",
        factory = "Заводской",
        // Номер один, и имя у него одно: «РНМ» здесь и «Номер КГД»
        // в таблицах кабинета читались как два разных номера.
        registrationNumber = "Номер КГД"
    ),
    topBar = TopBarTexts(
        noKkm = "Касса не выбрана",
        autonomous = "Автономный режим",
        blocked = "Заблокирована",
        changeCashier = "Сменить кассира",
        statusHints = StatusHints(
            active = "Касса в работе: пробивает чеки и отправляет их в БФД.",
            blocked = "Касса заблокирована: БФД не принимает её чеки. Причину и снятие блокировки " +
                "смотрите в кабинете БФД.",
            programming = "Касса в режиме программирования: пока он включён, чеки не пробиваются. Выключить " +
                "— в настройках кассы, раздел «Основное».",
            registration = "Касса ещё не поставлена на учёт в КГД: чеки пробивать нельзя. Подайте заявление " +
                "в кабинете БФД.",
            autonomous = "Нет связи с БФД: чеки пробиваются и копятся в очереди, касса дошлёт их сама, " +
                "когда связь вернётся.",
            shiftOpen = "Смена открыта: можно пробивать чеки. Закройте её Z-отчётом на главной до конца суток.",
            shiftClosed = "Смена закрыта: чтобы пробивать чеки, откройте смену на главной."
        ),
        moreActions = "Ещё"
    ),
    sections = SectionTexts(
        dashboard = "Главная",
        sale = "Продажа",
        returns = "Возврат",
        cash = "Деньги",
        history = "История",
        queue = "Очередь",
        users = "Кассиры",
        settings = "Настройки",
        register = "Новая касса",
        cabinet = "Кабинет БФД",
        kkms = "Кассы",
        menu = "Разделы"
    ),
    dashboard = dashboardTextsRu,
    autonomous = autonomousTextsRu,
    receipt = receiptTextsRu,
    returns = returnTextsRu,
    cash = cashTextsRu,
    queue = queueTextsRu,
    users = userTextsRu,
    settingsScreen = settingsScreenTextsRu,
    preview = previewTextsRu,
    share = shareTextsRu,
    status = statusTextsRu,
    enums = enumTextsRu
)
