package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи области «Возврат, история и очередь».
 *
 * Файл принадлежит одной области интерфейса целиком: так надпись заводится
 * в одном месте и переиспользуется, а правки разных экранов не сходятся
 * в одном файле. Каждое поле обязано существовать во всех трёх языках —
 * об этом заботится компилятор.
 *
 * Названий типов документов и состояний доставки здесь нет намеренно: они
 * приходят из справочников узла, и собственный перевод рано или поздно
 * разошёлся бы с тем, что напечатано на чеке.
 */
data class JournalTexts(
    val returns: ReturnJournalTexts,
    val history: HistoryJournalTexts,
    val shifts: ShiftJournalTexts,
    val queue: QueueJournalTexts
)

/** Экран возврата: выбор чека-основания и сумма возврата. */
data class ReturnJournalTexts(
    val itemsToReturn: String,
    val basis: String,
    val basisHint: String,
    val basisColumn: String,
    val chooseBasis: String,
    val chooseBasisHint: String,
    val noBasisHint: String,
    val shiftClosedHint: String,
    val receiptTotal: String,
    val fiscalSign: String,
    val noSaleBasis: String,
    val noBuyBasis: String,
    val shiftClosed: String,
    val amount: String,
    val wholeReceipt: String,
    val partialHint: String,
    val amountInvalid: String,
    val amountTooLarge: String,
    val amountEmpty: String
)

/** Экран истории: отбор по дню и по типу документа. */
data class HistoryJournalTexts(
    val byDay: String,
    val byShift: String,
    val day: String,
    val today: String,
    val earlierDay: String,
    val laterDay: String,
    val documentType: String,
    val allTypes: String,
    val emptyDay: String,
    val emptyDayHint: String,
    val emptyForType: String,
    val emptyForTypeHint: String,
    val colTime: String,
    val colType: String,
    val colNumber: String,
    val colAmount: String,
    val colFiscalSign: String,
    val shown: String,
    val showMore: String,
    val allShown: String
)

/** Прошлые смены и печать Z-отчёта закрытой смены. */
data class ShiftJournalTexts(
    val showMore: String,
    val allShown: String,
    val title: String,
    val load: String,
    val hint: String,
    val none: String,
    val noneHint: String,
    val number: String,
    val opened: String,
    val closed: String,
    val stillOpen: String,
    val zReport: String,
    val documents: String,
    val emptyDocuments: String,
    val emptyDocumentsHint: String,
    val back: String
)

/** Очередь отложенной отправки: состояния, причина неудачи и повтор. */
data class QueueJournalTexts(
    val task: String,
    val sentSection: String,
    val sending: String,
    val retrying: String,
    val rejectedForGood: String,
    val nextAttempt: String,
    val lastFailure: String,
    val retryHint: String,
    val nothingFailed: String,
    val emptyHint: String
)

internal val journalTextsRu = JournalTexts(
    returns = ReturnJournalTexts(
        itemsToReturn = "Что возвращаем",
        basis = "Чек-основание",
        basisHint = "Возврат оформляется только от чека этой же смены. Возврат по возврату не оформляется, " +
            "поэтому в списке их нет.",
        basisColumn = "Чеки смены",
        chooseBasis = "Выберите чек в списке",
        chooseBasisHint = "Сумма чека и фискальный признак появятся здесь.",
        noBasisHint = "Возврат оформляется только по чеку открытой смены.",
        shiftClosedHint = "Откройте смену на главном экране — после этого её чеки появятся в списке.",
        receiptTotal = "Сумма чека",
        fiscalSign = "Фискальный признак",
        noSaleBasis = "В открытой смене нет продаж, по которым можно оформить возврат.",
        noBuyBasis = "В открытой смене нет покупок, по которым можно оформить возврат.",
        shiftClosed = "Смена закрыта. Возврат оформляется в открытой смене.",
        amount = "Сумма возврата",
        wholeReceipt = "Весь чек",
        partialHint = "Можно вернуть часть чека: укажите сумму меньше суммы чека.",
        amountInvalid = "Сумма указана не числом — введите тенге и тиыны через запятую.",
        amountTooLarge = "Больше суммы чека вернуть нельзя.",
        amountEmpty = "Укажите сумму возврата."
    ),
    history = HistoryJournalTexts(
        byDay = "За день",
        byShift = "По сменам",
        day = "День",
        today = "Сегодня",
        earlierDay = "День раньше",
        laterDay = "День позже",
        documentType = "Тип документа",
        allTypes = "Все типы",
        emptyDay = "За этот день узел не отдал ни одного документа.",
        emptyDayHint = "Перелистните на соседний день или вернитесь на сегодня.",
        emptyForType = "За этот день документов выбранного типа нет.",
        emptyForTypeHint = "Снимите отбор по типу — за этот день документы есть.",
        colTime = "Время",
        colType = "Тип",
        colNumber = "Номер",
        colAmount = "Сумма",
        colFiscalSign = "Фискальный признак",
        shown = "Показано",
        showMore = "Показать ещё",
        allShown = "Показан весь день"
    ),
    shifts = ShiftJournalTexts(
        showMore = "Показать ещё",
        allShown = "Показаны все смены",
        title = "Прошлые смены",
        load = "Загрузить смены",
        hint = "Z-отчёт закрытой смены печатается отсюда: искать его в журнале за сутки не нужно.",
        none = "Узел не отдал ни одной смены.",
        noneHint = "Смены появятся здесь после первого открытия смены на этой кассе.",
        number = "Смена №",
        opened = "Открыта",
        closed = "Закрыта",
        stillOpen = "Не закрыта",
        zReport = "Z-отчёт",
        documents = "Документы смены",
        emptyDocuments = "В этой смене документов нет.",
        emptyDocumentsHint = "Смена открывалась и закрывалась, но чеков в ней не пробито.",
        back = "К списку смен"
    ),
    queue = QueueJournalTexts(
        task = "Задача",
        sentSection = "Уже отправлено",
        sending = "Отправляется",
        retrying = "Повтор",
        rejectedForGood = "Не будет отправлен",
        nextAttempt = "Следующая попытка",
        lastFailure = "Причина последней неудачи",
        retryHint = "«Повторить неудачные» ставит заново только те задачи, отправка которых не удалась. " +
            "Остальные уходят сами, вмешательства не требуют.",
        nothingFailed = "Неудачных задач нет — повторять нечего.",
        emptyHint = "Отправлять нечего: касса работает на связи с ОФД."
    )
)

