package kz.mybrain.superkassa.desktop.ui.strings

/**
 * Надписи о версии кассы и её обновлениях.
 *
 * Своим набором: живут в углу рельса, в окне о новой версии и в карточке
 * настроек, и ни к одному экрану целиком не относятся.
 */
data class UpdateTexts(
    /** Название приложения на языке кассира — для подсказки к версии. */
    val appName: String,
    val title: String,
    val hint: String,
    val installed: String,
    val automatic: String,
    val lastChecked: String,
    val neverChecked: String,
    val checkNow: String,
    val checking: String,
    val upToDate: String,
    /** «Доступна версия»: за ней ставится номер. */
    val available: String,
    val availableHint: String,
    val download: String,
    val later: String,
    val unreachable: String
)

/** Надписи об обновлениях на выбранном языке. */
fun updateTexts(language: Language): UpdateTexts = when (language) {
    Language.Kk -> updateTextsKk
    Language.Ru -> updateTextsRu
    Language.En -> updateTextsEn
}

private val updateTextsRu = UpdateTexts(
    appName = "Суперкасса",
    title = "Обновления",
    hint = "Касса раз в сутки спрашивает, не вышла ли новая версия. Сама она ничего не ставит: " +
        "новую версию скачивает и ставит владелец, когда смена закрыта",
    installed = "Установлена версия",
    automatic = "Проверять обновления автоматически",
    lastChecked = "Последняя проверка",
    neverChecked = "ещё не проверялось",
    checkNow = "Проверить сейчас",
    checking = "Проверяется…",
    upToDate = "Установлена последняя версия",
    available = "Доступна версия",
    availableHint = "Установщик скачается в браузере. Ставить новую версию лучше при закрытой смене: " +
        "касса закроется, а узел и её данные останутся на месте.",
    download = "Скачать",
    later = "Позже",
    unreachable = "Не удалось проверить: нет связи"
)

private val updateTextsKk = UpdateTexts(
    appName = "Суперкасса",
    title = "Жаңартулар",
    hint = "Касса тәулігіне бір рет жаңа нұсқа шықпағанын сұрайды. Өзі ештеңе орнатпайды: " +
        "жаңа нұсқаны иесі ауысым жабық кезде жүктеп орнатады",
    installed = "Орнатылған нұсқа",
    automatic = "Жаңартуларды автоматты тексеру",
    lastChecked = "Соңғы тексеру",
    neverChecked = "әлі тексерілмеген",
    checkNow = "Қазір тексеру",
    checking = "Тексерілуде…",
    upToDate = "Соңғы нұсқа орнатылған",
    available = "Қолжетімді нұсқа",
    availableHint = "Орнатқыш браузерде жүктеледі. Жаңа нұсқаны ауысым жабық кезде орнатқан жөн: " +
        "касса жабылады, ал түйін мен оның деректері орнында қалады.",
    download = "Жүктеу",
    later = "Кейінірек",
    unreachable = "Тексеру мүмкін емес: байланыс жоқ"
)

private val updateTextsEn = UpdateTexts(
    appName = "Superkassa",
    title = "Updates",
    hint = "Once a day the till asks whether a new version is out. It installs nothing itself: " +
        "the owner downloads and installs the new version once the shift is closed",
    installed = "Installed version",
    automatic = "Check for updates automatically",
    lastChecked = "Last checked",
    neverChecked = "never checked",
    checkNow = "Check now",
    checking = "Checking…",
    upToDate = "The latest version is installed",
    available = "Version available",
    availableHint = "The installer downloads in your browser. Install the new version with the shift closed: " +
        "the till closes, while the node and its data stay in place.",
    download = "Download",
    later = "Later",
    unreachable = "Could not check: no connection"
)
