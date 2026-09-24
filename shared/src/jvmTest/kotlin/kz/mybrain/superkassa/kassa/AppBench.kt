package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import io.github.texport.superkassa.testing.api.clock.MovableClock
import io.github.texport.superkassa.testing.api.kassa.KassaSetup
import io.github.texport.superkassa.testing.api.kassa.TestBench
import io.github.texport.superkassa.testing.api.kassa.VatMode
import io.github.texport.superkassa.testing.api.kassa.appSuperkassaConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * Стенд проверки с настройками приложения, а не ядра по умолчанию.
 *
 * Приложение говорит с БФД протоколом 2.0.4 и заводит кассы у БФД; стенд
 * ядра по умолчанию — 2.0.3 у другого провайдера, и проверка на нём
 * проверяла бы не ту кассу, что стоит у кассира.
 *
 * @param channels каналы доставки чека вместо одноимённых из настроек —
 *   например, подменный SMS.
 */
internal fun appBench(
    directory: File,
    bfd: FakeBfd = FakeBfd(),
    clock: MovableClock = MovableClock(),
    channels: List<DeliveryPort> = emptyList()
): TestBench = TestBench.open(SuperkassaPlatform(directory.path), bfd, clock, appSuperkassaConfig(channels))

/** Касса, заведённая так, как её заводит приложение: у БФД. */
internal fun appKassa(
    adminPin: String,
    cashierPin: String,
    vat: VatMode = VatMode.NotPayer,
    name: String? = null
): KassaSetup = KassaSetup(adminPin, cashierPin, vat, name, ofdProvider = KassaSetup.BFD)

/**
 * Заказывает доставку чека каналами [channels] — тем же файлом настроек
 * ядра, что у рабочего места. Настройки ядро читает при запуске, поэтому
 * заказ делается до открытия стенда: касса открывается раз, чтобы завести
 * файл, и он дополняется каналами. Получателя у канала нет: чек уходит
 * на контакт покупателя из самого чека.
 *
 * У каждого канала записан и провайдер — адрес или ключ, как их задаёт
 * владелец в настройках: без них экран продажи счёл бы канал ненастроенным
 * и не дал бы выбрать контакт. Отправляет всё равно подменный канал.
 */
internal fun orderDelivery(directory: File, bfd: FakeBfd, channels: List<DeliveryPort>) {
    appBench(directory, bfd).close()
    val file = File(directory, CORE_SETTINGS)
    val settings = Json.parseToJsonElement(file.readText()).jsonObject
    val routes = buildJsonArray {
        channels.forEach { port ->
            add(
                buildJsonObject {
                    put("channel", port.channel.name)
                    put("payloadType", "DOCUMENT")
                    put("documentFormat", "PDF")
                }
            )
        }
    }
    val delivery = buildJsonObject {
        put("channels", routes)
        channels.forEach { port -> providerOf(port.channel.name)?.let { (key, value) -> put(key, value) } }
    }
    file.writeText(JsonObject(settings + ("delivery" to delivery)).toString())
}

/** Провайдер канала в настройках ядра: имя поля и то, без чего канал не настроен. */
private fun providerOf(channel: String): Pair<String, JsonObject>? = when (channel) {
    "SMS" -> "sms" to buildJsonObject { put("providerUrl", "https://sms.example.kz/send?to={phone}&text={text}") }
    "TELEGRAM" -> "telegram" to buildJsonObject { put("botToken", "test-bot") }
    "WHATSAPP" -> "whatsapp" to buildJsonObject {
        put("accessToken", "test-token")
        put("phoneNumberId", "77010000000")
    }
    "EMAIL" -> "email" to buildJsonObject { put("host", "smtp.example.kz") }
    else -> null
}

/** Файл настроек ядра в каталоге данных. */
private const val CORE_SETTINGS = "core-settings.json"