internal val journalTextsKk = JournalTexts(
    returns = ReturnJournalTexts(
        itemsToReturn = "Нені қайтарамыз",
        basis = "Негіздеме чегі",
        basisHint = "Қайтару осы ауысымның чегі бойынша ғана ресімделеді. Қайтару бойынша қайтару " +
            "ресімделмейді, сондықтан олар тізімде жоқ.",
        basisColumn = "Ауысым чектері",
        chooseBasis = "Тізімнен чекті таңдаңыз",
        chooseBasisHint = "Чек сомасы мен фискалдық белгі осында шығады.",
        noBasisHint = "Қайтару ашық ауысымның чегі бойынша ғана ресімделеді.",
        shiftClosedHint = "Ауысымды басты экранда ашыңыз — содан кейін оның чектері тізімде шығады.",
        receiptTotal = "Чек сомасы",
        fiscalSign = "Фискалдық белгі",
        noSaleBasis = "Ашық ауысымда қайтаруға болатын сатылым жоқ.",
        noBuyBasis = "Ашық ауысымда қайтаруға болатын сатып алу жоқ.",
        shiftClosed = "Ауысым жабық. Қайтару ашық ауысымда ресімделеді.",
        amount = "Қайтару сомасы",
        wholeReceipt = "Бүкіл чек",
        partialHint = "Чектің бір бөлігін қайтаруға болады: чек сомасынан кем соманы көрсетіңіз.",
        amountInvalid = "Сома сан емес — теңге мен тиынды үтір арқылы енгізіңіз.",
        amountTooLarge = "Чек сомасынан артық қайтаруға болмайды.",
        amountEmpty = "Қайтару сомасын көрсетіңіз."
    ),
    history = HistoryJournalTexts(
        byDay = "Күн бойынша",
        byShift = "Ауысымдар",
        day = "Күн",
        today = "Бүгін",
        earlierDay = "Алдыңғы күн",
        laterDay = "Келесі күн",
        documentType = "Құжат түрі",
        allTypes = "Барлық түрлері",
        emptyDay = "Бұл күні түйін бірде-бір құжат бермеді.",
        emptyDayHint = "Көрші күнге ауысыңыз немесе бүгінге қайтыңыз.",
        emptyForType = "Бұл күні таңдалған түрдегі құжаттар жоқ.",
        emptyForTypeHint = "Түрі бойынша сүзгіні алыңыз — бұл күні құжаттар бар.",
        colTime = "Уақыты",
        colType = "Түрі",
        colNumber = "Нөмірі",
        colAmount = "Сомасы",
        colFiscalSign = "Фискалдық белгі",
        shown = "Көрсетілді",
        showMore = "Тағы көрсету",
        allShown = "Күн толық көрсетілді"
    ),
    shifts = ShiftJournalTexts(
        showMore = "Тағы көрсету",
        allShown = "Барлық ауысымдар көрсетілді",
        title = "Өткен ауысымдар",
        load = "Ауысымдарды жүктеу",
        hint = "Жабық ауысымның Z-есебі осы жерден басылады: оны тәулік журналынан іздеудің қажеті жоқ.",
        none = "Түйін бірде-бір ауысым бермеді.",
        noneHint = "Ауысымдар осы кассада ауысым алғаш ашылғаннан кейін осында шығады.",
        number = "Ауысым №",
        opened = "Ашылды",
        closed = "Жабылды",
        stillOpen = "Жабылмаған",
        zReport = "Z-есеп",
        documents = "Ауысым құжаттары",
        emptyDocuments = "Бұл ауысымда құжат жоқ.",
        emptyDocumentsHint = "Ауысым ашылып жабылған, бірақ онда чек бұзылмаған.",
        back = "Ауысымдар тізіміне"
    ),
    queue = QueueJournalTexts(
        task = "Тапсырма",
        sentSection = "Жіберілген",
        sending = "Жіберілуде",
        retrying = "Қайталау",
        rejectedForGood = "Жіберілмейді",
        nextAttempt = "Келесі әрекет",
        lastFailure = "Соңғы сәтсіздіктің себебі",
        retryHint = "«Сәтсіздерін қайталау» жіберілмей қалған тапсырмаларды ғана қайта кезекке қояды. " +
            "Қалғандары өздігінен кетеді, араласуды қажет етпейді.",
        nothingFailed = "Сәтсіз тапсырма жоқ — қайталайтын ештеңе жоқ.",
        emptyHint = "Жіберетін ештеңе жоқ: касса ОФД-мен байланыста жұмыс істеп тұр."
    )
)

