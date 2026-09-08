package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи области «Деньги, кассиры и настройки».
 *
 * Файл принадлежит одной области интерфейса целиком: так надпись заводится
 * в одном месте и переиспользуется, а правки разных экранов не сходятся
 * в одном файле. Каждое поле обязано существовать во всех трёх языках —
 * об этом заботится компилятор.
 *
 * Названия ролей, видов документов и состояний доставки сюда не попадают:
 * их отдаёт справочник узла сразу на трёх языках, и свой перевод рано или
 * поздно разошёлся бы с тем, что напечатано на чеке.
 */
data class MoneyTexts(
    val drawer: DrawerTexts,
    val cashiers: CashierTexts,
    val kkm: KkmSetupTexts
)

/** Денежный ящик: остаток, внесение, изъятие. */
data class DrawerTexts(
    val inDrawer: String,
    val countedByNode: String,
    val unknownBalance: String,
    val shiftClosed: String,
    val shiftClosedShort: String,
    val amountHint: String,
    val notANumber: String,
    val notPositive: String,
    val tooLarge: String,
    val notEnough: String,
    val confirmDeposit: String,
    val confirmWithdraw: String,
    val afterOperation: String,
    val confirm: String,
    val cancel: String,
    val working: String,
    val recent: String,
    val recentEmpty: String,
    val recentEmptyHint: String
)

/** Кассиры кассы и их пины. */
data class CashierTexts(
    val addTitle: String,
    val listTitle: String,
    val pinLength: String,
    val pinUnique: String,
    val roles: String,
    val ownPin: String,
    val pinTooShort: String,
    val pinTooLong: String,
    val nameRequired: String,
    val deleteConfirm: String,
    val deleteExplain: String,
    val deleteBlocked: String,
    val changePinFor: String,
    val onlyInRole: String,
    val empty: String,
    val emptyHint: String
)

/** Заведение кассы, сверка с ОФД и снятие с учёта. */
data class KkmSetupTexts(
    val stepOne: String,
    val stepTwo: String,
    val copy: String,
    val copied: String,
    val factoryReady: String,
    val renameSaved: String,
    val renameReset: String,
    val diagnosticsHint: String,
    val diagnosticsEmpty: String,
    val programmingOn: String,
    val programmingOff: String,
    val ofdEmpty: String,
    val ofdAnswer: String,
    val ofdKgdNumber: String,
    val ofdFactoryNumber: String,
    val ofdSystemId: String,
    val ofdProtocol: String,
    val ofdOrganization: String,
    val ofdAddress: String,
    val ofdBin: String,
    val nodeTitle: String,
    val nodeMode: String,
    val nodeProtocol: String,
    val nodeTimeout: String,
    val nodeStorage: String,
    val syncTitle: String,
    val syncService: String,
    val syncServiceHint: String,
    val syncServiceDone: String,
    val syncCounters: String,
    val syncCountersHint: String,
    val syncCountersDone: String,
    val decommission: String,
    val decommissionHint: String,
    val needProgramming: String,
    val needShiftClosed: String,
    val needQueueEmpty: String,
    val needOnline: String,
    val decommissionConfirm: String,
    val decommissionDone: String
)

