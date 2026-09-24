package kz.mybrain.superkassa.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/** Набор своих правил: в `detekt.yml` он называется `superkassa`. */
class SuperkassaRules : RuleSetProvider {

    override val ruleSetId: String = "superkassa"

    override fun instance(config: Config): RuleSet = RuleSet(ruleSetId, listOf(LongFile(config), NoSuppress(config)))
}
