package kz.mybrain.superkassa.navigation.step

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Шаг внутри раздела: подробности, открытые поверх списка на узком окне.
 *
 * Так Material 3 велит вести «список и подробности» (List-detail →
 * back navigation): на широком окне подробности стоят рядом со списком,
 * выбор в списке шагом истории не становится, и «назад» уводит из раздела;
 * на узком они открываются поверх списка, и «назад» — жест, Escape,
 * стрелка в шапке окна — возвращает к списку по общей истории окна.
 * Раздвинули окно, пока шаг открыт, — шаг снимается сам.
 */
@Serializable
sealed interface StepKey : NavKey

/**
 * Раздел настроек поверх их списка.
 *
 * @property section имя раздела настроек: сами разделы знает их область.
 */
@Serializable
data class SettingsSectionKey(val section: String) : StepKey

/**
 * Чек-основание возврата поверх списка чеков. Какой чек выбран, помнит
 * модель возврата: без неё — после выгрузки приложения — шаг снимается.
 */
@Serializable
data object ReturnBasisKey : StepKey

/**
 * Карточка точки или кассы кабинета поверх их списка. Выбранное помнит
 * рабочее место, как и прежде.
 */
@Serializable
data object PlaceCardKey : StepKey
