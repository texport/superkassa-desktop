package kz.mybrain.superkassa.desktop.server

import kotlinx.serialization.Serializable

/**
 * Сведения о кассе в том виде, в каком их отдаёт узел.
 *
 * Поля объявлены необязательными намеренно: узел не присылает то, чего у кассы
 * ещё нет, и приложение обязано это переживать, а не падать на разборе.
 */
@Serializable
data class Kkm(
    val kkmId: String,
    /**
     * Название, данное кассе владельцем и сохранённое на узле.
     *
     * Хранит его узел, а не рабочее место: тогда одну и ту же кассу
     * одинаково зовут за всеми машинами и видно это до входа кассира.
     * У касс, заведённых до появления поля, названия нет.
     */
    val name: String? = null,
    val ofdSystemId: String? = null,
    val kkmKgdId: String? = null,
    val factoryNumber: String? = null,
    val state: String? = null,
    val mode: String? = null,
    val ofdId: String? = null,
    val ofdEnvironment: String? = null,
    val lastShiftNo: Int? = null,
    val blockReasonCode: Int? = null,
    val autonomousSince: Long? = null,
    val taxRegime: String? = null,
    val defaultVatGroup: String? = null,
    /**
     * Закрывает ли узел смену сам по истечении суток.
     *
     * Узел не присылает поле, когда оно ложно, поэтому здесь ложь и стоит
     * значением по умолчанию: иначе отсутствие поля читалось бы как
     * включённое автозакрытие.
     */
    /** Изымать ли наличные при закрытии смены: узел делает это сам. */
    val autoCashout: Boolean = false,
    val ofdServiceInfo: OrgInfo? = null,
    val branding: Branding? = null
) {
    /**
     * Имя кассы для кассира.
     *
     * Название организации на всех кассах одно и то же, и в списке из
     * десяти строк «Организация» кассир свою не найдёт. Опознаётся касса
     * регистрационным номером КГД — он написан на самой машине и стоит
     * в чеке; заводской номер и внутренний код идут запасными.
     */
    val title: String
        get() = kkmKgdId?.takeIf { it.isNotBlank() }
            ?: factoryNumber?.takeIf { it.isNotBlank() }
            ?: kkmId.take(SHORT_ID)

    /** Организация, от имени которой касса работает. */
    val orgTitle: String
        get() = ofdServiceInfo?.orgTitle?.takeIf { it.isNotBlank() } ?: "Организация не указана"

    /** Адрес установки: две кассы одной организации различают по нему. */
    val orgAddress: String
        get() = ofdServiceInfo?.orgAddress?.takeIf { it.isNotBlank() } ?: ""

    /** Подходит ли касса под введённое кассиром: номер КГД, заводской или код. */
    fun matches(query: String, localName: String? = null): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return listOfNotNull(kkmKgdId, factoryNumber, ofdSystemId, kkmId, orgTitle, orgAddress, localName)
            .any { it.contains(needle, ignoreCase = true) }
    }

    val isBlocked: Boolean get() = state == "BLOCKED"

    /**
     * Касса в режиме программирования.
     *
     * Фискальных команд она в этом состоянии не принимает, и смену узел
     * в нём не показывает: «смена закрыта» на экране в этот момент —
     * не состояние смены, а отказ читать её.
     */
    val isProgramming: Boolean get() = state == "PROGRAMMING"
    val isAutonomous: Boolean get() = autonomousSince != null
}

@Serializable
data class OrgInfo(
    val orgTitle: String? = null,
    val orgAddress: String? = null,
    val orgInn: String? = null
)

/**
 * Постраничный ответ узла.
 *
 * Узел отдаёт перечни обёрнутыми: сам список, общее число и признак того,
 * что есть ещё. Приложение читало ответ как голый массив и не разбирало
 * его вовсе — на экране это выглядело недоступностью узла.
 */
@Serializable
data class Page<T>(
    val items: List<T> = emptyList(),
    val total: Int = 0,
    val limit: Int = 0,
    val offset: Int = 0,
    val hasMore: Boolean = false
)

/** Запрос на заведение кассы. */
/**
 * Настройки печатной формы, какими их держит узел.
 *
 * Здесь только то, что задаётся на рабочем месте: язык чека, ширина ленты
 * и печать рекламы ОФД. Прочие поля брендирования узел хранит, и при
 * сохранении их нужно вернуть как есть, иначе они обнулятся.
 */
@Serializable
data class Branding(
    val language: String? = null,
    val paperWidthMm: Int? = null,
    val printOfdTicketAds: Boolean? = null,
    val ofdTicketAds: List<TicketAd> = emptyList(),
    val themeColor: String? = null,
    val headerLogoUrl: String? = null,
    val beforeHeaderMsg: String? = null,
    val headerMsg: String? = null,
    val afterHeaderMsg: String? = null,
    val beforeItemsMsg: String? = null,
    val afterItemsMsg: String? = null,
    val beforeTotalsMsg: String? = null,
    val afterTotalsMsg: String? = null,
    val beforeQrMsg: String? = null,
    val footerMsg: String? = null,
    val useForceDarkTheme: Boolean? = null,
    val customBackgroundColorHex: String? = null,
    val customCardTopBorderColorHex: String? = null
)

/**
 * Рекламная строка ОФД: вид и версия нужны узлу, чтобы просить у ОФД
 * только новые объявления, а рабочему месту — чтобы вернуть их как есть.
 */
@Serializable
data class TicketAd(
    val type: String = "TICKET_AD_OFD",
    val version: Long = 0,
    val text: String
)

/**
 * Название кассы для узла.
 *
 * Пустое значение снимает название: касса снова показывается
 * регистрационным номером.
 */
@Serializable
data class KkmNameRequest(val name: String?)

@Serializable
data class KkmInitRequest(
    val ofdId: String,
    val ofdEnvironment: String,
    val ofdSystemId: String,
    val ofdToken: String,
    /** Пин администратора заводимой кассы: со стандартным в неё не войти. */
    val adminPin: String? = null
)

@Serializable
data class KkmUser(
    val userId: String? = null,
    val id: String? = null,
    val name: String? = null,
    val role: String? = null
) {
    val identifier: String get() = userId ?: id.orEmpty()
}

@Serializable
data class KkmUserRequest(
    val name: String,
    val role: String,
    val userPin: String
)

/** Сколько знаков внутреннего кода показывать, когда номеров нет. */
private const val SHORT_ID = 8

/** Заводские данные, которые касса относит в ОФД для регистрации. */
@Serializable
data class FactoryInfo(
    val factoryNumber: String,
    val manufactureYear: Int
)
