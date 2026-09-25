package kz.mybrain.superkassa

/**
 * Границы областей: область не видит внутренностей другой области.
 *
 * Область — второй уровень пакета в `presentation` и `domain`:
 * `presentation/kassa` и `domain/kassa` со всеми подпакетами — одна
 * область `kassa`.
 * Из чужой области брать нельзя ничего; общее лежит на общих полках,
 * которые видны всем:
 *
 * - `presentation/common`, `presentation/words`
 *   (перевод типов домена и ядра в тексты модуля `strings`)
 *   и `presentation/shell` (каркас окна: он собирает области и видит их все);
 * - модули текстов `strings` и дизайн-системы `designsystem` — не области
 *   приложения, их видят все;
 * - общие домены [SHARED_DOMAINS]: касса и её ответ, вход, журнал
 *   приложения, правила кассы и фискального документа, смена, версия
 *   и рабочее место — то, о чём спрашивает каждая область.
 *
 * Экраны области читают модель чужой области домена только там, где
 * они о ней и есть ([READS]): аналитика и карта — об учёте и адресах
 * кассы в кабинете, кабинет и мастер — о пине кассира и о кассе,
 * заводимой через кабинет. Такое чтение объявлено здесь, а не зашито
 * долгом: новое чтение чужого домена требует записи с причиной.
 *
 * Правило одно на всё приложение, а проверяет его каждый модуль у себя:
 * `domain` — свои исходники, модуль экранов — свои ([ScreenModuleRules]).
 */
object AreaRules {
    private val SHARED_PRESENTATION = setOf("common", "words", "shell")
    private val SHARED_DOMAINS = setOf("kassa", "signin", "log", "kkm", "document", "shift", "version", "workplace")

    /**
     * Чужие области домена, которые читают экраны области.
     *
     * - аналитика — учёт касс кабинета (`KkmRecord`): её взгляды и есть учёт;
     * - карта — адресный регистр кабинета: место точки выбирается по нему;
     * - кабинет — правила пина кассира (касса, заводимая здесь, получает
     *   администратора) и память мастера (контур ОФД);
     * - мастер — правила пина и касса, заведённая в кабинете.
     */
    private val READS = mapOf(
        "analytics" to setOf("cabinet"),
        "map" to setOf("cabinet"),
        "cabinet" to setOf("users", "setup"),
        "setup" to setOf("users", "cabinet")
    )

    /** Импорты чужих областей в [sources]: `путь -> слой.область`. */
    fun violations(sources: List<Source>): Set<String> = sources.flatMap { source ->
        val own = areaOf(source.pkg) ?: return@flatMap emptyList()
        source.imports
            .mapNotNull { areaOf(it) }
            .filter { it.name != own.name }
            .filterNot { own.layer == "presentation" && it.layer == "domain" && it.name in READS[own.name].orEmpty() }
            .map { "${source.path} -> ${it.layer}.${it.name}" }
    }.toSet()

    /**
     * Модели экранов, которые берут больше своих сценариев: `путь -> импорт`.
     *
     * Файл `*ViewModel.kt` не видит ни контейнера окна, ни держателя входа,
     * ни портов — только сценарии своей области и разговор с кассиром: модель
     * без окна и без кассы проверяется подделкой сценария.
     */
    fun modelViolations(sources: List<Source>): Set<String> = sources
        .filter { it.path.startsWith("presentation/") && it.path.endsWith("ViewModel.kt") }
        .flatMap { source ->
            source.imports.filter { import -> MODEL_FORBIDDEN.any { it.matches(import) } }
                .map { "${source.path} -> ${it.removePrefix("${SourceTree.ROOT}.")}" }
        }.toSet()

    /** Что модели экрана брать нельзя: весь контейнер окна, держатель входа, порты. */
    private val MODEL_FORBIDDEN = listOf(
        Regex("""\Q${SourceTree.ROOT}.presentation.shell.AppContainer\E"""),
        Regex("""\Q${SourceTree.ROOT}.domain.signin.model.SignIn\E"""),
        Regex("""\Q${SourceTree.ROOT}.domain.\E\w+\.port\..+""")
    )

    /** Область пакета или импорта; `null` — общая полка или не область вовсе. */
    private fun areaOf(name: String?): Area? {
        val parts = name?.takeIf { it.startsWith("${SourceTree.ROOT}.") }
            ?.removePrefix("${SourceTree.ROOT}.")?.split('.')
        if (parts == null || parts.size < 2) return null
        val area = Area(parts[0], parts[1])
        val shared = when (area.layer) {
            "presentation" -> area.name in SHARED_PRESENTATION
            "domain" -> area.name in SHARED_DOMAINS
            else -> true
        }
        return area.takeUnless { shared }
    }

    private data class Area(val layer: String, val name: String)
}
