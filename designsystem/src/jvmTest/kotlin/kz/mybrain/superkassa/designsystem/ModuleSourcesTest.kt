package kz.mybrain.superkassa.designsystem

import kz.mybrain.superkassa.PreviewSweep
import kz.mybrain.superkassa.SourceTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Устройство модуля по его исходникам: только вид, открытое описано.
 *
 * Что дизайн-системе не видны домен, данные и ядро, держит граф модулей.
 * Он не заметит файла экрана, положенного сюда по ошибке, импорта области
 * приложения из исходника, который однажды получит зависимость на неё,
 * открытого объявления без описания: токены и компоненты берут экраны
 * всех областей, и читают их снаружи, не открывая модуля, — и пакета,
 * куда свалены разные виды компонентов.
 */
class ModuleSourcesTest {

    @Test
    fun `в модуле только пакеты дизайн-системы`() {
        val strangers = SourceTree.main().filter { SourceTree.layerOf(it.pkg) != LAYER }.map { it.path }
        assertEquals(emptyList(), strangers, "файлы чужого слоя в дизайн-системе")
    }

    @Test
    fun `дизайн-система не знает ни домена, ни областей приложения`() {
        val foreign = SourceTree.main().flatMap { source ->
            source.imports.filterNot { import -> ALLOWED.any { import.startsWith(it) } }.map { "${source.path} -> $it" }
        }
        assertEquals(emptyList(), foreign, "дизайн-система берёт код приложения")
    }

    @Test
    fun `в пакете не больше пятнадцати файлов`() {
        assertEquals(emptyList(), SourceTree.crowded(), "пакеты, которые пора разложить по видам")
    }

    @Test
    fun `открытые объявления описаны`() {
        assertEquals(emptyList(), SourceTree.undocumented(), "открытые объявления без описания")
    }

    /** Превью общих элементов рисуются так же, как в Android Studio, — все до одного. */
    @Test
    fun `каждое превью рисуется`() {
        val found = PreviewSweep.found()
        assertTrue(found.isNotEmpty(), "проверка не нашла ни одного превью: аннотации не видны во время исполнения")
        assertEquals(emptyList(), PreviewSweep.failures(found), "превью, которые не рисуются")
    }

    /** `@Preview` живёт только в общих аннотациях: мимо них превью не нашла бы проверка. */
    @Test
    fun `превью идут через общие аннотации`() {
        assertEquals(emptyList(), PreviewSweep.strayPreviews(), "@Preview мимо общих аннотаций")
    }

    private companion object {
        const val LAYER = "designsystem"

        /** Что дизайн-системе можно брать из кода приложения: себя и тексты. */
        val ALLOWED = listOf("${SourceTree.ROOT}.$LAYER.", "${SourceTree.ROOT}.strings.api.")
    }
}
