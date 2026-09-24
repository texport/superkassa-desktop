package kz.mybrain.superkassa.presentation.setup.component

import androidx.compose.runtime.Composable
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import kz.mybrain.superkassa.designsystem.picker.LabelledPicker
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.domain.setup.model.OfdContours
import kz.mybrain.superkassa.presentation.words.common.of

/**
 * Выбор контура БФД: стенд, тестовый или промышленный.
 *
 * Названия контуров — со слов кассы, на трёх языках: свой список означал
 * бы, что нового контура владелец не увидит, пока не обновит программу.
 * Неподнятый контур гаснет по правилу [OfdContours.raised].
 */
@Composable
internal fun ContourPicker(contours: List<OfdEnvironmentResponse>, selected: String, onSelect: (String) -> Unit) {
    val language = LocalLanguage.current
    LabelledPicker(
        label = LocalStrings.current.settings.environment,
        options = contours,
        selected = contours.firstOrNull { it.code == selected },
        title = { contour -> contour?.name?.of(language) ?: selected },
        onSelect = { onSelect(it.code) },
        available = { OfdContours.raised(it.code) },
        width = Sizes.fieldChoice
    )
}
