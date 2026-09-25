package kz.mybrain.superkassa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Устройство модуля экранов по его исходникам — одно на все модули экранов.
 *
 * Модуль области видит только общее экранов, дизайн-систему, тексты
 * и домен — это держит граф модулей. Граф не заметит другого: файла чужой
 * области, положенного сюда, чтения домена чужой области без объявления
 * в [AreaRules], открытого объявления без описания, модели экрана, которая
 * тянется к портам мимо сценариев, размера, записанного числом или своим
 * токеном вместо раскладки ([DesignRules]), и пакета, в котором сценарии
 * свалены в одну кучу ([SourceTree.crowded]). Проверки читают исходники,
 * поэтому живут на JVM; модуль наследует их своим набором проверок.
 *
 * Превью модуля проверяются здесь же: каждое обязано рисоваться
 * ([PreviewSweep]) и стоять под общей аннотацией дизайн-системы.
 *
 * Поэлементные размеры, с которыми код пришёл к проверке, перечислены
 * в `size-debt.txt` модуля: долг только сокращается.
 *
 * @param own пакеты модуля от корня приложения: `presentation.kassa`.
 * @param allowed чужой код приложения, который модулю можно брать, — пакеты
 *   от корня приложения; свои пакеты разрешены всегда.
 */
abstract class ScreenModuleRules(private val own: List<String>, private val allowed: List<String>) {

    @Test
    fun onlyOwnPackagesLiveHere() {
        val strangers = SourceTree.main().filterNot { source -> own.any { inside(source.pkg, it) } }.map { it.path }
        assertEquals(emptyList(), strangers, "files of another module's packages")
    }

    @Test
    fun importsOnlyWhatTheModuleMayUse() {
        val foreign = SourceTree.main().flatMap { source ->
            source.imports.filterNot { import -> (own + allowed).any { inside(import, it) } }
                .map { "${source.path} -> $it" }
        }
        assertEquals(emptyList(), foreign, "imports the module may not use")
    }

    @Test
    fun readsOtherAreasOfDomainOnlyWhereDeclared() {
        val found = AreaRules.violations(SourceTree.main())
        assertEquals(emptySet(), found, "reads of another area not declared in AreaRules")
    }

    @Test
    fun publicDeclarationsAreDocumented() {
        assertEquals(emptyList(), SourceTree.undocumented(), "public declarations without KDoc")
    }

    @Test
    fun packagesHoldAtMostFifteenFiles() {
        assertEquals(emptyList(), SourceTree.crowded(), "packages to split by scenario")
    }

    @Test
    fun screenModelsSeeOnlyUseCases() {
        assertEquals(emptySet(), AreaRules.modelViolations(SourceTree.main()), "view models reaching past use cases")
    }

    @Test
    fun sizesComeOnlyFromTokens() {
        val found = DesignRules.code().flatMap { (path, lines) -> DesignRules.literals(path, lines) }
        assertTrue(found.isEmpty(), "sizes written as numbers:\n" + found.joinToString("\n"))
    }

    @Test
    fun noNewElementSizes() {
        val fresh = elementSizes() - SourceTree.debtOrEmpty(SIZE_DEBT)
        assertTrue(fresh.isEmpty(), "element sizes outside the allow list:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun sizeDebtListsOnlyWhatIsLeft() {
        assertEquals(emptySet(), SourceTree.debtOrEmpty(SIZE_DEBT) - elementSizes(), "sizes removed, strike them out")
    }

    @Test
    fun everyPreviewDraws() {
        assertEquals(emptyList(), PreviewSweep.failures(), "previews that do not draw")
    }

    @Test
    fun previewsUseSharedAnnotations() {
        assertEquals(emptyList(), PreviewSweep.strayPreviews(), "@Preview outside the shared preview annotations")
    }

    private fun elementSizes(): Set<String> =
        DesignRules.code().flatMap { (path, lines) -> DesignRules.elementSizes(path, lines) }.toSet()

    /** Пакет [name] — это [prefix] или лежит внутри него; оба от корня приложения. */
    private fun inside(name: String?, prefix: String): Boolean {
        val full = "${SourceTree.ROOT}.$prefix"
        return name == full || name?.startsWith("$full.") == true
    }

    private companion object {
        const val SIZE_DEBT = "size-debt.txt"
    }
}
