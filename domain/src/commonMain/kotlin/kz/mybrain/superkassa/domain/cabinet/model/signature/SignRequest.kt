package kz.mybrain.superkassa.domain.cabinet.model.signature

import kotlin.time.Instant

/**
 * Что подписывающий просит показать владельцу посреди подписи.
 *
 * NCALayer показывает своё окно сам; eGov mobile и файл ключа своего окна
 * в кассе не имеют, и просьбу показывает экран кассы.
 */
sealed interface SignRequest {

    /**
     * Подписать в eGov mobile: QR — для телефона, ссылка — для приложения
     * на этом же устройстве.
     *
     * @property launch ссылка, открывающая eGov mobile с этой подписью.
     * @property qr картинка QR в PNG.
     * @property until до какого мгновения подпись ждут; `null` — срок не объявлен.
     */
    class EgovMobile(val launch: String, val qr: ByteArray, val until: Instant?) : SignRequest

    /**
     * Ввести пароль к файлу ключа.
     *
     * Неудача с файлом не обрывает подпись: владелец видит её в том же окне
     * и вводит пароль заново или выбирает другой файл.
     *
     * @property file имя выбранного файла: владелец видит, каким ключом подписывает.
     * @property problem чем кончилась прошлая попытка; `null` — попытки не было.
     */
    data class KeyPassword(val file: String, val problem: KeyProblem? = null) : SignRequest
}

/** Почему файл ключа не подписал. */
enum class KeyProblem {
    /** Пароль не подошёл. */
    WrongPassword,

    /** Файл — не ключ ЭЦП или повреждён. */
    Unreadable,

    /** В файле ключ для входа (AUTH), а не для подписи. */
    NotForSigning,

    /** Срок сертификата истёк или ещё не начался. */
    Expired
}

/** Ответ владельца на просьбу подписывающего. */
sealed interface SignAnswer {

    /**
     * Пароль к файлу ключа.
     *
     * Массив, а не строка: подписывающий затирает его, как только ключ
     * открыт, — строку затереть нельзя, и она жила бы до сборки мусора.
     */
    class Password(val chars: CharArray) : SignAnswer

    /** Выбрать другой файл ключа. */
    data object OtherFile : SignAnswer

    /** Владелец передумал подписывать. */
    data object Cancel : SignAnswer
}