internal val moneyTextsRu = MoneyTexts(
    drawer = DrawerTexts(
        inDrawer = "В денежном ящике",
        countedByNode = "Остаток считает узел кассы: он же решает, хватает ли денег на изъятие.",
        unknownBalance = "Узел не отдал остаток — изъятие проверит он сам",
        shiftClosed = "Смена закрыта. Вносить и изымать деньги можно только при открытой смене.",
        shiftClosedShort = "Смена закрыта",
        amountHint = "Например, 1 500,00",
        notANumber = "Это не сумма. Введите число, например 1 500,00",
        notPositive = "Сумма должна быть больше нуля",
        tooLarge = "Сумма больше, чем касса проводит за одну операцию",
        notEnough = "В ящике только %s — изъять больше нечего",
        confirmDeposit = "Внести %s?",
        confirmWithdraw = "Изъять %s?",
        afterOperation = "В ящике станет %s",
        confirm = "Подтвердить",
        cancel = "Отмена",
        working = "Проводится…",
        recent = "Последние внесения и изъятия",
        recentEmpty = "За сутки внесений и изъятий не было",
        recentEmptyHint = "Внесённые и изъятые деньги появляются здесь сразу после проведения."
    ),
    cashiers = CashierTexts(
        addTitle = "Новый кассир",
        listTitle = "Кассиры кассы",
        pinLength = "4–8 цифр",
        pinUnique = "Двум кассирам одной кассы одинаковый пин узел не даст: он по пину и узнаёт, кто работает.",
        roles = "Администратор заводит кассиров и меняет настройки, кассир пробивает чеки.",
        ownPin = "Свой пин меняется здесь же: работа продолжится новым пином, входить заново не нужно.",
        pinTooShort = "В пине меньше четырёх цифр",
        pinTooLong = "В пине больше восьми цифр",
        nameRequired = "Впишите имя кассира",
        deleteConfirm = "Удалить %s?",
        deleteExplain = "Кассир потеряет доступ к кассе. Пробитые им чеки останутся на месте.",
        deleteBlocked = "Это единственный кассир с ролью «%s». Заведите второго, потом удаляйте.",
        changePinFor = "Новый пин для %s",
        onlyInRole = "Единственный в роли",
        empty = "На кассе не заведено ни одного кассира",
        emptyHint = "Заведите первого — он появится в этом списке."
    ),
    kkm = KkmSetupTexts(
        stepOne = "Шаг 1. Заводской номер и год выпуска",
        stepTwo = "Шаг 2. Идентификатор и токен от ОФД",
        copy = "Скопировать",
        copied = "Скопировано",
        factoryReady = "Номер получен. Отнесите его в кабинет ОФД и получите идентификатор и токен.",
        renameSaved = "Название сохранено на этом рабочем месте",
        renameReset = "Вернуть название от ОФД",
        diagnosticsHint = "Проверки ничего не меняют в кассе: они спрашивают узел и ОФД, как обстоят дела.",
        diagnosticsEmpty = "Проверки ещё не запускались",
        programmingOn = "Режим программирования включён",
        programmingOff = "Обычный режим",
        ofdEmpty = "ОФД не прислал сведений",
        ofdAnswer = "Ответ ОФД",
        ofdKgdNumber = "Регистрационный номер КГД",
        ofdFactoryNumber = "Заводской номер",
        ofdSystemId = "Идентификатор в ОФД",
        ofdProtocol = "Версия протокола",
        ofdOrganization = "Организация",
        ofdAddress = "Адрес установки",
        ofdBin = "БИН",
        nodeTitle = "Узел кассы",
        nodeMode = "Режим работы",
        nodeProtocol = "Протокол ОФД",
        nodeTimeout = "Ожидание ответа ОФД, с",
        nodeStorage = "Хранилище",
        syncTitle = "Сверка с ОФД",
        syncService = "Сверить сведения о кассе",
        syncServiceHint = "Организация, адрес и регистрационные номера придут из ОФД. " +
            "Нужны закрытая смена и пустая очередь.",
        syncServiceDone = "Сведения о кассе обновлены из ОФД",
        syncCounters = "Сверить счётчики",
        syncCountersHint = "Узел заберёт из ОФД счётчики и номер смены. Очередь должна быть пуста.",
        syncCountersDone = "Счётчики обновлены из ОФД",
        decommission = "Снять кассу с учёта",
        decommissionHint = "Узел удалит кассу вместе с её документами с этого рабочего места. Отменить нельзя.",
        needProgramming = "Касса в режиме программирования",
        needShiftClosed = "Смена закрыта",
        needQueueEmpty = "Очередь отправки пуста",
        needOnline = "Автономный режим выключен",
        decommissionConfirm = "Снять с учёта кассу %s?",
        decommissionDone = "Касса снята с учёта"
    )
)

