package kz.mybrain.superkassa.designsystem.theme

import kz.mybrain.superkassa.DesignRules
import kz.mybrain.superkassa.SourceTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Одна сетка на экран: ширину и высоту общего компонента задаёт раскладка.
 *
 * Поле, плашка или карточка берут ширину из колонки (`fillMaxWidth`) или
 * долю ряда (`weight`), а не свой токен: поэлементные ширины разводили
 * края полей одного экрана на десятки точек. Правило и белый список —
 * у [DesignRules]; экраны приложения проверяет модуль `shared` у себя.
 *
 * Места, с которыми код пришёл к проверке, перечислены в `size-debt.txt`
 * как `файл -> токен`: долг только сокращается. Новое место падает сразу,
 * убранное требует вычеркнуть строку.
 */
class ElementSizesTest {

    @Test
    fun `новых поэлементных размеров нет`() {
        val fresh = violations() - SourceTree.debt(DEBT)
        assertTrue(fresh.isEmpty(), "поэлементные размеры вне белого списка:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun `долг перечисляет только то, что ещё есть`() {
        assertEquals(emptySet(), SourceTree.debt(DEBT) - violations(), "размеры убраны — вычеркните их из $DEBT")
    }

    private fun violations(): Set<String> = DesignRules.code().filterKeys { !it.startsWith(DesignRules.THEME) }
        .flatMap { (path, lines) -> DesignRules.elementSizes(path, lines) }
        .toSet()

    private companion object {
        const val DEBT = "size-debt.txt"
    }
}