internal val journalTextsEn = JournalTexts(
    returns = ReturnJournalTexts(
        itemsToReturn = "What is returned",
        basis = "Original receipt",
        basisHint = "A refund is issued only against a receipt of the same shift. A refund of a refund is " +
            "not allowed, so such receipts are not listed.",
        basisColumn = "Receipts of the shift",
        chooseBasis = "Pick a receipt in the list",
        chooseBasisHint = "The receipt total and the fiscal sign will appear here.",
        noBasisHint = "A refund is issued only against a receipt of the open shift.",
        shiftClosedHint = "Open the shift on the dashboard — its receipts will then appear in the list.",
        receiptTotal = "Receipt total",
        fiscalSign = "Fiscal sign",
        noSaleBasis = "The open shift has no sales that can be refunded.",
        noBuyBasis = "The open shift has no purchases that can be refunded.",
        shiftClosed = "The shift is closed. A refund is issued while the shift is open.",
        amount = "Refund amount",
        wholeReceipt = "Whole receipt",
        partialHint = "Part of a receipt can be refunded: enter an amount below the receipt total.",
        amountInvalid = "The amount is not a number — enter tenge and tiyn separated by a comma.",
        amountTooLarge = "Cannot refund more than the receipt total.",
        amountEmpty = "Enter the refund amount."
    ),
    history = HistoryJournalTexts(
        byDay = "By day",
        byShift = "By shift",
        day = "Day",
        today = "Today",
        earlierDay = "Previous day",
        laterDay = "Next day",
        documentType = "Document type",
        allTypes = "All types",
        emptyDay = "The node returned no documents for this day.",
        emptyDayHint = "Step to a neighbouring day or go back to today.",
        emptyForType = "No documents of the selected type on this day.",
        emptyForTypeHint = "Clear the type filter — there are documents on this day.",
        colTime = "Time",
        colType = "Type",
        colNumber = "Number",
        colAmount = "Amount",
        colFiscalSign = "Fiscal sign",
        shown = "Shown",
        showMore = "Show more",
        allShown = "The whole day is shown"
    ),
    shifts = ShiftJournalTexts(
        showMore = "Show more",
        allShown = "All shifts are shown",
        title = "Past shifts",
        load = "Load shifts",
        hint = "The Z-report of a closed shift is printed here: no need to hunt for it in the daily journal.",
        none = "The node returned no shifts.",
        noneHint = "Shifts appear here after a shift has been opened on this cash register.",
        number = "Shift no.",
        opened = "Opened",
        closed = "Closed",
        stillOpen = "Not closed",
        zReport = "Z-report",
        documents = "Shift documents",
        emptyDocuments = "This shift has no documents.",
        emptyDocumentsHint = "The shift was opened and closed, but no receipts were issued in it.",
        back = "Back to shifts"
    ),
    queue = QueueJournalTexts(
        task = "Task",
        sentSection = "Already sent",
        sending = "Sending",
        retrying = "Retrying",
        rejectedForGood = "Will not be sent",
        nextAttempt = "Next attempt",
        lastFailure = "Reason of the last failure",
        retryHint = "“Retry failed” re-queues only the tasks whose sending failed. The rest go out on their " +
            "own and need no intervention.",
        nothingFailed = "No failed tasks — nothing to retry.",
        emptyHint = "Nothing to send: the cash register is online with the OFD."
    )
)

/** Надписи области на выбранном языке. */
fun journalTexts(language: Language): JournalTexts = when (language) {
    Language.Kk -> journalTextsKk
    Language.Ru -> journalTextsRu
    Language.En -> journalTextsEn
}
