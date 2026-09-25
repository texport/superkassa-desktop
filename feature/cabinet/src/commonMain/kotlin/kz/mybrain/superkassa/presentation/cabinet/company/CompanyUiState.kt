package kz.mybrain.superkassa.presentation.cabinet.company

import kz.mybrain.superkassa.domain.cabinet.model.CompanyProfile
import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.domain.cabinet.model.OkedEntry
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Компания владельца и её виды деятельности.
 *
 * @property profile карточка компании, как её отдал кабинет; `null` — ещё не отдал.
 * @property okeds виды деятельности, как их правит владелец: сохраняются кнопкой.
 * @property refused почему карточку не прочли, словами владельца. Отказ
 *   держится здесь, а не только всплывающей строкой: та гаснет за секунды,
 *   а раздел без карточки обязан называть причину, пока её нет.
 * @property titles наименования видов по классификатору: компания хранит
 *   одно — на каком языке вид завели, — а показывать надо на языке владельца.
 * @property search поиск по классификатору для нового вида.
 */
internal data class CompanyUiState(
    val profile: CompanyProfile? = null,
    val okeds: List<Oked> = emptyList(),
    val refused: String? = null,
    val titles: Map<String, OkedEntry> = emptyMap(),
    val search: OkedSearch = OkedSearch()
) {
    /** Сохранять есть что: список не пуст и отличается от того, что отдал кабинет. */
    val changed: Boolean get() = okeds.isNotEmpty() && okeds != profile?.okeds.orEmpty()
}

/**
 * Поиск по классификатору ОКЭД.
 *
 * @property taken сколько записей классификатор отдал с начала: смещение
 *   считается по полученным, а не по показанным — уже добавленные виды
 *   из показа убраны, и счёт по показанным пропускал бы часть классификатора.
 * @property total сколько записей у классификатора; ноль — кабинет не сказал.
 * @property ended продолжения нет: страница неполная или ничего нового не пришло.
 * @property searched кабинет уже ответил на этот запрос.
 */
internal data class OkedSearch(
    val query: String = "",
    val found: List<OkedEntry> = emptyList(),
    val taken: Int = 0,
    val total: Long = 0,
    val ended: Boolean = false,
    val searched: Boolean = false
) {
    val needle: String get() = query.trim()

    /**
     * Подсказка под полем поиска.
     *
     * «Классификатор длиннее показанного — уточните запрос» уместно только
     * при запросе. Пустая строка отдаёт начало классификатора, и эта
     * подсказка выходила сразу при раскрытии карточки, до единого набранного
     * знака: уточнять было нечего. До запроса стоит обычная подсказка о том,
     * чем искать.
     */
    fun hint(texts: CabinetTexts): String = when {
        !searched || needle.isEmpty() -> texts.hints.okedSearch
        found.isEmpty() -> texts.okedNotFound
        !ended -> texts.okedNarrowSearch
        else -> texts.hints.okedSearch
    }

    /** Сколько классификатор держит сверх полученного; `null` — продолжения нет. */
    val rest: Long? get() = (total - taken).coerceAtLeast(0).takeIf { !ended && (it > 0 || total == 0L) }
}
