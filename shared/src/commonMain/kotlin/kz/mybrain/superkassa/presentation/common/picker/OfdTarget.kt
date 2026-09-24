package kz.mybrain.superkassa.presentation.common.picker

import kz.mybrain.superkassa.domain.setup.model.OfdContours

/**
 * Куда касса будет слать чеки: БФД и её контур.
 *
 * Адреса серверов БФД касса знает сама — по поставщику и контуру. Владелец
 * хост и порт не вводит.
 *
 * Поставщик стоит подставленным, а не выбранным: в этом продукте он один.
 */
data class OfdTarget(
    val provider: String = BFD_PROVIDER,
    val environment: String = ""
) {
    /** Всё нужное для заведения кассы заполнено. */
    val complete: Boolean get() = provider.isNotBlank() && environment.isNotBlank()
}

/**
 * Код единственной базы фискальных данных этого продукта.
 *
 * Касса заводится только в БФД. Выбор из одного значения владельцу решать
 * нечего. Появится второй оператор — вместе с ним вернётся и список.
 */
const val BFD_PROVIDER = OfdContours.PROVIDER
