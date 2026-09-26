package kz.mybrain.superkassa.domain.print.model

/**
 * Как поделиться чеком с покупателем.
 *
 * Отправку ведёт не касса, а то, чем покупателю пишут и так: окно
 * «Поделиться» Android или мессенджер и почта компьютера. Шлюзы SMS,
 * ключи ботов и почтовые серверы для этого настраивать не нужно.
 *
 * @property carriesFile отдаётся сам файл формы; иначе — только ссылка
 *   на электронный чек, и без неё делиться нечем.
 */
enum class ShareWay(val carriesFile: Boolean) {
    /** Системное окно «Поделиться»: Android сам предложит мессенджеры и почту. */
    System(carriesFile = true),

    /** WhatsApp: сообщение со ссылкой на чек. */
    WhatsApp(carriesFile = false),

    /** Telegram: сообщение со ссылкой на чек. */
    Telegram(carriesFile = false),

    /** Письмо со ссылкой на чек в почтовой программе. */
    Email(carriesFile = false)
}

/**
 * Чек, которым делятся: файл формы, ссылка на электронный чек и слова к ним.
 *
 * @property name имя файла с расширением.
 * @property mime вид файла для системы.
 * @property subject заголовок письма или сообщения.
 * @property text слова сообщения; ссылка на чек, если она есть, — в них.
 * @property link ссылка на электронный чек у БФД; `null` — её ещё нет:
 *   чек не принят или пробит без связи.
 */
class SharedReceipt(
    val bytes: ByteArray,
    val name: String,
    val mime: String,
    val subject: String,
    val text: String,
    val link: String?
)

/** Чем кончилась попытка поделиться. */
enum class Shared {
    /** Окно системы или программа открыты: дальше отправляет кассир. */
    Opened,

    /** Этим путём уходит только ссылка, а её у чека ещё нет. */
    NoLink,

    /** Система ничего не открыла. */
    Failed
}

/** Вид файла для системы — по виду формы. */
val PrintKind.mime: String
    get() = when (this) {
        PrintKind.Png -> "image/png"
        PrintKind.Pdf -> "application/pdf"
        PrintKind.Html -> "text/html"
    }
