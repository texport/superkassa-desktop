package kz.mybrain.superkassa.presentation.strings.update

import kz.mybrain.superkassa.presentation.strings.common.Language

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
    /** Установщик скачивается и сверяется: кнопку «Скачать» гасят. */
    val downloading: String,
    val later: String,
    val unreachable: String,
    /** Установщик скачан, совпал с выпуском и открыт. */
    val installerOpened: String,
    /** Сверить установщик нечем: открыта страница выпуска. */
    val pageOpened: String,
    /** Скачанный установщик не совпал с выпуском. */
    val installerTampered: String
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
        "установщик она скачивает и сверяет по нажатию, а ставит владелец, когда смена закрыта",
    installed = "Установлена версия",
    automatic = "Проверять обновления автоматически",
    lastChecked = "Последняя проверка",
    neverChecked = "ещё не проверялось",
    checkNow = "Проверить сейчас",
    checking = "Проверяется…",
    upToDate = "Установлена последняя версия",
    available = "Доступна версия",
    availableHint = "Касса скачает установщик и сверит его с выпуском: не совпавший не откроется. " +
        "Ставить новую версию лучше при закрытой смене: касса закроется, а её данные останутся на месте.",
    download = "Скачать",
    downloading = "Скачивается и сверяется…",
    later = "Позже",
    unreachable = "Не удалось проверить: нет связи",
    installerOpened = "Установщик скачан, совпал с выпуском и открыт: ставьте новую версию при закрытой смене",
    pageOpened = "Сверить установщик не с чем: открыта страница выпуска, скачайте файл там",
    installerTampered = "Скачанный установщик не совпал с выпуском и удалён: ставить его нельзя. Повторите позже"
)

private val updateTextsKk = UpdateTexts(
    appName = "Суперкасса",
    title = "Жаңартулар",
    hint = "Касса тәулігіне бір рет жаңа нұсқа шықпағанын сұрайды. Өзі ештеңе орнатпайды: " +
        "орнатқышты басу бойынша жүктеп тексереді, ал иесі оны ауысым жабық кезде орнатады",
    installed = "Орнатылған нұсқа",
    automatic = "Жаңартуларды автоматты тексеру",
    lastChecked = "Соңғы тексеру",
    neverChecked = "әлі тексерілмеген",
    checkNow = "Қазір тексеру",
    checking = "Тексерілуде…",
    upToDate = "Соңғы нұсқа орнатылған",
    available = "Қолжетімді нұсқа",
    availableHint = "Касса орнатқышты жүктеп, шығарылыммен салыстырады: сәйкес келмегені ашылмайды. " +
        "Жаңа нұсқаны ауысым жабық кезде орнатқан жөн: касса жабылады, ал оның деректері орнында қалады.",
    download = "Жүктеу",
    downloading = "Жүктеліп, тексерілуде…",
    later = "Кейінірек",
    unreachable = "Тексеру мүмкін емес: байланыс жоқ",
    installerOpened = "Орнатқыш жүктелді, шығарылыммен сәйкес келді және ашылды: " +
        "жаңа нұсқаны ауысым жабық кезде орнатыңыз",
    pageOpened = "Орнатқышты салыстыратын ештеңе жоқ: шығарылым беті ашылды, файлды сол жерден жүктеңіз",
    installerTampered = "Жүктелген орнатқыш шығарылыммен сәйкес келмеді және жойылды: " +
        "оны орнатуға болмайды. Кейінірек қайталаңыз"
)

private val updateTextsEn = UpdateTexts(
    appName = "Superkassa",
    title = "Updates",
    hint = "Once a day the till asks whether a new version is out. It installs nothing itself: " +
        "it downloads and checks the installer on request, and the owner installs it once the shift is closed",
    installed = "Installed version",
    automatic = "Check for updates automatically",
    lastChecked = "Last checked",
    neverChecked = "never checked",
    checkNow = "Check now",
    checking = "Checking…",
    upToDate = "The latest version is installed",
    available = "Version available",
    availableHint = "The till downloads the installer and checks it against the release: " +
        "one that does not match will not open. Install the new version with the shift closed: " +
        "the till closes, while its data stay in place.",
    download = "Download",
    downloading = "Downloading and checking…",
    later = "Later",
    unreachable = "Could not check: no connection",
    installerOpened = "The installer is downloaded, matches the release and is open: " +
        "install the new version with the shift closed",
    pageOpened = "There is nothing to check the installer against: the release page is open, download the file there",
    installerTampered = "The downloaded installer did not match the release and was removed: " +
        "it must not be installed. Try again later"
)
