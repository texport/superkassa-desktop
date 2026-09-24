package kz.mybrain.superkassa.presentation.cabinet.address

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.section.SubsectionTitle
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.addressIn
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.words.debug.name
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    query: String,
    onQuery: (String) -> Unit,
    owner: Any? = null,
    title: String = texts.placeAddress,
    onChoose: (RegisterAddress) -> Unit
) {
    val model = addressSearchViewModel(cabinet.cabinet, owner?.toString() ?: NEW_PLACE)
    val path by model.state.collectAsScreenState()
    // Подбор начинается заново для каждой точки и каждого открытия формы:
    // выбранные для одной точки шаги не должны показываться у другой.
    LaunchedEffect(owner) { model.reset() }
    val chosen = chosenAddress(LocalLanguage.current, onQuery, onChoose)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
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
            ChosenLevel(label = path.labelAt(at, texts), name = level.name) { model.dropFrom(at) }
        }
        if (path.stepShown) {
            AddressStep(texts, path, onQuery = model::type) { model.choose(it, chosen) }
        }
    }
}

/** Выбранный адрес встаёт подписью в поле, а затем уходит владельцу формы. */
private fun chosenAddress(
    language: Language,
    onQuery: (String) -> Unit,
    onChoose: (RegisterAddress) -> Unit
): (RegisterAddress) -> Unit = { address ->
    onQuery(addressIn(language, address.address, address.addressKz))
    onChoose(address)
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

/** Чей адрес подбирается, когда точки ещё нет: форма новой точки. */
private const val NEW_PLACE = "new"
