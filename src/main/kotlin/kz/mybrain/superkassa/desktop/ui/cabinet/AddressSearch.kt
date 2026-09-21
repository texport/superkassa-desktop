package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterAddress
import kz.mybrain.superkassa.desktop.server.cabinet.addressNestedLocalities
import kz.mybrain.superkassa.desktop.server.cabinet.resolveAddress
import kz.mybrain.superkassa.desktop.ui.components.SubsectionTitle
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Выбор адреса в государственном регистре по шагам.
 *
 * Свободного поиска по адресу целиком регистр не даёт: адрес собирается
 * из региона, населённых пунктов, улицы и дома. Пунктов может быть
 * несколько подряд — у Караганды под городом лежат районы, у Астаны
 * районы лежат прямо под регионом, — поэтому после каждого пункта
 * приложение спрашивает регистр, есть ли вложенные, и только затем
 * переходит к улице. Выбор дома завершает подбор: регистр подтверждает
 * адрес по коду РКА, и он уходит наружу целиком.
 *
 * Поиск идёт за набором, а не по кнопке: регистр отвечает быстро,
 * а кнопка у каждого шага читалась как ещё одно действие. Найденное
 * раскрывается списком под полем, как у остальных выпадающих полей;
 * пока ничего не набрано, в нём первые записи шага — с пустого поля
 * подбор начинать не с чего.
 *
 * @param owner чей адрес подбирается; смена владельца (другая точка) сбрасывает
 *   начатый путь — иначе выбранные для одной точки шаги показывались у другой.
 * @param title название подраздела; у переезда своё — рядом с ним уже стоит
 *   строка с нынешним адресом точки, и два «Адреса» читались как один.
 * @param query подпись выбранного адреса; хранится снаружи, потому что
 *   после выбора поле заполняется адресом.
 * @param onChoose выбранный адрес; шаги после этого сворачиваются.
 */
@Composable
fun AddressSearch(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    query: String,
    onQuery: (String) -> Unit,
    owner: Any? = null,
    title: String = texts.placeAddress,
    onChoose: (RegisterAddress) -> Unit
) {
    val path = remember(owner) { AddressPath() }
    // У пункта могут быть вложенные пункты: регистр спрашивается об этом
    // сразу после выбора, и до ответа следующий шаг не показывается.
    val last = path.chosen.lastOrNull()
    LaunchedEffect(last) {
        if (last?.level != LEVEL_LOCALITY) return@LaunchedEffect
        val token = cabinet.token ?: return@LaunchedEffect
        val nested = cabinet.guard { cabinet.client.addressNestedLocalities(token, last.id).items } ?: emptyList()
        path.nestedResolved(nested.isNotEmpty())
    }
    val building = path.building
    LaunchedEffect(building) {
        val rka = building?.rka ?: return@LaunchedEffect
        val token = cabinet.token ?: return@LaunchedEffect
        val address = cabinet.guard { cabinet.client.resolveAddress(token, rka) } ?: return@LaunchedEffect
        onQuery(addressIn(session.language, address.address, address.addressKz))
        onChoose(address)
        path.reset()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        if (query.isNotBlank()) {
            // Адрес подобран: шаги спрятаны, иначе список регионов раскрывался бы
            // заново поверх готового адреса. Сменить его — отдельным действием.
            Text(text = query, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onQuery("") }) { Text(texts.addressPickAgain) }
            return@Column
        }
        SubsectionTitle(title, texts.hints.addressStep)
        path.chosen.forEachIndexed { at, level ->
            ChosenLevel(label = path.labelAt(at, texts), name = level.name) { path.dropFrom(at) }
        }
        if (path.building == null && path.stepKnown) {
            AddressStep(cabinet, texts, path)
        }
    }
}

/** Выбранный уровень показывается полем с названием; правка снимает его и всё, что ниже. */
@Composable
private fun ChosenLevel(label: String, name: String, onEdit: () -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = { onEdit() },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}
