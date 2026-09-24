package kz.mybrain.superkassa.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.presentation.cabinet.addressIn
import kz.mybrain.superkassa.presentation.common.mapview.HOUSE_ZOOM
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.map.MapAddressTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Адрес точки: выбирается в государственном адресном регистре, карта идёт за ним.
 *
 * Свободного поиска по набранному тексту здесь больше нет намеренно.
 * Прежде адрес точки приходил из регистра, а координаты — из постороннего
 * поиска по тому, что владелец набрал в окне карты, и сойтись они были
 * не обязаны: дом на Достык в одном районе, метка в другом. Теперь адрес
 * один — выбранный в регистре, — а карта ищет именно его.
 *
 * Координат регистр не отдаёт вовсе (`GET /api/addresses/{rka}` — это код
 * РКА, САТО и текст адреса), поэтому координаты по-прежнему даёт карта.
 * Обратный ход — адрес по метке — идёт другим путём (см. [PointAddress]):
 * место у службы карт, а запись всё равно из регистра.
 *
 * @param address выбранный адрес; его смена ведёт карту к новому дому.
 * @param pick что выбрано в этом окне и к какому адресу метка уже относится.
 * @param onAddress выбранный в этом окне адрес уходит наружу: у формы точки
 *   и у карты адрес один и тот же.
 */
@Composable
internal fun RegistryAddress(
    parts: MapPickerParts,
    address: RegisterAddress?,
    onAddress: (RegisterAddress) -> Unit
) {
    val language = LocalLanguage.current
    val texts = remember(language) { textsOf(language).cabinet }
    val notices = remember(language) { textsOf(language).map.address }
    val pick = parts.pick
    val shown = addressIn(language, address?.address, address?.addressKz)
    val lookup = rememberLookup(parts, shown, address?.addressRef)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        parts.registry.Search(
            query = pick.query,
            onQuery = { pick.query = it },
            owner = address?.addressRef
        ) { chosen ->
            onAddress(chosen)
            pick.chosen(chosen, language)
        }
        LookupNotice(lookup, texts, notices, hasAddress = shown.isNotBlank())
    }
}

/**
 * Карта идёт к выбранному в регистре дому — если метка ещё не поставлена
 * рукой владельца к другому месту; ход поиска — строкой под шагами.
 */
@Composable
private fun rememberLookup(parts: MapPickerParts, shown: String, addressRef: String?): Lookup {
    var lookup by remember { mutableStateOf(Lookup.Quiet) }
    LaunchedEffect(addressRef) {
        if (!mapFollowsAddress(shown, addressRef, parts.pick.settled, parts.state.marked)) return@LaunchedEffect
        lookup = Lookup.Searching
        val place = parts.cases.findAddress(shown)?.firstOrNull()
        place?.let { parts.state.show(it.latitude, it.longitude, HOUSE_ZOOM) }
        lookup = if (place == null) Lookup.Missing else Lookup.Quiet
    }
    return lookup
}

/**
 * Что выбрано в окне карты и к какому адресу метка уже относится.
 *
 * Состояние общее у двух путей подбора — шагов регистра над картой
 * и подбора по метке под ней, — поэтому живёт отдельно от обоих:
 * выбранный любым путём адрес показывается одной строкой, и согласие
 * адреса с меткой считается по одному и тому же правилу.
 *
 * @param address адрес, с которым окно открылось.
 */
internal class MapAddressPick(address: RegisterAddress?, language: Language) {

    /** Подпись выбранного адреса: пока она пуста, шаги регистра раскрыты. */
    var query: String by mutableStateOf(addressIn(language, address?.address, address?.addressKz))

    /**
     * Адрес, к которому метка уже относится.
     *
     * К такому адресу карта сама не идёт: координаты владелец для него
     * и выбирал — открыв окно у готовой точки или подобрав адрес по метке.
     */
    var settled: String? by mutableStateOf(address?.addressRef)
        private set

    /** Адрес выбран шагами регистра: карта пойдёт к его дому. */
    fun chosen(address: RegisterAddress, language: Language) {
        query = addressIn(language, address.address, address.addressKz)
    }

    /** Адрес подобран по метке: метка и есть место, и вести карту некуда. */
    fun byPoint(address: RegisterAddress, language: Language) {
        chosen(address, language)
        settled = address.addressRef
    }
}

/**
 * Идти ли карте к адресу регистра.
 *
 * Смена адреса ведёт карту всегда: адрес точки и дом под меткой должны
 * совпадать. А вот у адреса, к которому метка уже относится, трогать её
 * незачем: владелец её сам поставил — поправив дом двора или въезда —
 * либо по ней этот адрес и подобран.
 *
 * @param addressText текст адреса; пусто — адрес не выбран, искать нечего.
 * @param settled адрес, к которому метка уже относится: тот, с которым
 *   окно открылось, или подобранный по самой метке.
 */
internal fun mapFollowsAddress(addressText: String, addressRef: String?, settled: String?, marked: Boolean): Boolean =
    addressText.isNotBlank() && (!marked || addressRef != settled)

/**
 * Одна строка над картой о том, что сейчас делать.
 *
 * Строка одна на все состояния нарочно: подсказка про адрес, ожидание
 * поиска, отказ поиска и подсказка про саму карту стояли бы друг под
 * другом и отжимали карту вниз. Молчать нельзя ни в одном состоянии —
 * не найденный по адресу дом владелец иначе принял бы за пустую карту.
 */
@Composable
private fun LookupNotice(lookup: Lookup, texts: CabinetTexts, notices: MapAddressTexts, hasAddress: Boolean) {
    val (notice, tint) = when {
        !hasAddress -> notices.pickAddressFirst to MaterialTheme.colorScheme.onSurfaceVariant
        lookup == Lookup.Searching -> notices.searching to MaterialTheme.colorScheme.onSurfaceVariant
        lookup == Lookup.Missing -> notices.notOnMap to MaterialTheme.colorScheme.error
        else -> texts.map.pickOnMapHint to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = notice, style = MaterialTheme.typography.bodySmall, color = tint)
}

/** Чем кончился поиск дома по адресу регистра. */
private enum class Lookup { Quiet, Searching, Missing }
