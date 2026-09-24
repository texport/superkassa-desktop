package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.cabinet.model.AddressLevel
import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress

/**
 * Адресный регистр глазами окна карты.
 *
 * Регистр держит кабинет, а окно карты о кабинете не знает: регистр ему
 * отдаёт форма точки, которая окно открыла. Отказ регистра здесь
 * не отличается от пустого ответа — на отказ владельцу отвечает общая
 * строка сообщений, а подбору хватает того, что записи не нашлось.
 */
interface MapRegistry {

    /** Вошёл ли владелец в кабинет: без входа регистр не отвечает. */
    val open: Boolean

    /** Записи уровня [level] под родителем [parentId], подходящие под [query]. */
    suspend fun lookUp(level: AddressLevel, parentId: Long, query: String): List<AddressSuggestion>

    /** Адрес регистра по коду РКА; `null` — регистр его не выдал. */
    suspend fun resolve(rka: String): RegisterAddress?

    /**
     * Подбор адреса по шагам регистра — та же форма, что у точки.
     *
     * @param query подпись выбранного адреса; после выбора поле заполняется им.
     * @param owner чей адрес подбирается: смена владельца сбрасывает начатый путь.
     */
    @Composable
    fun Search(query: String, onQuery: (String) -> Unit, owner: Any?, onChoose: (RegisterAddress) -> Unit)
}
