package kz.mybrain.superkassa.navigation.section

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Раздел рабочего окна — верхний уровень навигации.
 *
 * Разделы открываются из навигации окна (нижней полосы, рельса) и стоят
 * в истории «назад» над главным экраном: по рекомендации Material 3
 * «назад» из любого раздела ведёт на главный, а с него — из приложения.
 */
@Serializable
sealed interface SectionKey : NavKey

/** Главный экран: смена и её итоги. */
@Serializable
data object DashboardKey : SectionKey

/** Продажа. */
@Serializable
data object SaleKey : SectionKey

/** Возврат по чеку. */
@Serializable
data object ReturnsKey : SectionKey

/** Деньги в ящике: внесение и изъятие. */
@Serializable
data object CashKey : SectionKey

/** История документов и смен. */
@Serializable
data object HistoryKey : SectionKey

/** Очередь отправки в БФД. */
@Serializable
data object QueueKey : SectionKey

/** Кассиры и пины. */
@Serializable
data object UsersKey : SectionKey

/** Заведение новой кассы. */
@Serializable
data object RegisterKey : SectionKey

/** Кабинет БФД. */
@Serializable
data object CabinetKey : SectionKey

/** Настройки. */
@Serializable
data object SettingsKey : SectionKey

/**
 * Кассы рабочего места и вход по пину — начало окна до входа.
 *
 * Окно до входа устроено так же, как рабочее: разделы «Кассы», «Новая
 * касса», «Кабинет БФД» и «Настройки» в той же навигации, «назад» из
 * любого ведёт сюда. Разделы, общие с рабочим окном, — те же ключи
 * [RegisterKey], [CabinetKey], [SettingsKey]; раздела касс в рабочем окне
 * нет, поэтому этот ключ — не [SectionKey].
 */
@Serializable
data object KkmsKey : NavKey
