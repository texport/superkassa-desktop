package kz.mybrain.superkassa.strings.api.cabinet.eds

/**
 * Надписи подписи ЭЦП: ожидание, выбор способа, окна eGov mobile и файла ключа.
 *
 * Свой набор, а не строки входа или заявлений: подпись просят и вход
 * в кабинет, и заявления в КГД, а подписывают всегда одинаково.
 *
 * Сказано в оставшемся времени, а не в «подождите»: владелец должен
 * понимать, ждать ему ещё или чинить.
 */
data class EdsTexts(
    /** Сколько ожидания осталось: подставляется «м:сс». */
    val remaining: String,

    /** Прервать ожидание — тем же местом, где оно началось. */
    val cancelWait: String,

    /** Способ подписи: NCALayer на компьютере. */
    val ncaLayer: String,

    /** Способ подписи: приложение eGov mobile. */
    val egov: String,

    /** Способ подписи: файл ключа на устройстве. */
    val keyFile: String,

    /** Подсказка у входа: как подписывает eGov mobile. */
    val aboutEgov: String,

    /** Подсказка у входа: как подписывает файл ключа. */
    val aboutKeyFile: String,

    /** Подсказка ожидания: подпись идёт в eGov mobile. */
    val waitEgov: String,

    /** Подсказка ожидания: касса ждёт пароль к ключу. */
    val waitKeyFile: String,

    /** Заголовок окна eGov mobile. */
    val egovTitle: String,

    /** Что сделать в окне eGov mobile: QR с телефона или приложение здесь. */
    val egovScan: String,

    /** Открыть eGov mobile на этом устройстве — главное действие окна. */
    val egovOpen: String,

    /** eGov mobile не открылся на этом устройстве: что сделать вместо. */
    val egovNotOpened: String,

    /** Подпись картинки QR для чтения с экрана. */
    val egovQr: String,

    /** Что подписывается — так его называет eGov mobile владельцу. */
    val egovDocument: String,

    /** Заголовок окна файла ключа. */
    val keyTitle: String,

    /** Выбрать другой файл ключа. */
    val keyOther: String,

    /** Поле пароля к ключу. */
    val keyPassword: String,

    /** Подписать ключом — главное действие окна. */
    val keySign: String,

    /** Закрыть окно подписи, не подписывая. */
    val cancel: String,

    /** Пароль не подошёл. */
    val wrongPassword: String,

    /** Файл — не ключ или повреждён: что выбрать. */
    val keyUnreadable: String,

    /** Ключ для входа, а не для подписи: какой выбрать. */
    val keyNotForSigning: String,

    /** Срок сертификата истёк. */
    val keyExpired: String,

    /** Служба подписи eGov mobile не отвечает: что проверить. */
    val egovUnreachable: String,

    /** Время на подпись в eGov mobile вышло. */
    val egovExpired: String,

    /** Владелец отменил подпись у кассы. */
    val cancelled: String
)