internal val moneyTextsKk = MoneyTexts(
    drawer = DrawerTexts(
        inDrawer = "Ақша жәшігінде",
        countedByNode = "Қалдықты касса торабы санайды: алуға ақша жете ме, соны да ол шешеді.",
        unknownBalance = "Тораб қалдықты бермеді — алуды оның өзі тексереді",
        shiftClosed = "Ауысым жабық. Ақша салу мен алу тек ашық ауысымда мүмкін.",
        shiftClosedShort = "Ауысым жабық",
        amountHint = "Мысалы, 1 500,00",
        notANumber = "Бұл сома емес. Сан енгізіңіз, мысалы 1 500,00",
        notPositive = "Сома нөлден үлкен болуы керек",
        tooLarge = "Сома касса бір операцияда өткізетін шектен асып тұр",
        notEnough = "Жәшікте бар болғаны %s — одан артық алуға болмайды",
        confirmDeposit = "%s салынсын ба?",
        confirmWithdraw = "%s алынсын ба?",
        afterOperation = "Жәшікте %s қалады",
        confirm = "Растау",
        cancel = "Болдырмау",
        working = "Өткізілуде…",
        recent = "Соңғы салымдар мен алулар",
        recentEmpty = "Тәулік ішінде салым да, алу да болған жоқ",
        recentEmptyHint = "Салынған және алынған ақша өткізілген бойда осында көрінеді."
    ),
    cashiers = CashierTexts(
        addTitle = "Жаңа кассир",
        listTitle = "Кассаның кассирлері",
        pinLength = "4–8 сан",
        pinUnique = "Бір кассадағы екі кассирге бірдей ПИН беруге тораб жол бермейді: " +
            "ол кім жұмыс істеп жатқанын ПИН арқылы таниды.",
        roles = "Әкімші кассирлерді енгізеді және баптауларды өзгертеді, кассир чек шығарады.",
        ownPin = "Өз ПИН-іңіз осында ауысады: жұмыс жаңа ПИН-мен жалғасады, қайта кірудің қажеті жоқ.",
        pinTooShort = "ПИН-де төрт цифрдан аз",
        pinTooLong = "ПИН-де сегіз цифрдан көп",
        nameRequired = "Кассирдің атын жазыңыз",
        deleteConfirm = "%s жойылсын ба?",
        deleteExplain = "Кассир кассаға қолжетімділігін жоғалтады. Ол шығарған чектер орнында қалады.",
        deleteBlocked = "Бұл «%s» рөліндегі жалғыз кассир. Алдымен екіншісін енгізіңіз, содан кейін жойыңыз.",
        changePinFor = "%s үшін жаңа ПИН",
        onlyInRole = "Рөлдегі жалғыз",
        empty = "Кассада бірде-бір кассир енгізілмеген",
        emptyHint = "Алғашқысын енгізіңіз — ол осы тізімде көрінеді."
    ),
    kkm = KkmSetupTexts(
        stepOne = "1-қадам. Зауыттық нөмір және шығарылған жылы",
        stepTwo = "2-қадам. ОФД берген сәйкестендіргіш пен токен",
        copy = "Көшіру",
        copied = "Көшірілді",
        factoryReady = "Нөмір алынды. Оны ОФД кабинетіне апарып, сәйкестендіргіш пен токен алыңыз.",
        renameSaved = "Атауы осы жұмыс орнында сақталды",
        renameReset = "ОФД берген атауды қайтару",
        diagnosticsHint = "Тексерулер кассада ештеңені өзгертпейді: олар тораб пен ОФД-дан жағдайды сұрайды.",
        diagnosticsEmpty = "Тексерулер әлі жүргізілген жоқ",
        programmingOn = "Бағдарламалау режимі қосулы",
        programmingOff = "Қалыпты режим",
        ofdEmpty = "ОФД мәлімет жіберген жоқ",
        ofdAnswer = "ОФД жауабы",
        ofdKgdNumber = "МКД тіркеу нөмірі",
        ofdFactoryNumber = "Зауыттық нөмір",
        ofdSystemId = "ОФД-дағы сәйкестендіргіш",
        ofdProtocol = "Хаттама нұсқасы",
        ofdOrganization = "Ұйым",
        ofdAddress = "Орнату мекенжайы",
        ofdBin = "БСН",
        nodeTitle = "Касса торабы",
        nodeMode = "Жұмыс режимі",
        nodeProtocol = "ОФД хаттамасы",
        nodeTimeout = "ОФД жауабын күту, с",
        nodeStorage = "Қойма",
        syncTitle = "ОФД-мен салыстыру",
        syncService = "Касса мәліметтерін салыстыру",
        syncServiceHint = "Ұйым, мекенжай және тіркеу нөмірлері ОФД-дан келеді. " +
            "Ауысым жабық, кезек бос болуы керек.",
        syncServiceDone = "Касса мәліметтері ОФД-дан жаңартылды",
        syncCounters = "Есептегіштерді салыстыру",
        syncCountersHint = "Тораб ОФД-дан есептегіштер мен ауысым нөмірін алады. Кезек бос болуы керек.",
        syncCountersDone = "Есептегіштер ОФД-дан жаңартылды",
        decommission = "Кассаны есептен шығару",
        decommissionHint = "Тораб кассаны құжаттарымен бірге осы жұмыс орнынан жояды. Кері қайтаруға болмайды.",
        needProgramming = "Касса бағдарламалау режимінде",
        needShiftClosed = "Ауысым жабық",
        needQueueEmpty = "Жіберу кезегі бос",
        needOnline = "Дербес режим өшірулі",
        decommissionConfirm = "%s кассасы есептен шығарылсын ба?",
        decommissionDone = "Касса есептен шығарылды"
    )
)

