package kz.mybrain.superkassa.desktop.server

import io.ktor.http.HttpMethod

/**
 * Обращения к узлу, относящиеся к самой кассе: список, заведение, состояние,
 * кассиры и связь с ОФД.
 */
suspend fun ServerClient.listKkms(): List<Kkm> =
    request<Page<Kkm>>(HttpMethod.Get, "/kkm?limit=$PAGE_LIMIT").items

suspend fun ServerClient.kkm(kkmId: String, pin: String): Kkm =
    request(HttpMethod.Get, "/kkm/$kkmId", pin = pin)

/**
 * Кто работает под этим пином.
 *
 * Роль спрашивается один раз при входе: рабочее место показывает кассиру
 * только его разделы, а не встречает отказом на каждом чужом.
 */
suspend fun ServerClient.currentUser(kkmId: String, pin: String): KkmUser =
    request(HttpMethod.Get, "/kkm/$kkmId/users/me", pin = pin)

suspend fun ServerClient.initKkm(body: KkmInitRequest, pin: String): Kkm =
    request(HttpMethod.Post, "/kkm/init", body, pin)

/**
 * Сохраняет настройки печатной формы.
 *
 * Узел принимает весь набор целиком, поэтому отправляется он весь:
 * послать одно поле значило бы обнулить остальные.
 */
suspend fun ServerClient.updateBranding(kkmId: String, branding: Branding, pin: String): Kkm =
    request(HttpMethod.Put, "/kkm/$kkmId/settings/branding", branding, pin)

/**
 * Сохраняет название кассы на узле.
 *
 * Название живёт у кассы, а не на рабочем месте: иначе назвавший её
 * владелец видит название только за своей машиной, а кассир за соседней —
 * по-прежнему регистрационный номер.
 */
suspend fun ServerClient.updateKkmName(kkmId: String, name: String?, pin: String): Kkm =
    request(HttpMethod.Put, "/kkm/$kkmId/settings/name", KkmNameRequest(name), pin)

/** Сколько касс читать за раз: парк узла в режиме DESKTOP заведомо меньше. */
private const val PAGE_LIMIT = 500

/**
 * Заводской номер и год выпуска для регистрации в ОФД.
 *
 * Номер выдаёт узел по алгоритму производителя: придуманный вручную ОФД
 * не примет. В базе кассы этот вызов ничего не создаёт.
 */
suspend fun ServerClient.factoryInfo(): FactoryInfo =
    request(HttpMethod.Get, "/kkm/factory-info")
