package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.KkmInitRequest
import kz.mybrain.superkassa.desktop.server.initKkm
import kz.mybrain.superkassa.desktop.server.updateKkmName

/**
 * Заведение кассы на этой машине.
 *
 * Ход один и тот же везде, откуда кассу заводят: мастер подключения,
 * настройки ОФД и паспорт кассы в кабинете. Написанный в каждом месте
 * заново, он расходился уже в мелочах — где-то список касс перечитывался
 * после заведения, где-то нет, и только что заведённая касса не появлялась
 * на экране до перезапуска.
 *
 * Подпись сообщения передаётся, а не берётся из надписей: в строке отказа
 * должно стоять название того действия, которое затеял владелец, — «завести
 * кассу» или «шаг мастера», — и ни в коем случае не пин администратора.
 *
 * Название владелец дал кассе в кабинете, и туда же, на узел, оно уходит
 * сразу за заведением: иначе касса рождается безымянной и на экране входа
 * её видно одним регистрационным номером. Пишется оно пином заводимой
 * кассы — со стандартным узел не пускает.
 *
 * @return заведённая касса, какой её знает узел; `null` — узел отказал.
 */
suspend fun Session.enrollKkm(request: KkmInitRequest, what: String, name: String? = null): Kkm? {
    val created = guard(what) { client.initKkm(request, BOOTSTRAP_PIN) } ?: return null
    nameNewKkm(created, name, request.adminPin)
    refreshKkms()
    report("${texts.settings.registered}: ${titleOf(Dictionary.KkmStates, created.state)}")
    // Из перечитанного списка, а не из ответа: узел досылает в списке то,
    // чего в ответе на заведение ещё нет, — номер КГД и сведения об ОФД.
    return kkms.firstOrNull { it.kkmId == created.kkmId } ?: created
}

/**
 * Даёт только что заведённой кассе название из кабинета.
 *
 * Отказ здесь не рвёт заведение: касса заведена и работает, а безымянную
 * её назовут из настроек или следующим входом владельца в кабинет.
 */
private suspend fun Session.nameNewKkm(created: Kkm, name: String?, adminPin: String?) {
    val chosen = name?.takeIf { it.isNotBlank() } ?: return
    val pin = adminPin?.takeIf { it.isNotBlank() } ?: return
    quietly(NAMING) { client.updateKkmName(created.kkmId, chosen, pin) }
}

/** Чем подписано наименование заведённой кассы в журнале. */
private const val NAMING = "название заведённой кассы"

/**
 * Переводит рабочее место на эту кассу.
 *
 * Пин принадлежит той кассе, в которую по нему вошли: оставить его
 * при переходе значит дать чужой кассе чужие права. Поэтому вход
 * начинается заново — с пина этой кассы.
 *
 * Та касса, за которой уже работают, не выбирается заново: выбор забывает
 * сведения смены, а входа, который наберёт их снова, здесь не будет —
 * на главной вместо наличных в кассе оставался прочерк.
 */
fun Session.workOn(kkm: Kkm) {
    if (selected?.kkmId == kkm.kkmId) {
        return
    }
    signOut()
    select(kkm)
}

/**
 * Пин, которым узел подтверждает право заводить кассы.
 *
 * Это не пин будущей кассы: её администратор получает тот, что набирает
 * владелец. Со стандартным касса рождалась бы недоступной — узел
 * не пускает по нему, а сменить его можно только войдя.
 */
private const val BOOTSTRAP_PIN = "0000"
