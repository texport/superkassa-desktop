package kz.mybrain.superkassa.presentation.settings.look

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.Typeface
import kz.mybrain.superkassa.designsystem.theme.color.Accent
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.domain.workplace.model.LookChoice
import kz.mybrain.superkassa.presentation.common.model.follow
import kz.mybrain.superkassa.strings.api.Language

/**
 * Вид окна: язык, тема, тон, шрифт, рельс разделов и колонка точек.
 *
 * Одна на окно: её читают тема окна, шапка, настройки и кабинет, и выбор,
 * сделанный в одном месте, сразу виден во всех. Первое состояние — уже
 * сохранённый выбор: окно не мигает умолчанием до первого чтения.
 */
class LookViewModel(private val cases: LookCases) : ViewModel() {
    private val screen = MutableStateFlow(LookUiState.of(cases.observe().value))

    val state: StateFlow<LookUiState> = screen.asStateFlow()

    init {
        follow(cases.observe()) { chosen -> screen.value = LookUiState.of(chosen) }
    }

    fun switchLanguage(chosen: Language) = change { it.copy(language = chosen.code) }

    fun switchAppearance(chosen: Appearance) = change { it.copy(appearance = chosen.code) }

    fun chooseAccent(chosen: Accent) = change { it.copy(accent = chosen.code) }

    fun chooseTypeface(chosen: Typeface) = change { it.copy(typeface = chosen.code) }

    fun chooseTextScale(chosen: TextScale) = change { it.copy(textScale = chosen.code) }

    fun toggleRail() = change { it.copy(railCollapsed = !it.railCollapsed) }

    fun togglePlaces() = change { it.copy(placesCollapsed = !it.placesCollapsed) }

    /** Новый выбор виден сразу, не дожидаясь, пока поток его отдаст. */
    private fun change(edit: (LookChoice) -> LookChoice) {
        val chosen = edit(cases.observe().value)
        cases.choose(chosen)
        screen.value = LookUiState.of(chosen)
    }
}
