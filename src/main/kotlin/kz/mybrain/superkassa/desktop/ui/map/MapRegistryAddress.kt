package kz.mybrain.superkassa.desktop.ui.map

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
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterAddress
import kz.mybrain.superkassa.desktop.ui.cabinet.AddressSearch
import kz.mybrain.superkassa.desktop.ui.cabinet.addressIn
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.MapAddressTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    state: MapState,
    geocoder: MapGeocoder,
    address: RegisterAddress?,
    pick: MapAddressPick,
    notices: MapAddressTexts,
    onAddress: (RegisterAddress) -> Unit
) {
    val shown = addressIn(session.language, address?.address, address?.addressKz)
    var lookup by remember { mutableStateOf(Lookup.Quiet) }

    LaunchedEffect(address?.addressRef) {
        if (!mapFollowsAddress(shown, address?.addressRef, pick.settled, state.marked)) return@LaunchedEffect
        lookup = Lookup.Searching
        val place = geocoder.find(shown).firstOrNull()
        place?.let { state.show(it.latitude, it.longitude, HOUSE_ZOOM) }
        lookup = if (place == null) Lookup.Missing else Lookup.Quiet
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        AddressSearch(
            session = session,
            cabinet = cabinet,
            texts = texts,
            query = pick.query,
            onQuery = { pick.query = it },
            owner = address?.addressRef
        ) { chosen ->
            onAddress(chosen)
            pick.chosen(chosen, session.language)
        }
        LookupNotice(lookup, texts, notices, hasAddress = shown.isNotBlank())
    }
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
