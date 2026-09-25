package kz.mybrain.superkassa.navigation.settings

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Раздел настроек, открытый поверх их списка — на узком окне.
 *
 * На широком окне раздел стоит рядом со списком и шагом истории не
 * становится: «назад» оттуда уводит из настроек. На узком он открывается
 * поверх списка, и «назад» — жест, Escape, стрелка в шапке — возвращает
 * к списку по той же истории, что и между разделами окна.
 *
 * @property section имя раздела настроек: сами разделы знает их область.
 */
@Serializable
data class SettingsSectionKey(val section: String) : NavKey
