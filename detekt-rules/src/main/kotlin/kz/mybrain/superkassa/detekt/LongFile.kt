package kz.mybrain.superkassa.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtFile

/**
 * Файл не длиннее порога.
 *
 * `LargeClass` меряет класс, а файл экрана — это десяток функций верхнего
 * уровня без класса вокруг: такой файл разрастался без предела, и ни одно
 * правило detekt его не замечало. Считаются все строки файла, как их видит
 * читающий, вместе с комментариями.
 */
class LongFile(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "LongFile",
        severity = Severity.Maintainability,
        description = "File is too long and should be split by responsibility.",
        debt = Debt.TWENTY_MINS
    )

    private val threshold: Int by config(DEFAULT_THRESHOLD)

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        val lines = file.text.lines().size
        if (lines >= threshold) {
            val message = "File has $lines lines, the limit is below $threshold."
            report(CodeSmell(issue, Entity.from(file), message))
        }
    }

    private companion object {
        const val DEFAULT_THRESHOLD = 200
    }
}
