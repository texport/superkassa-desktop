package kz.mybrain.superkassa.presentation.settings.core

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import kz.mybrain.superkassa.domain.settings.model.KassaFacts
import kz.mybrain.superkassa.domain.settings.model.frozen
import kz.mybrain.superkassa.domain.settings.model.secondsOf
import kz.mybrain.superkassa.domain.settings.model.server
import kz.mybrain.superkassa.presentation.strings.kassa.KkmSetupTexts
import kz.mybrain.superkassa.presentation.strings.settings.CoreSettingTexts
import kz.mybrain.superkassa.presentation.strings.settings.KassaFactsTexts

/**
 * Настройки кассы на этой машине, как их видит владелец.
 *
 * @property settings действующие настройки; `null` — ещё не прочитаны.
 * @property timeoutDraft набранное ожидание ответа БФД, секунды.
 * @property reconnectDraft набранный повтор связи с БФД, секунды.
 * @property about версии, каталог данных и число касс; `null` — ещё не прочитаны.
 */
data class CoreSettingsUiState(
    val settings: CoreSettings? = null,
    val about: KassaFacts? = null,
    val timeoutDraft: String? = null,
    val reconnectDraft: String? = null,
    val busy: Boolean = false
) {
    /** Ожидание в поле: набранное, иначе действующее. */
    val timeout: String get() = timeoutDraft ?: settings?.ofdTimeoutSeconds?.toString().orEmpty()

    /** Повтор в поле: набранный, иначе действующий. */
    val reconnect: String get() = reconnectDraft ?: settings?.ofdReconnectIntervalSeconds?.toString().orEmpty()

    /** Касса работает сервером: её настройки меняют там, а не здесь. */
    val server: Boolean get() = settings?.server == true

    /** Правка закрыта: видно до нажатия, а не отказом после него. */
    val frozen: Boolean get() = settings?.frozen == true

    /** Набранное годится: целое число секунд больше нуля. */
    val timeoutValid: Boolean get() = secondsOf(timeout) != null

    val reconnectValid: Boolean get() = secondsOf(reconnect) != null

    /** Набранное отличается от действующего и годится. */
    private val changed: Boolean
        get() {
            val newTimeout = secondsOf(timeout) ?: return false
            val newReconnect = secondsOf(reconnect) ?: return false
            val current = settings ?: return true
            return newTimeout != current.ofdTimeoutSeconds || newReconnect != current.ofdReconnectIntervalSeconds
        }

    /** Сохранять есть что, касса это примет, и сейчас она не занята. */
    val savable: Boolean get() = settings != null && !frozen && !busy && changed

    /**
     * Что касса рассказывает о себе: версии, режим, протокол, хранилище,
     * каталог данных и число касс — то, что прежде показывала карточка узла.
     *
     * Адрес сервера базы и его учётные данные на экран не выводятся: экран
     * настроек показывают и снимают, и строка с ними уехала бы в чужой снимок.
     * Каталог данных — путь на этой машине: секрета в нём нет, а поддержка
     * ищет там базу.
     */
    fun facts(about: KassaFactsTexts, texts: CoreSettingTexts, kkm: KkmSetupTexts): List<Pair<String, String>> {
        val now = settings
        return listOfNotNull(
            this.about?.let { about.appVersion to it.appVersion },
            this.about?.let { about.coreVersion to it.coreVersion },
            now?.let { texts.mode to if (it.server) texts.modeServer else texts.modeDesktop },
            now?.let { kkm.bfdProtocol to protocolLabel(it.ofdProtocolVersion) },
            now?.let { kkm.kassaStorage to storageLabel(it.storage.engine, kkm) },
            this.about?.directory?.let { about.dataDirectory to it },
            this.about?.kkmCount?.let { about.kkmCount to it.toString() }
        )
    }
}

/** Что владелец меняет в настройках кассы. Пустые действия — для снимков вида. */
interface CoreSettingsActions {
    fun reload() = Unit

    fun typeTimeout(text: String) = Unit

    fun typeReconnect(text: String) = Unit

    fun save() = Unit
}

/** Версия протокола, как её пишут в документах: «204» — «2.0.4». Иное — как есть. */
internal fun protocolLabel(version: String): String =
    if (version.length == PROTOCOL_DIGITS && version.all(Char::isDigit)) version.toList().joinToString(".") else version

/**
 * Хранилище кассы словами: где лежит база, а за ним — чья она. Имя
 * перечисления «SQLITE» владельцу ничего не говорит.
 */
internal fun storageLabel(engine: String, texts: KkmSetupTexts): String =
    when (val product = STORAGE_PRODUCTS[engine.uppercase()]) {
        null -> engine
        SQLITE -> "${texts.storageLocal} ($product)"
        else -> "${texts.storageServer} ($product)"
    }

private const val PROTOCOL_DIGITS = 3
private const val SQLITE = "SQLite"

/** Названия продуктов баз, как их пишут сами производители. */
private val STORAGE_PRODUCTS = mapOf("SQLITE" to SQLITE, "POSTGRESQL" to "PostgreSQL", "MYSQL" to "MySQL")
