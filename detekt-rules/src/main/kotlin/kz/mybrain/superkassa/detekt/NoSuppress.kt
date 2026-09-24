package kz.mybrain.superkassa.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * Ни `@Suppress`, ни `@SuppressWarnings`.
 *
 * Замечание detekt или компилятора исправляется в коде, а не заглушается:
 * заглушка прячет его от обоих, и правило перестаёт значить что-либо.
 * Находка не привязана к элементу кода, поэтому `@Suppress` на том же
 * объявлении её не снимает.
 */
class NoSuppress(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "NoSuppress",
        severity = Severity.Maintainability,
        description = "Findings are fixed in code, not suppressed.",
        debt = Debt.TWENTY_MINS
    )

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
        super.visitAnnotationEntry(annotationEntry)
        val name = annotationEntry.shortName?.asString() ?: return
        if (name in SUPPRESSING) {
            val entity = Entity.from(annotationEntry).copy(ktElement = null)
            report(CodeSmell(issue, entity, "@$name hides a finding instead of fixing it."))
        }
    }

    private companion object {
        val SUPPRESSING = setOf("Suppress", "SuppressWarnings")
    }
}
