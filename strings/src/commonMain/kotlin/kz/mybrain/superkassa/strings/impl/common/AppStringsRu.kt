package kz.mybrain.superkassa.strings.impl.common

import kz.mybrain.superkassa.strings.api.common.AppStrings
import kz.mybrain.superkassa.strings.api.common.CommonStrings
import kz.mybrain.superkassa.strings.api.common.LoginStrings
import kz.mybrain.superkassa.strings.api.common.SectionStrings
import kz.mybrain.superkassa.strings.api.common.ShellStrings

/** Надписи [AppStrings] по-русски. */
internal val appStringsRu = AppStrings(
    common = CommonStrings(
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
    login = LoginStrings(
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
    shell = ShellStrings(
        noKkm = "Касса не выбрана",
        autonomous = "Автономный режим",
        blocked = "Заблокирована",
        changeCashier = "Сменить кассира",
        moreActions = "Ещё"
    ),
    sections = SectionStrings(
        dashboard = "Главная",
        sale = "Продажа",
        returns = "Возврат",
        cash = "Деньги",
        history = "История",
        queue = "Очередь",
        users = "Кассиры",
        settings = "Настройки",
        register = "Новая касса",
        cabinet = "Кабинет БФД"
    ),
    dashboard = dashboardStringsRu,
    autonomous = autonomousStringsRu,
    sale = saleStringsRu,
    returns = returnStringsRu,
    cash = cashStringsRu,
    queue = queueStringsRu,
    users = userStringsRu,
    settings = settingStringsRu,
    preview = previewStringsRu,
    status = statusStringsRu,
    enums = enumStringsRu
)
