package kz.mybrain.superkassa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Границы областей экранов: область не видит внутренностей другой области.
 *
 * Правило и общие полки — [AreaRules]; здесь оно проверяет исходники
 * этого модуля. Области домена проверяет модуль `domain` у себя.
 *
 * Нарушения, с которыми код пришёл к этой проверке, перечислены
 * в `area-debt.txt`: долг снимается при переводе своей области. Новое
 * нарушение падает сразу, а исправленное требует вычеркнуть строку.
 *
 * Тем же порядком проверяется шаблон модели экрана: файл `*ViewModel.kt`
 * не видит ни контейнера окна, ни держателя входа, ни портов — только
 * сценарии своей области ([MODEL_FORBIDDEN]). Модели, ещё не переведённые
 * на шаблон, перечислены в `model-debt.txt`.
 */
class AreaBoundariesTest {

    @Test
    fun `no area imports the inside of another area`() {
        val fresh = violations() - SourceTree.debt(AREA_DEBT)
        assertTrue(fresh.isEmpty(), "new area violations:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun `debt lists only violations that still exist`() {
        val paid = SourceTree.debt(AREA_DEBT) - violations()
        assertEquals(emptySet(), paid, "violations fixed, remove them from $AREA_DEBT")
    }

    @Test
    fun `view models see only use cases`() {
        val fresh = modelViolations() - SourceTree.debt(MODEL_DEBT)
        assertTrue(fresh.isEmpty(), "view models reaching past use cases:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun `model debt lists only models still off the pattern`() {
        val paid = SourceTree.debt(MODEL_DEBT) - modelViolations()
        assertEquals(emptySet(), paid, "models moved to the pattern, remove them from $MODEL_DEBT")
    }

    private fun violations(): Set<String> = AreaRules.violations(SourceTree.main())

    /** Модель экрана, которая берёт больше своих сценариев: `путь -> импорт`. */
    private fun modelViolations(): Set<String> = SourceTree.main()
        .filter { it.path.startsWith("presentation/") && it.path.endsWith("ViewModel.kt") }
        .flatMap { source ->
            source.imports.filter { import -> MODEL_FORBIDDEN.any { it.matches(import) } }
                .map { "${source.path} -> ${it.removePrefix("${SourceTree.ROOT}.")}" }
        }.toSet()

    private companion object {
        const val ROOT = SourceTree.ROOT
        const val AREA_DEBT = "area-debt.txt"
        const val MODEL_DEBT = "model-debt.txt"

        /** Что модели экрана брать нельзя: весь контейнер окна, держатель входа, порты. */
        val MODEL_FORBIDDEN = listOf(
            Regex("""\Q$ROOT.presentation.shell.AppContainer\E"""),
            Regex("""\Q$ROOT.domain.signin.model.SignIn\E"""),
            Regex("""\Q$ROOT.domain.\E\w+\.port\..+""")
        )
    }
}
