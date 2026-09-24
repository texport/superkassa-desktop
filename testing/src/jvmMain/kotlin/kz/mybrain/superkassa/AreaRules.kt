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
 * - `presentation/common`, `presentation/theme`, `presentation/words`
 *   (перевод типов домена и ядра в тексты модуля `strings`)
 *   и `presentation/shell` (каркас окна: он собирает области и видит их все);
 * - модуль текстов `strings` — не область приложения, его видят все;
 * - общие домены [SHARED_DOMAINS]: касса и её ответ, вход, журнал
 *   приложения, правила кассы и фискального документа, смена, версия
 *   и рабочее место — то, о чём спрашивает каждая область.
 *
 * Правило одно на всё приложение, а проверяет его каждый модуль у себя:
 * `domain` — свои исходники, `shared` — экраны.
 */
object AreaRules {
    private val SHARED_PRESENTATION = setOf("common", "theme", "words", "shell")
    private val SHARED_DOMAINS = setOf("kassa", "signin", "log", "kkm", "document", "shift", "version", "workplace")

    /** Импорты чужих областей в [sources]: `путь -> слой.область`. */
    fun violations(sources: List<Source>): Set<String> = sources.flatMap { source ->
        val own = areaOf(source.pkg) ?: return@flatMap emptyList()
        source.imports
            .mapNotNull { areaOf(it) }
            .filter { it.name != own.name }
            .map { "${source.path} -> ${it.layer}.${it.name}" }
    }.toSet()

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