internal val moneyTextsEn = MoneyTexts(
    drawer = DrawerTexts(
        inDrawer = "In the cash drawer",
        countedByNode = "The balance is counted by the register node: it also decides whether a payout fits.",
        unknownBalance = "The node did not report the balance — it will check the payout itself",
        shiftClosed = "The shift is closed. Cash can be paid in and out only while a shift is open.",
        shiftClosedShort = "Shift is closed",
        amountHint = "For example, 1 500.00",
        notANumber = "This is not an amount. Enter a number, for example 1 500.00",
        notPositive = "The amount must be greater than zero",
        tooLarge = "The amount exceeds what the register handles in one operation",
        notEnough = "The drawer holds only %s — there is nothing more to take out",
        confirmDeposit = "Pay in %s?",
        confirmWithdraw = "Take out %s?",
        afterOperation = "The drawer will hold %s",
        confirm = "Confirm",
        cancel = "Cancel",
        working = "Processing…",
        recent = "Recent pay-ins and payouts",
        recentEmpty = "No pay-ins or payouts in the last day",
        recentEmptyHint = "Cash paid in and taken out appears here as soon as it goes through."
    ),
    cashiers = CashierTexts(
        addTitle = "New cashier",
        listTitle = "Cashiers of this register",
        pinLength = "4–8 digits",
        pinUnique = "The node will not give two cashiers of one register the same PIN: it tells them apart by PIN.",
        roles = "An administrator adds cashiers and changes settings, a cashier issues receipts.",
        ownPin = "Your own PIN is changed right here: work continues with the new PIN, no need to sign in again.",
        pinTooShort = "The PIN has fewer than four digits",
        pinTooLong = "The PIN has more than eight digits",
        nameRequired = "Enter the cashier name",
        deleteConfirm = "Delete %s?",
        deleteExplain = "The cashier loses access to the register. The receipts they issued stay in place.",
        deleteBlocked = "This is the only cashier with the role «%s». Add a second one first, then delete.",
        changePinFor = "New PIN for %s",
        onlyInRole = "Only one in the role",
        empty = "No cashiers have been added to this register",
        emptyHint = "Add the first one — they will appear in this list."
    ),
    kkm = KkmSetupTexts(
        stepOne = "Step 1. Factory number and year of manufacture",
        stepTwo = "Step 2. Identifier and token from the OFD",
        copy = "Copy",
        copied = "Copied",
        factoryReady = "The number is ready. Take it to the OFD office and get an identifier and a token.",
        renameSaved = "The name is saved on this workplace",
        renameReset = "Restore the name from the OFD",
        diagnosticsHint = "The checks change nothing in the register: they ask the node and the OFD how things stand.",
        diagnosticsEmpty = "No checks have been run yet",
        programmingOn = "Programming mode is on",
        programmingOff = "Normal mode",
        ofdEmpty = "The OFD sent no details",
        ofdAnswer = "OFD answer",
        ofdKgdNumber = "State Revenue Committee number",
        ofdFactoryNumber = "Factory number",
        ofdSystemId = "Identifier at the OFD",
        ofdProtocol = "Protocol version",
        ofdOrganization = "Organisation",
        ofdAddress = "Installation address",
        ofdBin = "BIN",
        nodeTitle = "Register node",
        nodeMode = "Operating mode",
        nodeProtocol = "OFD protocol",
        nodeTimeout = "OFD answer timeout, s",
        nodeStorage = "Storage",
        syncTitle = "Synchronisation with the OFD",
        syncService = "Refresh register details",
        syncServiceHint = "Organisation, address and registration numbers come from the OFD. " +
            "A closed shift and an empty queue are required.",
        syncServiceDone = "Register details refreshed from the OFD",
        syncCounters = "Refresh counters",
        syncCountersHint = "The node pulls counters and the shift number from the OFD. The queue must be empty.",
        syncCountersDone = "Counters refreshed from the OFD",
        decommission = "Deregister the cash register",
        decommissionHint = "The node deletes the register together with its documents from this workplace. " +
            "This cannot be undone.",
        needProgramming = "The register is in programming mode",
        needShiftClosed = "The shift is closed",
        needQueueEmpty = "The delivery queue is empty",
        needOnline = "Autonomous mode is off",
        decommissionConfirm = "Deregister the register %s?",
        decommissionDone = "The cash register is deregistered"
    )
)

/** Надписи области на выбранном языке. */
fun moneyTexts(language: Language): MoneyTexts = when (language) {
    Language.Kk -> moneyTextsKk
    Language.Ru -> moneyTextsRu
    Language.En -> moneyTextsEn
}
