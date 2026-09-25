package kz.mybrain.superkassa.navigation.step

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Шаг внутри раздела: подробности поверх списка или следующий шаг мастера.
 *
 * Так Material 3 велит вести «список и подробности» (List-detail →
 * back navigation): на широком окне подробности стоят рядом со списком,
 * выбор в списке шагом истории не становится, и «назад» уводит из раздела;
 * на узком они открываются поверх списка, и «назад» — жест, Escape,
 * стрелка в шапке окна — возвращает к списку по общей истории окна.
 * Раздвинули окно, пока шаг открыт, — шаг снимается сам.
 *
 * Шаги мастера ложатся той же историей: «назад» ведёт на предыдущий шаг.
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

/**
 * Шаг мастера подключения кассы поверх его первого шага.
 *
 * Каждый шаг — запись истории окна: жест «назад», Escape и стрелка
 * в шапке ведут по шагам мастера так же, как кнопка «Назад» под шагом.
 *
 * @property step имя шага: сами шаги знает мастер.
 */
@Serializable
data class SetupStepKey(val step: String) : StepKey
