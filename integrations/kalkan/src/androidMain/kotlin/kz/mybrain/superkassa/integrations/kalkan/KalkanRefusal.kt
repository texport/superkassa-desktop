package kz.mybrain.superkassa.integrations.kalkan

/** Почему файл ключа не подписал. */
enum class KalkanReason {
    /** Пароль к файлу не подошёл: владелец вводит его заново. */
    WrongPassword,

    /** Файл — не ключ PKCS#12, повреждён или ключ незнакомого алгоритма. */
    Unreadable,

    /** В файле ключ для входа (AUTH), а не для подписи. */
    NotForSigning,

    /** Срок сертификата истёк или ещё не начался. */
    Expired
}

/**
 * Файл ключа не подписал.
 *
 * В сообщение не попадают ни пароль, ни содержимое файла, ни сертификат:
 * только причина — отказ уходит в журнал.
 */
class KalkanRefusal(val reason: KalkanReason, cause: Throwable? = null) : Exception("key file: $reason", cause)
