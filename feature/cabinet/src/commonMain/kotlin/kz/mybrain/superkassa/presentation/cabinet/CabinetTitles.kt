package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/** Действие названо словами, а не именем перечисления. */
internal fun actionTitle(code: String, texts: CabinetTexts): String = when (code) {
    "REGISTRATION" -> texts.applications.registration
    "REREGISTRATION" -> texts.applications.reregistration
    "DEREGISTRATION" -> texts.applications.deregistration
    else -> code
}

/**
 * Поле карты, названное словом, а не именем контракта.
 *
 * Кабинет перечисляет изменённое кодами полей; владелец читает список
 * версий, чтобы понять, что именно переписала перерегистрация, и
 * `RETAIL_PLACE` ему об этом не говорит. Незнакомый код показывается
 * как пришёл: своего списка, расходящегося с кабинетом, здесь не заводят.
 */
internal fun cardFieldTitle(code: String, texts: CabinetTexts): String = when (code.uppercase()) {
    // Адрес карты — адрес торговой точки, записанный в КГД. Здесь стояла
    // подпись «Адрес кабинета» — та, которой на экране входа назван
    // сетевой адрес самой службы, — и список изменений карты сообщал,
    // что перерегистрация переписала адрес кабинета.
    "ADDRESS", "RKA", "CATO" -> texts.places.address
    "RETAIL_PLACE", "RETAILPLACE", "RETAIL_PLACE_ID" -> texts.places.name
    "MODEL", "KKM_MODEL", "MODEL_NAME" -> texts.register.model
    "FACTORY_NUMBER", "FACTORYNUMBER" -> texts.register.factoryNumber
    "REGISTRATION_NUMBER", "RNM" -> texts.register.registrationNumber
    "INTERNAL_NAME", "NAME" -> texts.register.internalName
    else -> code
}
