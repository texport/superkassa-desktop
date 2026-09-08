package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.desktop.server.cabinet.KkmModel
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import java.time.Year

/**
 * Черновик заводимой кассы.
 *
 * Пять полей формы жили пятью отдельными состояниями и передавались
 * дальше пятью же значениями и пятью обработчиками: подпись любой
 * составляющей, работающей с формой, занимала десяток строк. Здесь они
 * собраны в одно — форма получает черновик, а не разобранную на части
 * кассу.
 */
class RegisterDraft(known: FactoryStamp?) {
    var place by mutableStateOf<RetailPlace?>(null)
    var model by mutableStateOf<KkmModel?>(null)

    /** Заводской номер и год: из мастера, если узел их уже выдал. */
    var factory by mutableStateOf(known?.number.orEmpty())
    var year by mutableStateOf(known?.year ?: Year.now().value.toString())

    var name by mutableStateOf("")
}

/**
 * Заводской номер и год, уже выданные узлом.
 *
 * В мастере они получены на первом шаге и правке не подлежат: набранный
 * заново номер разошёлся бы с тем, что унесли в кабинет.
 */
data class FactoryStamp(val number: String, val year: String)

/** Год выпуска — четыре цифры. */
const val YEAR_DIGITS = 4
